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
import com.moduleforge.libraries.geometry._3d.Point

/**
 * A crease is a ridge in a face of an origami figure that is used to aid in defining a fold.
 *
 * Creases are typically made as the very first steps of the folding process of a figure where the main, more precise
 * folds are done.
 */
class Crease: LineSegment {
   /**
    * A set of one or two points that are not vertices of a face.
    */
   val points: Set<Point>
   /**
    * For convenience.
    * The is NO GUARANTEE that two objects of this class that are equal, have the same point order.
    */
   val pointList: List<Point>
   /**
    * A set of one or two points that are vertices of a face.
    */
   val vertices: Set<Vertex>
   /**
    * For convenience.
    * The is NO GUARANTEE that two objects of this class that are equal, have the same vertex order.
    */
   val vertexList: List<Point>

   private constructor(p1: Point, p2: Point) : super(p1, p2) {
      points = setOf(p1, p2)
      vertices = setOf()
      pointList = listOf(p1, p2)
      vertexList = listOf()
   }
   private constructor(v1: Vertex, v2: Vertex) : super(v1, v2) {
      points = setOf()
      vertices = setOf(v1, v2)
      pointList = listOf()
      vertexList = listOf(v1, v2)
   }
   private constructor(p: Point, v: Vertex) : super(p, v) {
      points = setOf(p)
      vertices = setOf(v)
      pointList = listOf(p)
      vertexList = listOf(v)
   }
   override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false
      if (!super.equals(other)) return false
      other as Crease
      if (points != other.points) return false
      if (vertices != other.vertices) return false
      return true
   }
   override fun hashCode(): Int {
      var result = super.hashCode()
      result = 31 * result + points.hashCode()
      result = 31 * result + vertices.hashCode()
      return result
   }
   companion object {
      fun creaseFromPoints(p1: Point, p2: Point) = Crease(p1, p2)
      fun creaseFromVertices(v1: Vertex, v2: Vertex) = Crease(v1, v2)
      fun creaseFromPointAndVertex(p: Point, v: Vertex) = Crease(p, v)
      /**
       * This ugly method, internal to the module, is just convenient to use for some operations of this module
       */
      internal fun creaseFromPointsAndVertices(ps: List<Point>, vs: List<Vertex>): Crease {
         assert((ps.size + vs.size) == 2)
         return when {
            ps.size == 2 -> creaseFromPoints(ps[0], ps[1])
            vs.size == 2 -> creaseFromVertices(vs[0], vs[1])
            else -> creaseFromPointAndVertex(ps.first(), vs.first())
         }

      }
   }
}