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

import com.google.common.annotations.VisibleForTesting
import com.moduleforge.libraries.geometry._3d.Line
import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Point

/**
 * Refers to the side of a face with respect to a fold.
 * It is defined recursively in the following way:
 * - A face has the same side as another face connected to it through any of its edges. Except if it passes over
 * the fold segment, in which case it has the opposite side.
 * - A face that is bisected by a fold segment, becomes two faces, each with a different side.
 * - For a vertex of a face bisected by a fold segment, the face is calculated with the method in this class.
 *
 * Note that the side is a function of a point, the line of the fold segment and the normal of a given plane.
 */
enum class SideOfFold {
   NEGATIVE {
      override fun opposite(): SideOfFold  = POSITIVE
   },
   POSITIVE {
      override fun opposite(): SideOfFold  = NEGATIVE
   };
   abstract fun opposite(): SideOfFold

   companion object {
      /**
       * Side of the point with respect to the line
       */
      @VisibleForTesting
      @JvmStatic fun calculateSideOfFold(l: Line, p: Point, plane: Plane): SideOfFold? {
         if(l.contains(p)) return null
         val closestPoint = l.closestPoint(p)
         val v = closestPoint.vectorTo(p)
         val cross = l.direction.cross(v)
         return if(plane.normal.dot(cross) > 0) POSITIVE else NEGATIVE
      }
   }
}