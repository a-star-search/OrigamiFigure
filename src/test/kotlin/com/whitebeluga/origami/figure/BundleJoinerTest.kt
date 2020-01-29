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

import com.moduleforge.libraries.geometry._3d.LineSegment
import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Point
import com.moduleforge.libraries.geometry._3d.Vector
import com.whitebeluga.origami.figure.Bundle.BundleJoiner
import com.whitebeluga.origami.figure.component.Face
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.hasItems
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.whitebeluga.origami.figure.component.TestVertex as V

class BundleJoinerTest {
   private lateinit var xyPlane_NormalTowardsZPositive: Plane
   private lateinit var xyPlane_NormalTowardsZNegative: Plane
   private lateinit var triangle: Face
   /**
    * triangle connected to the other one, ie. sharing two vertices
    */
   private lateinit var connectedTriangle: Face
   private lateinit var foldSegment: LineSegment
   private val towardsZPos = Vector(0.0, 0.0, 1.0)
   private val towardsZNeg = Vector(0.0, 0.0, -1.0)

   @Before
   fun setUp(){
      val antiClockwisePointsAsSeenFromZPositive = listOf( Point(0, 0, 0), Point(1, 0, 0), Point(1, 1, 0))
      xyPlane_NormalTowardsZPositive = Plane.planeFromOrderedPoints(antiClockwisePointsAsSeenFromZPositive)
      val antiClockwisePointsAsSeenFromZNegative = listOf( Point(0, 0, 0), Point(1, 1, 0), Point(1, 0, 0))
      xyPlane_NormalTowardsZNegative = Plane.planeFromOrderedPoints(antiClockwisePointsAsSeenFromZNegative)
      val p1 = V(0, 0)
      val p2 = V(1, 0)
      triangle = Face(p1, p2, V(1, 1))
      connectedTriangle = Face(p1, p2, V(1, 1))
      foldSegment = LineSegment(p1, p2)
   }
   @Test(expected = AssertionError::class)
   fun attemptToJoinTwoBundles_WhoseNormalGoInTheSameDirection_AssertionErrorThrown() {
      val stationary = Bundle(xyPlane_NormalTowardsZPositive, triangle )
      val incoming = Bundle(xyPlane_NormalTowardsZPositive, connectedTriangle )
      val originalRotationDirection = towardsZPos
      BundleJoiner(stationary, incoming, originalRotationDirection, foldSegment).join()
   }
   @Test
   fun attemptToJoinTwoBundles_IncomingEndsUpOnTopOfStationary() {
      val stationary = Bundle(xyPlane_NormalTowardsZPositive, triangle )
      val incoming = Bundle(xyPlane_NormalTowardsZNegative, connectedTriangle )
      val originalRotationDirection = towardsZPos
      val joined = BundleJoiner(stationary, incoming, originalRotationDirection, foldSegment).join()
      val facesAboveTriangle = joined.facesAbove(triangle)
      val expectedFacesAboveTriangle = setOf(connectedTriangle)
      assertThat(facesAboveTriangle, `is`(expectedFacesAboveTriangle))
      val facesBelowIncoming = joined.facesBelow(connectedTriangle)
      val expectedFacesBelowIncoming = setOf(triangle)
      assertThat(facesBelowIncoming, `is`(expectedFacesBelowIncoming))
   }
   @Test
   fun attemptToJoinTwoBundles_IncomingEndsUpAtBottomOfStationary() {
      val stationary = Bundle(xyPlane_NormalTowardsZPositive, triangle )
      val incoming = Bundle(xyPlane_NormalTowardsZNegative, connectedTriangle )
      val originalRotationDirection = towardsZNeg
      val joined = BundleJoiner(stationary, incoming, originalRotationDirection, foldSegment).join()

      val facesAboveTriangle = joined.facesAbove(triangle)
      val expectedFacesAboveTriangle = emptySet<Face>()
      assertThat(facesAboveTriangle, `is`(expectedFacesAboveTriangle))

      val facesBelowTriangle = joined.facesBelow(triangle)
      val expectedFacesBelowTriangle = setOf(connectedTriangle)
      assertThat(facesBelowTriangle, `is`(expectedFacesBelowTriangle))

      val facesAboveIncoming = joined.facesAbove(connectedTriangle)
      val expectedFacesAboveIncoming = setOf(triangle)
      assertThat(facesAboveIncoming, `is`(expectedFacesAboveIncoming))

      val facesBelowIncoming = joined.facesBelow(connectedTriangle)
      val expectedFacesBelowIncoming = emptySet<Face>()
      assertThat(facesBelowIncoming, `is`(expectedFacesBelowIncoming))
   }
   /**
    * Draw the vectors and faces to visualize how the faces above and below of the flap and all the directions of
    * the vectors are coherent.
    */
   @Test
   fun joinTwoBundles_WhenThereIsAnUnfoldedFace() {
      val topLeftOfRightFace = V(0, 1)
      val bottomLeftOfRightFace = V(0, 0)
      val rightVertex = V(1, 0)
      val rightFace =  Face(topLeftOfRightFace, bottomLeftOfRightFace, rightVertex)

      val topLeftOfLeftFace = V(0, 1)
      val bottomLeftOfLeftFace = V(0, 0)
      val leftFace = Face(topLeftOfLeftFace, bottomLeftOfLeftFace, V(-1, 0))

      val bundle = Bundle(xyPlane_NormalTowardsZPositive, setOf(leftFace, rightFace), emptyMap())
      val vertexOfFlap = V(-1, 0)

      //not really left and right sides on the flap, since both are on the left side but it means
      //  the left is connected to the left face in the stationary bundle and the right to the right
      val leftSideOfLeftFoldedFlap = Face(topLeftOfLeftFace, bottomLeftOfLeftFace, vertexOfFlap)
      val rightSideOfLeftFoldedFlap = Face(bottomLeftOfRightFace, topLeftOfRightFace, vertexOfFlap)

      val leftFoldedFlap = Bundle(xyPlane_NormalTowardsZNegative, setOf(leftSideOfLeftFoldedFlap, rightSideOfLeftFoldedFlap),
              mapOf(rightSideOfLeftFoldedFlap to setOf(leftSideOfLeftFoldedFlap)) )
      val originalRotationDirection = towardsZPos

      val foldSegment = LineSegment(Point(0.0, 0.0, 0.0), Point(0.0, 1.0, 0.0))

      val joined = BundleJoiner(bundle, leftFoldedFlap, originalRotationDirection, foldSegment).join()
      val expectedFaceCount = 3 //3 because two of the faces must have been "unfolded" into one
      assertThat(joined.faces.size, `is`(expectedFaceCount))

      assertThat(joined.faces, hasItems(leftFace, leftSideOfLeftFoldedFlap))
      val joinedFace = (joined.faces - leftFace - leftSideOfLeftFoldedFlap).first()
      assertTrue(joinedFace.hasSamePointStructure(listOf(rightVertex, topLeftOfRightFace, vertexOfFlap)))
   }
   /**
    * When attempting to join together two unfolded
    */
   @Test(expected = AssertionError::class)
   fun joinTwoBundles_WhenThereIsAnUnfoldedFace_AndTheTwoUnfoldedFacesFaceDifferentOrientation_ShouldThrowAssertionError() {
      val topLeftOfRightFace = V(0, 1)
      val bottomLeftOfRightFace = V(0, 0)
      val rightFace =  Face(topLeftOfRightFace, bottomLeftOfRightFace, V(1, 0))

      val topLeftOfLeftFace = V(0, 1)
      val bottomLeftOfLeftFace = V(0, 0)
      val leftFace = Face(topLeftOfLeftFace, bottomLeftOfLeftFace, V(-1, 0))

      val bundle = Bundle(xyPlane_NormalTowardsZPositive, setOf(leftFace, rightFace), emptyMap())
      val p5 = V(-1, 0)

      //not really left and right sides on the flap, since both are on the left side but it means
      //  the left is connected to the left face in the stationary bundle and the right to the right
      val leftSideOfLeftFoldedFlap = Face(topLeftOfLeftFace, bottomLeftOfLeftFace, p5)
      val rightSideOfLeftFoldedFlap = Face(topLeftOfRightFace, bottomLeftOfRightFace, p5)

      val leftFoldedFlap = Bundle(xyPlane_NormalTowardsZNegative, setOf(leftSideOfLeftFoldedFlap, rightSideOfLeftFoldedFlap),
              mapOf(rightSideOfLeftFoldedFlap to setOf(leftSideOfLeftFoldedFlap)) )
      val originalRotationDirection = towardsZPos

      val foldSegment = LineSegment(Point(0.0, 0.0, 0.0), Point(0.0, 1.0, 0.0))

      BundleJoiner(bundle, leftFoldedFlap, originalRotationDirection, foldSegment).join()
   }
}