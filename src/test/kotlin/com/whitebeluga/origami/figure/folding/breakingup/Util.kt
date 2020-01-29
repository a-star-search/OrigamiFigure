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

import com.moduleforge.libraries.geometry._3d.Point
import com.whitebeluga.origami.figure.Figure
import com.whitebeluga.origami.figure.component.Face

object Util {

   fun findFishBaseRightSideFlap(fishBase: Figure): Face {
      val someValue = -1.0 / Math.sqrt(2.0)
      val cornerRightFlap = Point(0.0, 1.0 + someValue, 0.0)
      val right = Point(1.0 + someValue, 0.0, 0.0)
      val bottom = Point(0.0,  someValue, 0.0)
      val pointsInRightSideFlap = listOf(cornerRightFlap, right, bottom)
      return fishBase.faces.first { face ->
         pointsInRightSideFlap.all { pointInRightSideFlap ->
            face.vertices.any { it.epsilonEquals(pointInRightSideFlap) }
         }
      }
   }
}