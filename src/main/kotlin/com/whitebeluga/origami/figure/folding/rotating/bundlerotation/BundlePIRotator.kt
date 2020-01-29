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
 * In the case of the pi rotator, it will be the case that the bundle will be folded over itself
 * (that's the very reason that the angle is exactly PI, and cannot be more than that).
 *
 * When bundles are folded over themselves, the bundle folded over (as opposed to the other bundles which are
 * connected to it) has a different treatment, as it will have to be merged to create a new bundle from the two
 * parts.
 *
 * For that reason, we distinguish between the bundle to fold over and the rest of the bundles connected to it, both
 * in taking them as parameters and in returning them.
 */
class BundlePIRotator(private val bundleToRotateOver:Bundle,
                               private val restOfBundles: Set<Bundle>, line: Line, rotationDirection: Vector,
                      verticesOnRotationLine: Set<Vertex>):
        BundleRotator(restOfBundles + bundleToRotateOver, line, rotationDirection, verticesOnRotationLine) {
   override val mapOfVertexToRotatedVertex: Map<Vertex, Vertex> = makeMapOfVertexToRotated(bundles)
   fun rotate(): Pair<Bundle, Set<Bundle>>{
      val rotated = rotateBundle(bundleToRotateOver)
      val restOfRotated = restOfBundles.map { rotateBundle(it) }.toSet()
      return Pair(rotated, restOfRotated)
   }
   override fun rotateVertex(vertex: Vertex): Vertex =
           Vertex(line.rotatePointPIRadians(vertex)!!)
   override fun rotate(point: Point, oppositeDirection: Boolean): Point = line.rotatePointPIRadians(point)!!
}
