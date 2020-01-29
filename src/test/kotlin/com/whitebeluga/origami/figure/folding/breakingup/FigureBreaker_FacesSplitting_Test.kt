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
import com.moduleforge.libraries.geometry._3d.Point
import com.whitebeluga.origami.figure.Bundle
import com.whitebeluga.origami.figure.TestUtil.makeFigureFrom
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.folding.SideOfFold.NEGATIVE
import com.whitebeluga.origami.figure.folding.SideOfFold.POSITIVE
import com.whitebeluga.origami.figure.folding.breakingup.bisectedfacefindingandfaceseparation.parameters.SeparatedFaces
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import com.whitebeluga.origami.figure.component.TestVertex as V

/**
 * This class only tests the function of the class that split a set of faces
 */
class FigureBreaker_FacesSplitting_Test {
   private lateinit var xyPlane_NormalTowardsZPositive: Plane
   @Before
   fun setUp(){
      val antiClockwisePointsAsSeenFromZPositive = listOf( Point(0, 0, 0), Point(1, 0, 0), Point(1, 1, 0))
      xyPlane_NormalTowardsZPositive = Plane.planeFromOrderedPoints(antiClockwisePointsAsSeenFromZPositive)
   }
   @Test
   fun testSplitOneFace_ShouldReturnMapOfOneFacePerSideOfFoldSegment(){
      val face = Face(V(0, 0), V(1, 0), V(1, 1), V(0, 1))
      val figure = makeFigureFrom(face)
      val foldSegment = LineSegment(Point(-10, 0.5, 0), Point(10, 0.5, 0))
      val bundle = figure.bundles.first()
      val bisectedAndSeparated = SeparatedFaces.fromBisectedFace(face)
      val breaker = FigureBreaker(bundle, bisectedAndSeparated, foldSegment)
      val splitFaces= breaker.split(setOf(face))
      assertThat(splitFaces.size, `is`(1))
      assertThat(splitFaces.first().newFaces.size, `is`(2)) //a new face per side
   }
   @Test
   fun testSplitOneSquareSideToSide_BetweenTheTwoFacesReturnedThereShouldBeSixDifferentPoints(){
      val face = Face(V(0, 0), V(1, 0), V(1, 1), V(0, 1))
      val figure = makeFigureFrom(face)
      val foldSegment = LineSegment(Point(-10, 0.5, 0), Point(10, 0.5, 0))
      val bisectedAndSeparated = SeparatedFaces.fromBisectedFace(face)
      val bundle = figure.bundles.first()
      val breaker = FigureBreaker(bundle, bisectedAndSeparated, foldSegment)
      val splitFace= splitFace(breaker, face).newFaces
      val allUniqueVertices = (splitFace[POSITIVE]!!.vertices + splitFace[NEGATIVE]!!.vertices).toSet()
      assertThat(allUniqueVertices.size, `is`(6))
   }
   private fun splitFace(breaker: FigureBreaker, face: Face): SplitFace {
      val splitFaces = breaker.split(setOf(face))
      assert(splitFaces.size == 1)
      return splitFaces.first()
   }
   /** Draw this for easy visualization.
    *
    * This tests that there is no duplication of vertices when faces need be split.
    */
   @Test
   fun testSplitTwoConnectedSquaresSideToSide_BetweenTheTwoFacesReturnedThereShouldBeNineDifferentPoints(){
      val rightBottom = V(1, 0)
      val rightTop = V(1, 1)
      val frontFace = Face(V(0, 0), rightBottom, rightTop, V(0, 1))
      val backFace = Face(V(0, 0), rightBottom, rightTop, V(0, 1))
      val bundle = Bundle(xyPlane_NormalTowardsZPositive,
              setOf(frontFace, backFace), mapOf(frontFace to setOf(backFace)))
      val foldsSideToSide = LineSegment(Point(-10, 0.5, 0), Point(10, 0.5, 0))

      val bisectedFaces = setOf(frontFace, backFace)
      val bisectedAndSeparated = SeparatedFaces.fromBisectedFaces(bisectedFaces)
      val breaker = FigureBreaker(bundle, bisectedAndSeparated, foldsSideToSide)
      val splitFaces= breaker.split(bisectedFaces).toList()
      val splitFace1 = splitFaces[0]
      val splitFace2 = splitFaces[1]

      assertThat(setOf(splitFace1.originalFace, splitFace2.originalFace), `is`(setOf(frontFace, backFace)))

      val verticesOfNewFaces1 =
              splitFace1.newFaces[POSITIVE]!!.vertices + splitFace1.newFaces[NEGATIVE]!!.vertices
      val verticesOfNewFaces2 =
              splitFace2.newFaces[POSITIVE]!!.vertices + splitFace2.newFaces[NEGATIVE]!!.vertices

      val allUniqueVertices = (verticesOfNewFaces1 + verticesOfNewFaces2).toSet()
      assertThat(allUniqueVertices.size, `is`(9))
   }
}