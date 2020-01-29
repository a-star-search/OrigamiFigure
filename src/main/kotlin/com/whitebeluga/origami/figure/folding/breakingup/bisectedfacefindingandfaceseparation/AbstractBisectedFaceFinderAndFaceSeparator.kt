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
import com.moduleforge.libraries.geometry._3d.LineSegment
import com.moduleforge.libraries.geometry._3d.Vector
import com.whitebeluga.origami.figure.Bundle
import com.whitebeluga.origami.figure.Figure
import com.whitebeluga.origami.figure.component.Edge
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.Vertex
import com.whitebeluga.origami.figure.folding.SideOfFold
import com.whitebeluga.origami.figure.folding.SideOfFold.NEGATIVE
import com.whitebeluga.origami.figure.folding.SideOfFold.POSITIVE
import com.whitebeluga.origami.figure.folding.breakingup.bisectedfacefindingandfaceseparation.parameters.SeparatedBisectedBundle
import com.whitebeluga.origami.figure.folding.breakingup.bisectedfacefindingandfaceseparation.parameters.SeparatedFaces
import com.whitebeluga.origami.figure.folding.breakingup.bisectedfacefindingandfaceseparation.parameters.SeparatedFaces.Companion.fromBisectedAndNonBisectedFaces
import com.whitebeluga.origami.figure.folding.exceptions.InvalidFoldException
import com.whitebeluga.origami.figure.folding.exceptions.NoSideDefinedException

/**
   The side is optional for any type of line fold, sometimes it's not necessary and sometimes there is ambiguity.

   If there is ambiguity and the side was not passed, an exception will be thrown.

   If no side was indicated then the result should either be the same for both sides, or
   on one of the sides there should be no flaps, and therefore no bisected faces (bisected faces are a subset of
   the found flap faces)

   Otherwise if there is an asymmetry and in both sides flaps are found that can be folded, then an exception will be
   thrown.

   As mentioned earlier, this is true for any kind of line fold, either the bisection is symmetric or the side needs to
   be passed.

   All algorithms that implement this class have, at the time of writing this comment, very poor error control:
   We expect a well intentioned user and cannot ensure that a correct result will be returned for users trying to perform
   impossible folds, in which case detection of the error cannot be guaranteed.

   It's a matter of "technical debt", while it would be nice to have error detection, the user cares, first and foremost,
   about being able to fold a figure correctly.
 */
