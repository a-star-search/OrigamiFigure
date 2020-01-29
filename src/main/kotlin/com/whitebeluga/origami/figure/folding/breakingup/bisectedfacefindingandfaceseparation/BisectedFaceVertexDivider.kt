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
import com.whitebeluga.origami.figure.Bundle
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.Vertex
import com.whitebeluga.origami.figure.folding.SideOfFold
import com.whitebeluga.origami.figure.folding.SideOfFold.Companion.calculateSideOfFold

/**
 * From the bisected faces, gets the vertices and makes a map of side to vertices.
 *
 * Vertices exactly on the fold line have no side and therefore are not returned in the map result.
 */
internal class BisectedFaceVertexDivider(
        private val bisectedBundle: Bundle,
        private val bisectedFaces: Set<Face>,
        private val foldLine: Line) {
   fun divide(): Map<SideOfFold, Set<Vertex>> {
      val allPointsOfBisectedFaces = bisectedFaces.map {it.vertices}.flatten()
      val allPointsOfBisectedFacesGroupedBySide = allPointsOfBisectedFaces
              .groupBy { calculateSideOfFold(foldLine, it, bisectedBundle.plane) }
              .filterKeys { it != null }
      return valueListsAsSets(allPointsOfBisectedFacesGroupedBySide) as Map<SideOfFold, Set<Vertex>>
   }
   companion object {
      private fun <K, V> valueListsAsSets(map: Map<K, List<V>>): Map<K, Set<V>>{
         val entries = map.map { (key, value) -> (key to value.toSet())}
         return mapOf(*entries.toTypedArray())
      }
   }
}