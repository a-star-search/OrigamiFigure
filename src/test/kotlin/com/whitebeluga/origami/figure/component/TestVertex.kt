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

import com.moduleforge.libraries.geometry._3d.Point

internal class TestVertex (a: Double, b: Double, c: Double) : Vertex(a, b, c) {

      constructor(p: Point): this(p.x(), p.y(), p.z())
      /** very usually in tests, we use a face in the XY plane*/
      constructor(a: Int, b: Int): this(a.toDouble(), b.toDouble(), 0.0)
   constructor(a: Double, b: Int): this(a, b.toDouble(), 0.0)
   constructor(a: Int, b: Double): this(a.toDouble(), b, 0.0)
   constructor(a: Double, b: Double): this(a, b, 0.0)
      /**
      These constructors are useful for unit testing, to avoid adding ".0" to every number.

      Outside of that, I want to limit the risk for bugs, so they cannot be used outside of the module.
      Even inside the module, it is kind of a risky choice and a source of insidious bugs.

       But even then it beats having to add ".0" everywhere in the tests.
       */
      constructor(a: Int, b: Int, c: Int): this(a.toDouble(), b.toDouble(), c.toDouble())
      constructor(a: Int, b: Int, c: Double): this(a.toDouble(), b.toDouble(), c)
      constructor(a: Double, b: Int, c: Int): this(a, b.toDouble(), c.toDouble())
      constructor(a: Double, b: Double, c: Int): this(a, b, c.toDouble())
      constructor(a: Double, b: Int, c: Double): this(a, b.toDouble(), c)
      constructor(a: Int, b: Double, c: Double): this(a.toDouble(), b, c)
      constructor(a: Int, b: Double, c: Int): this(a.toDouble(), b, c.toDouble())
   }