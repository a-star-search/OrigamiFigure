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
import com.moduleforge.libraries.geometry._3d.Point.midPoint
import com.whitebeluga.origami.figure.Bundle
import com.whitebeluga.origami.figure.Figure
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.Vertex
import com.whitebeluga.origami.figure.folding.SideOfFold.Companion.calculateSideOfFold
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import com.whitebeluga.origami.figure.component.TestVertex as V

/**
 * In this class we are only concerned with bundle separation part of the algorithm
 */
class AMAPBisectedFaceFinderAndFaceSeparator_BundleSeparation_Test {
   private lateinit var xyPlane_NormalTowardsZPositive: Plane
   private lateinit var xzPlane_NormalTowardsYPositive: Plane
   private lateinit var bottomLeft: Vertex
   private lateinit var bottomRight: Vertex
   private lateinit var topRight: Vertex
   private lateinit var topLeft: Vertex
   private lateinit var square: Face
   /**
    * These triangles constructed in such a way that segment bisecting top to bottom or vertex to vertex
    * (for any two vertices) of the
    * square won't  touch them. It simplifies the case and reduces the mental overload.
    */
   private lateinit var triangleAttachedToSquareOnLeftSide: Face
   private lateinit var triangleAttachedToSquareOnRightSide: Face
   private lateinit var foldSegmentDividesSquareIntoLeftAndRightSides: LineSegment
   private lateinit var foldSegmentDividesSquareFromTopLeftToBottomRight: LineSegment

   @Before
   fun setUp(){
      val antiClockwisePointsAsSeenFromZPositive =
              listOf(Point(0, 0, 0), Point(1, 0, 0), Point(1, 1, 0))
      xyPlane_NormalTowardsZPositive = planeFromOrderedPoints(antiClockwisePointsAsSeenFromZPositive)
      val antiClockwisePointsAsSeenFromYPositive = listOf( Point(0, 0, 0), Point(1, 0, 0), Point(1, 0, -1))
      xzPlane_NormalTowardsYPositive = planeFromOrderedPoints(antiClockwisePointsAsSeenFromYPositive)
      bottomLeft = V(0, 0, 0)
      bottomRight = V(1, 0, 0)
      topRight = V(1, 1, 0)
      topLeft = V(0, 1, 0)
      square = Face(bottomLeft, bottomRight, topRight, topLeft)
      triangleAttachedToSquareOnLeftSide = Face(bottomLeft, topLeft, V(0.01,0.5,0))
      triangleAttachedToSquareOnRightSide = Face(bottomRight, topRight, V(0.99,0.5,0))
      foldSegmentDividesSquareIntoLeftAndRightSides = LineSegment(midPoint(bottomLeft, bottomRight), midPoint(topLeft, topRight))
      foldSegmentDividesSquareFromTopLeftToBottomRight = LineSegment(topLeft, bottomRight)
   }
   /** draw this stuff on a paper for easy visualization */
   @Test
   fun testBundleSeparation_WhenOneBundleConnectedToBundleToBeFolded_ShouldReturnThatBundleOnRightSide() {
      val planeOfBisectedFace = xyPlane_NormalTowardsZPositive
      val verticesAtLeftSideOfBisection = listOf(V(0,1,0), V(0,0,0))
      val verticesAtRightSideOfBisection = listOf(V(1,0,0), V(1,1,0))
      val bisectedFace = Face(verticesAtLeftSideOfBisection + verticesAtRightSideOfBisection)
      val bundleToFold = Bundle(planeOfBisectedFace, bisectedFace)
      val faceInOtherBundle = Face(verticesAtRightSideOfBisection + V(1,0,1))
      val otherBundle = Bundle(xzPlane_NormalTowardsYPositive, faceInOtherBundle)
      val figure = Figure(setOf(bundleToFold, otherBundle))

      val foldSegmentBisectsBundleBetweenLeftAndRight = LineSegment(Point(0.5, -1, 0), Point(0.5, 2, 0))

      val userLookingDirection = planeOfBisectedFace.normal
      val amap = AMAPBisectedFaceFinderAndFaceSeparator(figure, bundleToFold, foldSegmentBisectsBundleBetweenLeftAndRight, userLookingDirection)

      val separated = amap.findBisectedAndSeparateTheRest()
      val restOfBundles = separated.mapOfSideToRestOfBundles

      assertThat(restOfBundles.entries.size, `is`(1))
      val separatedBundles = restOfBundles.values.first()
      assertThat(separatedBundles.size, `is`(1))
      val separatedBundle = separatedBundles.first()
      assertThat(separatedBundle, `is`(otherBundle))

      val sideOfSeparatedBundle = restOfBundles.keys.first()
      //side of the other bundle should be the same as the points on the right side of the bisected face, to which it is connected
      val expectedSide =
              calculateSideOfFold(foldSegmentBisectsBundleBetweenLeftAndRight.line,
                      verticesAtRightSideOfBisection.first(), planeOfBisectedFace)!!
      assertEquals(expectedSide, sideOfSeparatedBundle)
   }
}
