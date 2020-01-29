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
import com.whitebeluga.origami.figure.Bundle
import com.whitebeluga.origami.figure.TestUtil.makeFigureFrom
import com.whitebeluga.origami.figure.component.Edge
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.folding.SideOfFold.NEGATIVE
import com.whitebeluga.origami.figure.folding.SideOfFold.POSITIVE
import org.hamcrest.CoreMatchers.*
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import com.whitebeluga.origami.figure.component.TestVertex as V

class BundleSplitterTest {
   private lateinit var xyPlane_NormalTowardsZPositive: Plane

   @Before
   fun setUp() {
      val antiClockwisePointsAsSeenFromZPositive = listOf(Point(0, 0, 0), Point(1, 0, 0), Point(1, 1, 0))
      xyPlane_NormalTowardsZPositive = planeFromOrderedPoints(antiClockwisePointsAsSeenFromZPositive)
   }
   @Test
   fun split_whenBundleMadeOfOneFace_ReturnTwoBundlesWithTheCorrectSplitFacesAndSides(){
      val bottomLeft = V(0, 0, 0)
      val bottomRight = V(1, 0, 0)
      val topRight = V(1, 1, 0)
      val topLeft = V(0, 1, 0)
      val face = Face(bottomLeft, bottomRight, topRight, topLeft)
      val middleLeft = V(0, 0.5, 0)
      val middleRight = V(1, 0.5, 0)
      val foldSegment = LineSegment(middleLeft, middleRight)
      val figure = makeFigureFrom(face)
      val bundle = figure.bundles.first()

      val facePart1 = Face(bottomLeft, bottomRight, middleRight, middleLeft)
      val facePart2 = Face(middleLeft, middleRight, topRight, topLeft)

      val newFaces = mapOf(POSITIVE to facePart1, NEGATIVE to facePart2)
      val intersections = mapOf(Edge(topRight, bottomRight) to middleRight, Edge(topLeft, bottomLeft) to middleLeft)
      val splitFace = SplitFace(face, newFaces, intersections)
      val splitter = BundleSplitter.fromSingleFaceBundle(bundle, splitFace, foldSegment )

      val splitBundle = splitter.splitBundle().mapOfSideToBundlePart
      assertThat(splitBundle.size, `is`(2))
      assertThat(splitBundle[POSITIVE]!!.faces.size, `is`(1))
      assertThat(splitBundle[NEGATIVE]!!.faces.size, `is`(1))

      assertThat(splitBundle[POSITIVE]!!.faces.first(), `is`(facePart1))
      assertThat(splitBundle[NEGATIVE]!!.faces.first(), `is`(facePart2))
   }
   /**
    * Requires further explanation than the name of the test.
    * It is square with a tip folded, and the fold goes through the middle of the square,
    * so that one part folded will be half square and the other folded part is the other
    * half of the square plus the tip.
    */
   @Test
   fun split_whenBundleIsAFaceWithACornerFolded_ShouldReturnTwoBundlesWithTheCorrectSplitFacesAndSides(){
      val bottomLeft1 = V(0, 0.1, 0)
      val bottomLeft2 = V(0.1, 0, 0)
      val tip = V(0.1, 0.1, 0)
      val bottomRight = V(1, 0, 0)
      val topRight = V(1, 1, 0)
      val topLeft = V(0, 1, 0)
      val bottomFace = Face(bottomLeft1, bottomLeft2, bottomRight, topRight, topLeft)
      val corner = Face(bottomLeft2, bottomLeft1, tip)
      val middleLeft = V(0, 0.5, 0)
      val middleRight = V(1, 0.5, 0)
      val bottomPartOfFace = Face(bottomLeft1, bottomLeft2, bottomRight, middleRight, middleLeft)
      val topPartOfFace = Face(middleLeft, middleRight, topRight, topLeft)
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, setOf(bottomFace, corner), mapOf(bottomFace to setOf(corner)) )
      val foldSegment = LineSegment(middleLeft, middleRight)
      val splitFace = SplitFace(bottomFace,
              mapOf(POSITIVE to bottomPartOfFace, NEGATIVE to topPartOfFace),
              mapOf(Edge(topLeft, bottomLeft1) to middleLeft, Edge(bottomRight, topRight) to middleRight ))
      val splitter = BundleSplitter(bundle,
              mapOf( POSITIVE to setOf(corner)),
              setOf(splitFace),
              foldSegment)
      val splitBundle = splitter.splitBundle().mapOfSideToBundlePart
      assertThat(splitBundle.size, `is`(2))
      assertThat(splitBundle[POSITIVE]!!.faces.size, `is`(2))
      assertThat(splitBundle[NEGATIVE]!!.faces.size, `is`(1))
      assertThat(splitBundle[POSITIVE]!!.faces, hasItems(bottomPartOfFace, corner))
      assertThat(splitBundle[NEGATIVE]!!.faces, hasItem(topPartOfFace))

