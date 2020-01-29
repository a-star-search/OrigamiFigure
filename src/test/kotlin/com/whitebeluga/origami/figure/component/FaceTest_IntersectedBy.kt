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

package com.whitebeluga.origami.figure.component

import com.moduleforge.libraries.geometry._3d.LineSegment
import com.moduleforge.libraries.geometry._3d.Point
import com.whitebeluga.origami.figure.folding.FoldSegmentIntersectionType.*
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.whitebeluga.origami.figure.component.TestVertex as V

class FaceTest_IntersectedBy {
   private lateinit var fold: LineSegment
   private lateinit var fold2: LineSegment
   @Before
   fun initObjects(){
      fold = LineSegment(Point(0, 0, 7), Point(5, 4, 0))
      fold2 = LineSegment(Point(0, 0, 7), Point(2.0 * 5.0 / 3.0, 2.0 * 4.0 / 3, 7 + (2.0 * (-7.0) / 3.0)))
   }
   @Test
   fun foldLineDoesNotTouchThePolygonShouldFindNoIntersection() {
      val face = Face(V(10, 10, 10), V(-10, 10, 10), V(10, 10, -10))
      val intersection = face.intersectedBy(fold)
      assertTrue(intersection == NONE)
   }
   @Test
   fun foldLineCoversPolygonEdgeShouldBeRecognized() {
      val ratio = 2.0/3.0
      //two of the points of this polygon lie at one third and two thirds respectively of the fold line segment
      val face = Face( V(5.0/3.0, 4.0/3.0, 7 + (-7.0/3.0)), V(5.0*ratio, 4.0*ratio, 7 + (-7.0)*ratio), V(10, 10, 10))
      val intersection = face.intersectedBy(fold)
      assertTrue(intersection == COVERS_EDGE)
   }
   @Test
   fun foldLinePartiallyOverlapsPolygonEdgeShouldBeRecognized() {
      val face = Face(V(5.0/3.0, 4.0/3.0, 7.0 + (-7.0/3.0)), V(5, 4, 0), V(10, 10, 10))
      val intersection = face.intersectedBy(fold2)
      assertTrue(intersection == PARTIAL_EDGE)
   }
   @Test
   fun foldLineGoesFromAVertexToASide_ShouldBeConsideredIntersecting(){
      val face = Face(V(10, 10, 10), V(-10, 10, 10), V(10, 10, -10))
      val segmentFromVertexToSide = LineSegment(Point(10, 10, 10), Point(0, 10, 0))
      val intersectionType = face.intersectedBy(segmentFromVertexToSide)
      assertThat(intersectionType, `is`(ACROSS))
   }
   /**
    * In this example the segment does not go exactly between two points of the face, but from points outside of it
    * and it bisects the face
    */
   @Test
   fun foldLinePassesByVertexAndASide_ShouldBeConsideredIntersecting(){
      val face = Face(V(10, 10, 10), V(-10, 10, 10), V(10, 10, -10))
      val segmentFromVertexToSide = LineSegment(Point(11, 10, 11), Point(-1, 10, -1))
      val intersectionType = face.intersectedBy(segmentFromVertexToSide)
      assertThat(intersectionType, `is`(ACROSS))
   }
   @Test
   fun foldSegment_WhenIntersectsASideAndEntersTheFace_ShouldReturnPartialIntersection(){
      val face = Face(V(0, 0, 0), V(1, 0, 0), V(1, 1, 0))
      val pointOutsideFace = Point(2, 0.5, 0)
      val pointInsideFace = Point(0.8, 0.5, 0)
      val segment = LineSegment(pointOutsideFace, pointInsideFace)
      assertThat(face.intersectedBy(segment), `is`(PARTIAL))
   }
   @Test
   fun foldSegment_WhenTouchesASideButDoesNotEnterTheFace_ShouldReturnNoIntersection(){
      val face = Face(V(0, 0, 0), V(1, 0, 0), V(1, 1, 0))
      val pointOutsideFace = Point(2, 0.5, 0)
      val pointAtBoundary = Point(1, 0.5, 0)
      val segment = LineSegment(pointOutsideFace, pointAtBoundary)
      assertThat(face.intersectedBy(segment), `is`(NONE))
   }
   @Test
   fun foldSegment_WhenSegmentInsideFaceAndTouchesASide_ShouldReturnPartialIntersection(){
      val face = Face(V(0, 0, 0), V(1, 0, 0), V(1, 1, 0))
      val pointInsideFace = Point(0.9, 0.5, 0)
      val pointAtBoundary = Point(1, 0.5, 0)
      val segment = LineSegment(pointInsideFace, pointAtBoundary)
      assertThat(face.intersectedBy(segment), `is`(PARTIAL))
   }
   @Test
   fun foldSegment_WhenSegmentInsideFaceDoesNotTouchSides_ShouldReturnPartialIntersection(){
      val face = Face(V(0, 0, 0), V(1, 0, 0), V(1, 1, 0))
      val pointInsideFace = Point(0.9, 0.5, 0)
      val pointAtBoundary = Point(0.95, 0.5, 0)
      val segment = LineSegment(pointInsideFace, pointAtBoundary)
      assertThat(face.intersectedBy(segment), `is`(PARTIAL))
   }
}