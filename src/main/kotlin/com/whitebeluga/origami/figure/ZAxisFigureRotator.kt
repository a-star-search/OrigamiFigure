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

import com.moduleforge.libraries.geometry._3d.Line
import com.moduleforge.libraries.geometry._3d.Line.Z_AXIS
import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Plane.planeFromOrderedPoints
import com.moduleforge.libraries.geometry._3d.Point
import com.moduleforge.libraries.geometry._3d.Vector
import com.whitebeluga.origami.figure.Bundle.Companion.makeFaceToFacesAbove
import com.whitebeluga.origami.figure.component.Crease.Companion.creaseFromPointsAndVertices
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.Vertex
import java.util.*

/**
 * Rotates the figure around the Z axis. It assumes the figure will be centered in the origin of coordinates.
 *
 * The reason for this object is to rotate the figure, so that it will be displayed in an orientation that makes
 * more sense for the user of the GUI.
 *
 * While this module is independent from the GUI specifics, the origami application itself is, in any case,
 * designed to be used through some sort of GUI and it makes no sense without it.
 *
 * There might some front end libraries that can provide this transformation (rotation around any one of the three
 * spatial axes) but some others cannot, and this object is designed for the latter.
 *
 * Note that at the very least, any decent 3D graphical library should be able to rotate an object around two spatial
 * axis since they can be easily matched with the two dimensions in which the mouse pointer moves.
 */
object ZAxisFigureRotator {
   private val rotationLine = Z_AXIS
   @JvmStatic
   fun rotateFigure(figure: Figure, angle: Double): Figure {
      val vertexToRotated = rotateVertices(figure, angle)
      val creasePointToRotated = rotateCreasePoints(figure, angle)
      val faceToRotatedFace = rotateFaces(figure, vertexToRotated, creasePointToRotated)
      return rotateFigure(figure, faceToRotatedFace, angle)
   }
   private fun rotateVertices(figure: Figure, angle: Double): Map<Vertex, Vertex> {
      val allVertices = figure.vertices
      val vertexToRotated = HashMap<Vertex, Vertex>()
      for (vertex in allVertices) {
         val rotated = rotate(vertex, angle)
         vertexToRotated[vertex] = rotated
      }
      return vertexToRotated
   }
   private fun rotateCreasePoints(figure: Figure, angle: Double): Map<Point, Point> {
      val allCreases = figure.bundles.flatMap { bundle -> bundle.faces.map { it.creases } }.flatten()
      val map = mutableMapOf<Point, Point>()
      allCreases.forEach {
         val points = it.points
         points.forEach { map[it] = rotatePoint(it, angle) }
      }
      return map
   }
   private fun rotate(vertex: Vertex, angle: Double): Vertex {
      val rotated = rotatePoint(vertex, angle)
      return Vertex(rotated)
   }
   private fun rotatePoint(p: Point, angle: Double): Point {
      val closestPointInLine = rotationLine.closestPoint(p)
      val lineToVertex = closestPointInLine.vectorTo(p).normalize()
      val zVector = if (angle > 0)
            Vector(0.0, 0.0, -1.0)
         else
            Vector(0.0, 0.0, 1.0)
      val direction = zVector.cross(lineToVertex)
      return rotationLine.rotatePointAround(p, angle, direction)
   }
   private fun rotateFaces(figure: Figure,
                           vertexToRotatedVertex: Map<Vertex, Vertex>,
                           creasePointToRotated: Map<Point, Point>): Map<Face, Face> {
      val faces = figure.faces
      val faceToRotatedFace = HashMap<Face, Face>()
      for (face in faces) {
         val rotatedVertices = face.vertices.map { vertex -> vertexToRotatedVertex[vertex]!! }
         val rotatedCreases = face.creases.map {
            creaseFromPointsAndVertices( it.points.map { p -> creasePointToRotated[p]!! }, it.vertices.map { v -> vertexToRotatedVertex[v]!!} )
         }.toSet()
         val rotatedFace = Face(rotatedVertices, face.colors, rotatedCreases)
         faceToRotatedFace[face] = rotatedFace
      }
      return faceToRotatedFace
   }
   private fun rotateFigure(figure: Figure, faceToRotatedFace: Map<Face, Face>, angle: Double): Figure {
      val bundles = figure.bundles
      val rotated = HashSet<Bundle>()
      for (bundle in bundles) {
         val rotatedBundle = rotateBundle(bundle, faceToRotatedFace, angle)
         rotated.add(rotatedBundle)
      }
      return Figure(rotated)
   }
   private fun rotateBundle(bundle: Bundle, faceToRotatedFace: Map<Face, Face>, angle: Double): Bundle {
      val faces = bundle.faces
      val rotatedFaces = HashSet<Face>()
      for (face in faces)
         rotatedFaces.add(faceToRotatedFace[face]!!)
      val faceToFacesAbove = bundle.facesToFacesAbove
      val rotatedFacesToFacesAbove = makeFaceToFacesAbove(faceToFacesAbove, faceToRotatedFace)
      val rotatedPlane = rotateBundlePlane(bundle, rotationLine, angle)
      return Bundle(rotatedPlane, rotatedFaces, rotatedFacesToFacesAbove)
   }
   private fun rotateBundlePlane(bundle: Bundle, rotationLine: Line, angle: Double): Plane {
      val planeCreationPoints = bundle.plane.creationPoints
      val rotatedPlaneCreationPoints = ArrayList<Point>()
      for (p in planeCreationPoints) {
         val rotatedPoint = rotatePoint(p, angle)
         rotatedPlaneCreationPoints.add(rotatedPoint)
      }
      return planeFromOrderedPoints(rotatedPlaneCreationPoints)
   }
}
