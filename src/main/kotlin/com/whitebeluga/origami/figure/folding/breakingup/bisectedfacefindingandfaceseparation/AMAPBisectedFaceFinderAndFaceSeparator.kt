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
import com.whitebeluga.origami.figure.folding.FoldSegmentIntersectionType.PARTIAL
import com.whitebeluga.origami.figure.folding.SideOfFold
import com.whitebeluga.origami.figure.folding.breakingup.bisectedfacefindingandfaceseparation.parameters.SeparatedFaces


/**
 * AMAP stands for "as many as possible", that is a fold that attempts to fold as many layers as possible.
 *
 * This class encapsulates an algorithm that does two different things:
 *
 * 1) From a vector that represents the GUI's user looking direction, layer by layer in the order of the direction of
 * said vector, find what we call "flaps". A flap is a bundle (but we'll avoid this word that has another meaning in
 * this project) of faces that can be folded together. Fold all flaps that are preceded by no faces or foldable faces
 * in the region of the fold segment.
 *
 * find all of the faces that are
 * bisected, visible or not visible, just by being intersected by the fold segment and not connected to a partially
 * intersected face or meet any other condition that prevents the fold.
 *
 * 2) Find the other faces of the bundle and separate them in two sides. Do the same for the rest of the bundles in
 * other planes.
 *
 * There are different types of line fold and this class is the implementation for one of them.
 * Look at the project's documentation for a better understanding of the problem.
 *
 *
 * The result is passed to the rotation algorithm, which then knows all the faces that need rotating.
 *
 * ===================================
 *
 * We assume that:
 *
 * 1) The segment bisects at least one face. ie. we don't pass a segment
 * that does not divide the figure in two parts or a segment that passes exactly by the connection between two faces
 * (The case in which the fold passes exactly by the edge between two faces should not really happen. Two faces, on the
 * same plane that do not overlap (are not folded over each other) is an impossibility, because it should be,
 * in fact, the same polygon.)
 *
 * 2) The fold segment passes by one, and exactly one, bundle.
 * The corollary to this is that there are no folds going between two layer stacks: once a fold has been done, it cannot
 * be modified. Modifying a fold in an origami application would be a nice-to-have, but not high priority. The
 * exception is, of course, in the case that the folded flap(s) ends in the same bundle (flat fold).
 *
 * Maybe there are some edge cases in origami in which this is not true, I'm not sure, but they would be so rare as to
 * being very confident about this restriction.
 *
 * Other considerations:
 *
 * The result for a user that chooses this kind of fold in the GUI will normally be a symmetric bisection of the bundle
 * however it can also easily be an asymmetric result. If that's the case and no side was passed then an exception
 * will be thrown.
 *
 */
/*
 About the implementation:

 We calculate the result each time (ie. not cache it).
 we aren't worried about performance since it's our own internal code, we have control over how we call it and it makes
 the code more legible.

 As mentioned before, the reason to do that is to make the code as legible as possible (it is not an obvious algorithm
 due to corner cases).

 Some variables might be extracted as class members to avoid passing them around and make the code more legible...
 and other times variables will be passed to methods and methods be made static, just for testing purposes.

 Note that in this algorithm we are not considering "inside" hidden flaps as the effective segment for a layer will
 find them as it will find any other not behind a partially intersected one.
*/
internal class AMAPBisectedFaceFinderAndFaceSeparator(
        figure: Figure,
        bisectedBundle: Bundle,
        foldSegment: LineSegment,
        /**
         * User looking direction determines the side from which to find the faces to fold
         */
        userLookingDirection: Vector,
        sideOfFlapsToFold: SideOfFold? = null):
        AbstractBisectedFaceFinderAndFaceSeparator(figure, bisectedBundle, foldSegment, userLookingDirection, sideOfFlapsToFold) {
   private val facesToFacesAbove = bisectedBundle.facesToFacesAbove
   private val facesToFacesBelow = bisectedBundle.facesToFacesBelow

   override fun findBisectedInBundleAndSeparateTheRest(side: SideOfFold): SeparatedFaces {
      val faces = bisectedBundle.faces
      val bisectedFaces = faces.filter { it.intersectedAcrossBy(foldSegment) }
      val areAllFacesBisected = faces.size == bisectedFaces.size
      return if(areAllFacesBisected)
            SeparatedFaces.fromBisectedFaces(faces)
         else
            findBisectedInBundleAndSeparateTheRestWhenNotAllFacesBisected(side)
   }
   private fun findBisectedInBundleAndSeparateTheRestWhenNotAllFacesBisected(side: SideOfFold): SeparatedFaces {
      val allFlapFaces = findAllFacesOfFlaps(side)
      return makeSeparatedFacesObjectFromAllFacesOfFlaps(allFlapFaces, side)
   }
   //TODO To be honest I'm not sure why this function needs a side. I think it shouldn't need it
   internal fun findAllFacesOfFlaps(side: SideOfFold): Set<Face> {
      val bundleFaces = bisectedBundle.faces
      val bisectedBundleFaces = bundleFaces.filter { it.intersectedAcrossBy(foldSegment) }.toSet()
      val bisectedAndFoldable = bisectedBundleFaces.filter { face -> facesCoveringAllowFold(face) }.toSet()
      return bisectedAndFoldable
              .map { findAllFlapFacesAtSideOfFoldSegmentGivenAFace(it, side, foldSegment)}
              .flatten().toSet()
   }
   private fun facesCoveringAllowFold(face: Face): Boolean {
      val facesCovering =
              (if(isUserLookingInPlaneNormalDirection)
                 facesToFacesBelow[face]
              else
               facesToFacesAbove[face])
              ?: return true
      val intersectingSegment = face.intersectingSegment(foldSegment)!!
      return facesCovering.all { coveringFace ->
         coveringFace.intersectedAcrossBy(foldSegment) ||
         coveringFace.intersectedBy(intersectingSegment) != PARTIAL //any kind of intersection but "partial" (the only one that prevents folding)
      }
   }
}