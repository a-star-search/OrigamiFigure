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

package com.whitebeluga.origami.figure.folding.linefolding

import com.moduleforge.libraries.geometry._3d.LineSegment
import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Plane.planeFromOrderedPoints
import com.moduleforge.libraries.geometry._3d.Point
import com.moduleforge.libraries.geometry._3d.Vector
import com.whitebeluga.origami.figure.Bundle
import com.whitebeluga.origami.figure.Figure
import com.whitebeluga.origami.figure.OrigamiBase.WATERBOMB
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.Vertex
import com.whitebeluga.origami.figure.folding.linefolding.LineFoldParameters.Companion.FLAT_FOLD_ANGLE
import com.whitebeluga.origami.figure.folding.linefolding.LineFolder.Companion.makeAMAPLineFolder
import com.whitebeluga.origami.figure.folding.linefolding.LineFolder.Companion.makeFFLineFolder
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.lessThan
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LineFolderTest {

   private lateinit var xyPlane_NormalTowardsZPositive: Plane
   private lateinit var xyPlane_NormalTowardsZNegative: Plane
   /**
    * remember waterbomb's bundle normal goes toward z neg
    */
   private lateinit var waterbomb: Figure
   /**
    * flat-folds the right tip towards the z pos direction
    */
   private lateinit var parametersToFoldRightTip: LineFoldParameters
   private lateinit var waterbombBundle: Bundle

   @Before
   fun setUp() {
      val width = 1.0
      waterbomb = WATERBOMB.make(width)

      val segmentPassesByRightTip = LineSegment(Point(width/4, 1.0, 0.0), Point(width/4, -1.0, 0.0))
      val normalOfValleySide = Vector(0.0, 0.0, 1.0)
      val guiUserLookingDirection = Vector(0.0, 0.0, -1.0)
      parametersToFoldRightTip = LineFoldParameters(segmentPassesByRightTip, normalOfValleySide, guiUserLookingDirection,
              listOf(), FLAT_FOLD_ANGLE, pickedPointAsSideToRotate = Point(0.5, 0.0, 0.0))
      waterbombBundle = waterbomb.bundles.first()

      val antiClockwisePointsAsSeenFromZPositive =
              listOf(Point(0, 0, 0), Point(1, 0, 0), Point(1, 1, 0))
      xyPlane_NormalTowardsZPositive = planeFromOrderedPoints(antiClockwisePointsAsSeenFromZPositive)
      val antiClockwisePointsAsSeenFromZNegative =
              listOf(Point(1, 1, 0), Point(1, 0, 0), Point(0, 0, 0))
      xyPlane_NormalTowardsZNegative = planeFromOrderedPoints(antiClockwisePointsAsSeenFromZNegative)
   }

   /**
    * perform different tests on the waterbomb with a single flap folded
    */
   @Test
   fun testFoldSingleFlapOfRightTipOfWaterbombBase_Calculated(){
      val folder = makeFFLineFolder(waterbomb, parametersToFoldRightTip)
      val folded = folder.fold()
      val foldedBundle = folded.bundles.first()

      val facesWithNoFacesBelow = foldedBundle.faces.filter { foldedBundle.facesBelow(it).isEmpty() }
      assertThat(facesWithNoFacesBelow.size, `is`(1)) //only one face has no faces above
      val faceWithNoFacesBelow = facesWithNoFacesBelow.first()

      val faceOfWaterbombWithNoFacesBelow = waterbombBundle.faces.first { waterbombBundle.facesBelow(it).isEmpty() }
      /*
      the face that has no faces below of the folded figure has to be different from the one in the waterbomb base
      since the fold was made towards z pos and the bundle normal goes towards z neg, therefore the flap is folded to
      the bottom
       */
      assertNotEquals(faceWithNoFacesBelow, faceOfWaterbombWithNoFacesBelow)

      /*
      List of faces for which the only faces below them is the face with no faces below.

      Naturally we expect only one such face which is the other face of the folded tip.
      */
      val facesWithOnly_TheFaceWithNoFacesBelow_Below =
              foldedBundle.faces.filter { foldedBundle.facesBelow(it) == setOf(faceWithNoFacesBelow) }
      assertThat(facesWithOnly_TheFaceWithNoFacesBelow_Below.size, `is`(1))
      val faceWithOnly_TheFaceWithNoFacesBelow_Below = facesWithOnly_TheFaceWithNoFacesBelow_Below.first()

      /*
       * the faces that has, as faces below it, only the face with no faces below and the face with only the
       * face with no faces below below
       *
       * there should be only one
       *
       * incidentally this is the face that was the bottom one in the waterbomb base pre-fold
       */
      val facesEtcEtc = foldedBundle.faces
              .filter { foldedBundle.facesBelow(it) == setOf(faceWithNoFacesBelow, faceWithOnly_TheFaceWithNoFacesBelow_Below) }
              .toSet()
      assertThat(facesEtcEtc.size, `is`(1))

      val faceEtcEtc = facesEtcEtc.first()

      assertThat( faceWithNoFacesBelow.vertices.intersect(faceWithOnly_TheFaceWithNoFacesBelow_Below.vertices).size, `is`(2) )
      assertThat( faceEtcEtc.vertices.intersect(faceWithOnly_TheFaceWithNoFacesBelow_Below.vertices).size, `is`(2) )
      assertThat( faceEtcEtc.vertices.intersect(faceWithNoFacesBelow.vertices).size, `is`(1) )

   }
   /**
    * performs various tests
    */
   @Test
   fun testFoldAllFlapsOfRightTipOfWaterbombBase_Calculated(){
      val folder = makeAMAPLineFolder(waterbomb, parametersToFoldRightTip)
      val folded = folder.fold()
      val foldedBundle = folded.bundles.first()
      assertThat(foldedBundle.faces.size, `is`(waterbombBundle.faces.size + 4)) //four more faces in the folded figure
      val facesWithNoFacesAbove = foldedBundle.faces.filter { foldedBundle.facesAbove(it).isEmpty() }
      val facesWithNoFacesBelow = foldedBundle.faces.filter { foldedBundle.facesBelow(it).isEmpty() }
      assertThat(facesWithNoFacesAbove.size, `is`(1))
      assertThat(facesWithNoFacesBelow.size, `is`(1))
      val faceWithNoFacesAbove = facesWithNoFacesAbove.first()
      val faceWithNoFacesBelow = facesWithNoFacesBelow.first()
      /*
      In the folded figure, the face that has no faces above (the bottom one from the user's viewpoint in this
      example) and the face that has no faces below come from the same face of the waterbomb base pre-fold.

      Therefore they must share two vertices
      */
      assertThat(faceWithNoFacesAbove.vertices.intersect(faceWithNoFacesBelow.vertices).size, `is`(2))
      /*
      The face with no faces below, which is one the faces of the folded tips of the flaps must have a smaller
      area than the face with no faces above (which is the one that it comes from and the bottom one from
      the user's viewpoint)
      */
      assertThat(faceWithNoFacesBelow.area(), lessThan(faceWithNoFacesAbove.area()))
   }
   /**
    * This test is different:
    *
    * In other tests the figure is split in two parts and folded over itself, with one part on top of another.
    *
    * This test is different in that the face being folded doesn't end up on the other side of the rest of the faces.
    * Since there are faces that due to their dimensions cannot be wrapped around.
    *
    *
    * The following figure illustrates the test, where the small triangular tip has been folded to be inside
    * and covered on both sides by the other two faces.
    * 1 __________________ 2
    *  \                 \
    *  \                 \
    *  \    ls1 _________\ 3
    *  \      /\        /
    *  \ ls2/__\      /
    *  \   \        /
    *  \   \      /
    *  \   \    /
    *  \   \  /
    *  \___\/
    *  5   4
    *
    */
   @Test
   fun testFoldFlapInside_Calculated() {
      val v5 = Vertex(0.0, 0.0, 0.0)
      val v1 = Vertex(0.0, 10.0, 0.0)
      val v2 = Vertex(10.0, 10.0, 0.0)
      val v3 = Vertex(10.0, 9.0, 0.0)
      val v4 = Vertex(1.0, 0.0, 0.0)

      val bottomFace = Face(listOf(v1, v2, v3, v4, v5))

      val v6 = Vertex(1.0, 9.0, 0.0)
      val topFace = Face(v3, v6, v4)

      val bundle = Bundle(xyPlane_NormalTowardsZPositive,
                          setOf(bottomFace, topFace),
                          mapOf(bottomFace to setOf(topFace)) )
      val figure = Figure(bundle)

      val ls1 = Point(2.0, 9.0, 0.0)
      val ls2 = Point(1.0, 8.0, 0.0)
      val foldSegment = LineSegment(ls1, ls2)
      /*
      Explanation for normal of valley side:
         The valley side is the direction towards which the rotation is done.
         Since what we are trying to accomplish is fold part of the top face on itself so that the folded
         tip will be below that face (which, again, is on the top), the valley side direction
         has to be the opposite of the of the bundle normal. Because the bundle's plane normal points towards
         z positive, the valley side normal has to point to z neg.
       */
      val normalOfValleySide = Vector(0.0, 0.0, -1.0)
      /*
      reminder that the user looking direction is used to establish the faces that are visible
      to the user (it is necessary in order to resolve ambiguities in certain cases)

      the user is looking "down" at the top face, so looking towards z neg
       */
      val guiUserLookingDirection = Vector(0.0, 0.0, -1.0)

      val foldParameters = LineFoldParameters(
              foldSegment,
              normalOfValleySide,
              guiUserLookingDirection,
              segmentEndsDockedToVertices = listOf(),
              angle = FLAT_FOLD_ANGLE,
              pickedPointAsSideToRotate = null)
      val folded = figure.doFirstFlapLineFold(foldParameters)
      val foldedBundle = folded.bundles.first()
      val facesOfFolded = folded.faces
      val tip = facesOfFolded.first { it.vertices.size == 3}
      val quadrangularFace = facesOfFolded.first { it.vertices.size == 4}
      val pentagonalFace = facesOfFolded.first { it.vertices.size == 5}

      val facesAboveFoldedTip = foldedBundle.facesAbove(tip)
      assertThat(facesAboveFoldedTip, `is`(setOf(quadrangularFace)))
      val facesBelowFoldedTip = foldedBundle.facesBelow(tip)
      assertThat(facesBelowFoldedTip, `is`(setOf(pentagonalFace)))

      val facesAbovePentagonalFace = foldedBundle.facesAbove(pentagonalFace)
      assertThat(facesAbovePentagonalFace, `is`(setOf(tip, quadrangularFace)))

      val facesAboveQuadrangularFace = foldedBundle.facesAbove(quadrangularFace)
      assertTrue(facesAboveQuadrangularFace.isEmpty())
   }

   /**
    * similar to the previous but the tip is fold in the opposite direction
    */
   @Test
   fun testFoldFlapOutside_Calculated() {
      val v5 = Vertex(0.0, 0.0, 0.0)
      val v1 = Vertex(0.0, 10.0, 0.0)
      val v2 = Vertex(10.0, 10.0, 0.0)
      val v3 = Vertex(10.0, 9.0, 0.0)
      val v4 = Vertex(1.0, 0.0, 0.0)

      val bottomFace = Face(listOf(v1, v2, v3, v4, v5))

      val v6 = Vertex(1.0, 9.0, 0.0)
      val topFace = Face(v3, v6, v4)

      val bundle = Bundle(xyPlane_NormalTowardsZPositive,
              setOf(bottomFace, topFace),
              mapOf(bottomFace to setOf(topFace)) )
      val figure = Figure(bundle)

      val ls1 = Point(2.0, 9.0, 0.0)
      val ls2 = Point(1.0, 8.0, 0.0)
      val foldSegment = LineSegment(ls1, ls2)
      val normalOfValleySide = Vector(0.0, 0.0, 1.0)
      /*
      reminder that the user looking direction is used to establish the faces that are visible
      to the user (it is necessary in order to resolve ambiguities in certain cases)

      the user is looking "down" at the top face, so looking towards z neg
       */
      val guiUserLookingDirection = Vector(0.0, 0.0, -1.0)

      val foldParameters = LineFoldParameters(
              foldSegment,
              normalOfValleySide,
              guiUserLookingDirection,
              segmentEndsDockedToVertices = listOf(),
              angle = FLAT_FOLD_ANGLE,
              pickedPointAsSideToRotate = null)
      val folded = figure.doFirstFlapLineFold(foldParameters)
      val foldedBundle = folded.bundles.first()
      val facesOfFolded = folded.faces
      val tip = facesOfFolded.first { it.vertices.size == 3}
      val quadrangularFace = facesOfFolded.first { it.vertices.size == 4}
      val pentagonalFace = facesOfFolded.first { it.vertices.size == 5}

      val facesAboveFoldedTip = foldedBundle.facesAbove(tip)
      assertThat(facesAboveFoldedTip, `is`(emptySet()))
      val facesBelowFoldedTip = foldedBundle.facesBelow(tip)
      assertThat(facesBelowFoldedTip, `is`(setOf(pentagonalFace, quadrangularFace)))
   }

   /**
    * similar to the previous but the bundle normal points to the other direction
    */
   @Test
   fun testFoldFlapInside_WhenBundleNormalTowardsZNeg_Calculated() {
      val v5 = Vertex(0.0, 0.0, 0.0)
      val v1 = Vertex(0.0, 10.0, 0.0)
      val v2 = Vertex(10.0, 10.0, 0.0)
      val v3 = Vertex(10.0, 9.0, 0.0)
      val v4 = Vertex(1.0, 0.0, 0.0)

      val bottomFace = Face(listOf(v1, v2, v3, v4, v5))

      val v6 = Vertex(1.0, 9.0, 0.0)
      val topFace = Face(v3, v6, v4)

      val bundle = Bundle(xyPlane_NormalTowardsZNegative,
              setOf(bottomFace, topFace),
              mapOf(bottomFace to setOf(topFace)) )
      val figure = Figure(bundle)

      val ls1 = Point(2.0, 9.0, 0.0)
      val ls2 = Point(1.0, 8.0, 0.0)
      val foldSegment = LineSegment(ls1, ls2)
      val normalOfValleySide = Vector(0.0, 0.0, 1.0)
      val guiUserLookingDirection = Vector(0.0, 0.0, 1.0)

      val foldParameters = LineFoldParameters(
              foldSegment,
              normalOfValleySide,
              guiUserLookingDirection,
              segmentEndsDockedToVertices = listOf(),
              angle = FLAT_FOLD_ANGLE,
              pickedPointAsSideToRotate = null)
      val folded = figure.doFirstFlapLineFold(foldParameters)
      val foldedBundle = folded.bundles.first()
      val facesOfFolded = folded.faces
      val tip = facesOfFolded.first { it.vertices.size == 3}
      val quadrangularFace = facesOfFolded.first { it.vertices.size == 4}
      val pentagonalFace = facesOfFolded.first { it.vertices.size == 5}

      val facesAboveFoldedTip = foldedBundle.facesAbove(tip)
      assertThat(facesAboveFoldedTip, `is`(setOf(quadrangularFace)))
      val facesBelowFoldedTip = foldedBundle.facesBelow(tip)
      assertThat(facesBelowFoldedTip, `is`(setOf(pentagonalFace)))
   }
   /**
    * similar to the previous but the bundle normal points to z neg
    * and now the top face is the smaller one
    */
   @Test
   fun testFoldFlapInside_AnotherVariation_Calculated() {
      val v5 = Vertex(0.0, 0.0, 0.0)
      val v1 = Vertex(0.0, 10.0, 0.0)
      val v2 = Vertex(10.0, 10.0, 0.0)
      val v3 = Vertex(10.0, 9.0, 0.0)
      val v4 = Vertex(1.0, 0.0, 0.0)

      val topFace = Face(listOf(v1, v2, v3, v4, v5))

      val v6 = Vertex(1.0, 9.0, 0.0)
      val bottomFace = Face(v3, v6, v4)

      val bundle = Bundle(xyPlane_NormalTowardsZNegative,
              setOf(bottomFace, topFace),
              mapOf(bottomFace to setOf(topFace)) )
      val figure = Figure(bundle)

      val ls1 = Point(2.0, 9.0, 0.0)
      val ls2 = Point(1.0, 8.0, 0.0)
      val foldSegment = LineSegment(ls1, ls2)
      val normalOfValleySide = Vector(0.0, 0.0, -1.0)
      val guiUserLookingDirection = Vector(0.0, 0.0, -1.0)

      val foldParameters = LineFoldParameters(
              foldSegment,
              normalOfValleySide,
              guiUserLookingDirection,
              segmentEndsDockedToVertices = listOf(),
              angle = FLAT_FOLD_ANGLE,
              pickedPointAsSideToRotate = null)
      val folded = figure.doFirstFlapLineFold(foldParameters)
      val foldedBundle = folded.bundles.first()
      val facesOfFolded = folded.faces
      val tip = facesOfFolded.first { it.vertices.size == 3}
      val quadrangularFace = facesOfFolded.first { it.vertices.size == 4}
      val pentagonalFace = facesOfFolded.first { it.vertices.size == 5}

      val facesAboveFoldedTip = foldedBundle.facesAbove(tip)
      assertThat(facesAboveFoldedTip, `is`(setOf(pentagonalFace)))
//      val facesBelowFoldedTip = foldedBundle.facesBelow(tip)
//      assertThat(facesBelowFoldedTip, `is`(setOf(pentagonalFace)))
   }
}