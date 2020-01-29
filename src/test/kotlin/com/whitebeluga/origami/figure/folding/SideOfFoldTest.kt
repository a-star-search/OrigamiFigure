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

package com.whitebeluga.origami.figure.folding

import com.moduleforge.libraries.geometry._3d.Line
import com.moduleforge.libraries.geometry._3d.Line.Z_AXIS
import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Point
import com.whitebeluga.origami.figure.folding.SideOfFold.Companion.calculateSideOfFold
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.ThreadLocalRandom

class SideOfFoldTest {
   @Test
   fun calculateSide_WhenPointOnLine_ShouldReturnNull(){
      val pl = Plane.planeFromOrderedPoints(Point(0, 0, 0), Point(1, 0, 0), Point(1, 1, 0))
      val line = Line.linePassingBy(Point(1, 0, 0), Point(2, 1, 0))
      val pointOnLine = Point(0, -1, 0)
      val side = calculateSideOfFold(line, pointOnLine, pl)
      assertNull(side)
   }
   @Test
   fun calculateSide_WhenTwoPointsOnXYPlaneAreOnDifferentSidesOfALine_ShouldReturnDifferentValues(){
      val pl = Plane.planeFromOrderedPoints(
              Point(0, 0, 0), Point(1, 0, 0), Point(1, 1, 0))
      val line = Line.linePassingBy(Point(1, 0, 0), Point(2, 1, 0))
      val pointsAtOneSide = listOf(
              Point(0, 2, 0), Point(-10, -1, 0), Point(3, 9, 0)) //a few random points
      val pointsAtOtherSide = listOf(
              Point(5, 0, 0), Point(7, -3, 0), Point(6, 1, 0)) //a few random points
      val sidesOfPointsAtOneSide = pointsAtOneSide.map { calculateSideOfFold(line, it, pl) }.toSet()
      assertThat(sidesOfPointsAtOneSide.size, `is`(1))
      val sidesOfPointsAtOtherSide = pointsAtOtherSide.map { calculateSideOfFold(line, it, pl) }.toSet()
      assertThat(sidesOfPointsAtOtherSide.size, `is`(1))
   }
   @Test
   fun calculateSide_WhenTwoPointsOnXZPlaneAreOnDifferentSidesOfALine_ShouldReturnDifferentValues() {
      val random = ThreadLocalRandom.current()
      val xzPlane = Plane.planeFromOrderedPoints(Point(0, 0, 0), Point(1, 0, 0), Point(1, 0, 1))
      val randomPointOnOneSide = Point(random.nextInt(-10, -1), 0, random.nextInt(-10, 10))
      val oneSide = calculateSideOfFold(Z_AXIS, randomPointOnOneSide, xzPlane)
      for (i in 1..10){
         val randomNeg = random.nextInt(-10, -1)
         val anotherRandomPointOnOneSide = Point(randomNeg, 0, random.nextInt(-10, 10))
         val side = calculateSideOfFold(Z_AXIS, anotherRandomPointOnOneSide, xzPlane)
         assertEquals(oneSide, side)
      }
      val randomPointOnAnotherSide = Point(random.nextInt(1, 10), 0, random.nextInt(-10, 10))
      val anotherSide = calculateSideOfFold(Z_AXIS, randomPointOnAnotherSide, xzPlane)
      for (i in 1..10){
         val randomPos = random.nextInt(1, 10)
         val anotherRandomPointOnAnotherSide = Point(randomPos, 0, random.nextInt(-10, 10))
         val side = calculateSideOfFold(Z_AXIS, anotherRandomPointOnAnotherSide, xzPlane)
         assertEquals(anotherSide, side)
      }
   }
}