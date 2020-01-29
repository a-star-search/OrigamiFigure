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
import com.moduleforge.libraries.geometry._3d.Vector
import com.whitebeluga.origami.figure.Bundle
import com.whitebeluga.origami.figure.Figure
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.folding.SideOfFold
import com.whitebeluga.origami.figure.folding.breakingup.bisectedfacefindingandfaceseparation.parameters.SeparatedFaces

/**
 * "First flap" bisected face finder and face separator.
 *
 * Normally it finds a single flap, but can be more than one.
 *
 * It only folds flaps of faces that are visible to the user on the fold segment defined by him. First flap means the
 * first visible, as it's often the case in origami the we want to fold a single flap on the front.
 *
 */
internal class FFBisectedFaceFinderAndFaceSeparator(
        figure: Figure,
        bisectedBundle: Bundle,
        foldSegment: LineSegment,
        userLookingDirection: Vector,
        sideOfFlapsToFold: SideOfFold? = null)
   : AbstractBisectedFaceFinderAndFaceSeparator(figure, bisectedBundle, foldSegment, userLookingDirection, sideOfFlapsToFold) {
   /*
    In this algorithm we need to consider those "inside" hidden flaps covered from the front and the back by a flap that
    has to be folded
    */
   override fun findBisectedInBundleAndSeparateTheRest(side: SideOfFold): SeparatedFaces {
      val allFacesOfFlapsToFold = findAllFacesOfFlapsToFold(side)
      return makeSeparatedFacesObjectFromAllFacesOfFlaps(allFacesOfFlapsToFold, side)
   }
   internal fun findAllFacesOfFlapsToFold(side: SideOfFold): Set<Face> {
      val visibleBisectedFaceFinder = VisibleBisectedFaceFinder(foldSegment, bisectedBundle)
      val visibleBisectedFaces =
              if(isUserLookingInPlaneNormalDirection)
                 visibleBisectedFaceFinder.findVisibleBisectedFacesFromBottom()
              else
                 visibleBisectedFaceFinder.findVisibleBisectedFacesFromTop()
      val allFlapsThatContainAVisibleBisectedFace = visibleBisectedFaces
              .map { findAllFlapFacesAtSideOfFoldSegmentGivenAFace(it, side, foldSegment)}
              .flatten().toSet()
      val insideFlaps = findInsideFlaps(allFlapsThatContainAVisibleBisectedFace, side)
      return allFlapsThatContainAVisibleBisectedFace + insideFlaps
   }
   private fun findInsideFlaps(flapFaces: Set<Face>, side: SideOfFold): Set<Face> {
      /*
      Algorithm explanation:

      Find those bisected faces which are not part of the flaps and
      have faces of the flap "behind" and "in front of" it ("wrapped in the flap").

      But that's not all, to be perfectly precise, it is also necessary to find all connected to them that
      are completely on the side to be folded.
      */
      val bundleFaces = bisectedBundle.faces
      val bisectedBundleFacesAndNotFlaps =
              bundleFaces.filter { it.intersectedAcrossBy(foldSegment) }.toSet() - flapFaces
      val facesInsideFlap = bisectedBundleFacesAndNotFlaps.filter { faceIsInsideFlap(it, flapFaces) }
      return facesInsideFlap
              .map { findAllFlapFacesAtSideOfFoldSegmentGivenAFace(it, side, foldSegment)}
              .flatten().toSet()
   }

   private fun faceIsInsideFlap(face: Face, flapFaces: Set<Face>): Boolean {
      val intersectingSegment = face.intersectingSegment(foldSegment)
              ?: return false
      val facesAbove = bisectedBundle.facesToFacesAbove[face] ?: return false
      val facesBelow = bisectedBundle.facesToFacesBelow[face] ?: return false
      val facesAboveThatAreFlaps = facesAbove.intersect(flapFaces)
      val facesBelowThatAreFlaps = facesBelow.intersect(flapFaces)
      val thereAreFacesAboveThatAreFlapsAndAreIntersected =
              facesAboveThatAreFlaps.any { it.partiallyOrFullyIntersectedBy(intersectingSegment) }
      val thereAreFacesBelowThatAreFlapsAndAreIntersected =
              facesBelowThatAreFlaps.any { it.partiallyOrFullyIntersectedBy(intersectingSegment) }
      return thereAreFacesAboveThatAreFlapsAndAreIntersected && thereAreFacesBelowThatAreFlapsAndAreIntersected
   }
}