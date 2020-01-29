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

import com.google.common.base.Preconditions.checkArgument
import com.whitebeluga.origami.figure.component.Edge
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.Vertex

/**
 * A Figure or a Bundle are different face containers.
 *
 * Some times we want to perform an operation on the faces of a bundle only,
 * sometimes one that involves all faces of the figure.
 *
 * It is a very subtle distinction that can only be understood when examining certain
 * folding or opening (loosening) algorithms.
 *
 */
open class FaceContainer(val faces: Set<Face>) {
   val vertices: Set<Vertex> = faces.map {it.vertices}.flatten().toSet()
   val edges = faces.flatMap { it.edges }.toSet()
   val creases = faces.flatMap { it.creases }.toSet()
   /**
    * Returns a set of one face if the edge doesn't connect two faces
    * or
    * Returns a set of two faces that this edge connects
    */
   fun facesConnectedBy(edge: Edge): Set<Face> {
      val connected = faces.filter { it.edges.contains(edge) }.toSet()
      assert(connected.isNotEmpty() && connected.size <= 2)
      return connected
   }
   /**
    * Returns the face the face passed by parameter is connected to by the edge passed as parameter
    */
   fun connectedToByEdge(face: Face, edge: Edge): Face {
      checkArgument(faces.contains(face))
      checkArgument(face.edges.contains(edge))
      checkArgument(isAConnectingEdge(edge))
      val connected = facesConnectedBy(edge)
      return (connected - face).first()
   }
   /**
    * An edge that doesn't connect two faces and only belongs to one
    */
   fun isAFreeEdge(edge: Edge) = facesConnectedBy(edge).size == 1
   /**
    * An edge that connects two faces. An edge can belong to a single face or connect two faces. There are no
    * other alternatives.
    */
   fun isAConnectingEdge(edge: Edge) = facesConnectedBy(edge).size == 2
   fun mapOfFacesConnectedTo(face: Face): Map<Edge, Face> {
      if(!faces.contains(face))
         throw IllegalArgumentException("The face is not part of the figure.")
      return when {
         faces.size == 1 -> emptyMap()
         faces.size == 2 -> mapOfFacesConnectedTo(face, (faces - face).first())
         else -> mapOfFacesConnectedToWhenMoreThanTwoFaces(face)
      }
   }
   private fun mapOfFacesConnectedTo(face: Face, connectedFace: Face): Map<Edge, Face> {
      val edge = face.edges.intersect(connectedFace.edges).first()
      return mapOf(edge to connectedFace)
   }
   private fun mapOfFacesConnectedToWhenMoreThanTwoFaces(face: Face): Map<Edge, Face> {
      val connected = facesConnectedToWhenMoreThanTwoFaces(face)
      return connected.map { it.edges.intersect(face.edges).first() to it }.toMap()
   }
   fun facesConnectedTo(face: Face): Set<Face> {
      if(!faces.contains(face))
         throw IllegalArgumentException("The face is not part of the figure.")
      return when {
         faces.size == 1 -> emptySet()
         faces.size == 2 -> faces - face
         else -> facesConnectedToWhenMoreThanTwoFaces(face)
      }
   }
   private fun facesConnectedToWhenMoreThanTwoFaces(face: Face): Set<Face> {
      val otherFaces = faces - face
      return otherFaces.filter { it.edges.intersect(face.edges).isNotEmpty() }.toSet()
   }
}