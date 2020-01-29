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

import com.moduleforge.libraries.geometry._3d.Line
import com.moduleforge.libraries.geometry._3d.Line.linePassingBy
import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Plane.planeFromOrderedPoints
import com.moduleforge.libraries.geometry._3d.Point
import com.moduleforge.libraries.geometry._3d.Point.midPoint
import com.whitebeluga.origami.figure.Bundle
import com.whitebeluga.origami.figure.Figure
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.TestVertex
import com.whitebeluga.origami.figure.component.Vertex
import com.whitebeluga.origami.figure.folding.SideOfFold.Companion.calculateSideOfFold
import com.whitebeluga.origami.figure.folding.SideOfFold.NEGATIVE
import com.whitebeluga.origami.figure.folding.SideOfFold.POSITIVE
import com.whitebeluga.origami.figure.folding.breakingup.bisectedfacefindingandfaceseparation.parameters.SeparatedBisectedBundle
import com.whitebeluga.origami.figure.folding.exceptions.InvalidFoldException
import org.hamcrest.CoreMatchers.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BundleSeparatorTest {
   private lateinit var xyPlane_NormalTowardsZPositive: Plane
   private lateinit var bottomLeft: Vertex
   private lateinit var bottomRight: Vertex
   private lateinit var topRight: Vertex
   private lateinit var topLeft: Vertex
   /** just a face */
   private lateinit var face: Face
   private lateinit var square: Face
   private lateinit var bundleWithASquare: Bundle

   @Before
   fun setUp() {
      val antiClockwisePointsAsSeenFromZPositive =
              listOf(Point(0, 0, 0), Point(1, 0, 0), Point(1, 1, 0))
      xyPlane_NormalTowardsZPositive = planeFromOrderedPoints(antiClockwisePointsAsSeenFromZPositive)
      bottomLeft = TestVertex(0, 0)
      bottomRight = TestVertex(1, 0)
      topRight = TestVertex(1, 1)
      topLeft = TestVertex(0, 1)
      face = Face(bottomLeft, bottomRight, topLeft)
      square = Face(bottomLeft, bottomRight, topRight, topLeft)
      bundleWithASquare = Bundle(xyPlane_NormalTowardsZPositive, square)
   }
   @Test
   fun separateBundles_WhenNoOtherBundles_ShouldReturnEmptySet() {
      val figure = Figure(bundleWithASquare)
      val foldLine = linePassingBy(midPoint(bottomLeft, bottomRight), midPoint(topLeft, topRight))
      val sideToPoints = mapOf(
            POSITIVE to face.vertices.filter { calculateSideOfFold(foldLine, it, bundleWithASquare.plane) == POSITIVE}.toSet(),
            NEGATIVE to face.vertices.filter { calculateSideOfFold(foldLine, it, bundleWithASquare.plane) == NEGATIVE}.toSet() )
      val separatedBisectedBundle = SeparatedBisectedBundle(bundleWithASquare, emptyMap(), sideToPoints)
      val separator = BundleSeparator(separatedBisectedBundle, emptySet(), figure)
      val separatedBundles = separator.separateBundles()
      assertTrue(separatedBundles.isEmpty())
   }
   @Test(expected = InvalidFoldException::class)
   fun separateBundles_WhenOneOtherBundleConnectedAtBothSidesOfFoldSegment_ShouldThrowException() {
      val anotherFaceHingesAtBottom = Face(bottomLeft, bottomRight, TestVertex(1, 1, 1))
      val anotherBundleHingesAtBottom = Bundle(xyPlane_NormalTowardsZPositive, anotherFaceHingesAtBottom)
      val foldLineDividesLeftFromRight = linePassingBy(midPoint(bottomLeft, bottomRight), midPoint(topLeft, topRight))
      fun sideOfFold(p: Point) =
              calculateSideOfFold(foldLineDividesLeftFromRight, p, xyPlane_NormalTowardsZPositive)
      val separatedBisectedBundle = SeparatedBisectedBundle(bundleWithASquare, emptyMap(),
                 mapOf(
                         POSITIVE to face.vertices.filter { sideOfFold(it) == POSITIVE}.toSet(),
                         NEGATIVE to face.vertices.filter { sideOfFold(it) == NEGATIVE}.toSet()
                 ) )
      val figure = Figure(setOf(bundleWithASquare, anotherBundleHingesAtBottom))
      val separator = BundleSeparator(separatedBisectedBundle, setOf(anotherBundleHingesAtBottom), figure)
      separator.separateBundles()
   }
   @Test
   fun separateBundles_WhenOneOtherBundle_ShouldReturnBundleOnTheCorrectSide() {
      val anotherFaceHingesAtBottom = Face(bottomLeft, bottomRight, TestVertex(1, 1, 1))
      val anotherBundleHingesAtBottom = Bundle(xyPlane_NormalTowardsZPositive, anotherFaceHingesAtBottom)
      val foldLineDividesBottomFromTop = linePassingBy(midPoint(bottomLeft, topLeft), midPoint(bottomRight, topRight))
      fun sideOfFold(p: Point) =
              calculateSideOfFold(foldLineDividesBottomFromTop, p, xyPlane_NormalTowardsZPositive)
      val separatedBisectedBundle = SeparatedBisectedBundle(bundleWithASquare, emptyMap(),
              mapOf(
                      POSITIVE to face.vertices.filter { sideOfFold(it) == POSITIVE}.toSet(),
                      NEGATIVE to face.vertices.filter { sideOfFold(it) == NEGATIVE}.toSet()
              ) )
      val figure = Figure(setOf(bundleWithASquare, anotherBundleHingesAtBottom))
      val separator = BundleSeparator(separatedBisectedBundle, setOf(anotherBundleHingesAtBottom), figure)
      val separatedBundles = separator.separateBundles()
      assertThat(separatedBundles.size, `is`(1)) //only one entry in the map
      val values = separatedBundles.values.first()
      assertThat(values.size, `is`(1)) //only one bundle value at only one side
      val value = values.first()
      assertThat(value, `is`(anotherBundleHingesAtBottom))
      val expectedSideOfFold = sideOfFold(bottomLeft) //the other bundle hinges at the two bottom vertices
      val keySide = separatedBundles.keys.first()
      assertThat(keySide, `is`(expectedSideOfFold))
   }
   @Test
   fun separateBundles_WhenBundleConnectedToBisectedBundle_AndYetAnotherBundleConnectedToIt_ShouldReturnTwoBundlesOnTheSameCorrectSide() {
      //make figure
      val chainedBundlesAtBottom = makeTwoChainedBundlesHingingAtBottomOfSquare()
      val anotherBundleHingesAtBottom = chainedBundlesAtBottom.first
      val yetAnotherBundle = chainedBundlesAtBottom.second
      //separate bundles
      val foldLineDividesBottomFromTop = linePassingBy(midPoint(bottomLeft, topLeft), midPoint(bottomRight, topRight))
      fun sideOfFold(p: Point) =
              calculateSideOfFold(foldLineDividesBottomFromTop, p, xyPlane_NormalTowardsZPositive)
      val separatedBisectedBundle = makeSeparatedBisectedBundle(bundleWithASquare, foldLineDividesBottomFromTop)
      val figure = Figure(setOf(bundleWithASquare, anotherBundleHingesAtBottom, yetAnotherBundle))
      val restOfBundles = figure.bundles - bundleWithASquare
      val separator = BundleSeparator(separatedBisectedBundle, restOfBundles, figure)
      val separatedBundles = separator.separateBundles()
      //assertions
      assertThat(separatedBundles.size, `is`(1)) //only one entry in the map because there is only one side for the rest of the bundles
      val values = separatedBundles.values.first()
      assertThat(values.size, `is`(2)) //two bundles at only one side
      assertThat(values, hasItems(anotherBundleHingesAtBottom, yetAnotherBundle))
      val expectedSideOfFold = sideOfFold(bottomLeft) //the other bundles hinges at the two bottom vertices
      val keySide = separatedBundles.keys.first()
      assertThat(keySide, `is`(expectedSideOfFold))
   }
   /**
    * Rather than an extra long method name, I'll describe the method.
    *
    * It's similar to the previous which was a bundle with a single face connected to the bisected bundle and yet
    * another bundle connected to the connected bundle. Also the face is not a triangle, but a square now.
    *
    * In this case we also add another bundle connected at the other side, so that there are two bundles
    * connected through one side and another bundle connected at the other side.
    */
   @Test
   fun separateBundles_ComplexCase1() {
      //make figure
      val anotherBundleHingesAtTop = makeBundleHingingAtTopOfSquare()
      val chainedBundlesAtBottom = makeTwoChainedBundlesHingingAtBottomOfSquare()
      val anotherBundleHingesAtBottom = chainedBundlesAtBottom.first
      val yetAnotherBundle = chainedBundlesAtBottom.second

      val figure = Figure(setOf(bundleWithASquare, anotherBundleHingesAtBottom, yetAnotherBundle, anotherBundleHingesAtTop))
      //separate bundles
      val foldLineDividesBottomFromTop = linePassingBy(midPoint(bottomLeft, topLeft), midPoint(bottomRight, topRight))
      fun sideOfFold(p: Point) =
              calculateSideOfFold(foldLineDividesBottomFromTop, p, xyPlane_NormalTowardsZPositive)
      val separatedBisectedBundle = makeSeparatedBisectedBundle(bundleWithASquare, foldLineDividesBottomFromTop)
      val restOfBundles = figure.bundles - bundleWithASquare
      val separator = BundleSeparator(separatedBisectedBundle, restOfBundles, figure)
      val separatedBundles = separator.separateBundles()
      //assertions
      assertThat(separatedBundles.size, `is`(2)) //two entries since there are bundles at both sides
      val bottomExpectedSideOfFold = sideOfFold(bottomLeft)!!
      val bundlesAtTheBottom = separatedBundles[bottomExpectedSideOfFold]!!
      val bundlesAtTheTop = separatedBundles[bottomExpectedSideOfFold.opposite()]!!
      assertThat(bundlesAtTheBottom, hasItems(anotherBundleHingesAtBottom, yetAnotherBundle))
      assertThat(bundlesAtTheTop, hasItem(anotherBundleHingesAtTop))
   }
   private fun makeBundleHingingAtTopOfSquare(): Bundle {
      val anotherFaceHingesAtTop = Face(topLeft, topRight, TestVertex(1, 1, 0.5), TestVertex(0, 1, 0.5))
      val planeBundleHingesAtTop = planeFromOrderedPoints(anotherFaceHingesAtTop.vertices.take(3))
      return Bundle(planeBundleHingesAtTop, anotherFaceHingesAtTop)
   }
   private fun makeTwoChainedBundlesHingingAtBottomOfSquare(): Pair<Bundle, Bundle> {
      val v1 = TestVertex(1, 1, 1)
      val v2 = TestVertex(0, 1, 1)
      val anotherFaceHingesAtBottom = Face(bottomLeft, bottomRight, v1, v2)
      val yetAnotherFace = Face(v1, v2, TestVertex(1, 1, 2))

      val planeBundleHingesAtBottom = planeFromOrderedPoints(anotherFaceHingesAtBottom.vertices.take(3))
      val anotherBundleHingesAtBottom = Bundle(planeBundleHingesAtBottom, anotherFaceHingesAtBottom)

      val planeYetAnotherBundle = planeFromOrderedPoints(yetAnotherFace.vertices.take(3))
      val yetAnotherBundle = Bundle(planeYetAnotherBundle, yetAnotherFace)

      return Pair(anotherBundleHingesAtBottom, yetAnotherBundle)
   }
   private fun makeSeparatedBisectedBundle(bundle: Bundle, foldLine: Line): SeparatedBisectedBundle {
      fun sideOfFold(p: Point) =
              calculateSideOfFold(foldLine, p, xyPlane_NormalTowardsZPositive)
      val verticesOfBisectedSquareOnPosSide = square.vertices.filter { sideOfFold(it) == POSITIVE}.toSet()
      val verticesOfBisectedSquareOnNegSide = square.vertices.filter { sideOfFold(it) == NEGATIVE}.toSet()
      return SeparatedBisectedBundle(bundle, emptyMap(),
              mapOf(POSITIVE to verticesOfBisectedSquareOnPosSide, NEGATIVE to verticesOfBisectedSquareOnNegSide ) )
   }
   /**
    * Check out the image with the name
    * separateBundles_WhenAnotherBundleConnectedToBisectedBundle_ThroughANonBisectedFaceInTheBisectedBundle
    * in the resources folder to understand what this function tests
    */
   @Test fun separateBundles_WhenAnotherBundleConnectedToBisectedBundle_ThroughANonBisectedFaceInTheBisectedBundle() {
      //make bundles and figure
      val leftMidPoint = midPoint(bottomLeft, topLeft)
      val rightMidPoint = midPoint(bottomRight, topRight)

      val leftQuarterPoint =  midPoint(topLeft, leftMidPoint)
      val rightQuarterPoint =  midPoint(topRight, rightMidPoint)

      val vertexAtLeftQuarterPoint = Vertex(leftQuarterPoint)
      val vertexAtRightQuarterPoint = Vertex(rightQuarterPoint)

      val aNonBisectedFaceInBisectedBundle = Face(topLeft, topRight, vertexAtRightQuarterPoint, vertexAtLeftQuarterPoint)

      val bisectedBundle =  Bundle(xyPlane_NormalTowardsZPositive,
              setOf(square, aNonBisectedFaceInBisectedBundle),
              mapOf(square to setOf(aNonBisectedFaceInBisectedBundle)))

      val connectedFaceThroughNonBisectedFace = Face(vertexAtLeftQuarterPoint, vertexAtRightQuarterPoint, Vertex(1.0, 1.0, 1.0))
      val connectedBundleThroughNonBisectedFace = Bundle(
              Plane.planeFromOrderedPoints(connectedFaceThroughNonBisectedFace.vertices.take(3)),
              connectedFaceThroughNonBisectedFace)
      val figure = Figure(setOf(bisectedBundle, connectedBundleThroughNonBisectedFace))
      //~make bundles and figure

      //fold line
      val foldLineDividesBottomFromTop = linePassingBy(leftMidPoint, rightMidPoint)

      //separate
      fun sideOfFold(p: Point) =
              calculateSideOfFold(foldLineDividesBottomFromTop, p, xyPlane_NormalTowardsZPositive)
      val verticesOfBisectedSquareOnPosSide = square.vertices.filter { sideOfFold(it) == POSITIVE}.toSet()
      val verticesOfBisectedSquareOnNegSide = square.vertices.filter { sideOfFold(it) == NEGATIVE}.toSet()
      val topSideOfFold = sideOfFold(topLeft)!!
      val separatedBisectedBundle = SeparatedBisectedBundle(bisectedBundle,
              mapOf(topSideOfFold to setOf(aNonBisectedFaceInBisectedBundle) ),
              mapOf(POSITIVE to verticesOfBisectedSquareOnPosSide, NEGATIVE to verticesOfBisectedSquareOnNegSide ) )
      val restOfBundles = figure.bundles - bisectedBundle
      val separator = BundleSeparator(separatedBisectedBundle, restOfBundles, figure)
      val separatedBundles = separator.separateBundles()

      //assertions
      assertThat(separatedBundles.size, `is`(1)) //just one entry, since there is a single bundle connected to the bisected bundle
      val bottomSideOfFold = sideOfFold(bottomLeft)!!
      val bundlesAtTheBottom = separatedBundles[bottomSideOfFold]
      assertNull(bundlesAtTheBottom)

      val bundlesAtTheTop = separatedBundles[topSideOfFold]!!
      // the other bundle should be connected through the top side
      assertThat(bundlesAtTheTop, `is`(setOf(connectedBundleThroughNonBisectedFace)))
   }
}