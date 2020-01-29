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

package com.whitebeluga.origami.figure.folding.rotating

import com.moduleforge.libraries.geometry.Geometry.epsilonEquals
import com.moduleforge.libraries.geometry._3d.LineSegment
import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Point
import com.moduleforge.libraries.geometry._3d.Vector
import com.whitebeluga.origami.figure.Bundle
import com.whitebeluga.origami.figure.component.Edge
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.TestVertex
import com.whitebeluga.origami.figure.folding.SideOfFold
import com.whitebeluga.origami.figure.folding.SideOfFold.Companion.calculateSideOfFold
import com.whitebeluga.origami.figure.folding.breakingup.BrokenUpFigure
import com.whitebeluga.origami.figure.folding.breakingup.SplitBundle
import com.whitebeluga.origami.figure.folding.rotating.Rotator.Companion.makePIRotator
import com.whitebeluga.origami.figure.folding.rotating.Rotator.Companion.makeRotator
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.hasItem
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.Math.*

class RotatorTest {
   private lateinit var xyPlane_NormalTowardsZPositive: Plane
   private lateinit var xyPlane_NormalTowardsZNegative: Plane

   @Before
   fun setUp(){
      val antiClockwisePointsAsSeenFromZPositive = listOf( Point(0, 0, 0), Point(1, 0, 0), Point(1, 1, 0))
      xyPlane_NormalTowardsZPositive = Plane.planeFromOrderedPoints(antiClockwisePointsAsSeenFromZPositive)
      val antiClockwisePointsAsSeenFromZNegative = listOf( Point(0, 0, 0), Point(1, 1, 0), Point(1, 0, 0))
      xyPlane_NormalTowardsZNegative = Plane.planeFromOrderedPoints(antiClockwisePointsAsSeenFromZNegative)
   }
   @Test
   fun nonFlatRotationOfABundleComposedOfASingleFace_Calculated(){
      val segment = LineSegment(Point(0.0, 0.0, 0.0), Point(0.0, 1.0, 0.0))
      val line = segment.line
      val plane = xyPlane_NormalTowardsZPositive
      val leftSide = calculateSideOfFold(line, Point(-1.0, 0.0, 0.0), plane)!!
      val rightSide = calculateSideOfFold(line, Point(1.0, 0.0, 0.0), plane)!!

      //make bundles
      val top = TestVertex(0, 1)
      val bottomMiddle = TestVertex(0, 0)
      val left = TestVertex(-1, 0)
      val right = TestVertex(1, 0)
      val rightSideFace = Face(top, bottomMiddle, right)
      val rightSideBundle = Bundle(plane, rightSideFace)
      val leftSideFace = Face(top, left, bottomMiddle)
      val leftSideBundle = Bundle(plane, leftSideFace)

      //do the rotation
      val splitBundleMap = mapOf(rightSide to rightSideBundle, leftSide to leftSideBundle)
      val edgesOnFoldLine = setOf(Edge(top, bottomMiddle))
      val splitBundle = SplitBundle(splitBundleMap, edgesOnFoldLine)
      val restOfBundles = emptyMap<SideOfFold, Set<Bundle>>()
      val brokenUp = BrokenUpFigure( splitBundle, restOfBundles, sideToRotate = rightSide)
      val rotationTowardsZPos = Vector(0.0, 0.0, 1.0)
      val angle = PI / 10.0
      val rotator = makeRotator(brokenUp, segment, rotationTowardsZPos, angle)
      val rotated = rotator.rotate()

      assertThat(rotated.bundles.size, `is`(2))
      assertThat(rotated.bundles, hasItem(leftSideBundle))
      val rotatedBundle = (rotated.bundles - leftSideBundle).first()
      val rotatedFace = rotatedBundle.faces.first()
      val rotatedPoints = rotatedFace.vertices.filterNot { line.contains(it) }.toSet()
      assertThat(rotatedPoints.size, `is`(1)) //only one vertex of the triangle is rotated
      val rotatedPoint = rotatedPoints.first()

      assertTrue(epsilonEquals(rotatedPoint.x(), right.x() * cos(angle)))
      assertTrue(epsilonEquals(rotatedPoint.y(), right.y()))
      assertTrue(epsilonEquals(rotatedPoint.z(), right.x() * sin(angle)))

      //check the plane of the rotated bundle
      val antiClockwiseRotatedPointsAsSeenFromZPositive = listOf( Point(0, 0, 0), rotatedPoint, Point(0, 1, 0))
      val expectedRotatedPlane = Plane.planeFromOrderedPoints(antiClockwiseRotatedPointsAsSeenFromZPositive)

      assertTrue(rotatedBundle.plane.normal.epsilonEquals(expectedRotatedPlane.normal))
   }
   /**
    * In this case we will use a bundle with two faces and a fold line perpendicular to the segment
    * connecting the two faces.
    *
    * The purpose of this test is mainly testing the map of faces above and below are correctly calculated.
    * And also test that rotated vertices shared by two or more faces are not duplicated.
    */
   @Test
   fun nonFlatRotationOfABundleComposedOfTwoFaces_Calculated(){
      val segment = LineSegment(Point(0.0, 0.0, 0.0), Point(0.0, 1.0, 0.0))
      val line = segment.line
      val plane = xyPlane_NormalTowardsZPositive
      val leftSide = calculateSideOfFold(line, Point(-1.0, 0.0, 0.0), plane)!!
      val rightSide = calculateSideOfFold(line, Point(1.0, 0.0, 0.0), plane)!!

      //make bundles
      /*
       * There are two top points and one bottom point in the middle, one point on the left and another on the right
       * so that the faces are connected by a segment on the X axis.
       */
      val topAtTheBack = TestVertex(0, 1)
      val topAtTheFront = TestVertex(0, 1)
      val bottomMiddle = TestVertex(0, 0)
      val left = TestVertex(-1, 0)
      val right = TestVertex(1, 0)
      val rightSideFaceAtTheBack = Face(topAtTheBack, bottomMiddle, right)
      val rightSideFaceAtTheFront = Face(topAtTheFront, bottomMiddle, right)
      val rightSideBundle = Bundle(plane, setOf(rightSideFaceAtTheBack, rightSideFaceAtTheFront),
              mapOf(rightSideFaceAtTheBack to setOf(rightSideFaceAtTheFront)))
      val leftSideFaceAtTheFront = Face(topAtTheFront, left, bottomMiddle)
      val leftSideFaceAtTheBack = Face(topAtTheBack, left, bottomMiddle)
      val leftSideBundle = Bundle(plane, setOf(leftSideFaceAtTheBack, leftSideFaceAtTheFront),
              mapOf(leftSideFaceAtTheBack to setOf(leftSideFaceAtTheFront)))

      //do the rotation
      val splitBundleMap = mapOf(rightSide to rightSideBundle, leftSide to leftSideBundle)
      val edgesOnFoldLine = setOf(Edge(topAtTheBack, bottomMiddle), Edge(topAtTheFront, bottomMiddle))
      val splitBundle = SplitBundle(splitBundleMap, edgesOnFoldLine)
      val restOfBundles = emptyMap<SideOfFold, Set<Bundle>>()
      val brokenUp = BrokenUpFigure( splitBundle, restOfBundles, sideToRotate = rightSide)
      val rotationTowardsZPos = Vector(0.0, 0.0, 1.0)
      val angle = PI / 10.0
      val rotator = makeRotator(brokenUp, segment, rotationTowardsZPos, angle)
      val rotated = rotator.rotate()

      assertThat(rotated.bundles.size, `is`(2))
      assertThat(rotated.bundles, hasItem(leftSideBundle))
      val rotatedBundle = (rotated.bundles - leftSideBundle).first()
      val rotatedFaces = rotatedBundle.faces
      assertThat(rotatedFaces.size, `is`(2)) //two faces have been rotated

      //rotated points from the two faces: should be only one vertex is rotated because it's shared by both faces
      val rotatedPoints = rotatedFaces.flatMap { it.vertices }.toSet().filterNot { line.contains(it) }.toSet()
      assertThat(rotatedPoints.size, `is`(1))

      //assert that faces above and below are correct
      //face on top is the one connected to the stationary face on top on the left
      val rotatedFaceAtTheFront = rotatedFaces.first { it.vertices.contains(topAtTheFront) }
      val rotatedFaceAtTheBack = rotatedFaces.first { it.vertices.contains(topAtTheBack) }
      assertThat(rotatedBundle.facesAbove(rotatedFaceAtTheBack), `is`(setOf(rotatedFaceAtTheFront)))
      assertThat(rotatedBundle.facesBelow(rotatedFaceAtTheFront), `is`(setOf(rotatedFaceAtTheBack)))

      //finally, run the same tests as before, just in case:
      val rotatedPoint = rotatedPoints.first()

      assertTrue(epsilonEquals(rotatedPoint.x(), right.x() * cos(angle)))
      assertTrue(epsilonEquals(rotatedPoint.y(), right.y()))
      assertTrue(epsilonEquals(rotatedPoint.z(), right.x() * sin(angle)))

      //check the plane of the rotated bundle
      val antiClockwiseRotatedPointsAsSeenFromZPositive = listOf( Point(0, 0, 0), rotatedPoint, Point(0, 1, 0))
      val expectedRotatedPlane = Plane.planeFromOrderedPoints(antiClockwiseRotatedPointsAsSeenFromZPositive)

      assertTrue(rotatedBundle.plane.normal.epsilonEquals(expectedRotatedPlane.normal))
   }

