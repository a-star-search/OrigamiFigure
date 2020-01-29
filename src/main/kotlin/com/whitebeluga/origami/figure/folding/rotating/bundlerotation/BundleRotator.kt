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

import com.moduleforge.libraries.geometry.Geometry.epsilonEquals
import com.moduleforge.libraries.geometry._3d.Line
import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Plane.planeFromOrderedPoints
import com.moduleforge.libraries.geometry._3d.Point
import com.moduleforge.libraries.geometry._3d.Vector
import com.whitebeluga.origami.figure.Bundle
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.Vertex
import com.whitebeluga.origami.figure.folding.SideOfFold
import com.whitebeluga.origami.figure.folding.SideOfFold.Companion.calculateSideOfFold
import java.lang.Math.PI

abstract class BundleRotator(val bundles: Set<Bundle>,
                             protected val line: Line,
                             protected val rotationDirection: Vector,
                             private val verticesOnRotationLine: Set<Vertex>) {
   abstract val mapOfVertexToRotatedVertex: Map<Vertex, Vertex>

   protected fun makeMapOfVertexToRotated(toBeRotated: Set<Bundle>): Map<Vertex, Vertex> {
      val allVertices = toBeRotated.flatMap { it.faces.flatMap { it.vertices } }.toSet()
      return allVertices.associateBy({ it }, { rotate(it) })
   }
   protected fun rotate(vertex: Vertex): Vertex =
           if(verticesOnRotationLine.contains(vertex)) vertex else rotateVertex(vertex)
   protected abstract fun rotateVertex(vertex: Vertex):  Vertex
   protected abstract fun rotate(point: Point, oppositeDirection: Boolean): Point
   protected fun rotateBundle(bundle: Bundle): Bundle {
      val aRotatedVertex = mapOfVertexToRotatedVertex.entries.first { (k, v) -> ! k.epsilonEquals(v) }.value
      val sideOfFold = calculateSideOfFold(line, aRotatedVertex, bundle.plane)!!
      val rotatedPlane = makeRotatedPlane(bundle.plane, sideOfFold)
      val faceToRotatedFace = bundle.faces.associateBy ({it}, { makeRotatedFace(it, mapOfVertexToRotatedVertex) })
      val facesToFacesAbove = bundle.facesToFacesAbove
              .map { (k, v) -> Pair(faceToRotatedFace[k]!!, v.map { faceToRotatedFace[it]!! }.toSet() ) }.toMap()
      return Bundle(rotatedPlane, faceToRotatedFace.values.toSet(), facesToFacesAbove )
   }
   private fun makeRotatedPlane(plane: Plane, sideOfFold: SideOfFold): Plane {
      val planeCreationPoints = plane.creationPoints
      return planeFromOrderedPoints(planeCreationPoints.map {
         val side = calculateSideOfFold(line, it, plane)
         val oppositeDirection = side == sideOfFold.opposite()
         rotate(it, oppositeDirection)
      })
   }
   companion object {
      private fun makeRotatedFace(face: Face, vertexToRotated: Map<Vertex, Vertex>) =
              Face(face.vertices.map { vertexToRotated[it]!!}, face.colors)
      fun makeRotator(bundles: Set<Bundle>, line: Line, rotationDirection: Vector, angle: Double,
                      verticesOnRotationLine: Set<Vertex> = setOf()): RegularBundleRotator {
         val isFlatFold = epsilonEquals(angle, PI)
         if(isFlatFold)
            throw IllegalArgumentException("Cannot make a flat fold with a regular rotator")
         return RegularBundleRotator(bundles, line, rotationDirection, angle, verticesOnRotationLine)
      }
      fun makePIRotator(bundleToRotateOver:Bundle, other: Set<Bundle>, line: Line, rotationDirection: Vector,
                        verticesOnRotationLine: Set<Vertex>) =
              BundlePIRotator(bundleToRotateOver, other, line, rotationDirection, verticesOnRotationLine)
   }
}