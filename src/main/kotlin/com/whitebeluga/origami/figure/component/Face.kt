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

package com.whitebeluga.origami.figure.component

import com.moduleforge.libraries.geometry.Geometry.epsilonEquals
import com.moduleforge.libraries.geometry.GeometryConstants.TOLERANCE_EPSILON
import com.moduleforge.libraries.geometry._3d.*
import com.moduleforge.libraries.geometry._3d.Plane.planeFromOrderedPoints
import com.whitebeluga.origami.figure.component.Crease.Companion.creaseFromPointsAndVertices
import com.whitebeluga.origami.figure.folding.FoldSegmentIntersectionType
import com.whitebeluga.origami.figure.folding.FoldSegmentIntersectionType.*
import java.awt.Color
import java.lang.Math.PI

/**
 This class represent a face of the figure, which is a flat (not folded) continuous part of the
 sheet that makes the figure.

 In every figure, every continuous, flat surface is a face of the figure. In other words, in such a surface
 there can be only one face. There is a one-to-one relationship between faces and flat continuous surfaces
 of a figure.

 Important: Any constructor or factory method uses the same vertices they are passed and does not create new ones.
 This is crucial to maintain the connection integrity.
 */
class Face: Polygon {
   override val vertices: List<Vertex>
   override val edges: Set<Edge>
   /**
    * The edges as a list, where the first edge is made of the first and second vertex and so on.
    */
   val edgeList: List<Edge>
   /* this is only used to decide if this polygon face the same way as the layer stack's plane
   that contains it, if the normal of the plane is needed, use the plane variable */
   private val polygonNormal: Vector
   val creases: Set<Crease>

   /** constructors with list of vertices */
   constructor(vertices: List<Vertex>, frontColor: Color=DEFAULT_COLOR, backColor: Color=DEFAULT_COLOR,
               creases: Set<Crease> = setOf()):
           super(vertices, frontColor, backColor) {
      this.vertices = vertices
      edgeList = makeSegments(vertices)
      edges = edgeList.toSet()
      polygonNormal = planeFromOrderedPoints(vertices.take(3)).normal
      this.creases = creases
      assertCreases()
   }
   private fun assertCreases() {
      for(crease in creases) {
         val vs = crease.vertices
         assert(vertices.containsAll(vs))
         val ps = crease.points
         for(p in ps) {
            assert(edges.any {it.contains(p)} )
            assert(!vertices.contains(p))
         }
      }
   }
   constructor(vertices: List<Vertex>, colors: ColorCombination, creases: Set<Crease> = setOf()):
           this(vertices, colors.front, colors.back, creases)
   /** constructors with three or more vertices */
   constructor(v1: Vertex, v2: Vertex, v3: Vertex, vararg vRest: Vertex,
               frontColor: Color=DEFAULT_COLOR, backColor: Color=DEFAULT_COLOR):
           this(listOf(v1, v2, v3, *vRest), frontColor, backColor)
   constructor(v1: Vertex, v2: Vertex, v3: Vertex, vararg vRest: Vertex, colors: ColorCombination):
           this(listOf(v1, v2, v3, *vRest), colors)
   constructor(v1: Vertex, v2: Vertex, v3: Vertex, colors: ColorCombination):
           this(listOf(v1, v2, v3), colors)

