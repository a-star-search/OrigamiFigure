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

package com.whitebeluga.origami.figure.folding.rotating

import com.moduleforge.libraries.geometry._3d.LineSegment
import com.moduleforge.libraries.geometry._3d.Point
import com.moduleforge.libraries.geometry._3d.Point.midPoint
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.folding.SideOfFold
import com.whitebeluga.origami.figure.folding.SideOfFold.NEGATIVE
import com.whitebeluga.origami.figure.folding.SideOfFold.POSITIVE

/**
 * When making a line fold, the figure is divided in two parts by the fold segment,
 * There is decision of which of the two parts "rotates" in order to complete the fold
 * Naturally, this concerns the application graphical interface ONLY, but it can be used by
 * any interface we choose, whether a web interface, desktop application, ... All of them
 * need coordinates, and those coordinates imply a certain movement of each part hinging on
 * the fold segment
 *
 * At the time of this comment, this class offers a single algorithm which is not very sophisticated
 * (this might be improved in the future): The middle point of the fold segment is calculated
 */
internal object SideToRotateResolver {
   /**
    * Returns the part to be rotated.
    * It returns the same object passed as parameter.
    * No polygon in the parameters should intersects the fold segment.
    * Each polygon in the parameters should either connected to another polygon or one of its side lays on the fold segment.
    * And the rest of conditions that an Face should meet (refer to the class' javadocs)
    *
    * Returns a map because it makes it easier for the class user to get either the part to rotate or the stationary part.
    */
   fun resolveSideToRotate(foldSegment: LineSegment, parts: Map<SideOfFold, Set<Face>>): SideOfFold =
           partWithoutTheFarthestPoint(foldSegment, parts[POSITIVE]!!, parts[NEGATIVE]!!)
   private fun partWithoutTheFarthestPoint(foldSegment: LineSegment, positivePart: Set<Face>, negativePart: Set<Face>):
           SideOfFold {
      val midPointOfFoldSegment = midPoint(foldSegment.points.value0, foldSegment.points.value1)
      val farthestPointInPosPart = farthestPointInFaces(midPointOfFoldSegment, positivePart).distance(midPointOfFoldSegment)
      val farthestPointInNegPart = farthestPointInFaces(midPointOfFoldSegment, negativePart).distance(midPointOfFoldSegment)
      return if(farthestPointInPosPart > farthestPointInNegPart)
         NEGATIVE
      else POSITIVE
   }
   private fun farthestPointInFaces(reference: Point, polygons: Set<Face>): Point {
      val allPoints= mutableSetOf<Point>()
      polygons.forEach { pol -> allPoints.addAll(pol.vertices ) }
      return farthestPointAmongPoints(reference, allPoints)
   }
   private fun farthestPointAmongPoints(reference: Point, points: Set<Point>): Point {
      var farthestDistance = 0.0
      var farthest: Point? = null
      for (it in points) {
         val distance = it.distance(reference)
         if (farthest == null || distance > farthestDistance ){
            farthest = it
            farthestDistance = distance
         }
      }
      return farthest!!
   }
}