      //I'm using bottom with two different meanings here
      val bottomBundle = splitBundle[POSITIVE]!!
      val faceAboveBottomFace = bottomBundle.facesToFacesAbove[bottomPartOfFace]!!
      assertThat(faceAboveBottomFace, `is`(setOf(corner)))
   }
   /**
    * Not really that complex, but it tests a different thing.
    * It's difficult to describe with words, but very easy to visualize. I suggest making a drawing
    * to see what's different about this case and why it's not as simple as other basic cases.
    */
   @Test
   fun split_ComplexCase1_Calculated(){
      //figure creation
      val bottomLeft = V(0, 0)
      val bottomRight = V(1, 0)
      val topRight = V(1, 1)
      val topLeft = V(0, 1)
      val bottomFace = Face(bottomLeft, bottomRight, topRight, topLeft)
      val faceOnRight = Face(bottomRight, topRight, V(0.9, 1), V(0.9, 0))

      //the face on the left side is on top of the other two
      val bottomRightOfFaceOnLeft = V(0.95, 0, 0)
      val topRightOfFaceOnLeft = V(0.95, 1, 0)
      val faceOnLeft = Face(bottomLeft, bottomRightOfFaceOnLeft, topRightOfFaceOnLeft, topLeft)

      //bundle
      val faces = setOf(bottomFace, faceOnRight, faceOnLeft)
      val facesToFacesAbove = mapOf(
              bottomFace to setOf(faceOnRight, faceOnLeft ),
              faceOnRight to setOf(faceOnLeft))
      val bundle = Bundle(xyPlane_NormalTowardsZPositive, faces, facesToFacesAbove)

      //splitting down the middle
      val bottomMiddle = V(0.5, 0, 0)
      val topMiddle = V(0.5, 1, 0)
      val leftPartOfBottomFace = Face(bottomLeft, bottomMiddle, topMiddle, topLeft)
      val rightPartOfBottomFace = Face(bottomMiddle, bottomRight, topRight, topMiddle)
      val bottomMiddle2 = V(bottomMiddle)
      val topMiddle2 = V(topMiddle)
      val leftPartOfFaceOnLeft = Face(bottomLeft, bottomMiddle2, topMiddle2, topLeft)
      val rightPartOfFaceOnLeft = Face(bottomMiddle2, bottomRightOfFaceOnLeft, topRightOfFaceOnLeft, topMiddle2)

      val foldSegment = LineSegment(bottomMiddle, topMiddle)

      val splitBottomFace = SplitFace(bottomFace,
              mapOf(POSITIVE to rightPartOfBottomFace, NEGATIVE to leftPartOfBottomFace),
              mapOf(Edge(topLeft, topRight) to topMiddle, Edge(bottomLeft, bottomRight) to bottomMiddle))

      val splitLeftFace = SplitFace(faceOnLeft,
              mapOf(POSITIVE to rightPartOfFaceOnLeft, NEGATIVE to leftPartOfFaceOnLeft),
              mapOf(Edge(topLeft, topRightOfFaceOnLeft) to topMiddle2, Edge(bottomLeft, bottomRightOfFaceOnLeft) to bottomMiddle2))

      val splitter = BundleSplitter(bundle,
              mapOf( POSITIVE to setOf(faceOnRight)),
              setOf(splitBottomFace, splitLeftFace),
              foldSegment)
      val splitBundle = splitter.splitBundle().mapOfSideToBundlePart

      //assertions
      // layer count on each side
      val layerCountRightPartOfSplitBundle = splitBundle[POSITIVE]!!.faces.size
      assertThat(layerCountRightPartOfSplitBundle, `is`(3))
      val layerCountLeftPartOfSplitBundle = splitBundle[NEGATIVE]!!.faces.size
      assertThat(layerCountLeftPartOfSplitBundle, `is`(2))

      // left side bundle
      val leftSideBundle = splitBundle[NEGATIVE]!!
      assertThat(leftSideBundle.faces, `is`(setOf(leftPartOfBottomFace, leftPartOfFaceOnLeft)))
      val faceAboveLeftPartOfBottomFace = leftSideBundle.facesToFacesAbove[leftPartOfBottomFace]!!
      assertThat(faceAboveLeftPartOfBottomFace, `is`(setOf(leftPartOfFaceOnLeft)))

      // right side bundle
      val rightSideBundle = splitBundle[POSITIVE]!!
      assertThat(rightSideBundle.faces, `is`(setOf(rightPartOfBottomFace, rightPartOfFaceOnLeft, faceOnRight)))
      val facesAboveRightPartOfBottomFace = rightSideBundle.facesToFacesAbove[rightPartOfBottomFace]!!
      assertThat(facesAboveRightPartOfBottomFace, `is`(setOf(rightPartOfFaceOnLeft, faceOnRight)))
      val facesAboveRightFace = rightSideBundle.facesToFacesAbove[faceOnRight]!!
      assertThat(facesAboveRightFace, `is`(setOf(rightPartOfFaceOnLeft)))
   }
   /**
    just take a look at the image in the test/resources folder
    this is for testing an actual merging of two layers
    */
   @Test
   fun split_ComplexCase2_Calculated(){
      //figure creation
      val topLeft1 =          V(0.01, 1)
      val topLeft2 =          V(0, 0.99)
      val cornerTip =         V(0.01, 0.99)
      val cornerFace = Face(topLeft1, topLeft2, cornerTip)

      val bottomVertex =      V(0.5, 0)
      val closeToTopRight =   V(1, 0.8)
      val topRight =          V(1, 1)
      val backFace = Face(bottomVertex, closeToTopRight, topRight, topLeft1, topLeft2)

      val cornerOfRightFace = V(0.2, 0.8)
      val rightFace = Face(bottomVertex, closeToTopRight, cornerOfRightFace)

      val cornerOfLeftFace = V(0.6, 0.2)
      val leftFace = Face(topLeft2, bottomVertex, cornerOfLeftFace) //the one on top

      val bundle = Bundle(xyPlane_NormalTowardsZPositive,
              setOf(  backFace, cornerFace, rightFace, leftFace),
              mapOf(  backFace to setOf(cornerFace, rightFace, leftFace),
                      rightFace to setOf(leftFace) ))
      //splitting

      val foldSegment = LineSegment(Point(0, 0.9, 0), Point(0.1, 1, 0))
      val intersection1 = V(foldSegment.intersectionPoint(LineSegment(topLeft2, bottomVertex)))
      val intersection2 = V(foldSegment.intersectionPoint(LineSegment(topLeft2, cornerOfLeftFace)))
      val intersection3 = V(foldSegment.intersectionPoint(LineSegment(topLeft1, topRight)))

      val topLeftPartOfLeftFace = Face(topLeft2, intersection1, intersection2)
      val topLeftPartOfBackFace = Face(topLeft1, topLeft2, intersection1, intersection3)
      //don't really care about the negative side, this test focuses on the merging on the positive side:
      val bottomRightPartOfBackFace = Face(intersection1, intersection3, topRight, closeToTopRight, bottomVertex)
      val bottomRightPartOfLeftFace = Face(intersection1, intersection2, cornerOfLeftFace, bottomVertex)

      val splitBackFace = SplitFace(backFace,
              mapOf(POSITIVE to topLeftPartOfBackFace, NEGATIVE to bottomRightPartOfBackFace),
              mapOf(Edge(topLeft2, bottomVertex) to intersection1, Edge(topLeft1, topRight) to intersection3))

      val splitLeftFace = SplitFace(leftFace,
              mapOf(POSITIVE to topLeftPartOfLeftFace, NEGATIVE to bottomRightPartOfLeftFace),
              mapOf(Edge(topLeft2, bottomVertex) to intersection1, Edge(topLeft2, cornerOfLeftFace) to intersection2))

      val splitter = BundleSplitter(bundle,
              mapOf( POSITIVE to setOf(cornerFace), NEGATIVE to setOf(rightFace)),
              setOf(splitBackFace, splitLeftFace),
              foldSegment)
      val splitBundle = splitter.splitBundle().mapOfSideToBundlePart

      //assertions
      // 2 layers only on the positive side (merging)
      val leftPartOfSplitBundle = splitBundle[POSITIVE]!!
      assertThat(leftPartOfSplitBundle.faces.size, `is`(3))
      //there is only one face (the part of the back face) that has other faces above it
      assertThat(leftPartOfSplitBundle.facesToFacesAbove.size, `is`(1))
      //there are two faces above the only face that has faces above
      assertThat(leftPartOfSplitBundle.facesToFacesAbove.values.first().size, `is`(2))

      val faceWithFacesAbove = leftPartOfSplitBundle.facesToFacesAbove.keys.first()
      assertThat(faceWithFacesAbove, `is`(topLeftPartOfBackFace))
      val facesAbove = leftPartOfSplitBundle.facesToFacesAbove.values.first()
      assertThat(facesAbove, `is`(setOf(topLeftPartOfLeftFace, cornerFace)))
   }
}