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

import com.moduleforge.libraries.geometry._3d.Line.linePassingBy
import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Point
import com.moduleforge.libraries.geometry._3d.Point.midPoint
import com.whitebeluga.origami.figure.TestUtil.makeFigureFrom
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.TestVertex
import com.whitebeluga.origami.figure.component.Vertex
import com.whitebeluga.origami.figure.folding.SideOfFold.Companion.calculateSideOfFold
import com.whitebeluga.origami.figure.folding.SideOfFold.NEGATIVE
import com.whitebeluga.origami.figure.folding.SideOfFold.POSITIVE
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test

class BisectedFaceVertexDividerTest {
   private lateinit var xyPlane_NormalTowardsZPositive: Plane
   private lateinit var bottomLeft: Vertex
   private lateinit var bottomRight: Vertex
   private lateinit var topRight: Vertex
   private lateinit var topLeft: Vertex
   /** just a face */
   private lateinit var face: Face
   private lateinit var square: Face

   @Before
   fun setUp() {
      val antiClockwisePointsAsSeenFromZPositive =
              listOf(Point(0, 0, 0), Point(1, 0, 0), Point(1, 1, 0))
      xyPlane_NormalTowardsZPositive = Plane.planeFromOrderedPoints(antiClockwisePointsAsSeenFromZPositive)
      bottomLeft = TestVertex(0, 0, 0)
      bottomRight = TestVertex(1, 0, 0)
      topRight = TestVertex(1, 1, 0)
      topLeft = TestVertex(0, 1, 0)
      face = Face(bottomLeft, bottomRight, topLeft)
      square = Face(bottomLeft, bottomRight, topRight, topLeft)
   }
   @Test
   fun whenBisectedSquareAndTwoVerticesAtEachSideOfFoldLine_Calculated(){
      val figure = makeFigureFrom(square)
      val bundle = figure.bundles.first()
      val foldLineDividesLeftFromRight = linePassingBy(midPoint(bottomLeft, bottomRight), midPoint(topLeft, topRight))
      val divider = BisectedFaceVertexDivider(bundle, setOf(square), foldLineDividesLeftFromRight)
      val dividedVertices = divider.divide()
      val expectedVerticesAtPosSide = square.vertices
              .filter { calculateSideOfFold(foldLineDividesLeftFromRight, it, bundle.plane) == POSITIVE }
      assertThat(dividedVertices[POSITIVE], `is`(expectedVerticesAtPosSide.toSet()))
      val expectedVerticesAtNegSide = square.vertices
              .filter { calculateSideOfFold(foldLineDividesLeftFromRight, it, bundle.plane) == NEGATIVE }
      assertThat(dividedVertices[NEGATIVE], `is`(expectedVerticesAtNegSide.toSet()))
   }
}