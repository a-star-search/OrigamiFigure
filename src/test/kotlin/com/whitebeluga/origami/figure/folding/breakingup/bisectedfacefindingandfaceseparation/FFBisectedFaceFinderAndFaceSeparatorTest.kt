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
import com.whitebeluga.origami.figure.OrigamiBase.WATERBOMB
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.Vertex
import com.whitebeluga.origami.figure.folding.SideOfFold.Companion.calculateSideOfFold
import com.whitebeluga.origami.figure.folding.SideOfFold.POSITIVE
import com.whitebeluga.origami.figure.folding.exceptions.NoSideDefinedException
import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import com.whitebeluga.origami.figure.component.TestVertex as V

class FFBisectedFaceFinderAndFaceSeparatorTest {
   private lateinit var xyPlane_NormalTowardsZPositive: Plane
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
   /**
    * In the waterbomb base, attempting to fold a flap without specifying a side should throw an exception because the
    * result depends on the flap being folded (left or right)
    */
   @Test(expected = NoSideDefinedException::class)
   fun whenWaterbombBase_AndFoldSegmentDownTheMiddle_AndNoSideSpecified_ShouldThrowException() {
      val sideLength = 1.0
      val halfFigureHeight = sideLength/4.0
      val waterbomb = WATERBOMB.make(sideLength)
      val foldSegmentBisectsDownTheMiddle = LineSegment(Point(0.0, halfFigureHeight, 0.0), Point(0.0, -halfFigureHeight, 0.0))
      val bundle = waterbomb.bundles.first()
      val userLookingDirection = bundle.normal
      FFBisectedFaceFinderAndFaceSeparator(waterbomb, bundle, foldSegmentBisectsDownTheMiddle, userLookingDirection)
         .findBisectedAndSeparateTheRest()
   }
   @Test
   fun whenWaterbombBase_AndFoldSegmentDownTheMiddle_AndSideSpecified_Calculated() {
      val sideLength = 1.0
      val halfFigureHeight = sideLength/4.0
      val waterbomb = WATERBOMB.make(sideLength)
      val foldSegmentBisectsDownTheMiddle = LineSegment(Point(0.0, halfFigureHeight, 0.0), Point(0.0, -halfFigureHeight, 0.0))
      val bundle = waterbomb.bundles.first()
      val userLookingDirection = bundle.normal
      val sideOfFlap = POSITIVE
      val bisectedAndSeparated =
              FFBisectedFaceFinderAndFaceSeparator(waterbomb, bundle, foldSegmentBisectsDownTheMiddle,
                      userLookingDirection, sideOfFlap).findBisectedAndSeparateTheRest()
      val bisected = bisectedAndSeparated.bisected
      //bisected is only the bottom face (looking direction is the plane normal which is the same as looking at the bottom)
      //and of course, the bottom face is the one with the most faces above it
      val bottomFace = bundle.facesToFacesAbove.maxBy { it.value.size }!!.key
      val expectedBisected = setOf(bottomFace)
      assertThat(bisected, `is`(expectedBisected))
      val otherFacesOfFlap = bisectedAndSeparated.mapOfSideToWholeFaces[sideOfFlap]!!
      //there should be only one other face in the flap
      assertThat(otherFacesOfFlap.size, `is`(1))
      val facesNotOfFlap = bisectedAndSeparated.mapOfSideToWholeFaces[sideOfFlap.opposite()]!!
      //if the faces not bisected and not entirely in the flap are two, and the waterbomb has six faces, then the rest of the faces should be four
      assertThat(facesNotOfFlap.size, `is`(4))
   }
   /**
    * If the segment intersects all faces but one is completely hidden then the bisected face
    * will be only the top one (top from the point of view of the user defining the fold line)
    */
   @Test
   fun whenTwoFaces_AndOneIsHiddenFromTheSegment_AndTheSegmentIntersectsBoth_Then_OnlyTheTopOneIsBisected() {
      val hiddenFace = Face(bottomLeft, topLeft, V(0.5,0.5,0))
      val bundle = Bundle(xyPlane_NormalTowardsZPositive,
              setOf(square, hiddenFace),
              mapOf(hiddenFace to setOf(square)))
      val figure = Figure(bundle)
      //looking down on the bundle, looking to the top face (in this case actual 'top' of the bundle)
      val userLookingDirection = bundle.downwards
      //the following fold segment fully bisects both faces
      val foldSegment = LineSegment(Point(0.2, 0.0, 0.0), Point(0.2, 1.0, 0.0))
      /*
      Including the side of fold as parameter might look superfluous on an intuitive level
      but bear in mind that a single "flap" of the right side is not the same as one of the left side!

      In this case it is the right side that the flap is in.
       */
      val rightSide = calculateSideOfFold(foldSegment.line, topRight, xyPlane_NormalTowardsZPositive)
      val bisectedAndSeparated =
              FFBisectedFaceFinderAndFaceSeparator(figure, bundle, foldSegment, userLookingDirection, rightSide)
                      .findBisectedAndSeparateTheRest()
      val bisected = bisectedAndSeparated.bisected
      //only bisects the top face
      assertThat(bisected, `is`(setOf(square)))
   }
}
