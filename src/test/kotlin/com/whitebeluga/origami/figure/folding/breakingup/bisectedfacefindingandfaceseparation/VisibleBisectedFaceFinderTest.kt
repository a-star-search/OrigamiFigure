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

package com.whitebeluga.origami.figure.folding.breakingup.bisectedfacefindingandfaceseparation

import com.moduleforge.libraries.geometry._3d.LineSegment
import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Plane.planeFromOrderedPoints
import com.moduleforge.libraries.geometry._3d.Point
import com.whitebeluga.origami.figure.Bundle
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.folding.exceptions.InvalidFoldException
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.hasItems
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import com.whitebeluga.origami.figure.component.TestVertex as V

class VisibleBisectedFaceFinderTest {
   private lateinit var xyPlane_NormalTowardsZPositive: Plane
   private lateinit var squareFace: Face
   private lateinit var bottomLeft: V
   private lateinit var bottomRight: V
   private lateinit var topRight: V
   private lateinit var topLeft: V
   private lateinit var topMiddle: V
   private lateinit var bottomMiddle: V

   @Before
   fun setUp() {
      val antiClockwisePointsAsSeenFromZPositive =
              listOf(Point(0, 0, 0), Point(1, 0, 0), Point(1, 1, 0))
      xyPlane_NormalTowardsZPositive = planeFromOrderedPoints(antiClockwisePointsAsSeenFromZPositive)
      bottomLeft = V(0, 0)
      bottomRight = V(1, 0)
      topRight = V(1, 1)
      topLeft = V(0, 1)
      squareFace = Face(bottomLeft, bottomRight, topRight, topLeft)

      bottomMiddle = V(0.5, 0)
      topMiddle = V(0.5, 1)
   }
   @Test
   fun findVisibleBisectedFaces_ForASingleFaceFigure_AndIntersected_ShouldReturnTheFace() {
      val foldSegment = LineSegment(bottomMiddle, topMiddle)
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, squareFace)

      val visibleBisectedFaceFinder = VisibleBisectedFaceFinder(foldSegment, bundle)
      val visibleBisectedFacesFromTop = visibleBisectedFaceFinder.findVisibleBisectedFacesFromTop()
      assertThat(visibleBisectedFacesFromTop.size, `is`(1))
      assertThat(visibleBisectedFacesFromTop.first(), `is`(squareFace))
      val visibleBisectedFacesFromBottom = visibleBisectedFaceFinder.findVisibleBisectedFacesFromBottom()
      assertThat(visibleBisectedFacesFromBottom.size, `is`(1))
      assertThat(visibleBisectedFacesFromBottom.first(), `is`(squareFace))
   }
   @Test
   fun findVisibleBisectedFaces_WhenTwoFacesInABundle_AndOneVisiblyIntersected_AndTheOneBehindIntersected_ShouldReturnTheOneVisiblyIntersected(){
      val topFace = Face(bottomLeft, topLeft, topMiddle, bottomMiddle)
      val bundle =
              Bundle(xyPlane_NormalTowardsZPositive, setOf(squareFace, topFace), mapOf(squareFace to setOf(topFace)) )
      val foldSegment = LineSegment(topLeft, bottomMiddle)
      val visibleBisectedFaceFinder = VisibleBisectedFaceFinder(foldSegment, bundle)
      val visibleFromTop = visibleBisectedFaceFinder.findVisibleBisectedFacesFromTop()

      assertThat(visibleFromTop.size, `is`(1))
      assertThat(visibleFromTop.first(), `is`(topFace))

      val visibleFromBottom = visibleBisectedFaceFinder.findVisibleBisectedFacesFromBottom()
      assertThat(visibleFromBottom.size, `is`(1))
      assertThat(visibleFromBottom.first(), `is`(squareFace))
   }
   @Test(expected= InvalidFoldException::class)
   fun findVisibleBisectedFaces_WhenTwoFacesInABundle_AndOneIntersectedButHiddenByNotIntersected_AndTheHiddingFaceIsPartiallyIntersected_ShouldThrowExceptionBecauseOfInvalidFold(){
      val middle = V(0.5, 0.5, 0)
      val bottomFace = Face(bottomLeft, topLeft, topMiddle, middle)
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, setOf(squareFace, bottomFace), mapOf(bottomFace to setOf(squareFace))  )
      val foldSegment = LineSegment(topLeft, middle)
      val visibleBisectedFaceFinder = VisibleBisectedFaceFinder(foldSegment, bundle)
      //the one that is fully intersected is the bottom one, so looking at top should yield no faces / error
      visibleBisectedFaceFinder.findVisibleBisectedFacesFromTop()
   }
   @Test
   fun findVisibleBisectedFaces_WhenTwoFacesInABundle_AndOneVisiblyIntersected_AndOtherBehindIsNotIntersected_ShouldReturnVisiblyIntersectedFace() {
      val middle = V(0.5, 0.5)
      val bottomFace = Face(bottomLeft, topLeft, topMiddle, middle)
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, setOf(squareFace, bottomFace), mapOf(bottomFace to setOf(squareFace)))
      val foldSegment = LineSegment(topLeft, middle)
      val visibleBisectedFaceFinder = VisibleBisectedFaceFinder(foldSegment, bundle)
      val visibleBisectedFaces = visibleBisectedFaceFinder.findVisibleBisectedFacesFromBottom()
      assertThat(visibleBisectedFaces, `is`(setOf(bottomFace)))
   }
   @Test
   fun findVisibleBisectedFaces_WhenTwoFacesInABundle_AndOneFullyVisiblyIntersected_OtherPartiallyVisiblyIntersected_ShouldReturnBothFaces() {
      val face1 = Face(bottomLeft, topRight, topLeft)
      val face2 = Face(bottomLeft, bottomRight, topLeft)
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, setOf(face1, face2), mapOf(face1 to setOf(face2)))

      val pointAtBottom = Point(0.1, 0, 0)
      val foldSegment = LineSegment(topLeft, pointAtBottom)

      val visibleBisectedFaceFinder = VisibleBisectedFaceFinder(foldSegment, bundle)

      val visibleBisectedFaces = visibleBisectedFaceFinder.findVisibleBisectedFacesFromBottom()
      assertThat(visibleBisectedFaces.size, `is`(2))
      assertThat(visibleBisectedFaces, hasItems(face1, face2))
   }
}