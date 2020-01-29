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

import com.google.common.annotations.VisibleForTesting
import com.moduleforge.libraries.geometry._3d.LineSegment
import com.whitebeluga.origami.figure.Bundle
import com.whitebeluga.origami.figure.component.Edge
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.Vertex
import com.whitebeluga.origami.figure.folding.SideOfFold
import com.whitebeluga.origami.figure.folding.SideOfFold.NEGATIVE
import com.whitebeluga.origami.figure.folding.SideOfFold.POSITIVE
import com.whitebeluga.origami.figure.folding.breakingup.BrokenUpFigure.Companion.joinMaps
import com.whitebeluga.origami.figure.folding.breakingup.bisectedfacefindingandfaceseparation.parameters.SeparatedFaces
import com.whitebeluga.origami.figure.folding.rotating.SideToRotateResolver.resolveSideToRotate

/**
 * Takes an object that holds
 * 1) the bisected faces of the bundle
 * 2) the non-bisected of the bundle separated by side
 * 3) the rest of bundles also separated by side.
 *
 * Then transforms this object into a "broken up figure"
 * which is 1) a set of bundles divided by side and 2) a side to fold, either passed from the GUI or decided by the
 * algorithm.
 *
 * The object returned is the previous step to doing the rotation of a subset of bundles.
 */
internal class FigureBreaker(
        private val originalBundleToFold: Bundle,
        private val bisectedAndSeparated: SeparatedFaces,
        private val foldSegment: LineSegment,
        private val sideToRotate: SideOfFold? = null) {
   private val bundleToFoldPlane = originalBundleToFold.plane

   fun breakUpFigure(): BrokenUpFigure {
      val allBisectedFaces = bisectedAndSeparated.bisected
      val notBisected = bisectedAndSeparated.mapOfSideToWholeFaces
      val restOfBundles = bisectedAndSeparated.mapOfSideToRestOfBundles
      val splitBundle = splitBundle(allBisectedFaces, notBisected)
      val sideToRotate= calculateSideToRotate(splitBundle.mapOfSideToBundlePart, restOfBundles)
      return BrokenUpFigure(splitBundle, restOfBundles, sideToRotate)
   }
   private fun calculateSideToRotate(splitBundle: Map<SideOfFold, Bundle>, restOfBundles: Map<SideOfFold, Set<Bundle>>): SideOfFold {
      if(sideToRotate != null)
         return sideToRotate
      val allBundles = joinMaps(splitBundle, restOfBundles)
      val allFaces = allBundles.mapValues { it.value.map { bundle -> bundle.faces }.flatten().toSet() }
      return resolveSideToRotate(foldSegment, allFaces)
   }
   private fun splitBundle(bisected: Set<Face>, notBisected: Map<SideOfFold, Set<Face>>): SplitBundle {
      val faceToSplitFace = split(bisected)
      return BundleSplitter(originalBundleToFold, notBisected, faceToSplitFace, foldSegment).splitBundle()
   }
   /**
    * Splits the faces and does point replacements for new points created from the intersection between a fold segment
    * and an edge of a face.
    *
    * The split faces returned might have a wrong value for the edge to vertex map, but that is not an issue, we
    * are only interested in the mapping of old faces to new.
    */
   @VisibleForTesting
   internal fun split(faces: Set<Face>): Set<SplitFace> {
      val splitFaces: MutableSet<SplitFace> = mutableSetOf()
      val edgeToVertexSubstitutions: MutableMap<Edge, Vertex> = mutableMapOf()
      for(face in faces) {
         val split = FaceSplitter(face, foldSegment, bundleToFoldPlane).splitFace()
         val edgeToVertexMap = split.intersectedEdgeToNewVertexMap
         val noSubstitutionsToMake = edgeToVertexMap.keys.none { edgeToVertexSubstitutions.containsKey(it)}
         if(noSubstitutionsToMake)
            splitFaces.add(split)
         else {
            val vertexSubstitutions = vertexSubstitutions(edgeToVertexSubstitutions, edgeToVertexMap)
            val positiveSideFace = split.newFaces[POSITIVE]!!.replaceVertices(vertexSubstitutions)
            val negativeSideFace = split.newFaces[NEGATIVE]!!.replaceVertices(vertexSubstitutions)
            val map = mapOf(POSITIVE to positiveSideFace, NEGATIVE to negativeSideFace)
            val newEdgeToVertex = edgeToVertexMap.entries.associate { (e, v) -> e to (edgeToVertexSubstitutions[e] ?: v) }
            splitFaces.add(SplitFace(face, map, newEdgeToVertex))
         }
         updateEdgeToVertexSubstitutionMap(edgeToVertexSubstitutions, edgeToVertexMap)
      }
      return splitFaces
   }
   companion object {
      internal fun vertexSubstitutions(edgeToVertexSubstitutions: Map<Edge, Vertex>, edgeToVertex: Map<Edge, Vertex>):
              Map<Vertex, Vertex> {
         val newToInitialEntries =
                 edgeToVertex.map { Pair(it.value, edgeToVertexSubstitutions[it.key]) }
         return newToInitialEntries.toMap().filterValues { it != null } as Map<Vertex, Vertex>
      }
      internal fun updateEdgeToVertexSubstitutionMap(edgeToVertexSubstitutions: MutableMap<Edge, Vertex>,
                                                     edgeToVertexMapOfAFace: Map<Edge, Vertex> ) =
              edgeToVertexMapOfAFace.forEach {
                 (edge, vertex) ->
                 if(!edgeToVertexSubstitutions.containsKey(edge)) edgeToVertexSubstitutions[edge] = vertex
              }
   }
}