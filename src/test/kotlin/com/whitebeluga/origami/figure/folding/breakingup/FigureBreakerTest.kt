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
import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Plane.planeFromOrderedPoints
import com.moduleforge.libraries.geometry._3d.Point
import com.moduleforge.libraries.geometry._3d.Point.midPoint
import com.whitebeluga.origami.figure.Bundle
import com.whitebeluga.origami.figure.Figure
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.Vertex
import com.whitebeluga.origami.figure.folding.SideOfFold.NEGATIVE
import com.whitebeluga.origami.figure.folding.SideOfFold.POSITIVE
import com.whitebeluga.origami.figure.folding.breakingup.bisectedfacefindingandfaceseparation.FFBisectedFaceFinderAndFaceSeparator
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.hasItems
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import com.whitebeluga.origami.figure.component.TestVertex as V

class FigureBreakerTest {
   private lateinit var xyPlane_NormalTowardsZPositive: Plane
   private lateinit var bottomLeft: Vertex
   private lateinit var bottomRight: Vertex
   private lateinit var topRight: Vertex
   private lateinit var topLeft: Vertex
   private lateinit var square: Face
   private lateinit var foldSegmentDividesSquareIntoLeftAndRightSides: LineSegment
   private lateinit var foldSegmentDividesSquareFromTopLeftToBottomRight: LineSegment

   @Before
   fun setUp(){
      val antiClockwisePointsAsSeenFromZPositive =
              listOf(Point(0, 0, 0), Point(1, 0, 0), Point(1, 1, 0))
      xyPlane_NormalTowardsZPositive = planeFromOrderedPoints(antiClockwisePointsAsSeenFromZPositive)
      bottomLeft = V(0, 0, 0)
      bottomRight = V(1, 0, 0)
      topRight = V(1, 1, 0)
      topLeft = V(0, 1, 0)
      square = Face(bottomLeft, bottomRight, topRight, topLeft)
      foldSegmentDividesSquareIntoLeftAndRightSides = LineSegment(midPoint(bottomLeft, bottomRight), midPoint(topLeft, topRight))
      foldSegmentDividesSquareFromTopLeftToBottomRight = LineSegment(topLeft, bottomRight)
   }
   /**
    * The simplest possible case, a single face figure broken up by the middle
    */
   @Test fun breakUpASquare_WhenFoldSegmentSideToSide_Calculated(){
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, square)
      val f = Figure(bundle)
      val lookingDirection = xyPlane_NormalTowardsZPositive.normal
      val fold = foldSegmentDividesSquareIntoLeftAndRightSides
      val bisectedAndSeparated = FFBisectedFaceFinderAndFaceSeparator(f, bundle, fold, lookingDirection)
                      .findBisectedAndSeparateTheRest()
      val breaker = FigureBreaker(bundle, bisectedAndSeparated, fold)
      val brokenUp = breaker.breakUpFigure()
      val nonBisectedBundles = brokenUp.mapOfSideToWholeBundles
      assertTrue(nonBisectedBundles.isEmpty())
      val splitBundleParts = brokenUp.mapOfSideToSplitBundlePart
      assertThat(splitBundleParts.size, `is`(2)) //a pos side and a neg side
      val posSideBundle = splitBundleParts[POSITIVE]!!
      val negSideBundle = splitBundleParts[NEGATIVE]!!
      //each new bundle has exactly one face
      assertThat(posSideBundle.faces.size, `is`(1))
      assertThat(negSideBundle.faces.size, `is`(1))
      val allVertices = posSideBundle.faces.map { it.vertices }.flatten().toSet() +
              negSideBundle.faces.map { it.vertices }.flatten().toSet()
      assertThat(allVertices.size, `is`(6))
      assertThat(allVertices, hasItems(*square.vertices.toTypedArray()))
   }
   @Test fun breakUpASquare_WhenFoldSegmentVertexToVertex_Calculated(){
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, square)
      val f = Figure(bundle)
      val lookingDirection = xyPlane_NormalTowardsZPositive.normal
      val fold = foldSegmentDividesSquareFromTopLeftToBottomRight
      val bisectedAndSeparated = FFBisectedFaceFinderAndFaceSeparator(f, bundle, fold, lookingDirection)
              .findBisectedAndSeparateTheRest()
      val breaker = FigureBreaker(bundle, bisectedAndSeparated, fold)
      val brokenUp = breaker.breakUpFigure()
      val nonBisectedBundles = brokenUp.mapOfSideToWholeBundles
      assertTrue(nonBisectedBundles.isEmpty())
      val splitBundleParts = brokenUp.mapOfSideToSplitBundlePart
      assertThat(splitBundleParts.size, `is`(2)) //a pos side and a neg side
      val posSideBundle = splitBundleParts[POSITIVE]!!
      val negSideBundle = splitBundleParts[NEGATIVE]!!
      //each new bundle has exactly one face
      assertThat(posSideBundle.faces.size, `is`(1))
      assertThat(negSideBundle.faces.size, `is`(1))
      val allVertices = posSideBundle.faces.map { it.vertices }.flatten().toSet() +
              negSideBundle.faces.map { it.vertices }.flatten().toSet()
      assertEquals(allVertices, square.vertices.toSet()) //in this case there are no new vertices created
   }
}