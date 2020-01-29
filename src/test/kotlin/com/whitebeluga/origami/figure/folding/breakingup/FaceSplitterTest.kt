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

package com.whitebeluga.origami.figure.folding.breakingup

import com.moduleforge.libraries.geometry._3d.LineSegment
import com.moduleforge.libraries.geometry._3d.Plane.planeFromOrderedPoints
import com.moduleforge.libraries.geometry._3d.Point
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.Vertex
import com.whitebeluga.origami.figure.folding.SideOfFold.NEGATIVE
import com.whitebeluga.origami.figure.folding.SideOfFold.POSITIVE
import org.hamcrest.CoreMatchers.hasItem
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.whitebeluga.origami.figure.component.TestVertex as V

class FaceSplitterTest {
   lateinit var triangle: Face
   lateinit var intersectsTriangleVertexToSide: LineSegment
   lateinit var intersectsTriangleSideToSide: LineSegment
   lateinit var quadrilateral: Face
   lateinit var intersectsQuadVertexToVertex: LineSegment
   lateinit var expectedSplitIntersectedTriangleVertexToSide: Pair<List<Point>, List<Point>>
   lateinit var expectedSplitIntersectedTriangleSideToSide: Pair<List<Point>, List<Point>>
   lateinit var expectedSplitIntersectedQuadVertexToVertex: Pair<List<Point>, List<Point>>
   private lateinit var intersectedTriangleVertex: Vertex

   @Before
   fun setUp(){
      intersectedTriangleVertex = V(1, 1, -1)
      triangle = Face(V(1,0,1), V(-1, 0, -1), intersectedTriangleVertex)
      intersectsTriangleVertexToSide = LineSegment(intersectedTriangleVertex, Point(0, 0, 0))
      intersectsTriangleSideToSide = LineSegment(Point(0, 0.5, -1), Point(0, 0, 0))
      quadrilateral = Face(V(1,0,1), V(-1, 0, -1), V(1, 1, -1), V(3, 1, 1))
      intersectsQuadVertexToVertex = LineSegment(Point(1,0,1), Point(1, 1, -1))
      expectedSplitIntersectedTriangleVertexToSide = Pair(
              listOf(Point(1,0,1), Point(0, 0, 0), Point(1, 1, -1)),
              listOf(Point(0, 0, 0), Point(-1, 0, -1), Point(1, 1, -1)) )
      expectedSplitIntersectedTriangleSideToSide = Pair(
              listOf(Point(0,0,0), Point(-1, 0, -1), Point(0, 0.5, -1)),
              listOf(Point(0, 0, 0), Point(0, 0.5, -1), Point(1, 1, -1), Point(1, 0, 1)) )
      expectedSplitIntersectedQuadVertexToVertex = Pair(
              listOf(Point(1, 0, 1), Point(-1, 0, -1), Point(1, 1, -1) ),
              listOf(Point(1, 0, 1), Point(1, 1, -1), Point(3, 1, 1) ) )
   }
   @Test
   fun triangleIntersectedBetweenVertexAndSide_PointStructureOfSplitFacesCalculated(){
      val splitter = FaceSplitter(triangle, intersectsTriangleVertexToSide, planeFromOrderedPoints(triangle.vertices))
      val splitFaces = splitter.splitFace().newFaces
      val expected1 = expectedSplitIntersectedTriangleVertexToSide.first
      val expected2 = expectedSplitIntersectedTriangleVertexToSide.second
      val firstSplitFaceAsExpected =
              splitFaces[POSITIVE]!!.hasSamePointStructure(expected1) || splitFaces[POSITIVE]!!.hasSamePointStructure(expected2)
      assertTrue(firstSplitFaceAsExpected)
      val secondSplitFaceAsExpected =
              splitFaces[NEGATIVE]!!.hasSamePointStructure(expected1) || splitFaces[NEGATIVE]!!.hasSamePointStructure(expected2)
      assertTrue(secondSplitFaceAsExpected)
   }
   @Test
   fun triangleIntersectedSideToSide_PointStructureOfSplitFacesCalculated(){
      val splitter = FaceSplitter(triangle, intersectsTriangleSideToSide, planeFromOrderedPoints(triangle.vertices))
      val splitFaces = splitter.splitFace().newFaces
      val expected1 = expectedSplitIntersectedTriangleSideToSide.first
      val expected2 = expectedSplitIntersectedTriangleSideToSide.second
      val firstSplitFaceAsExpected =
              splitFaces[POSITIVE]!!.hasSamePointStructure(expected1) || splitFaces[POSITIVE]!!.hasSamePointStructure(expected2)
      assertTrue(firstSplitFaceAsExpected)
      val secondSplitFaceAsExpected =
              splitFaces[NEGATIVE]!!.hasSamePointStructure(expected1) || splitFaces[NEGATIVE]!!.hasSamePointStructure(expected2)
      assertTrue(secondSplitFaceAsExpected)
   }
   @Test
   fun quadrilateralIntersectedVertexToVertex_PointStructureOfSplitFacesCalculated(){
      val planePoints = quadrilateral.vertices.take(3)
      val splitter = FaceSplitter(quadrilateral, intersectsQuadVertexToVertex, planeFromOrderedPoints(planePoints))
      val splitFaces = splitter.splitFace().newFaces
      val expected1 = expectedSplitIntersectedQuadVertexToVertex.first
      val expected2 = expectedSplitIntersectedQuadVertexToVertex.second
      val firstSplitFaceAsExpected =
              splitFaces[POSITIVE]!!.hasSamePointStructure(expected1) || splitFaces[POSITIVE]!!.hasSamePointStructure(expected2)
      assertTrue(firstSplitFaceAsExpected)
      val secondSplitFaceAsExpected =
              splitFaces[NEGATIVE]!!.hasSamePointStructure(expected1) || splitFaces[NEGATIVE]!!.hasSamePointStructure(expected2)
      assertTrue(secondSplitFaceAsExpected)
   }
   /**
    * does not create unnecessary new point
    */
   @Test
   fun whenTriangleIntersectedBetweenVertexAndSide_ShouldKeepTheSameIntersectedVertexObjectInTheNewFaces(){
      val splitter = FaceSplitter(triangle, intersectsTriangleVertexToSide, planeFromOrderedPoints(triangle.vertices))
      val splitFaces = splitter.splitFace().newFaces
      assertThat(splitFaces[POSITIVE]!!.vertices, hasItem(intersectedTriangleVertex))
      assertThat(splitFaces[NEGATIVE]!!.vertices, hasItem(intersectedTriangleVertex))
   }
}