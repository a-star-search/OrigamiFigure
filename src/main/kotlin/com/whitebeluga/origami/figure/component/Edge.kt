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

package com.whitebeluga.origami.figure.component

import com.moduleforge.libraries.geometry._3d.LineSegment

class Edge : LineSegment {
   /**
    * It's a list for convenient access, there is no order in the vertices!
    */
	val vertices: List<Vertex>

   constructor(pointA: Vertex, pointB: Vertex) : super(pointA, pointB) {
      vertices = listOf(pointA, pointB)
   }
   constructor(pointPair: Pair<Vertex, Vertex>) : super(pointPair.first, pointPair.second) {
      vertices = pointPair.toList()
   }
   fun isAVertex(v: Vertex): Boolean = vertices.contains(v)
   fun isJoinedTo(other: Edge) = vertices.intersect(other.vertices).size == 1
}
