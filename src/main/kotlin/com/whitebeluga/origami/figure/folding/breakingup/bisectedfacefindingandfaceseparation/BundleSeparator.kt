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

import com.whitebeluga.origami.figure.Bundle
import com.whitebeluga.origami.figure.Figure
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.Vertex
import com.whitebeluga.origami.figure.folding.SideOfFold
import com.whitebeluga.origami.figure.folding.SideOfFold.NEGATIVE
import com.whitebeluga.origami.figure.folding.SideOfFold.POSITIVE
import com.whitebeluga.origami.figure.folding.breakingup.bisectedfacefindingandfaceseparation.parameters.SeparatedBisectedBundle
import com.whitebeluga.origami.figure.folding.exceptions.InvalidFoldException

internal class BundleSeparator constructor(
        separatedBisectedBundle: SeparatedBisectedBundle,
        private val restOfBundles: Set<Bundle>,
        private val figure: Figure) {
   private val nonBisectedFacesOfBisectedBundle = separatedBisectedBundle.nonBisectedFacesOfBisectedBundle
   private val sideToPointsOfBisectedFaces = separatedBisectedBundle.sideToPointsOfBisectedFaces
   private val bisectedBundle: Bundle = separatedBisectedBundle.originalBundle

   fun separateBundles(): Map<SideOfFold, Set<Bundle>> {
      if(restOfBundles.isEmpty())
         return emptyMap()
      val adjacentBundles = adjacentBundles(nonBisectedFacesOfBisectedBundle, sideToPointsOfBisectedFaces)
      val allBundlesAreOnOneSide = adjacentBundles.values.toSet().size == 1
      if(allBundlesAreOnOneSide) {
         val side = adjacentBundles.values.first()
         return mapOf(side to restOfBundles)
      }
      return connectSubtreesOfBundles(adjacentBundles)
   }
   /* a bundle can be adjacent to the bundle to fold, either through a non-bisected face or through one
    * or more of the points of the bisected faces on a particular side */
   private fun adjacentBundles(sideToFacesOfBundle: Map<SideOfFold, Set<Face>>,
                               sideToPointsOfBisectedFaces: Map<SideOfFold, Set<Vertex>>): Map<Bundle, SideOfFold> {
      val adjacentThroughPosFaces = findOtherBundlesConnectedTo(sideToFacesOfBundle[POSITIVE]?: emptySet())
      val adjacentThroughNegFaces = findOtherBundlesConnectedTo(sideToFacesOfBundle[NEGATIVE]?: emptySet())
      val adjacentThroughPosPoints = findOtherBundlesSharingVertices(sideToPointsOfBisectedFaces[POSITIVE]?: emptySet())
      val adjacentThroughNegPoints = findOtherBundlesSharingVertices(sideToPointsOfBisectedFaces[NEGATIVE]?: emptySet())
      val bundleToSideOfFold =  mutableMapOf<Bundle, SideOfFold>().also {
         adjacentThroughPosFaces.forEach { bundle -> it[bundle] = POSITIVE }
         adjacentThroughNegFaces.forEach { bundle -> it[bundle] = NEGATIVE }
         adjacentThroughPosPoints.forEach { bundle -> it[bundle] = POSITIVE }
         adjacentThroughNegPoints.forEach { bundle -> it[bundle] = NEGATIVE }
      }
      val allPositive = adjacentThroughPosFaces + adjacentThroughPosPoints
      val allNegative = adjacentThroughNegFaces + adjacentThroughNegPoints
      val intersection = allPositive.intersect(allNegative)
      if(intersection.isNotEmpty())
         throw InvalidFoldException("One or more bundles are connected to points or faces at both sides of the fold.")
      return bundleToSideOfFold
   }
   private fun findOtherBundlesConnectedTo(faces: Set<Face>): Set<Bundle> {
      if(faces.isEmpty())
         return emptySet()
      val allFacesBelongToSameBundle = faces.map { figure.bundleOf(it) }.toSet().size == 1
      assert(allFacesBelongToSameBundle)
      val facesConnectedToGivenFaces = faces.flatMap {figure.facesConnectedTo(it) }
      val bundlesOfConnectedFaces = facesConnectedToGivenFaces.map {figure.bundleOf(it)}.toSet()
      return bundlesOfConnectedFaces - bisectedBundle
   }
   private fun findOtherBundlesSharingVertices(vertices: Set<Vertex>): Set<Bundle> =
           if(vertices.isEmpty())
              emptySet()
           else
              vertices.flatMap {  figure.facesSharing(it).map { face ->  figure.bundleOf(face) }}.toSet() - bisectedBundle
   private fun connectSubtreesOfBundles(headBundleToSide: Map<Bundle, SideOfFold>): Map<SideOfFold, Set<Bundle>> =
           mapOf<SideOfFold, MutableSet<Bundle>>(POSITIVE to mutableSetOf(), NEGATIVE to mutableSetOf()).also {
              headBundleToSide.forEach { bundle, side ->
                 it[side]!!.addAll(connectSubtreeOfBundlesRec(setOf(bundle), setOf()))
              } }
   private fun connectSubtreeOfBundlesRec(unexplored: Set<Bundle>, explored: Set<Bundle>): Set<Bundle> {
      val node = unexplored.first()
      val updatedUnexplored = unexplored - node
      val updatedExplored = explored + node
      //never get the "bundle to fold" in, otherwise we might jump to a different side
      val newUnexplored = (figure.bundlesConnectedTo(node) - bisectedBundle) - explored
      return if(newUnexplored.isEmpty()) updatedExplored
         else connectSubtreeOfBundlesRec(updatedUnexplored + newUnexplored, updatedExplored)
   }
}