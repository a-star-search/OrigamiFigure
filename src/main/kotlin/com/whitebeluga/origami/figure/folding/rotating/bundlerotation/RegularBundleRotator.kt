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

package com.whitebeluga.origami.figure.folding.rotating.bundlerotation

import com.moduleforge.libraries.geometry._3d.Line
import com.moduleforge.libraries.geometry._3d.Point
import com.moduleforge.libraries.geometry._3d.Vector
import com.whitebeluga.origami.figure.Bundle
import com.whitebeluga.origami.figure.component.Vertex

/**
 * Regular Rotator class for non-flat folds.
 *
 * Rotates the bundles of the side that needs rotating while avoiding point duplication.
 */
class RegularBundleRotator(bundles: Set<Bundle>, line: Line, rotationDirection: Vector,
                           val angle: Double, verticesOnRotationLine: Set<Vertex>):
        BundleRotator(bundles, line, rotationDirection, verticesOnRotationLine) {
   override val mapOfVertexToRotatedVertex: Map<Vertex, Vertex> = makeMapOfVertexToRotated(bundles)

   fun rotateBundles(): Set<Bundle> = bundles.map { rotateBundle(it) }.toSet()
   override fun rotateVertex(vertex: Vertex): Vertex =
           Vertex(line.rotatePointAround(vertex, angle, rotationDirection)!!)
   /**
    * When rotating a vertex, the side with respect to the line is not important (every vertex of a face
    * is on the same side)
    *
    * However point rotation is used to rotate the plane and some creation points of the plane might be
    * at different sides of the fold line and, thus, have to be rotated in opposite directions.
    */
   override fun rotate(point: Point, oppositeDirection: Boolean): Point =
      if(oppositeDirection)
         line.rotatePointAround(point, angle, rotationDirection.negate())!!
      else
         line.rotatePointAround(point, angle, rotationDirection)!!
}