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

import com.moduleforge.libraries.geometry.Geometry.almostZero
import com.moduleforge.libraries.geometry.Geometry.epsilonEquals
import com.moduleforge.libraries.geometry._3d.Line
import com.moduleforge.libraries.geometry._3d.Line.*
import com.moduleforge.libraries.geometry._3d.Point
import com.moduleforge.libraries.geometry._3d.Vector
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.Math.PI
import java.util.concurrent.ThreadLocalRandom
import com.whitebeluga.origami.figure.component.TestVertex as V

class FaceTest {
   private lateinit var lineOnXAxis: Line
   private lateinit var lineOnZAxis: Line
   private lateinit var pointOfTriangle: V
   private lateinit var triangleOnXZPlaneWithASideOnXAxis: Face
   private var angle90  = PI / 2
   private lateinit var anyValidPointsOfAPolygon: List<V>

   @Before
   fun setup(){
      lineOnXAxis = linePassingBy(V(0, 0, 0), V(1, 0, 0))
      lineOnZAxis = linePassingBy(V(0, 0, 0), V(0, 0, 1))
      pointOfTriangle = V(1, 0, 1)
      triangleOnXZPlaneWithASideOnXAxis = Face(V(-1,0,0), V(1,0,0), pointOfTriangle)
      anyValidPointsOfAPolygon = listOf(V(0, 0, 0), V(1, 0, 0), V(1, 1, 0), V(1, 1, 1))
   }
   /**
    * "Upwards" defined as:
    *  An observer on the Z+ part of space, X grows to the observer's right, Y grows upwards, as
    *  cartesian coordinate system are usually represented
    */
   @Test
   fun rotate90DegreesUpwardsPointOnXPosZPos_ShouldEndOnXPosYPos() {
      val rotateYPositive = Vector(0, 1, 0)
      val rotated: Pair<Face, Map<Vertex, Vertex>> =
              triangleOnXZPlaneWithASideOnXAxis.rotateAroundAndReturnWithOldToNewPointMapping(
                      lineOnXAxis, angle90, rotateYPositive)
      val expectedRotatedPoint = Point(1, 1, 0)
      val rotatedPointsMap = rotated.second
      assertTrue(expectedRotatedPoint.epsilonEqualsFloatPrecision(rotatedPointsMap[pointOfTriangle]!!))
   }
   @Test
   fun rotate90DegreesDownwardsPointOnXPosZPos_ShouldEndOnXPosYNeg() {
      val rotateYNegative = Vector(0, -1, 0)
      val rotated: Pair<Face, Map<Vertex, Vertex>> =
              triangleOnXZPlaneWithASideOnXAxis.rotateAroundAndReturnWithOldToNewPointMapping(
                      lineOnXAxis, angle90, rotateYNegative)
      val expectedRotatedPoint = Point(1, -1, 0)
      val rotatedPointsMap = rotated.second
      assertTrue(expectedRotatedPoint.epsilonEqualsFloatPrecision(rotatedPointsMap[pointOfTriangle]!!))
   }
   @Test
   fun rotate_WhenASideOfPolygonOnTheRotationLine_TheTwoPointsOnTheLineAreTheSameInRotatedFigure() {
      val rotationDirection = Vector(0, 0, 1)
      val rotated: Pair<Face, Map<Vertex, Vertex>> =
              triangleOnXZPlaneWithASideOnXAxis.rotateAroundAndReturnWithOldToNewPointMapping(
                      lineOnXAxis, angle90, rotationDirection)
      //first: test the map, there is no point to point replacement for a point that does not rotate and
      // thus stays the same
      assertTrue(rotated.second.keys.none { lineOnXAxis.contains(it) })
      //second: test the rotated figure
      val pointsOfRotatedFigure = rotated.first.vertices
      val pointsNotRotated = pointsOfRotatedFigure.filter {lineOnXAxis.contains(it)}
      assertThat(pointsNotRotated.size, `is`(2))
      assertTrue(triangleOnXZPlaneWithASideOnXAxis.vertices.containsAll(pointsNotRotated))
   }
   @Test
   fun isShowingFaceWhenObserverVectorMakesObtuseAngleWithNormal(){
      //triangleOnXZPlaneWithASideOnXAxis is "looking down" towards Y negative
      val towardsYPositive = Vector(0, 1, 0)
      assertTrue(triangleOnXZPlaneWithASideOnXAxis.isShowingFront(towardsYPositive))

      val triangleLookingAtZPositive = Face(V(0, 0, 0), V(1, 0, 0), V(1, 1, 0))
      assertTrue(triangleLookingAtZPositive.isShowingFront(Vector(0, 0, -1)))
   }
   @Test
   fun pointListToCreatePolygon_ShouldMaintainSamePointStructureAfterCreation(){
      val pol = Face(anyValidPointsOfAPolygon)
      assertTrue(pol.hasSamePointStructure(anyValidPointsOfAPolygon))
   }
   @Test
   fun addPointToListThatCreatedPolygon_NotSamePointStructure(){
      val pol = Face(anyValidPointsOfAPolygon)
      val differentList = anyValidPointsOfAPolygon + V(4, 0, 0)
      assertFalse(pol.hasSamePointStructure(differentList))
   }
   @Test
   fun removePointFromListThatCreatedPolygon_NotSamePointStructure(){
      val pol = Face(anyValidPointsOfAPolygon)
      val differentList = anyValidPointsOfAPolygon.dropLast(1)
      assertFalse(pol.hasSamePointStructure(differentList))
   }
   @Test
   fun reverseListThatCreatedPolygon_NotSamePointStructure(){
      val pol = Face(anyValidPointsOfAPolygon)
      val reversed = anyValidPointsOfAPolygon.asReversed()
      assertFalse(pol.hasSamePointStructure(reversed))
   }
   @Test
   fun shiftPointListThatCreatedPolygon_ShouldMaintainSamePointStructure() {
      val points = listOf(V(0, 0, 0), V(1, 0, 0),
              V(1, 1, 0), V(1, 1, 1))
      val pol = Face(points)
      val shiftedPointList = points.takeLast(2) + points.take(2)
      assertTrue(pol.hasSamePointStructure(shiftedPointList))
   }
   @Test
   fun testNextVertex_ForATriangle() {
      val v1 = V(0, 0, 0)
      val v2 = V(0, 1, 0)
      val v3 = V(0, 0, 1)
      val face = Face(v1, v2, v3)
      assertEquals(v2, face.nextVertex(v1))
      assertEquals(v3, face.nextVertex(v2))
      assertEquals(v1, face.nextVertex(v3))
   }
   @Test
   fun rotateTrianglesAcross_XAxis_ShouldHave_SameX_InverseY_InverseZ_AndInversePointOrder(){
      val randomTrialCount = 100
      for(i in 0..randomTrialCount){
         val randomTriangle = makeTriangleOfRandomPoints()
         val vertices = randomTriangle.vertices
         val rotatedAcross = randomTriangle.rotateAcross(X_AXIS)
         val rotatedVertices = rotatedAcross.vertices
         val rotatedReversedVertices = rotatedVertices.asReversed()
         val rotatedReversedVerticesRotations = triangleVerticesRotationsMaintainingOrder(rotatedReversedVertices)
         val matchesFirstRotation = verticesMatchAcrossXAxis(vertices, rotatedReversedVerticesRotations[0])
         val matchesSecondRotation = verticesMatchAcrossXAxis(vertices, rotatedReversedVerticesRotations[1])
         val matchesThirdRotation = verticesMatchAcrossXAxis(vertices, rotatedReversedVerticesRotations[2])
         assertTrue(matchesFirstRotation || matchesSecondRotation || matchesThirdRotation)
      }
   }
   @Test
   fun rotateTrianglesAcross_YAxis_ShouldHave_SameY_InverseX_InverseZ_AndInversePointOrder(){
      val randomTrialCount = 100
      for(i in 0..randomTrialCount){
         val randomTriangle = makeTriangleOfRandomPoints()
         val vertices = randomTriangle.vertices
         val rotatedAcross = randomTriangle.rotateAcross(Y_AXIS)
         val rotatedVertices = rotatedAcross.vertices
         val rotatedReversedVertices = rotatedVertices.asReversed()
         val rotatedReversedVerticesRotations = triangleVerticesRotationsMaintainingOrder(rotatedReversedVertices)
         val matchesFirstRotation = verticesMatchAcrossYAxis(vertices, rotatedReversedVerticesRotations[0])
         val matchesSecondRotation = verticesMatchAcrossYAxis(vertices, rotatedReversedVerticesRotations[1])
         val matchesThirdRotation = verticesMatchAcrossYAxis(vertices, rotatedReversedVerticesRotations[2])
         assertTrue(matchesFirstRotation || matchesSecondRotation || matchesThirdRotation)
      }
   }
   @Test
   fun rotateTrianglesAcross_ZAxis_ShouldHave_SameZ_InverseX_InverseY_AndInversePointOrder(){
      val randomTrialCount = 100
      for(i in 0..randomTrialCount){
         val randomTriangle = makeTriangleOfRandomPoints()
         val vertices = randomTriangle.vertices
         val rotatedAcross = randomTriangle.rotateAcross(Z_AXIS)
         val rotatedVertices = rotatedAcross.vertices
         val rotatedReversedVertices = rotatedVertices.asReversed()
         val rotatedReversedVerticesRotations = triangleVerticesRotationsMaintainingOrder(rotatedReversedVertices)
         val matchesFirstRotation = verticesMatchAcrossZAxis(vertices, rotatedReversedVerticesRotations[0])
         val matchesSecondRotation = verticesMatchAcrossZAxis(vertices, rotatedReversedVerticesRotations[1])
         val matchesThirdRotation = verticesMatchAcrossZAxis(vertices, rotatedReversedVerticesRotations[2])
         assertTrue(matchesFirstRotation || matchesSecondRotation || matchesThirdRotation)
      }
   }
   /**
    * Vague method name but it's hard to name this geometry methods. Take a look at the similar method above
    */
   @Test
   fun rotateTrianglesAcross_ParallelOfXAxis_ShouldHaveCorrectPointsAndPointOrder(){
      val randomTrialCount = 100
      for(i in 0..randomTrialCount){
         val randomTriangle = makeTriangleOfRandomPoints()
         val vertices = randomTriangle.vertices
         val lineOriginPoint = Point(3, -2, 8) //any random point in space
         val parallelToXAxis = linePassingBy(lineOriginPoint, Point(lineOriginPoint.x() + 1, lineOriginPoint.y(), lineOriginPoint.z()))
         val rotatedAcross = randomTriangle.rotateAcross(parallelToXAxis)
         val rotatedVertices = rotatedAcross.vertices
         val rotatedReversedVertices = rotatedVertices.asReversed()
         //for an easier comparison, translate the points as if the points had been rotated across an axis
         val translatedVertices = vertices
                 .map { point -> V(point.x(), point.y() - lineOriginPoint.y(), point.z() - lineOriginPoint.z())}
         val translatedRotatedReversedVertices = rotatedReversedVertices
                 .map { point -> V(point.x(), point.y() - lineOriginPoint.y(), point.z() - lineOriginPoint.z())}
         val rotatedReversedVerticesRotations = triangleVerticesRotationsMaintainingOrder(translatedRotatedReversedVertices)
         val matchesFirstRotation = verticesMatchAcrossXAxis(translatedVertices, rotatedReversedVerticesRotations[0])
         val matchesSecondRotation = verticesMatchAcrossXAxis(translatedVertices, rotatedReversedVerticesRotations[1])
         val matchesThirdRotation = verticesMatchAcrossXAxis(translatedVertices, rotatedReversedVerticesRotations[2])
         assertTrue(matchesFirstRotation || matchesSecondRotation || matchesThirdRotation)
      }
   }
   /**
    * Vague method name but it's hard to name this geometry methods. Take a look at the similar method above
    */
   @Test
   fun rotateTrianglesAcross_ParallelOfYAxis_ShouldHaveCorrectPointsAndPointOrder(){
      val randomTrialCount = 100
      for(i in 0..randomTrialCount){
         val randomTriangle = makeTriangleOfRandomPoints()
         val vertices = randomTriangle.vertices
         val lineOriginPoint = Point(3, -2, 8) //any random point in space
         val parallelToYAxis = linePassingBy(lineOriginPoint, Point(lineOriginPoint.x(), lineOriginPoint.y() + 1, lineOriginPoint.z()))
         val rotatedAcross = randomTriangle.rotateAcross(parallelToYAxis)
         val rotatedVertices = rotatedAcross.vertices
         val rotatedReversedVertices = rotatedVertices.asReversed()
         //for an easier comparison, translate the points as if the points had been rotated across an axis
         val translatedVertices = vertices
                 .map { point -> V(point.x() - lineOriginPoint.x(), point.y(), point.z() - lineOriginPoint.z())}
         val translatedRotatedReversedVertices = rotatedReversedVertices
                 .map { point -> V(point.x() - lineOriginPoint.x(), point.y(), point.z() - lineOriginPoint.z())}
         val rotatedReversedVerticesRotations = triangleVerticesRotationsMaintainingOrder(translatedRotatedReversedVertices)
         val matchesFirstRotation = verticesMatchAcrossYAxis(translatedVertices, rotatedReversedVerticesRotations[0])
         val matchesSecondRotation = verticesMatchAcrossYAxis(translatedVertices, rotatedReversedVerticesRotations[1])
         val matchesThirdRotation = verticesMatchAcrossYAxis(translatedVertices, rotatedReversedVerticesRotations[2])
         assertTrue(matchesFirstRotation || matchesSecondRotation || matchesThirdRotation)
      }
   }
   @Test
   fun rotateTrianglesAcross_XAxis_OldToNewMapping_ShouldHave_SameX_InverseY_InverseZ(){
      val randomTrialCount = 1000
      for(i in 0..randomTrialCount){
         val randomTriangle = makeTriangleOfRandomPoints()
         val vertices = randomTriangle.vertices
         val rotation = randomTriangle.rotateAcrossAndReturnWithOldToNewPointMapping(X_AXIS)
         val mapping = rotation.second
         for(vertex in vertices) {
            if (mapping[vertex] != null) {
               assertTrue(isRotatedAcross(vertex, mapping[vertex]!!, X_AXIS))
            } else {
               //on the x axis
               assertTrue(almostZero(vertex.y()))
               assertTrue(almostZero(vertex.z()))
            }
         }
      }
   }
   @Test
   fun rotateTrianglesAcross_YAxis_OldToNewMapping_ShouldHave_SameY_InverseX_InverseZ(){
      val randomTrialCount = 100
      for(i in 0..randomTrialCount){
         val randomTriangle = makeTriangleOfRandomPoints()
         val vertices = randomTriangle.vertices
         val rotation = randomTriangle.rotateAcrossAndReturnWithOldToNewPointMapping(Y_AXIS)
         val mapping = rotation.second
         for(vertex in vertices)
            mapping[vertex]?.let {
               assertTrue(isRotatedAcross(vertex, it, Y_AXIS))}
      }
   }
   @Test
   fun rotateTrianglesAcross_ZAxis_OldToNewMapping_ShouldHave_SameZ_InverseX_InverseY(){
      val randomTrialCount = 100
      for(i in 0..randomTrialCount){
         val randomTriangle = makeTriangleOfRandomPoints()
         val vertices = randomTriangle.vertices
         val rotation = randomTriangle.rotateAcrossAndReturnWithOldToNewPointMapping(Z_AXIS)
         val mapping = rotation.second
         for(vertex in vertices)
            mapping[vertex]?.let {
               assertTrue(isRotatedAcross(vertex, it, Z_AXIS))}
      }
   }
   @Test
   fun testEdgeToNextVertex_Calculated(){
      val v1 = V(0, 0, 0)
      val v2 = V(0, 1, 0)
      val v3 = V(0, 0, 1)
      val face = Face(v1, v2, v3)
      val v1ToV2 = face.edges.first { it.isAnEnd(v1) && it.isAnEnd(v2)}
      assertThat(face.edgeToNextVertex(v1), `is`(v1ToV2))
      val v3ToV1 = face.edges.first { it.isAnEnd(v3) && it.isAnEnd(v1)}
      assertThat(face.edgeToNextVertex(v3), `is`(v3ToV1))
   }
   /**
    * Point is assumed to be in the same plane as face
    */
   @Test
   fun testContainsPoint_whenFaceIsTriangleAndPointOutsideFace_ShouldReturnFalse(){
      val v1 = V(0, 0, 0)
      val v2 = V(0, 1, 0)
      val v3 = V(0, 0, 1)
      val face = Face(v1, v2, v3)
      val pointOutside = Point(0, 0, -1)
      assertFalse(face.contains(pointOutside))
   }
   @Test
   fun testContainsPoint_whenFaceHasTwentySidesAndPointOutsideFace_ShouldReturnFalse(){
      val v1 = V(0, 0, 0)
      val v2 = V(0, 1, 0)
      val v3 = V(0, 0.99, 0.1)
      val v4 = V(0, 0.97, 0.2)
      val v5 = V(0, 0.94, 0.3)
      val v6 = V(0, 0.9, 0.4)
      val v7 = V(0, 0.85, 0.5)
      val v8 = V(0, 0.79, 0.6)
      val v9 = V(0, 0.72, 0.7)
      val v10 = V(0, 0.64, 0.8)
      val v11 = V(0, 0.55,0.9)
      val v12 = V(0, 0.45, 1)
      val v13 = V(0, 0.34, 1.1)
      val v14 = V(0, 0.22, 1.2)
      val v15 = V(0, 0.09, 1.3)
      val v16 = V(0, -0.05, 1.4)
      val v17 = V(0, -0.20, 1.5)
      val v18 = V(0, -0.36, 1.6)
      val v19 = V(0, -0.53, 1.7)
      val v20 = V(0, -0.71, 1.8)
      val face = Face(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20)
      val pointOutsideCloseToBoundary = Point(0, 0.5501, 0.9)
      assertFalse(face.contains(pointOutsideCloseToBoundary))
   }
   @Test
   fun testContainsPoint_whenFaceHasTwentySidesAndPointInsideFace_ShouldReturnTrue(){
      val v1 = V(0, 0, 0)
      val v2 = V(0, 1, 0)
      val v3 = V(0, 0.99, 0.1)
      val v4 = V(0, 0.97, 0.2)
      val v5 = V(0, 0.94, 0.3)
      val v6 = V(0, 0.9, 0.4)
      val v7 = V(0, 0.85, 0.5)
      val v8 = V(0, 0.79, 0.6)
      val v9 = V(0, 0.72, 0.7)
      val v10 = V(0, 0.64, 0.8)
      val v11 = V(0, 0.55,0.9)
      val v12 = V(0, 0.45, 1)
      val v13 = V(0, 0.34, 1.1)
      val v14 = V(0, 0.22, 1.2)
      val v15 = V(0, 0.09, 1.3)
      val v16 = V(0, -0.05, 1.4)
      val v17 = V(0, -0.20, 1.5)
      val v18 = V(0, -0.36, 1.6)
      val v19 = V(0, -0.53, 1.7)
      val v20 = V(0, -0.71, 1.8)
      val face = Face(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20)
      val pointInsideCloseToBoundary = Point(0, 0.5499, 0.9)
      assertTrue(face.contains(pointInsideCloseToBoundary))
   }
   companion object {
      fun makeTriangleOfRandomPoints(): Face {
         val trianglePointSet = mutableSetOf<List<Int>>()
         val min = 0
         val max = 10
         val thingy = ThreadLocalRandom.current()
         while (trianglePointSet.size < 3)
            trianglePointSet.add(listOf(thingy.nextInt(min, max), thingy.nextInt(min, max), thingy.nextInt(min, max)))
         val trianglePointList = trianglePointSet.toList()
         val v1 = trianglePointList[0]
         val v2 = trianglePointList[1]
         val v3 = trianglePointList[2]
         return Face(V(v1[0], v1[1], v1[2]), V(v2[0], v2[1], v2[2]), V(v3[0], v3[1], v3[2]))
      }
      private fun triangleVerticesRotationsMaintainingOrder(vertices: List<Vertex>): List<List<Vertex>>{
         assert(vertices.size == 3)
         val possibleMatching_2 = vertices.drop(1) + vertices[0]
         val possibleMatching_3 = listOf(vertices[2]) + vertices.take(2)
         return listOf(vertices, possibleMatching_2, possibleMatching_3)
      }
      private fun verticesMatchAcrossXAxis(vertices1: List<Vertex>, vertices2: List<Vertex>) =
              verticesMatchAcrossAxis(vertices1, vertices2, X_AXIS)
      private fun verticesMatchAcrossYAxis(vertices1: List<Vertex>, vertices2: List<Vertex>) =
              verticesMatchAcrossAxis(vertices1, vertices2, Y_AXIS)
      private fun verticesMatchAcrossZAxis(vertices1: List<Vertex>, vertices2: List<Vertex>) =
              verticesMatchAcrossAxis(vertices1, vertices2, Z_AXIS)
      private fun verticesMatchAcrossAxis(vertices1: List<Vertex>, vertices2: List<Vertex>, axis: Line) =
              vertices1.zip(vertices2).fold(true){ result, elem -> isRotatedAcross(elem.first, elem.second, axis) && result }
      private fun isRotatedAcross(v: Vertex, rotated: Vertex, axis: Line) : Boolean =
              when (axis) {
                 X_AXIS -> epsilonEquals(v.x(), rotated.x()) && epsilonEquals(v.y(), -rotated.y()) && epsilonEquals(v.z(), -rotated.z())
                 Y_AXIS -> epsilonEquals(v.x(), -rotated.x()) && epsilonEquals(v.y(), rotated.y()) && epsilonEquals(v.z(), -rotated.z())
                 Z_AXIS -> epsilonEquals(v.x(), -rotated.x()) && epsilonEquals(v.y(), -rotated.y()) && epsilonEquals(v.z(), rotated.z())
                 else -> throw RuntimeException("not an axis")
              }
   }

}
