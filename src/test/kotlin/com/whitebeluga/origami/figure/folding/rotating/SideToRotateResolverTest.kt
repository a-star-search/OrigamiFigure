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
import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Point
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.TestVertex
import com.whitebeluga.origami.figure.folding.breakingup.FaceSplitter
import com.whitebeluga.origami.figure.folding.rotating.SideToRotateResolver.resolveSideToRotate
import org.hamcrest.Matchers.lessThan
import org.junit.Assert.assertThat
import org.junit.Test

class SideToRotateResolverTest {
   @Test
   fun segmentSplitsTriangleIntoDifferentSizedTriangles_ThePartToRotateShouldBeTheSmallerTriangle(){
      val maxY = 1
      val closeToMaxY = 0.8
      val segmentBisectsTriangleCloseToAVertex = LineSegment(Point(0, closeToMaxY, -10), Point(0, closeToMaxY, 10))
      val v1 = TestVertex(0, 0, 0)
      val v2 = TestVertex(0, maxY, 0)
      val v3 = TestVertex(0, 0, 1)
      val face = Face(v1, v2, v3)
      val plane = Plane.planeFromOrderedPoints(v1, v2, v3)
      val splitFace = FaceSplitter(face, segmentBisectsTriangleCloseToAVertex, plane).splitFace()
      val split = splitFace.newFaces
      val splitSets = split.map{Pair(it.key, setOf(it.value))}.toMap()
      val sideToRotate = resolveSideToRotate(segmentBisectsTriangleCloseToAVertex, splitSets)
      val partToRotate = split[sideToRotate]!!
      val stationaryPart = split[sideToRotate.opposite()]!!
      assertThat(partToRotate.area(), lessThan(stationaryPart.area()))
   }
}