   /**
    * Rotates a point 180 or pi rad around a line
    * I choose to call it 180, because most people immediately understand what it means
    */
   fun rotate180(line: Line): Face = rotateAcross(line)
   fun rotateAcross(line: Line): Face = rotateAcrossAndReturnWithOldToNewPointMapping(line).first
   /**
    * The second element of the pair is the map of vertex to rotated vertex.
    * If a vertex is not to be rotated (ie, falls exactly on the rotation line)
    * then it's not included in the map.
    */
   fun rotateAcrossAndReturnWithOldToNewPointMapping(line: Line): Pair<Face, Map<Vertex, Vertex>> {
      val rotatedPoints: MutableList<Vertex> = mutableListOf()
      val mapOriginalToRotated: MutableMap<Vertex, Vertex> = mutableMapOf()
      for(v in vertices){
         if (line.contains(v)) {
            rotatedPoints.add(v)
         } else {
            val rotated = Vertex(line.movePointAcross(v))
            mapOriginalToRotated[v] = rotated
            rotatedPoints.add(rotated)
         }
      }
      val rotationFunction: (Point)->Point = { p -> line.movePointAcross(p) }
      val newCreases = rotateCreases(mapOriginalToRotated, rotationFunction)
      val newPoints = rotatedPoints.asReversed() // facing opposite direction
      return Pair(Face(newPoints, colors, newCreases), mapOriginalToRotated)
   }
   fun rotateAround(line: Line, angle: Double, rotationDirection: Vector): Face =
           rotateAroundAndReturnWithOldToNewPointMapping(line, angle, rotationDirection).first
   /**
    * Rotates the polygon around a line and returns a new Face
    * That polygon should contain the vertices of this polygon that need not be rotated (fall on the line)
    *
    * It also returns a map of vertices of the original polygon to the new rotated polygon
    * with only for vertices
    */
   fun rotateAroundAndReturnWithOldToNewPointMapping(line: Line, angle: Double, rotationDirection: Vector):
           Pair<Face, Map<Vertex, Vertex>> {
      val rotatedPoints: MutableList<Vertex> = mutableListOf()
      val mapOriginalToRotated: MutableMap<Vertex, Vertex> = mutableMapOf()
      for(vertexToRotate in vertices)
         if (line.contains(vertexToRotate))
            rotatedPoints.add(vertexToRotate)
         else {
            val rotated = Vertex(line.rotatePointAround(vertexToRotate, angle, rotationDirection))
            mapOriginalToRotated[vertexToRotate] = rotated
            rotatedPoints.add(rotated)
         }
      val rotationFunction: (Point)->Point = { p -> line.rotatePointAround(p, angle, rotationDirection) }
      val newCreases = rotateCreases(mapOriginalToRotated, rotationFunction)
      return Pair(Face(rotatedPoints, colors, newCreases), mapOriginalToRotated)
   }
   private fun rotateCreases(mapOriginalToRotated: MutableMap<Vertex, Vertex>, rotate: (Point) -> Point): Set<Crease> {
      val newCreases = mutableSetOf<Crease>()
      for (crease in creases) {
         val creaseVertices = crease.vertices.map { mapOriginalToRotated.getOrDefault(it, it) }
         val creasePoints = crease.points.map { rotate(it) }
         val newCrease = creaseFromPointsAndVertices(creasePoints, creaseVertices)
         newCreases.add(newCrease)
      }
      return newCreases
   }
   fun edgeToNextVertex(vertex: Vertex): Edge {
      val next = nextVertex(vertex)
      return edges.first {it.isAnEnd(vertex) && it.isAnEnd(next)}
   }
   /**
    * True if a point is contained either inside the face or in one of the segments of the perimeter.
    *
    * Point is assumed to be in the same plane as face.
    *
    * This method is not in the polygon class because the algorithm is not generally valid for concave polygon
    * and I think I will only use it for "Face" objects anyway.
    */
   fun contains(p: Point): Boolean {
      if(edges.any{it.contains(p)})
         return true
      val angleAddition = vertices.withIndex().sumByDouble {
         (index, vertex) ->
         val indexOfPrevious = if(index == 0) vertices.lastIndex else index-1
         val vectorToPrevious = p.vectorTo(vertices[indexOfPrevious])
         val vectorToVertex = p.vectorTo(vertex)
         vectorToPrevious.angle(vectorToVertex)
      }
      val angleAdditionEpsilon  = TOLERANCE_EPSILON * vertices.size
      return epsilonEquals(angleAddition,2 * PI, angleAdditionEpsilon)
   }
   /**
    * Returns the part of the fold segment that intersects the face side to side (or vertex to side, vertex to vertex
    * for that matter)
    *
    * If the segment does not intersect or just goes along the side of a face, then null is returned
    */
   fun intersectingSegment(segment: LineSegment): LineSegment? {
      val intersections = this.intersections(segment).toList()
      return if (intersections.size == 2)
         LineSegment(intersections[0], intersections[1])
       else null
   }
   fun intersectedAcrossBy(segment: LineSegment): Boolean = intersectedBy(segment) == ACROSS
   fun partiallyIntersectedBy(segment: LineSegment): Boolean = intersectedBy(segment) == PARTIAL
   fun partiallyOrFullyIntersectedBy(segment: LineSegment): Boolean =
           with(intersectedBy(segment)){ this  == PARTIAL || this == ACROSS }
   /**
    * determines if the fold segment crosses the face side to side
    * touches an entire edge of the face
    * or does not touch the face at all
    *
    * note that this method cannot belong in the "polygon" parent class
    * because that class represents a concave polygon, while a face of
    * an origami figure is a convex polygon
    */
   fun intersectedBy(segment: LineSegment): FoldSegmentIntersectionType {
      val intersectionPoints: MutableSet<Point> = mutableSetOf()
      for (edge in edges) {
         if (segment.contains(edge))
            return COVERS_EDGE
         if (edge.overlaps(segment))
            return PARTIAL_EDGE
         segment.intersectionPoint(edge)?.let { point ->
            if(intersectionPoints.none { elem -> elem.epsilonEquals(point) })
               intersectionPoints.add(point) }
      }
      if (intersectionPoints.size >= 2)
         return ACROSS
      //both ends of segment either inside face or touching the boundary
      val ends = segment.endsAsList
      val bothEndsOfSegmentInsideFace = ends.all { contains(it) }
      if(bothEndsOfSegmentInsideFace)
         return PARTIAL
      //segment properly intersects the boundary (not just touches)
      if(intersectionPoints.size == 1){
         val intersectionPoint = intersectionPoints.first()
         val intersectionIsNotASegmentEnd = !segment.isAtEndPosition(intersectionPoint)
         if(intersectionIsNotASegmentEnd)
            return PARTIAL
      }
      return NONE
   }
   fun edgeWith(other: Face): Edge? {
      val commonEdges = edges.intersect(other.edges)
      assert(commonEdges.size <= 1)
      return if(commonEdges.isEmpty()) null else commonEdges.first()
   }
   /** Useful as part of the rotating algorithms
    *  Map where the keys are the new points and the values are the points that should replace those
    *  in the returned face
    */
   internal fun replaceVertices(vertexToReplacementVertex: Map<Vertex, Vertex>): Face {
      val noVertexToReplace = vertices.intersect(vertexToReplacementVertex.keys).isEmpty()
      return if(noVertexToReplace)
         this
      else {
         val newCreases = creases.map { crease ->
            val noCreaseVertexToReplace = crease.vertices.none { vertexToReplacementVertex.containsKey(it) }
            if(noCreaseVertexToReplace) {
               crease
            } else {
               val newVertices = crease.vertices.map { vertexToReplacementVertex.getOrDefault(it, it) }
               creaseFromPointsAndVertices(crease.pointList, newVertices)
            }
         }.toSet()
         Face(vertices.map { vertex ->  vertexToReplacementVertex.getOrDefault(vertex, vertex)}, colors, newCreases)
      }
   }
   fun addCrease(crease: Crease): Face =
      Face(this.vertices, frontColor, backColor, creases + crease)
	companion object {
      fun faceFromPoints(points: List<Point>, colors: ColorCombination=DEFAULT_COLOR_COMBINATION): Face {
         val vertices: List<Vertex> = points.map { it as? Vertex ?: Vertex(it)}
         return Face(vertices, colors)
      }
      private fun makeSegments(points: List<Vertex>): List<Edge> =
              (0 until points.size).map { Edge(points[it], points[(it + 1) % points.size]) }
   }
}

