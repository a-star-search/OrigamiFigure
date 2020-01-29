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

import com.google.common.annotations.VisibleForTesting
import com.moduleforge.libraries.geometry._3d.LineSegment
import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Point
import com.whitebeluga.origami.figure.component.Crease
import com.whitebeluga.origami.figure.component.Crease.Companion.creaseFromPointAndVertex
import com.whitebeluga.origami.figure.component.Crease.Companion.creaseFromPoints
import com.whitebeluga.origami.figure.component.Edge
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.Vertex
import com.whitebeluga.origami.figure.folding.SideOfFold
import com.whitebeluga.origami.figure.folding.SideOfFold.Companion.calculateSideOfFold
import com.whitebeluga.origami.figure.folding.SideOfFold.NEGATIVE
import com.whitebeluga.origami.figure.folding.SideOfFold.POSITIVE

/**
 * Takes a face and a segment that bisects it and divides it into two new faces.
 * 
 * Belongs in the Face class but it's too much code to have it there.
 *
 * We assume that the segment does bisect the polygon in two parts.
 *
 * It's a class instantiated by a face and a segment instead of an object, in order to reduce passing parameters
 * throughout all of the algorithm functions.
 *
 */
internal class FaceSplitter(
        val face: Face,
        private val foldSegment: LineSegment,
        /**
         * The plane of the bundle the face is in.
         *
         * It is necessary to establish the side of each new face
         */
        private val bundlePlane: Plane) {
   val vertices = face.vertices
   val edges = face.edges
   val creases = face.creases
   @VisibleForTesting
   private val splitPoints: Map<SideOfFold, MutableList<Vertex>> = mapOf( POSITIVE to mutableListOf(), NEGATIVE to mutableListOf() )
   private val edgeToVertexMap: MutableMap<Edge, Vertex> = mutableMapOf()
   private val splitCreases: Map<SideOfFold, MutableSet<Crease>> = mapOf( POSITIVE to mutableSetOf(), NEGATIVE to mutableSetOf() )

   /**
    * We make a method to actually calculate the split faces. The reason for not calculating on creation
    * is so that some of the methods might be testable, methods that use mutable class members.
    * (And the reason for mutable class members is
    * for brevity of code (ie. not always creating and returning variables throughout the class) )
    */
   fun splitFace(): SplitFace {
      dividePointsOfFaceBySegment()
      splitCreases()
      val newFaces = mapOf(
              POSITIVE to Face(splitPoints[POSITIVE]!!, face.colors, splitCreases[POSITIVE]!!),
              NEGATIVE to Face(splitPoints[NEGATIVE]!!, face.colors, splitCreases[NEGATIVE]!!))
      return SplitFace(face, newFaces, edgeToVertexMap)
   }
   /**
    * Segment can be from side to side, or from vertex to vertex or from side to vertex.
    *
    * Keeps the order of the points, that is the face orientation of the faces that can be constructed with each of the
    * two lists of divided points.
    */
   private fun dividePointsOfFaceBySegment() {
      val intersectedVertices: List<Vertex> = vertices.filter { vertex -> foldSegment.contains(vertex) }
      assert(intersectedVertices.size <= 2)
      val intersectedEdgesMap: Map<Edge, Point> =
              edges.associateBy({it}, {foldSegment.intersectionPoint(it) }).filterValues { it != null }
                      //ensure it intersects the segment, and doesn't just pass by a vertex
                      .filter { (edge, intersection) -> edge.containsAndNotAtEndPosition(intersection) }
      assert(intersectedEdgesMap.size <= 2)
      assert(intersectedEdgesMap.size + intersectedVertices.size == 2)
      dividePointsOfFace(intersectedVertices, intersectedEdgesMap)
   }
   private fun dividePointsOfFace(intersectedVertices: List<Vertex>, intersectedEdgesMap: Map<Edge, Point>) {
      val isVertexToVertex = intersectedVertices.size == 2
      val isSideToSide = intersectedEdgesMap.size == 2
      when {
         isVertexToVertex -> {
            dividePointsOfFace_VertexToVertexSegment(intersectedVertices)
            assert(splitPoints.values.sumBy { it.size } == (face.vertices.size + 2))
         }
         isSideToSide -> {
            dividePointsOfFace_EdgeToEdgeSegment(intersectedEdgesMap)
            assert(splitPoints.values.sumBy { it.size } == (face.vertices.size + 4))
         }
         else -> { //between vertex and side
            val vertex = intersectedVertices.first()
            val segment = intersectedEdgesMap.keys.first()
            val intersectionPoint = intersectedEdgesMap.values.first()
            dividePointsOfFace_VertexToEdgeSegment(vertex, segment, intersectionPoint)
            assert(splitPoints.values.sumBy { it.size } == (face.vertices.size + 3))
         }
      }
   }
   private fun dividePointsOfFace_VertexToVertexSegment(intersectionVertices: List<Vertex>) =
           vertices.forEach {vertex ->
              if (intersectionVertices.contains(vertex))
                 splitPoints.values.forEach { it.add(vertex)}
              else
                 splitPoints[sideOfFold(vertex)]!!.add(vertex)
           }
   /**
    * Divides the points of a face in two parts divided by a segment (segment is not passed as parameter)
    * The intersection points belong in both lists that are returned
    */
   private fun dividePointsOfFace_EdgeToEdgeSegment(edgeToIntersection: Map<Edge, Point>) {
      assert(edges.containsAll(edgeToIntersection.keys))
      for (vertex in vertices) {
         splitPoints[sideOfFold(vertex)]!!.add(vertex)
         val edgeOfVertex = face.edgeToNextVertex(vertex)
         edgeToIntersection[edgeOfVertex]?.let { intersectionPoint ->
            newVertexCreationFromIntersectionBetweenFoldSegmentAndEdge(intersectionPoint, edgeOfVertex) }
      }
   }
   private fun dividePointsOfFace_VertexToEdgeSegment(intersectionVertex: Point, intersectedEdge: Edge, intersectionPoint: Point) =
      vertices.forEach { vertex ->
         val foldSide = sideOfFold(vertex)
         val isIntersectionVertex = vertex == intersectionVertex
         val nextEdgeIsIntersected = face.edgeToNextVertex(vertex) == intersectedEdge
         when {
            isIntersectionVertex -> splitPoints.values.forEach { it.add(vertex)} //add to both lists
            nextEdgeIsIntersected -> {
               //add vertex and add intersection point with next edge to both lists
               splitPoints[foldSide]!!.add(vertex)
               newVertexCreationFromIntersectionBetweenFoldSegmentAndEdge(intersectionPoint, intersectedEdge)}
            //simply add vertex
            else -> splitPoints[foldSide]!!.add(vertex)
         }
      }
   /** Adds the new vertex to a list and to a map of edges to new points. */
   private fun newVertexCreationFromIntersectionBetweenFoldSegmentAndEdge(intersectionPoint: Point, edge: Edge) {
      val vertex = Vertex(intersectionPoint)
      //add to both lists
      splitPoints.values.forEach { it.add(vertex) }
      edgeToVertexMap[edge] = vertex
   }
   private fun splitCreases() {
      for(crease in creases)
         splitCrease(crease)
   }
   private fun splitCrease(crease: Crease) {
      val isOnFoldLine = crease.line.containsAll(foldSegment.endsAsList)
      if(isOnFoldLine) //the crease "disappears" when the fold is done along it.
         return
      val intersectionWithFoldSegment = foldSegment.intersectionPoint(crease)
      val noIntersection = intersectionWithFoldSegment == null
      if(noIntersection)
         assignNotIntersectedCrease(crease)
      else
         splitCreaseWhenItIntersectsFoldSegment(crease, intersectionWithFoldSegment)
   }
   private fun assignNotIntersectedCrease(crease: Crease) {
      val randomPointOfCrease = crease.getPoints().value0
      val side = sideOfFold(randomPointOfCrease)!!
      splitCreases[side]!!.add(crease)
   }
   private fun splitCreaseWhenItIntersectsFoldSegment(crease: Crease, intersection: Point) {
      //neither on an edge nor at a vertex position
      val foldSegmentAndCreaseIntersectionIsNotOnTheFaceBoundary =
              edges.none { it.contains(intersection) }
      if(foldSegmentAndCreaseIntersectionIsNotOnTheFaceBoundary) {
         //it means the two segments make a cross, and neither end of the crease lays exactly on the fold segment
         val foldSegmentAndCreaseMakeCross =
            crease.endsAsList.none { foldSegment.contains(it) }
         if(foldSegmentAndCreaseMakeCross)
            splitCreaseWhenThereIsAPartOfTheCreaseOnEachSide(crease, intersection)
      }
      //at this point, the crease can only wholly belong to only one of the split face parts
      assignCreaseWhenTheCreaseBelongsToOnlyASplitFacePart(crease)
   }
   private fun splitCreaseWhenThereIsAPartOfTheCreaseOnEachSide(crease: Crease, intersection: Point) {
      val creaseEnds = crease.endsAsList
      val firstCreaseEnd = creaseEnds.first()
      val side = sideOfFold(firstCreaseEnd)!!
      val crease1 = if(firstCreaseEnd is Vertex) creaseFromPointAndVertex(intersection, firstCreaseEnd)
         else creaseFromPoints(intersection, firstCreaseEnd)
      splitCreases[side]!!.add(crease1)
      val secondCreaseEnd = creaseEnds[1]
      val crease2 = if(secondCreaseEnd is Vertex) creaseFromPointAndVertex(intersection, secondCreaseEnd)
         else creaseFromPoints(intersection, secondCreaseEnd)
      splitCreases[side.opposite()]!!.add(crease2)
   }
   private fun assignCreaseWhenTheCreaseBelongsToOnlyASplitFacePart(crease: Crease) {
      val creaseEnds = crease.endsAsList
      val side = sideOfFold(creaseEnds.first()) ?: sideOfFold(creaseEnds[1])!!
      splitCreases[side]!!.add(crease)
   }
   private fun sideOfFold(p: Point) = calculateSideOfFold(foldSegment.line, p, bundlePlane)
}