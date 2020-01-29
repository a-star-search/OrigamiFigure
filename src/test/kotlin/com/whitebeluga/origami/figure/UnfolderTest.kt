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

import com.moduleforge.libraries.geometry._3d.Line.linePassingBy
import com.moduleforge.libraries.geometry._3d.LineSegment
import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Point
import com.moduleforge.libraries.geometry._3d.Point.midPoint
import com.whitebeluga.origami.figure.Bundle.Unfolder.Companion.areUnfoldedFaces
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.Vertex
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class UnfolderTest {
   private lateinit var xyPlane_NormalTowardsZPositive: Plane
   private lateinit var bottomLeft: Vertex
   private lateinit var bottomRight: Vertex
   private lateinit var topRight: Vertex
   private lateinit var topLeft: Vertex
   private lateinit var foldSegmentDividesSquareIntoLeftAndRightSides: LineSegment
   private lateinit var foldSegmentDividesSquareFromTopLeftToBottomRight: LineSegment
   private lateinit var bottomMiddle: Vertex
   private lateinit var topMiddle: Vertex
   //Square and other faces from dividing the square
   private lateinit var square: Face
   // it is a quadrilateral
   private lateinit var leftSideOfSquareClockwise: Face
   // it is a quadrilateral
   private lateinit var rightSideOfSquareClockwise: Face
   // it is a triangle
   private lateinit var bottomLeftSideOfSquare: Face
   // it is a triangle
   private lateinit var topRightSideOfSquare: Face

   //Triangle and faces from dividing it
   private lateinit var triangle: Face
   private lateinit var foldSegmentDividesTriangleFromTopToBottomMiddle: LineSegment
   private lateinit var triangleLeftSide: Face
   private lateinit var triangleRightSide: Face

   @Before
   fun setUp(){
      val antiClockwisePointsAsSeenFromZPositive =
              listOf(Point(0, 0, 0), Point(1, 0, 0), Point(1, 1, 0))
      xyPlane_NormalTowardsZPositive = Plane.planeFromOrderedPoints(antiClockwisePointsAsSeenFromZPositive)
      bottomLeft = Vertex(0.0, 0.0, 0.0)
      bottomRight = Vertex(1.0, 0.0, 0.0)
      topRight = Vertex(1.0, 1.0, 0.0)
      topLeft = Vertex(0.0, 1.0, 0.0)
      square = Face(bottomLeft, bottomRight, topRight, topLeft)
      bottomMiddle = Vertex(midPoint(bottomLeft, bottomRight))
      topMiddle = Vertex(midPoint(topLeft, topRight))
      foldSegmentDividesSquareIntoLeftAndRightSides = LineSegment(bottomMiddle, topMiddle)
      foldSegmentDividesSquareFromTopLeftToBottomRight = LineSegment(topLeft, bottomRight)

      leftSideOfSquareClockwise = Face(bottomMiddle, bottomLeft, topLeft, topMiddle)
      rightSideOfSquareClockwise = Face(topRight, bottomRight, bottomMiddle, topMiddle)
      bottomLeftSideOfSquare = Face(topLeft, bottomRight, bottomLeft)
      topRightSideOfSquare = Face(topLeft, topRight, bottomRight)

      triangle = Face(topLeft, bottomLeft, bottomRight)
      foldSegmentDividesTriangleFromTopToBottomMiddle = LineSegment(topLeft, bottomMiddle)
      triangleLeftSide = Face(topLeft, bottomLeft, bottomMiddle)
      triangleRightSide = Face(topLeft, bottomMiddle, bottomRight)
   }
   @Test
   fun twoFacesAtSameSideOfTheLine_CannotBeUnfolded() {
      val anotherTriangle = Face(topLeft, bottomLeft, bottomMiddle)
      assertFalse(areUnfoldedFaces(triangle, anotherTriangle, linePassingBy(topLeft, bottomLeft)))
   }
   @Test
   fun leftAndRightSidesOfTriangle_CanBeUnfolded() {
      assertTrue(areUnfoldedFaces(triangleLeftSide, triangleRightSide, foldSegmentDividesTriangleFromTopToBottomMiddle.line))
   }
   /**
    * If the direction of two faces (given by the order of the vertices) point to opposite directions,
    * the faces cannot be joined.
    *
    * But because in this example the faces are articulated along a side and on different sides of the fold line, not
    * only it is not a valid pair of unfolded faces, but it is also
    * an invalid state in the application! (if you think about it, it cannot happen)
    *
    * An assertion error is thrown.
    */
   @Test(expected = AssertionError::class)
   fun leftSideAndReversedRightSideOfTriangle_AssertionErrorThrown() {
      val reversedRightTriangle = Face(triangleRightSide.vertices.asReversed())
      areUnfoldedFaces(triangleLeftSide, reversedRightTriangle, foldSegmentDividesTriangleFromTopToBottomMiddle.line)
   }
   @Test
   fun unfoldLeftAndRightSidesOfTriangle() {
      val unfolder = Bundle.Unfolder(triangleLeftSide, triangleRightSide)
      val joined = unfolder.joinUnfoldedFaces()
      val joinedVertices = joined.verticesStartingWith(topLeft)
      val expectedTriangleVertices = triangle.verticesStartingWith(topLeft)
      assertEquals(joinedVertices, expectedTriangleVertices)
   }
   @Test
   fun unfoldLeftAndRightSidesOfSquare() {
      val unfolder = Bundle.Unfolder(leftSideOfSquareClockwise, rightSideOfSquareClockwise)
      val joined = unfolder.joinUnfoldedFaces()
      val joinedVertices = joined.verticesStartingWith(topLeft)
      val expectedSquareVertices = listOf(topLeft, topRight, bottomRight, bottomLeft)
      assertEquals(joinedVertices, expectedSquareVertices)
   }
}