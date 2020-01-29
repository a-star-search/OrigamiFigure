/*
 *    This file is part of "Origami".
 *
 *     Origami is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Origami is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Origami.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.whitebeluga.origami.figure

import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Plane.planeFromOrderedPoints
import com.moduleforge.libraries.geometry._3d.Point
import com.moduleforge.libraries.geometry._3d.Vector
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.Vertex
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.whitebeluga.origami.figure.component.TestVertex as V

class BundleTest {
   private lateinit var xyPlane_NormalTowardsZPositive: Plane
   private lateinit var xzPlane_NormalTowardsYNegative: Plane
   private lateinit var anyFaceInXYPlane: Face
   private lateinit var anyFaceInXZPlane: Face
   @Before
   fun setUp(){
      val antiClockwisePointsAsSeenFromZPositive = listOf( Point(0, 0, 0), Point(1, 0, 0), Point(1, 1, 0))
      xyPlane_NormalTowardsZPositive = planeFromOrderedPoints(antiClockwisePointsAsSeenFromZPositive)
      anyFaceInXYPlane = Face(V(2, -4, 0), V(-7, 3, 0), V(-1, -11, 0))
      val antiClockwisePointsAsSeenFromYNegative = listOf( Point(0, 0, 0), Point(1, 0, 0), Point(0, 0, 1))
      xzPlane_NormalTowardsYNegative = planeFromOrderedPoints(antiClockwisePointsAsSeenFromYNegative)
      anyFaceInXZPlane = Face(V(2, 0, 1), V(-7, 0, 0), V(-1, 0, -5))
   }
   @Test
   fun vectorBottomToTopDirection_ShouldGoInTheSameDirectionAsLayerNormal_CaseXYPlaneLookingAtZPos(){
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, anyFaceInXYPlane)
      val bottomToTop = bundle.upwards
      val normalTowardsZPositive = Vector(0, 0, 1) //plane "looking" direction
      assertTrue(bottomToTop.epsilonEquals(normalTowardsZPositive))
   }
   @Test
   fun vectorTopToBottomDirection_ShouldGoInTheOppositeDirectionAsLayerNormal_CaseXYPlaneLookingAtZPos(){
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, anyFaceInXYPlane)
      val topToBottom = bundle.downwards
      val normalTowardsZNeg = Vector(0, 0, -1) //opposite of plane "looking" direction
      assertTrue(topToBottom.epsilonEquals(normalTowardsZNeg))
   }
   @Test
   fun vectorBottomToTopDirection_ShouldGoInTheSameDirectionAsLayerNormal_CaseXZPlaneLookingAtYNeg(){
      val bundle = Bundle(xzPlane_NormalTowardsYNegative, anyFaceInXZPlane)
      val topToBottom = bundle.upwards
      val normalTowardsYNeg = Vector(0, -1, 0)
      assertTrue(topToBottom.epsilonEquals(normalTowardsYNeg))
   }
   /**
    * In this example we have a figure which is a waterbomb with its right flap folded over to the left.
    *
    */
   @Test
   fun unfoldFrontFaceWaterbombFlap_Calculated(){
      val halfFigureWidth =   1
      val figureHeight =      1
      val top =         V(0, figureHeight)
      val right =       V(halfFigureWidth, 0)
      val leftFront =   V(-halfFigureWidth, 0)

      val middleBottomAtFront =  V(0, 0)
      val middleBottomNo2 =      V(0, 0)
      val middleBottomNo3 =      V(0, 0)

      val leftFrontPart = Face(top, middleBottomAtFront, leftFront)
      val rightFrontPart = Face(top, right, middleBottomAtFront)
      val innerLeftTriangle1 = Face(top, middleBottomNo2, leftFront)
      val leftNo2 = Vertex(leftFront)
      val innerLeftTriangle2 = Face(top, middleBottomNo2, leftNo2)
      val innerLeftTriangle3 = Face(top, middleBottomNo3, leftNo2)
      val leftBack = Vertex(leftFront)
      val innerLeftTriangle4 = Face(top, middleBottomNo3, leftBack)
      val back = Face(top, right, leftBack)

      val faces = setOf(leftFrontPart, rightFrontPart, innerLeftTriangle1, innerLeftTriangle2,
              innerLeftTriangle3, innerLeftTriangle4, back)
      val facesToFacesAbove = mapOf(
              innerLeftTriangle1 to setOf(leftFrontPart),
              innerLeftTriangle2 to setOf(leftFrontPart, innerLeftTriangle1),
              innerLeftTriangle3 to setOf(leftFrontPart, innerLeftTriangle1, innerLeftTriangle2),
              innerLeftTriangle4 to setOf(leftFrontPart, innerLeftTriangle1, innerLeftTriangle2, innerLeftTriangle3),
              back to faces - back)
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, faces, facesToFacesAbove)
      val wb = Figure(bundle)

      val toReplace = Pair(leftFrontPart, rightFrontPart)
      val unfoldedFace = Face(top, right, leftFront)

      val newBundle = bundle.unfold(unfoldedFace, toReplace)
      assertThat(newBundle.faces.size, `is`(6)) //Six faces: back and front and four inner triangles at the left side
      assertThat(newBundle.faces, `is`(setOf(
              unfoldedFace, innerLeftTriangle1, innerLeftTriangle2, innerLeftTriangle3, innerLeftTriangle4, back )))
      val expectedFacesAboveBackFace = setOf(unfoldedFace, innerLeftTriangle1, innerLeftTriangle2, innerLeftTriangle3, innerLeftTriangle4)
      assertThat(newBundle.facesAbove(back), `is`(expectedFacesAboveBackFace))
      val expectedFacesAboveInnerLeftTriangle4= setOf(unfoldedFace, innerLeftTriangle1, innerLeftTriangle2, innerLeftTriangle3)
      assertThat(newBundle.facesAbove(innerLeftTriangle4), `is`(expectedFacesAboveInnerLeftTriangle4))
      val expectedFacesAboveInnerLeftTriangle3= setOf(unfoldedFace, innerLeftTriangle1, innerLeftTriangle2)
      assertThat(newBundle.facesAbove(innerLeftTriangle3), `is`(expectedFacesAboveInnerLeftTriangle3))
      val expectedFacesAboveInnerLeftTriangle2= setOf(unfoldedFace, innerLeftTriangle1)
      assertThat(newBundle.facesAbove(innerLeftTriangle2), `is`(expectedFacesAboveInnerLeftTriangle2))
      val expectedFacesAboveInnerLeftTriangle1= setOf(unfoldedFace)
      assertThat(newBundle.facesAbove(innerLeftTriangle1), `is`(expectedFacesAboveInnerLeftTriangle1))
      assertTrue(newBundle.facesAbove(unfoldedFace).isEmpty())
   }
   /**
    * Same as before, but now is the split back face that has the rest of the faces above it
    */
   @Test
   fun unfoldBackFaceWaterbombFlap_Calculated(){
      val halfFigureWidth =   1
      val figureHeight =      1
      val top =         V(0, figureHeight)
      val right =       V(halfFigureWidth, 0)
      val leftFront =   V(-halfFigureWidth, 0)
      val leftBack =    V(-halfFigureWidth, 0)

      val middleBottomAtBack =   V(0, 0)
      val middleBottomNo2 =      V(0, 0)
      val middleBottomNo3 =      V(0, 0)

      val leftBackPart = Face(top, middleBottomAtBack, leftBack)
      val rightBackPart = Face(top, right, middleBottomAtBack)
      val innerLeftTriangle1 = Face(top, middleBottomNo2, leftFront)
      val leftNo2 = Vertex(leftFront)
      val innerLeftTriangle2 = Face(top, middleBottomNo2, leftNo2)
      val innerLeftTriangle3 = Face(top, middleBottomNo3, leftNo2)
      val innerLeftTriangle4 = Face(top, middleBottomNo3, leftBack)
      val front = Face(top, right, leftFront)

      val faces = setOf(leftBackPart, rightBackPart, innerLeftTriangle1, innerLeftTriangle2,
              innerLeftTriangle3, innerLeftTriangle4, front)
      val facesToFacesAbove = mapOf(
              innerLeftTriangle1 to setOf(front),
              innerLeftTriangle2 to setOf(front, innerLeftTriangle1),
              innerLeftTriangle3 to setOf(front, innerLeftTriangle1, innerLeftTriangle2),
              innerLeftTriangle4 to setOf(front, innerLeftTriangle1, innerLeftTriangle2, innerLeftTriangle3),
              leftBackPart to faces - leftBackPart  - rightBackPart,
              rightBackPart to setOf(front))
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, faces, facesToFacesAbove)
      val wb = Figure(bundle)

      val toReplace = Pair(leftBackPart, rightBackPart)
      val unfoldedFace = Face(top, right, leftBack)

      val newBundle = bundle.unfold(unfoldedFace, toReplace)
      assertThat(newBundle.faces.size, `is`(6)) //Six faces: back and front and four inner triangles at the left side
      assertThat(newBundle.faces, `is`(setOf(
              unfoldedFace, innerLeftTriangle1, innerLeftTriangle2, innerLeftTriangle3, innerLeftTriangle4, front )))
      val expectedFacesAboveNewUnfoldedBackFace = setOf(front, innerLeftTriangle1, innerLeftTriangle2, innerLeftTriangle3, innerLeftTriangle4)
      assertThat(newBundle.facesAbove(unfoldedFace), `is`(expectedFacesAboveNewUnfoldedBackFace))
      val expectedFacesAboveInnerLeftTriangle4= setOf(front, innerLeftTriangle1, innerLeftTriangle2, innerLeftTriangle3)
      assertThat(newBundle.facesAbove(innerLeftTriangle4), `is`(expectedFacesAboveInnerLeftTriangle4))
      val expectedFacesAboveInnerLeftTriangle3= setOf(front, innerLeftTriangle1, innerLeftTriangle2)
      assertThat(newBundle.facesAbove(innerLeftTriangle3), `is`(expectedFacesAboveInnerLeftTriangle3))
      val expectedFacesAboveInnerLeftTriangle2= setOf(front, innerLeftTriangle1)
      assertThat(newBundle.facesAbove(innerLeftTriangle2), `is`(expectedFacesAboveInnerLeftTriangle2))
      val expectedFacesAboveInnerLeftTriangle1= setOf(front)
      assertThat(newBundle.facesAbove(innerLeftTriangle1), `is`(expectedFacesAboveInnerLeftTriangle1))
      assertTrue(newBundle.facesAbove(front).isEmpty())
   }
   /**
    * For the waterbomb base, test that the faces above and below each face are correctly calculated.
    */
   @Test
   fun testFacesBelowAndAbove_InWaterbombBase_Calculated(){
      val halfFigureWidth = 1
      val figureHeight =    1
      val top =         V(0, figureHeight)
      val rightFront =  V(halfFigureWidth, 0)
      val leftFront =   V(-halfFigureWidth, 0)
      val front = Face(top, leftFront, rightFront)
      val middleBottomLeftPart =  V(0, 0)
      val middleBottomRightPart = V(0, 0)
      val innerLeftTriangle1 = Face(top, middleBottomLeftPart, leftFront)
      val innerRightTriangle1 = Face(top, rightFront, middleBottomRightPart)
      val leftBack = Vertex(leftFront)
      val innerLeftTriangle2 = Face(top, leftBack, middleBottomLeftPart)
      val rightBack = Vertex(rightFront)
      val innerRightTriangle2 = Face(top, middleBottomRightPart, rightBack)
      val backTriangle = Face(top, rightBack, leftBack)
      val faces = setOf(front,
              backTriangle, innerLeftTriangle1, innerRightTriangle1,
              innerLeftTriangle2, innerRightTriangle2)
      val facesToFacesAbove = mapOf(
              front to setOf(backTriangle, innerLeftTriangle1, innerRightTriangle1, innerLeftTriangle2, innerRightTriangle2),
              innerLeftTriangle1 to setOf(innerLeftTriangle2, backTriangle),
              innerRightTriangle1 to setOf(innerRightTriangle2, backTriangle),
              innerLeftTriangle2 to setOf(backTriangle),
              innerRightTriangle2 to setOf(backTriangle) )
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, faces, facesToFacesAbove)
      val wb = Figure(bundle)
      assertThat(bundle.facesAbove(front), `is`(faces - front))
      assertTrue(bundle.facesBelow(front).isEmpty())
      assertThat(bundle.facesAbove(innerLeftTriangle1), `is`(setOf(innerLeftTriangle2, backTriangle)) )
      assertThat(bundle.facesBelow(innerLeftTriangle1), `is`(setOf(front)) )
      assertThat(bundle.facesAbove(innerRightTriangle1), `is`(setOf(innerRightTriangle2, backTriangle)) )
      assertThat(bundle.facesBelow(innerRightTriangle1), `is`(setOf(front)) )
      assertThat(bundle.facesAbove(innerLeftTriangle2), `is`(setOf(backTriangle)) )
      assertThat(bundle.facesBelow(innerLeftTriangle2), `is`(setOf(innerLeftTriangle1, front)) )
      assertThat(bundle.facesAbove(innerRightTriangle2), `is`(setOf(backTriangle)) )
      assertThat(bundle.facesBelow(innerRightTriangle2), `is`(setOf(innerRightTriangle1, front)) )
      assertTrue(bundle.facesAbove(backTriangle).isEmpty() )
      assertThat(bundle.facesBelow(backTriangle), `is`(faces - backTriangle) )
   }
   @Test
   fun insertFaceInBundleOfOneFace_WhenNewFaceGoesOnTop_Calculated(){
      val face = anyFaceInXYPlane
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, face)
      val newFaceVertices = anyFaceInXYPlane.vertices.take(2) + Vertex(anyFaceInXYPlane.vertices.last())
      val newFace = Face(newFaceVertices)
      val newBundle = bundle.insertFace(newFace, facesAbove = emptySet(), facesBelow = setOf(face))
      assertThat(newBundle.faces.size, `is`(2))
      assertThat(newBundle.facesAbove(face), `is`(setOf(newFace)))
      assertTrue(newBundle.facesBelow(face).isEmpty())
      assertTrue(newBundle.facesAbove(newFace).isEmpty())
      assertThat(newBundle.facesBelow(newFace), `is`(setOf(face)))
   }
   @Test
   fun insertFaceInBundleOfOneFace_WhenNewFaceGoesOnBottom_Calculated(){
      val face = anyFaceInXYPlane
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, face)
      val newFaceVertices = anyFaceInXYPlane.vertices.take(2) + Vertex(anyFaceInXYPlane.vertices.last())
      val newFace = Face(newFaceVertices)
      val newBundle = bundle.insertFace(newFace, facesAbove = setOf(face), facesBelow = emptySet())
      assertThat(newBundle.faces.size, `is`(2))
      assertThat(newBundle.facesAbove(newFace), `is`(setOf(face)))
      assertTrue(newBundle.facesBelow(newFace).isEmpty())
      assertTrue(newBundle.facesAbove(face).isEmpty())
      assertThat(newBundle.facesBelow(face), `is`(setOf(newFace)))
   }
   @Test
   fun insertFaceInBundleOfTwoFaces_WhenNewFaceGoesInTheMiddle_Calculated() {
      val p1 = Vertex(0.0, 0.0, 0.0)
      val p2 = Vertex(1.0, 0.0, 0.0)
      val p3 = Vertex(1.0, 1.0, 0.0)
      val bottomFace = Face(p1, p2, p3)
      val topFace = Face(p1, p2, Vertex(p3))
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, setOf(bottomFace, topFace), mapOf(bottomFace to setOf(topFace)) )

      val middleFace = Face(p2, p3, Vertex(p1))
      val newBundle = bundle.insertFace(middleFace, facesAbove = setOf(topFace), facesBelow = setOf(bottomFace))
      assertThat(newBundle.faces.size, `is`(3))
      assertThat(newBundle.facesAbove(middleFace), `is`(setOf(topFace)))
      assertThat(newBundle.facesBelow(middleFace), `is`(setOf(bottomFace)))
      assertTrue(newBundle.facesBelow(bottomFace).isEmpty())
      assertTrue(newBundle.facesAbove(topFace).isEmpty())
      assertThat(newBundle.facesAbove(bottomFace), `is`(setOf(topFace, middleFace)))
      assertThat(newBundle.facesBelow(topFace), `is`(setOf(bottomFace, middleFace)))
   }
}