internal abstract class AbstractBisectedFaceFinderAndFaceSeparator(
        protected val figure: Figure,
        /** The bundle to fold. */
        protected val bisectedBundle: Bundle,
        protected val foldSegment: LineSegment,
        userLookingDirection: Vector,
        protected val sideOfFlapsToFold: SideOfFold? = null) {
   private val foldLine: Line = foldSegment.line
   protected val isUserLookingInPlaneNormalDirection: Boolean

   init {
      val normalOfBundle = bisectedBundle.plane.normal
      isUserLookingInPlaneNormalDirection = normalOfBundle.dot(userLookingDirection) > 0
   }
   /** This method should be the only member of the class's public interface.*/
   fun findBisectedAndSeparateTheRest(): SeparatedFaces {
      val separatedBundleFaces = findBisectedInBundleAndSeparateTheRest()
      if(figure.isMonoBundle())
         return separatedBundleFaces
      val restOfBundles: Map<SideOfFold, Set<Bundle>> = separateRestOfBundles(separatedBundleFaces)
      return SeparatedFaces(separatedBundleFaces.bisected, separatedBundleFaces.mapOfSideToWholeFaces, restOfBundles)
   }
   private fun findBisectedInBundleAndSeparateTheRest(): SeparatedFaces =
      if(sideOfFlapsToFold == null)
         findBisectedInBundleAndSeparateTheRestWhenNoSideToFoldSpecified()
      else
         findBisectedInBundleAndSeparateTheRest(sideOfFlapsToFold)
   /**
    * Throws an exception when there is a different result depending on the side to fold and no side was chosen.
    *
    * Important to note that the two parts of the figure divided by the fold can be asymmetric and yet, the division
    * is the same regardless of the intended side to fold.
    *
    * For any number of flaps that we desire to fold (and thus for any implementation of the algorithm whose only
    * variable is the number of flaps to fold) the same condition holds true:
    *
    * If no side was chosen, there is no problem as long as:
    * - The result for both sides is symmetric or
    * - One and only one of the sides is an empty set
    *
    * An exception will be thrown for an asymmetric result, which is the desired result for any variation of the
    * algorithm.
    */
   private fun findBisectedInBundleAndSeparateTheRestWhenNoSideToFoldSpecified(): SeparatedFaces {
      val separatedWithPositiveDefaultSide = findBisectedInBundleAndSeparateTheRest(POSITIVE)
      val noBisectedFacesWithPosDefaultSide = separatedWithPositiveDefaultSide.bisected.isEmpty()

      val separatedWithNegativeDefaultSide = findBisectedInBundleAndSeparateTheRest(NEGATIVE)
      val noBisectedFacesWithNegDefaultSide = separatedWithNegativeDefaultSide.bisected.isEmpty()

      if(noBisectedFacesWithPosDefaultSide && noBisectedFacesWithNegDefaultSide)
         throw InvalidFoldException("No faces to fold (bisected by the fold segment) were found.")
      if(noBisectedFacesWithPosDefaultSide)
         return separatedWithNegativeDefaultSide
      if(noBisectedFacesWithNegDefaultSide)
         return separatedWithPositiveDefaultSide
      val sideMakesADifference = separatedWithNegativeDefaultSide != separatedWithPositiveDefaultSide
      if(sideMakesADifference)
         throw NoSideDefinedException("Side needs to be specified.")
      return separatedWithPositiveDefaultSide //return any of the two
   }
   protected abstract fun findBisectedInBundleAndSeparateTheRest(side: SideOfFold): SeparatedFaces
   /**
    * Finds all connected faces given an initial face through connections totally or partially at the side of a fold
    * segment. This concept can be thought as a "flap" (minus the faces that are inside the flap and also bisected by
    * the segment, but those are not considered in this algorithm).
    *
    * If there is a connection to a face that is partially intersected by the fold segment, then an empty set will be
    * returned, signifying that that face does not belong to any flap that can be folded.
    *
    * What does it mean that a connecting edge is totally or partially on a side?
    *
    * It means that at least one of the ends of the edge have to be at the side of the fold. If both ends are on the
    * other side or if the edge lays exactly on the fold segment, then that connecting edge is not counted towards
    * finding new connected faces.
    *
    * The 'initial' face passed as parameter is returned in the set of flap faces (as long as any face is returned, and
    * not an empty set)
    */
   protected fun findAllFlapFacesAtSideOfFoldSegmentGivenAFace(
           // an arbitrary face of a flap from which we attempt to find the rest of faces of that flap
           initial: Face,
           side: SideOfFold,
           foldSegment: LineSegment): Set<Face> {
      /*
       * Maybe you are wondering if we are considering those faces that are "inside" a flap. The answer is yes. It's not
       * a special case: they are flaps themselves and the are surrounded by faces that can be folded (the only peculiarity
       * is that those surrounding faces form a flap, but it doesn't make a difference to the algorithm.
       */
      val notLookedAt: Set<Face> = bisectedBundle.faces - initial
      return findFlapAtSideOfFoldSegmentRecursive(setOf(initial), notLookedAt, side, foldSegment)
   }
   private fun findFlapAtSideOfFoldSegmentRecursive(
           /** an arbitrary face of a flap from which we attempt to find the rest of faces of that flap */
           connected: Set<Face>,
           notLookedAtYet: Set<Face>,
           side: SideOfFold,
           foldSegment: LineSegment): Set<Face> {
      if(notLookedAtYet.isEmpty())
         return connected
      val edgesOfConnectedFaces = connected.flatMap { it.edges }.toSet()
      val edgesConnectingToNotLookedAtFaces = edgesOfConnectedFaces
              .filter { edge ->
                 val facesConnectedByEdge = figure.facesConnectedBy(edge)
                 if(facesConnectedByEdge.size == 1)false
                 else facesConnectedByEdge.any {notLookedAtYet.contains(it)} }
              .toSet()
      val randomEdgeOnTheSide = edgesConnectingToNotLookedAtFaces.firstOrNull { edgeTotallyOrPartiallyOnTheSide(it, side) } ?:
         return connected
      val randomNotLookedAtFaceConnectedOnTheSide = figure.facesConnectedBy(randomEdgeOnTheSide)
              .intersect(notLookedAtYet).first()
      //check that face is foldable
      val partiallyIntersected = randomNotLookedAtFaceConnectedOnTheSide.partiallyIntersectedBy(foldSegment)
      if(partiallyIntersected)
         return emptySet() //the flap cannot be folded, return empty set
      return findFlapAtSideOfFoldSegmentRecursive(
              connected + randomNotLookedAtFaceConnectedOnTheSide,
              notLookedAtYet - randomNotLookedAtFaceConnectedOnTheSide, side,
              foldSegment)
   }
   private fun edgeTotallyOrPartiallyOnTheSide(edge: Edge, side: SideOfFold): Boolean {
      val ends = edge.endsAsList
      val sidesOfEnds = ends.map { SideOfFold.calculateSideOfFold(foldLine, it, bisectedBundle.plane) }
      return sidesOfEnds.any { it == side } //at least one end at the side
   }
   private fun separateRestOfBundles(separatedFaces: SeparatedFaces): Map<SideOfFold, Set<Bundle>> {
      if(figure.bundles == setOf(bisectedBundle))
         return emptyMap()
       val restOfBundles = figure.bundles - bisectedBundle
      val sideToFaces = separatedFaces.mapOfSideToWholeFaces
      val sideToVertices = makeSideToVerticesMap(separatedFaces)
      val separatedBisectedBundle = SeparatedBisectedBundle(bisectedBundle, sideToFaces, sideToVertices)
      return BundleSeparator(separatedBisectedBundle, restOfBundles, figure).separateBundles()
   }
   private fun makeSideToVerticesMap(separatedBundleFaces: SeparatedFaces): Map<SideOfFold, Set<Vertex>> =
           BisectedFaceVertexDivider(bisectedBundle, separatedBundleFaces.bisected, foldLine).divide()
   protected fun makeSeparatedFacesObjectFromAllFacesOfFlaps(allFacesOfAllFlaps: Set<Face>, side: SideOfFold): SeparatedFaces {
      val allBisectedFlapFaces = allFacesOfAllFlaps.filter { it.intersectedAcrossBy(foldSegment) }.toSet()
      val allNonBisectedFlapFacesAtSide = allFacesOfAllFlaps - allBisectedFlapFaces
      val allNonBisectedFlapFacesAtOtherSide = bisectedBundle.faces - allFacesOfAllFlaps
      val nonBisected = mapOf(
              side to allNonBisectedFlapFacesAtSide,
              side.opposite() to allNonBisectedFlapFacesAtOtherSide)
      val nonBisectedFilteredMap = nonBisected.filterValues { it.isNotEmpty() }
      return fromBisectedAndNonBisectedFaces(allBisectedFlapFaces, nonBisectedFilteredMap)
   }
}