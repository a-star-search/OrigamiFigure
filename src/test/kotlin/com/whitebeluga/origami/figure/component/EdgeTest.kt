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

import org.junit.Assert.assertEquals
import org.junit.Test

class EdgeTest {
   @Test
   fun twoEdgesMadeFromSameVerticesAreEquals() {
      val v1 = Vertex(0.0, 0.0, 0.0)
      val v2 = Vertex(0.0, 0.0, 1.0)
      val edge1 = Edge(v1, v2)
      val edge2 = Edge(v1, v2)
      val edge3 = Edge(v2, v1)
      val edge4 = Edge(Pair(v1, v2))
      assertEquals(edge1, edge2)
      assertEquals(edge1, edge3)
      assertEquals(edge1, edge4)
   }
}