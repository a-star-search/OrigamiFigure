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

import com.whitebeluga.origami.figure.component.Edge
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.Vertex
import com.whitebeluga.origami.figure.folding.SideOfFold

internal class SplitFace(
        val originalFace: Face,
        val newFaces: Map<SideOfFold, Face>,
        /**
         * This has to do with avoiding vertex duplication when breaking up a figure.
         *
         * Vertex duplication can happen when a fold segment intersects one or two edges of a face, thus needing
         * to create a new vertex.
         *
         * If this happens for a segment that joins two faces, after splitting all faces one by one,
         * a duplicated new vertex can occur per edge intersected.
         *
         * When an edge is intersected by the fold segment, we keep track of it as part of the information of the
         * split face.
         */
        val intersectedEdgeToNewVertexMap: Map<Edge, Vertex>) {
   /**
    * The edge connecting the two new faces
    */
   val connectingEdge: Edge
   init {
      assert(intersectedEdgeToNewVertexMap.entries.size <= 2)
      connectingEdge = initConnectingEdge()
   }
   private fun initConnectingEdge(): Edge {
      val listOfNewFaces = newFaces.values.toList()
      val connectingEdges = listOfNewFaces[0].edges.intersect(listOfNewFaces[1].edges)
      assert(connectingEdges.size == 1)
      val connectingEdge = connectingEdges.first()
      assert(!originalFace.edges.contains(connectingEdge))
      return connectingEdge
   }
}