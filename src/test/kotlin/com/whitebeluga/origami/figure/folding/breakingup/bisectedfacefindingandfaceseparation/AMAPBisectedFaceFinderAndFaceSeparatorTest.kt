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
import com.whitebeluga.origami.figure.OrigamiBase.FISH
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.Vertex
import com.whitebeluga.origami.figure.folding.SideOfFold
import com.whitebeluga.origami.figure.folding.SideOfFold.Companion.calculateSideOfFold
import com.whitebeluga.origami.figure.folding.SideOfFold.NEGATIVE
import com.whitebeluga.origami.figure.folding.SideOfFold.POSITIVE
import com.whitebeluga.origami.figure.folding.breakingup.Util.findFishBaseRightSideFlap
import com.whitebeluga.origami.figure.folding.exceptions.InvalidFoldException
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.Math.*
import com.whitebeluga.origami.figure.component.TestVertex as V

class AMAPBisectedFaceFinderAndFaceSeparatorTest {
   private lateinit var xyPlane_NormalTowardsZPositive: Plane
   private lateinit var xyPlane_NormalTowardsZNegative: Plane
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
      xyPlane_NormalTowardsZNegative = planeFromOrderedPoints(antiClockwisePointsAsSeenFromZPositive.asReversed())
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
   @Test
   fun testSeparateFaces_WhenSingleFace_ShouldReturnJustOneBisectedFace(){
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, square)
      val figure = Figure(bundle)
      val amap = AMAPBisectedFaceFinderAndFaceSeparator(figure, bundle, foldSegmentDividesSquareIntoLeftAndRightSides, bundle.plane.normal)
      val separated = amap.findBisectedAndSeparateTheRest()
      assertTrue(separated.mapOfSideToWholeFaces.isEmpty())
      assertEquals(separated.bisected, setOf(square))
   }
   /** draw this stuff on a paper for easy visualization */
   @Test
   fun testSeparateFaces_WhenOneFaceBisectedAndOneFaceNotBisectedInSameBundle_AndNoSideToFoldSpecified_AndFoldSegmentSideToSide_Calculated() {
      val anotherFace = triangleAttachedToSquareOnLeftSide
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, setOf(square, anotherFace), mapOf(square to setOf(anotherFace)))
      val figure = Figure(bundle) //create a figure, so that the different elements are properly initialized
      val userLookingDirection = bundle.normal
      val foldSegment = foldSegmentDividesSquareIntoLeftAndRightSides
      val amap = AMAPBisectedFaceFinderAndFaceSeparator(figure, bundle, foldSegment , userLookingDirection)
      val bisectedAndSeparated = amap.findBisectedAndSeparateTheRest()
      val bisected = bisectedAndSeparated.bisected
      val expectedBisected = setOf(square)
      assertThat(bisected, `is`(expectedBisected))
      val leftSide = calculateSideOfFold(foldSegment.line, bottomLeft, xyPlane_NormalTowardsZPositive)
      val expectedNonBisectedFacesOfLeftSide = setOf(anotherFace)
      assertThat(bisectedAndSeparated.mapOfSideToWholeFaces[leftSide], `is`(expectedNonBisectedFacesOfLeftSide))
      assertThat(bisectedAndSeparated.mapOfSideToWholeFaces.size, `is`(1)) // only one entry, since there is only one side with non-bisected faces
   }
   @Test
   fun testSeparateFaces_WhenOneFaceBisectedAndOneFaceNotBisectedInSameBundle_AndNoSideToFoldSpecified_AndFoldSegmentVertexToVertex_Calculated() {
      val anotherFace = triangleAttachedToSquareOnRightSide
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, setOf(square, anotherFace), mapOf(square to setOf(anotherFace)))
      val figure = Figure(bundle) //create a figure, so that the different elements are properly initialized
      val userLookingDirection = bundle.normal
      val foldSegment = foldSegmentDividesSquareFromTopLeftToBottomRight
      val amap = AMAPBisectedFaceFinderAndFaceSeparator(figure, bundle, foldSegment, userLookingDirection)
      val bisectedAndSeparated = amap.findBisectedAndSeparateTheRest()
      val bisected = bisectedAndSeparated.bisected
      val expectedBisected = setOf(square)
      assertThat(bisected, `is`(expectedBisected))
      val rightSide = calculateSideOfFold(foldSegment.line, topRight, xyPlane_NormalTowardsZPositive)
      val expectedNonBisectedFacesOfRightSide = setOf(anotherFace)
      assertThat(bisectedAndSeparated.mapOfSideToWholeFaces[rightSide], `is`(expectedNonBisectedFacesOfRightSide))
      assertThat(bisectedAndSeparated.mapOfSideToWholeFaces.size, `is`(1)) // only one entry, since there is only one side with non-bisected faces
   }
   /**
    * Take a look at: AsManyAsPossibleFaceSeparatorTest@testSeparateFaces_ComplexCase1.png
    */
   @Test
   fun testSeparateFaces_ComplexCase1(){
      val bottomLeft = V(0, 0)
      val bottomRight = V(1, 0)
      val v2 = V(0, 0.1)
      val v3 = V(1, 0.1)
      val faceAtBottom = Face(bottomLeft, bottomRight, v3, v2)
      val topLeft = V(0, 1)
      val topRight = V(1, 1)
      val topLeftBis = V(0, 1)
      val topRightBis = V(1, 1)
      val v6 = V(1, 0.9)
      val v7 = V(0, 0.9)
      val bottomRightAtTheBack = V(1, 0)
      val bottomLeftAtTheBack = V(0, 0)
      val v10 = V(1, 0.1)
      val v11 = V(0, 0.1)
      val faceB = Face(bottomLeft, bottomRight, topRight, topLeft)
      val faceC = Face(topRight, topLeft, v7, v6)
      val faceD = Face(topRightBis, topLeftBis, v7, v6)
      val faceE = Face(topRightBis, topLeftBis, bottomLeftAtTheBack, bottomRightAtTheBack)
      val faceAtBottomAndAtTheBack = Face(bottomLeftAtTheBack, bottomRightAtTheBack, v10, v11)
      //listOf(Layer(faceAtBottom), Layer(faceB) , Layer(faceE) , Layer(faceD, faceAtBottomAndAtTheBack), Layer(faceC))
      val bundle = Bundle(xyPlane_NormalTowardsZPositive,
              setOf(faceAtBottom, faceB, faceC, faceD, faceE, faceAtBottomAndAtTheBack),
              mapOf(  faceAtBottom to setOf(faceB, faceE, faceAtBottomAndAtTheBack),
                      faceB to setOf(faceC, faceD, faceE, faceAtBottomAndAtTheBack),
                      faceD to setOf(faceC),
                      faceE to setOf(faceC, faceD) ) )
      val complexFigure = Figure(bundle)
      val foldSegment = LineSegment(Point(0, 0.5, 0), Point(1, 0.5, 0))
      val amap = AMAPBisectedFaceFinderAndFaceSeparator(complexFigure, bundle, foldSegment, bundle.plane.normal)
      val bisectedAndSeparated = amap.findBisectedAndSeparateTheRest()
      val bisected = bisectedAndSeparated.bisected
      val expectedBisected = setOf(faceB, faceE)
      assertThat(bisected, `is`(expectedBisected))
      val restOfBundles = bisectedAndSeparated.mapOfSideToRestOfBundles
      assertTrue(restOfBundles.isEmpty())
      val sideAtBottom = SideOfFold.calculateSideOfFold(foldSegment.line, bottomLeft, xyPlane_NormalTowardsZPositive)
      val notBisected = bisectedAndSeparated.mapOfSideToWholeFaces
      val notBisectedAtTheBottom = notBisected[sideAtBottom]
      assertThat(notBisectedAtTheBottom, `is`(setOf(faceAtBottom, faceAtBottomAndAtTheBack)))
      val notBisectedAtTheTop = notBisected[sideAtBottom!!.opposite()]
      assertThat(notBisectedAtTheTop, `is`(setOf(faceC, faceD)))
   }
   /**
    * When folding the top of the tip of a kite base
    * The two front flaps are not bisected on one side, on the other side there are no non-bisected faces
    * There is a single bisected face, which is the big face behind the flaps and the face whose top tip we are
    * folding.
    */
   @Test
   fun whenFoldAcrossTheTopTipOfKiteBase_Calculated() {
      //make the kite
      val sideLength = 1.0
      val sqrt_2 = sqrt(2.0)
      val top = V(0, sideLength/sqrt_2, 0)
      val bottom = V(0, -sideLength/sqrt_2, 0)
      val halfFigureWidth = tan(PI/8.0) * sideLength
      val right = V(halfFigureWidth, sideLength - sideLength/sqrt_2, 0)
      val left = V(-halfFigureWidth, sideLength - sideLength/sqrt_2, 0)
      val backFace = Face(top, left, bottom, right)
      val crossPoint = V(0, sideLength - sideLength/sqrt_2, 0)
      val leftFlap = Face(left, crossPoint, bottom)
      val rightFlap = Face(V(crossPoint), right, bottom)
      val kiteBundle = Bundle(xyPlane_NormalTowardsZPositive, setOf(backFace, rightFlap, leftFlap),
              mapOf( backFace to setOf(rightFlap, leftFlap)) )
      val kite = Figure(kiteBundle)
      //~make the kite

      //fold segment
      val farXRight = sideLength*10
      val farXLeft = -sideLength*10
      val yCloseToTheTip = midPoint(top, crossPoint).y()
      val foldThatCutsOffTip = LineSegment(Point(farXLeft, yCloseToTheTip,0), Point(farXRight, yCloseToTheTip,0))
      //~fold segment

      //user looking direction is irrelevant in this example
      val amap = AMAPBisectedFaceFinderAndFaceSeparator(kite, kiteBundle, foldThatCutsOffTip, xyPlane_NormalTowardsZPositive.normal)
      val separated = amap.findBisectedAndSeparateTheRest()

      //assertions
      assertEquals(separated.bisected, setOf(backFace))

      val smallerSetOfSeparatedFaces =
              min(separated.mapOfSideToWholeFaces[POSITIVE]?.size ?: 0, separated.mapOfSideToWholeFaces[NEGATIVE]?.size ?: 0)
      val biggerSetOfSeparatedFaces =
              max(separated.mapOfSideToWholeFaces[POSITIVE]?.size ?: 0, separated.mapOfSideToWholeFaces[NEGATIVE]?.size ?: 0)
      assertThat(smallerSetOfSeparatedFaces, `is`(0))
      assertThat(biggerSetOfSeparatedFaces, `is`(2))

      val separatedFaces = separated.mapOfSideToWholeFaces.values.first() //we only expect one entry
      val flaps = setOf(leftFlap, rightFlap)
      assertThat(separatedFaces, `is`(flaps))
   }
   /**
    * As if you take the right flap of a fish base (both flaps pointing up) and fold it to point downwards
    *
    * Should Return The Hidden Face Of The Flap On One Side And All Other Faces On The Other Side
    *
    */
   @Test
   fun whenAsIfFoldingRightFlapOfFishBaseTheOtherWay_Calculated(){
      val sideLength = 1.0
      val fish = FISH.make(sideLength)
      val x1 = 0.0
      val x2 = 1.0 - (1.0 / sqrt(2.0))
      val foldsFlap = LineSegment(Point(x1, 0.0, 0.0), Point(x2, 0.0, 0.0))
      //in this case the user looking direction if of importance
      // the fish base bundle normal is goes towards z-
      //and the face at the bottom of the bundle, the one that has all other faces above it,
      // is the big face on the side with no flaps
      // therefore the user that folds a flap must be looking towards z+
      val userLookingDirection = xyPlane_NormalTowardsZPositive.normal
      val bundle = fish.bundles.first()
      val amap = AMAPBisectedFaceFinderAndFaceSeparator(fish, bundle, foldsFlap, userLookingDirection)
      val separated = amap.findBisectedAndSeparateTheRest()
      assertThat(separated.bisected.size, `is`(1)) //only one face is really bisected
      assertTrue(separated.mapOfSideToRestOfBundles.isEmpty())
      val smallerSetOfNonBisectedFaces = separated.mapOfSideToWholeFaces.values.minBy { it.size }!!
      assertThat(smallerSetOfNonBisectedFaces.size, `is`(1)) //only one other non bisected face in the "flap"
      val biggerSetOfNonBisectedFaces = separated.mapOfSideToWholeFaces.values.maxBy { it.size }!!
      assertThat(biggerSetOfNonBisectedFaces.size, `is`(5)) //all the other 5 of the 7 seven faces of the fish base are not part of the flap
      val rightSideFlap = findFishBaseRightSideFlap(fish)
      assertThat(separated.bisected.first(), `is`(rightSideFlap))
   }
   @Test(expected = InvalidFoldException::class)
   fun foldSegmentAsIfFoldingRightFlapOfFishBaseTheOtherWay_ButAppliedToTheOtherSide_ShouldThrowException(){
      val sideLength = 1.0
      val fish = FISH.make(sideLength)
      val x1 = 0.0
      val x2 = 1.0 - (1.0 / sqrt(2.0))
      val foldsFlap = LineSegment(Point(x1, 0.0, 0.0), Point(x2, 0.0, 0.0))
      //This is what's different from the similarly named other unit test: the user looking at the opposite side
      val userLookingDirection = xyPlane_NormalTowardsZNegative.normal
      val bundle = fish.bundles.first()
      AMAPBisectedFaceFinderAndFaceSeparator(fish, bundle, foldsFlap, userLookingDirection)
         .findBisectedAndSeparateTheRest()
   }
}
