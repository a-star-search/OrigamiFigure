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

import com.moduleforge.libraries.geometry.GeometryConstants.TOLERANCE_EPSILON
import com.moduleforge.libraries.geometry._3d.ColorCombination
import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Plane.planeFromOrderedPoints
import com.moduleforge.libraries.geometry._3d.Point
import com.whitebeluga.origami.figure.OrigamiBase.SQUARE
import com.whitebeluga.origami.figure.component.Face
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.closeTo
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.awt.Color.*
import com.whitebeluga.origami.figure.component.TestVertex as V

class FigureTest {
   private lateinit var complexFigure: Figure
   private lateinit var colors: ColorCombination
   private lateinit var xyPlane_NormalTowardsZPositive: Plane
   @Before
   fun setUp(){
      colors = ColorCombination(ORANGE, MAGENTA)
      val plane1 = planeFromOrderedPoints(Point(0, 0, 0), Point(1, 0, 0), Point(1, 1, 0))
      val origin = V(0, 0, 0)
      val rightDown = V(1, 0, 0)
      val rightUp = V(1, 1, 0)
      val rightDownFront = V(1, 0, 1)
      val face1 = Face(origin, rightDown, rightUp, colors=colors)
      val bundle1 = Bundle(plane1, face1)
      val face2 = Face(rightUp, rightDown, rightDownFront, colors=colors)
      val plane2 = planeFromOrderedPoints(Point(1, 1, 0), Point(1, 0, 0), Point(1, 0, 1))
      val bundle2 = Bundle(plane2, face2)
      complexFigure = Figure(setOf(bundle1, bundle2))
      val antiClockwisePointsAsSeenFromZPositive = listOf( Point(0, 0, 0), Point(1, 0, 0), Point(1, 1, 0))
      xyPlane_NormalTowardsZPositive = planeFromOrderedPoints(antiClockwisePointsAsSeenFromZPositive)
   }
   @Test
   fun afterColorChange_TheColorsShouldHaveChanged(){
      val greenFront = complexFigure.changeFrontColor(GREEN)
      greenFront.faces.forEach { assertThat(it.frontColor, `is`(GREEN)) }
      greenFront.faces.forEach { assertThat(it.backColor, `is`(colors.back)) }

      val greenBack = complexFigure.changeBackColor(GREEN)
      greenBack.faces.forEach { assertThat(it.backColor, `is`(GREEN)) }
      greenBack.faces.forEach { assertThat(it.frontColor, `is`(colors.front)) }
   }
   @Test
   fun afterColorChange_FigureShouldRetainTheSameStructure(){
      val coloredFigure= complexFigure.changeFrontColor(RED)
      assertTrue(hasSameStructure(complexFigure, coloredFigure))
   }
   @Test
   fun afterTranslation_FigureShouldHaveTheSameColors(){
      val newCenter = Point(5, 5, 5)
      val translated= complexFigure.translated(newCenter)
      translated.faces.forEach { assertThat(it.frontColor, `is`(colors.front)) }
      translated.faces.forEach { assertThat(it.backColor, `is`(colors.back)) }
   }
   @Test
   fun afterTranslation_FigureShouldHaveTheSameStructure(){
      val newCenter = Point(5, 5, 5)
      val translated= complexFigure.translated(newCenter)
      assertTrue(hasSameStructure(complexFigure, translated))
   }
   @Test
   fun translation_ShouldTranslateCenter(){
      val origin = Point(0, 0, 0)
      val square = SQUARE.make(2, center = origin)
      val coordVal = 5
      val newCenter = Point(coordVal, coordVal, coordVal)
      val centerOfTranslated = square.translated(newCenter).center
      assertThat(centerOfTranslated.x(), closeTo(newCenter.x(), TOLERANCE_EPSILON))
      assertThat(centerOfTranslated.y(), closeTo(newCenter.y(), TOLERANCE_EPSILON))
      assertThat(centerOfTranslated.z(), closeTo(newCenter.z(), TOLERANCE_EPSILON))
   }
   /**
    * in this test, we test figure translation against point and vector methods
    */
   @Test
   fun translation_ShouldTranslateAnArbitraryPointOfTheFigure(){
      val figureCenter = complexFigure.center
      val newCenter = Point(145.0, 32.1, -8.3)
      val translationVector = figureCenter.vectorTo(newCenter)
      val arbitraryPoint = complexFigure.vertices.first()
      val translatedArbitraryPoint = arbitraryPoint.translate(translationVector)
      val translated = complexFigure.translated(newCenter)
      assertTrue(translated.vertices.any { it.epsilonEqualsFloatPrecision(translatedArbitraryPoint) })
   }
   @Test
   fun removeFace_WhenFigureHasTwoFacesInABundle_ShouldReturnNewFigureWithABundle_AndOneFace(){
      val face1 = Face(V(0, 0, 0), V(1, 0, 0), V(1, 1, 0))
      val face2 = Face(V(0, 0, 0), V(-1, 0, 0), V(-1, -1, 0))
      val ls = Bundle(xyPlane_NormalTowardsZPositive, setOf(face1, face2), emptyMap())
      val f = Figure(ls)
      val faceRemoved = f.removeFace(face1)
      assertThat(faceRemoved.bundles.size, `is`(1))
      val bundle  = faceRemoved.bundles.first()
      assertThat(bundle.faces.size, `is`(1))
   }
   @Test
   fun figureOfTwoTrianglesConnectedByAnEdge_FigureShouldHaveFiveEdges(){
      val v1 = V(0, 0, 0)
      val v2 = V(0, 1, 0)
      val face1 = Face(v1, v2, V(1, 1, 0))
      val face2 = Face(v1, v2, V(-1, 1, 0))
      val ls = Bundle(xyPlane_NormalTowardsZPositive, setOf(face1, face2), emptyMap())
      val figure = Figure(ls)
      val edgeCount = figure.edges.size
      val expectedEdgeCount = 5
      assertThat(edgeCount, `is`(expectedEdgeCount))
   }
   @Test
   fun facesSharingAVertex_WhenThereIsOnlyOneFaceInTheFigure_ShouldReturnTheFace(){
      val aVertex = V(1, 1)
      val aFace = Face(V(0, 0), V(1, 0), aVertex)
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, aFace)
      val f = Figure(bundle)
      assertThat(f.facesSharing(aVertex), `is`(setOf(aFace)))
   }
   @Test
   fun bundlesConnectedToABundle_WhenThereIsOnlyOneBundleInTheFigure_ShouldReturnAnEmptySet(){
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, Face(V(0, 0), V(1, 0), V(1, 1)))
      val f = Figure(bundle)
      assertTrue(f.bundlesConnectedTo(bundle).isEmpty())
   }
   /**
    * Adding faces to the bottom of a bundle is part of the algorithm of joining two bundles together that
    * happens when doing a so called "flat" fold
    */
   @Test
   fun addAFaceAtTheBottomOfABundle_Calculated(){
      val faceOfBundle = Face(V(0, 0), V(1, 0), V(1, 1))
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, faceOfBundle)

      val faceComesAtTheBottom = Face(V(0, 0), V(1, 0), V(1, 1))
      val newBundle =
              bundle.addNewBottomFaces(setOf(faceComesAtTheBottom), mapOf(faceComesAtTheBottom to setOf(faceOfBundle)))
      assertTrue(newBundle.facesAbove(faceOfBundle).isEmpty())
      assertThat(newBundle.facesBelow(faceOfBundle), `is`(setOf(faceComesAtTheBottom)))
      assertThat(newBundle.facesAbove(faceComesAtTheBottom), `is`(setOf(faceOfBundle)))
      assertTrue(newBundle.facesBelow(faceComesAtTheBottom).isEmpty())
   }
   /**
    * Add face at the bottom where there is a face that already has a face on top of it
    */
   @Test
   fun addAFaceAtTheBottomOfABundleWithTwoOverlappingFaces_Calculated(){
      val v1 = V(0, 0)
      val v2 = V(1, 0)
      val faceOfBundle = Face(v1, v2, V(1, 1))
      val anotherFaceOfBundle = Face(v1, v2, V(1, 1))
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, setOf(faceOfBundle, anotherFaceOfBundle),
              mapOf(faceOfBundle to setOf(anotherFaceOfBundle)))

      val faceComesAtTheBottom = Face(V(0, 0), V(1, 0), V(1, 1))
      val newBundle =
              bundle.addNewBottomFaces(setOf(faceComesAtTheBottom),
                      mapOf(faceComesAtTheBottom to setOf(faceOfBundle, anotherFaceOfBundle)))

      assertTrue(newBundle.facesAbove(anotherFaceOfBundle).isEmpty())
      assertThat(newBundle.facesBelow(anotherFaceOfBundle), `is`(setOf(faceOfBundle, faceComesAtTheBottom)))

      assertThat(newBundle.facesAbove(faceOfBundle), `is`(setOf(anotherFaceOfBundle)))
      assertThat(newBundle.facesBelow(faceOfBundle), `is`(setOf(faceComesAtTheBottom)))

      assertThat(newBundle.facesAbove(faceComesAtTheBottom), `is`(setOf(faceOfBundle, anotherFaceOfBundle)))
      assertTrue(newBundle.facesBelow(faceComesAtTheBottom).isEmpty())
   }
   @Test
   fun removeFaceOfBundle_WhenBundleHasASingleFace_ShouldReturnNull(){
      val faceOfBundle = Face(V(0, 0), V(1, 0), V(1, 1))
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, faceOfBundle)
      val faceRemoved = bundle.remove(faceOfBundle)
      assertNull(faceRemoved)
   }
   @Test
   fun removeFaceOfBundleWithTwoFaces_Calculated(){
      val v1 = V(0, 0)
      val v2 = V(1, 0)
      val bottomFace = Face(v1, v2, V(1, 1))
      val topFace = Face(v1, v2, V(1, 1))
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, setOf(bottomFace, topFace),
              mapOf(bottomFace to setOf(topFace)))
      val bottomFaceRemoved = bundle.remove(bottomFace)!!
      val topFaceRemoved = bundle.remove(topFace)!!
      assertThat(bottomFaceRemoved.faces, `is`(setOf(topFace)))
      assertThat(topFaceRemoved.faces, `is`(setOf(bottomFace)))
      assertTrue(bottomFaceRemoved.facesAbove(topFace).isEmpty())
      assertTrue(bottomFaceRemoved.facesBelow(topFace).isEmpty())
      assertTrue(topFaceRemoved.facesAbove(bottomFace).isEmpty())
      assertTrue(topFaceRemoved.facesBelow(bottomFace).isEmpty())
   }
   /**
    * All three faces overlap each other
    */
   @Test
   fun removeMiddleFaceOfBundleWithThreeOverlappingFaces_Calculated(){
      val v1 = V(0, 0)
      val v2 = V(1, 0)
      val v3 = V(1, 1)
      val bottomFace = Face(v1, v2, V(1, 1))
      val middleFace = Face(v1, v2, v3)
      val topFace = Face(V(0, 0), v2, v3)
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, setOf(bottomFace, middleFace, topFace),
              mapOf(  bottomFace to setOf(middleFace, topFace),
                      middleFace to setOf(topFace) ))
      val middleFaceRemoved = bundle.remove(middleFace)!!
      assertThat(middleFaceRemoved.faces, `is`(setOf(bottomFace, topFace)))
      assertThat(middleFaceRemoved.facesAbove(bottomFace), `is`(setOf(topFace)))
      assertThat(middleFaceRemoved.facesBelow(topFace), `is`(setOf(bottomFace)))
   }
   private fun hasSameStructure(f1: Figure, f2: Figure): Boolean {
      //very simplistic comparison, could be improved
      if(f1.faces.size != f2.faces.size)
         return false
      if(f1.vertices.size != f2.vertices.size)
         return false
      if(f1.planes.size != f2.planes.size)
         return false
      return true
   }
}