   /**
    * Same figure as in the other test: a bundle with two faces and a fold line perpendicular to the segment
    * connecting the two faces.
    *
    * In this case, we will do a flat fold
    */
   @Test
   fun flatRotationOfABundleComposedOfTwoFaces_Calculated(){
      val segment = LineSegment(Point(0.0, 0.0, 0.0), Point(0.0, 1.0, 0.0))
      val line = segment.line
      val plane = xyPlane_NormalTowardsZPositive
      val leftSide = calculateSideOfFold(line, Point(-1.0, 0.0, 0.0), plane)!!
      val rightSide = calculateSideOfFold(line, Point(1.0, 0.0, 0.0), plane)!!

      //make bundles
      /*
       * There are two top points and one bottom point in the middle, one point on the left and another on the right
       * so that the faces are connected by a segment on the X axis.
       */
      val topAtTheBack = TestVertex(0, 1)
      val topAtTheFront = TestVertex(0, 1)
      val bottomMiddle = TestVertex(0, 0)
      val left = TestVertex(-1, 0)
      val right = TestVertex(1, 0)
      val rightSideFaceAtTheBack = Face(topAtTheBack, bottomMiddle, right)
      val rightSideFaceAtTheFront = Face(topAtTheFront, bottomMiddle, right)
      val rightSideBundle = Bundle(plane, setOf(rightSideFaceAtTheBack, rightSideFaceAtTheFront),
              mapOf(rightSideFaceAtTheBack to setOf(rightSideFaceAtTheFront)))

      val leftSideFaceAtTheFront = Face(topAtTheFront, left, bottomMiddle)
      val leftSideFaceAtTheBack = Face(topAtTheBack, left, bottomMiddle)
      val leftSideBundle = Bundle(plane, setOf(leftSideFaceAtTheBack, leftSideFaceAtTheFront),
              mapOf(leftSideFaceAtTheBack to setOf(leftSideFaceAtTheFront)))

      //do the rotation

      val splitBundleMap = mapOf(rightSide to rightSideBundle, leftSide to leftSideBundle)
      val edgesOnFoldLine = setOf(Edge(topAtTheBack, bottomMiddle), Edge(topAtTheFront, bottomMiddle))
      val splitBundle = SplitBundle(splitBundleMap, edgesOnFoldLine)

      val brokenUp = BrokenUpFigure(
              splitBundle = splitBundle,
              mapOfSideToWholeBundles = emptyMap(),
              sideToRotate = rightSide)
      val rotationTowardsZPos = Vector(0.0, 0.0, 1.0)
      val rotator = makePIRotator(brokenUp, segment, rotationTowardsZPos)
      val rotated = rotator.rotate()

      assertThat(rotated.bundles.size, `is`(1))
      val bundleFoldedOver = rotated.bundles.first()
      assertThat(bundleFoldedOver.faces.size, `is`(4)) //four faces: two per side of the fold

      val rotatedFaces = bundleFoldedOver.faces - leftSideBundle.faces

      //rotated points from the two faces: should be only one vertex is rotated because it's shared by both faces
      val rotatedPoints = rotatedFaces.flatMap { it.vertices }.toSet().filterNot { line.contains(it) }.toSet()
      assertThat(rotatedPoints.size, `is`(1))

      //check the faces to faces above and below
      assertThat(rotatedFaces.size, `is`(2))
      val rotatedFaceThatEndsOnTop = rotatedFaces.first { it.vertices.contains(topAtTheBack) }
      assertTrue(bundleFoldedOver.facesAbove(rotatedFaceThatEndsOnTop).isEmpty())
      val rotatedFaceTheEndsAsSecondLayer = rotatedFaces.first { it.vertices.contains(topAtTheFront) }
      val facesBelowTheNewTopFace = leftSideBundle.faces + rotatedFaceTheEndsAsSecondLayer
      assertThat(bundleFoldedOver.facesBelow(rotatedFaceThatEndsOnTop), `is`(facesBelowTheNewTopFace))
   }
   /**
    * Same as previous but folding direction in the opposite direction of the bundle's direction
    */
   @Test
   fun flatRotationOfABundleComposedOfTwoFaces_RotateDirectionOppositeBundleDirection_Calculated(){
      val segment = LineSegment(Point(0.0, 0.0, 0.0), Point(0.0, 1.0, 0.0))
      val line = segment.line
      val plane = xyPlane_NormalTowardsZPositive
      val leftSide = calculateSideOfFold(line, Point(-1.0, 0.0, 0.0), plane)!!
      val rightSide = calculateSideOfFold(line, Point(1.0, 0.0, 0.0), plane)!!

      //make bundles
      /*
       * There are two top points and one bottom point in the middle, one point on the left and another on the right
       * so that the faces are connected by a segment on the X axis.
       */
      val topAtTheBack = TestVertex(0, 1)
      val topAtTheFront = TestVertex(0, 1)
      val bottomMiddle = TestVertex(0, 0)
      val left = TestVertex(-1, 0)
      val right = TestVertex(1, 0)
      val rightSideFaceAtTheBack = Face(topAtTheBack, bottomMiddle, right)
      val rightSideFaceAtTheFront = Face(topAtTheFront, bottomMiddle, right)
      val rightSideBundle = Bundle(plane, setOf(rightSideFaceAtTheBack, rightSideFaceAtTheFront),
              mapOf(rightSideFaceAtTheBack to setOf(rightSideFaceAtTheFront)))

      val leftSideFaceAtTheFront = Face(topAtTheFront, left, bottomMiddle)
      val leftSideFaceAtTheBack = Face(topAtTheBack, left, bottomMiddle)
      val leftSideBundle = Bundle(plane, setOf(leftSideFaceAtTheBack, leftSideFaceAtTheFront),
              mapOf(leftSideFaceAtTheBack to setOf(leftSideFaceAtTheFront)))

      //do the rotation

      val splitBundleMap = mapOf(rightSide to rightSideBundle, leftSide to leftSideBundle)
      val edgesOnFoldLine = setOf(Edge(topAtTheBack, bottomMiddle), Edge(topAtTheFront, bottomMiddle))
      val splitBundle = SplitBundle(splitBundleMap, edgesOnFoldLine)

      val brokenUp = BrokenUpFigure(
              splitBundle = splitBundle,
              mapOfSideToWholeBundles = emptyMap(),
              sideToRotate = rightSide)
      val rotationTowardsZNeg = Vector(0.0, 0.0, -1.0)
      val rotator = makePIRotator(brokenUp, segment, rotationTowardsZNeg)
      val rotated = rotator.rotate()

      assertThat(rotated.bundles.size, `is`(1))
      val bundleFoldedOver = rotated.bundles.first()
      assertThat(bundleFoldedOver.faces.size, `is`(4)) //four faces: two per side of the fold

      val rotatedFaces = bundleFoldedOver.faces - leftSideBundle.faces

      //rotated points from the two faces: should be only one vertex is rotated because it's shared by both faces
      val rotatedPoints = rotatedFaces.flatMap { it.vertices }.toSet().filterNot { line.contains(it) }.toSet()
      assertThat(rotatedPoints.size, `is`(1))

      //check the faces to faces above and below
      assertThat(rotatedFaces.size, `is`(2))
      assertTrue(bundleFoldedOver.facesAbove(leftSideFaceAtTheFront).isEmpty())
      val rotatedFaceThatEndsOnBottom = rotatedFaces.first { it.vertices.contains(topAtTheFront) }

      assertTrue(bundleFoldedOver.facesBelow(rotatedFaceThatEndsOnBottom).isEmpty())
      val rotatedFaceTheEndsAsSecondToLastLayer = rotatedFaces.first { it.vertices.contains(topAtTheBack) }
      assertThat(bundleFoldedOver.facesAbove(rotatedFaceThatEndsOnBottom), `is`(
              leftSideBundle.faces + rotatedFaceTheEndsAsSecondToLastLayer ))

      assertThat(bundleFoldedOver.facesBelow(rotatedFaceTheEndsAsSecondToLastLayer), `is`(setOf(rotatedFaceThatEndsOnBottom)))
      assertThat(bundleFoldedOver.facesAbove(rotatedFaceTheEndsAsSecondToLastLayer), `is`(leftSideBundle.faces))
   }
   /**
    * Similar as in the other test. A bundle of two connected faces is folded.
    * In this occasion the rotated part is connected to another bundle, a simple one of one face.
    */
   @Test
   fun flatRotationOfABundleComposedOfTwoFaces_AndAnotherBundleConnectedToTheRotatedPart_Calculated(){
      val segment = LineSegment(Point(0.0, 0.0, 0.0), Point(0.0, 1.0, 0.0))
      val line = segment.line
      val plane = xyPlane_NormalTowardsZPositive
      val leftSide = calculateSideOfFold(line, Point(-1.0, 0.0, 0.0), plane)!!
      val rightSide = calculateSideOfFold(line, Point(1.0, 0.0, 0.0), plane)!!

      //make bundles
      /*
       * There are two top points and one bottom point in the middle, one point on the left and another on the right
       * so that the faces are connected by a segment on the X axis.
       */
      val topAtTheBack = TestVertex(0, 1)
      val topAtTheFront = TestVertex(0, 1)
      val bottomMiddle = TestVertex(0, 0)
      val left = TestVertex(-1, 0)
      val right = TestVertex(1, 0)
      val rightSideFaceAtTheBack = Face(topAtTheBack, bottomMiddle, right)
      val rightSideFaceAtTheFront = Face(topAtTheFront, bottomMiddle, right)
      val rightSideBundle = Bundle(plane, setOf(rightSideFaceAtTheBack, rightSideFaceAtTheFront),
              mapOf(rightSideFaceAtTheBack to setOf(rightSideFaceAtTheFront)))

      val leftSideFaceAtTheFront = Face(topAtTheFront, left, bottomMiddle)
      val leftSideFaceAtTheBack = Face(topAtTheBack, left, bottomMiddle)
      val leftSideBundle = Bundle(plane, setOf(leftSideFaceAtTheBack, leftSideFaceAtTheFront),
              mapOf(leftSideFaceAtTheBack to setOf(leftSideFaceAtTheFront)))

      val vertexOfFaceConnectedToRightSide = TestVertex(1, 0, -1)
      val plane2 = Plane.planeFromOrderedPoints( vertexOfFaceConnectedToRightSide, topAtTheBack, right )
      val faceConnectedToRightSide = Face(vertexOfFaceConnectedToRightSide, topAtTheBack, right )
      val bundleConnectedToRightSide = Bundle(plane2, faceConnectedToRightSide)

      //do the rotation

      val splitBundleMap = mapOf(rightSide to rightSideBundle, leftSide to leftSideBundle)
      val edgesOnFoldLine = setOf(Edge(topAtTheBack, bottomMiddle), Edge(topAtTheFront, bottomMiddle))
      val splitBundle = SplitBundle(splitBundleMap, edgesOnFoldLine)

      val brokenUp = BrokenUpFigure(
              splitBundle = splitBundle,
              mapOfSideToWholeBundles = mapOf(rightSide to setOf(bundleConnectedToRightSide)),
              sideToRotate = rightSide)
      val rotationTowardsZPos = Vector(0.0, 0.0, 1.0)
      val rotator = makePIRotator(brokenUp, segment, rotationTowardsZPos)
      val rotated = rotator.rotate()

      assertThat(rotated.bundles.size, `is`(2))
      assertThat(rotated.vertices.size, `is`(6)) //the figure has to have six vertices (do a simple drawing that will demonstrate this)
      val rotatedBundleConnectedToRightSide = rotated.bundles.filterNot { it.faces.contains(leftSideFaceAtTheBack) }.first()
      /* the point of the face in the bundle connected to the right side that is not part of the other bundle
      has to be at the following position:
      */
      val expectedPointPosition = Point(-1, 0, 1)
      val rotatedFaceConnectedToRightSide = rotatedBundleConnectedToRightSide.faces.first()
      assertTrue(rotatedFaceConnectedToRightSide.vertices.any { it.epsilonEquals(expectedPointPosition)})
   }
}