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

package com.whitebeluga.origami.figure

import com.moduleforge.libraries.geometry.Geometry.almostZero
import com.moduleforge.libraries.geometry.Geometry.epsilonEquals
import com.moduleforge.libraries.geometry._3d.Plane.planeFromOrderedPoints
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.Vertex
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.Math.PI
import java.lang.Math.sqrt

class ZAxisFigureRotatorTest {
   private val halfRightAngle = PI / 4.0
   @Test
   fun testTriangleRotatedQuarterPIRadians_Calculated() {
      val v1 = Vertex(1.0, -1.0, 0.0)
      val v2 = Vertex(1.0, 1.0, 0.0)
      val v3 = Vertex(-1.0, 1.0, 0.0)
      val v4 = Vertex(-1.0, -1.0, 0.0)
      val square = Face(listOf(v1, v2, v3, v4))
      val points = listOf(v1, v2, v3)
      val plane = planeFromOrderedPoints(points)
      val bundle = Bundle(plane, square)
      val figure = Figure(bundle)
      val rotated = ZAxisFigureRotator.rotateFigure(figure, halfRightAngle)
      val rotatedPoints = rotated.faces.iterator().next().vertices
      val rotatedFirst = rotatedPoints[0]
      assertTrue(almostZero(rotatedFirst.x))
      assertTrue(epsilonEquals(rotatedFirst.y, -sqrt(2.0)))
   }
}
