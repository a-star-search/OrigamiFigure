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

package com.whitebeluga.origami.figure.folding.breakingup.bisectedfacefindingandfaceseparation

import com.moduleforge.libraries.geometry._3d.LineSegment
import com.moduleforge.libraries.geometry._3d.Point
import com.whitebeluga.origami.figure.Bundle
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.folding.exceptions.InvalidFoldException

/**
 A "first flap" type line fold needs to have the visible bisected faces as seen by the user.

 It could be possible to simply find the shallowest bisected face and from it find all connected to it.
 However that doesn't allow to discover errors or fold more than one flap when both are visible and there might be
 more flaps behind them that the user does not want to fold.

 To gain more control and information we use this algorithm to find all visible faces as seen by the user.

 The algorithm is similar to that used by the effective fold segment finder but in that case a fold segment is only
 hidden by partially bisected face. While in this case it's hidden by either a total or partial bisection (obvious since
 we are looking for the 'visible' part)
 */
internal class VisibleBisectedFaceFinder(
        private val foldSegment: LineSegment,
        private val bundle: Bundle) {
   fun findVisibleBisectedFacesFromBottom(): Set<Face> =
           VisibleBisectedFaceFinder(foldSegment, bundle.reverse()).findVisibleBisectedFacesFromTop()
   fun findVisibleBisectedFacesFromTop(): Set<Face> {
      val totallyIntersectedByFoldSegment = bundle.faces.filter { it.intersectedAcrossBy(foldSegment) }.toSet()
      val visible = totallyIntersectedByFoldSegment
              .filter { fullyOrPartiallyVisibleAlongFoldSegment(it) }
      return visible.toSet()
   }
   /** true if visible or partially visible in the part of the intersecting segment*/
   private fun fullyOrPartiallyVisibleAlongFoldSegment(intersectedFace: Face): Boolean {
      val facesAbove = bundle.facesToFacesAbove[intersectedFace] ?: return true
      //we need the part of the segment that intersects the face, not the original full fold segment
      val intersectingSegment = intersectedFace.intersectingSegment(foldSegment)
              ?: throw RuntimeException("does not intersect the face")
      val fullyOrPartiallyIntersectedFacesAbove_ByIntersectingSegment = facesAbove.filter {  it.partiallyOrFullyIntersectedBy(intersectingSegment) }.toSet()
      val partiallyIntersectedFacesAbove_ByFoldSegment = facesAbove.filter { it.partiallyIntersectedBy(foldSegment) }.toSet()
      /*
      If another face is
        - Above the *face* in question
          AND
        - The part of the fold segment that intersects the *face* at least partially intersects that other face, then
        the full fold segment has to fully intersect that other face (or the fold is impossible)
        */
      if(fullyOrPartiallyIntersectedFacesAbove_ByIntersectingSegment.intersect(partiallyIntersectedFacesAbove_ByFoldSegment).isNotEmpty())
         throw InvalidFoldException()
      /*
        Given that the fold is possible with the previous conditions, check if visible by calculating the remaining
        part of the intersecting segment not covered by the faces above
      */
      val visibleIntersectingSegmentParts =
              calculateVisibleFoldSegmentsBehindFaces(setOf(intersectingSegment), fullyOrPartiallyIntersectedFacesAbove_ByIntersectingSegment)
      return visibleIntersectingSegmentParts.isNotEmpty()
   }
   companion object {
      private fun calculateVisibleFoldSegmentsBehindFaces(foldSegments: Set<LineSegment>, faces: Set<Face>): Set<LineSegment> {
         var visibleFoldSegments = foldSegments
         for(face in faces) {
            visibleFoldSegments = calculateVisibleFoldSegmentsBehindFace(visibleFoldSegments, face)
            if(visibleFoldSegments.isEmpty())
               return emptySet()
         }
         return visibleFoldSegments
      }
      private fun calculateVisibleFoldSegmentsBehindFace(foldSegments: Set<LineSegment>, face: Face): Set<LineSegment> {
         val intersecting = foldSegments.filter {face.partiallyOrFullyIntersectedBy(it)}.toSet()
         if(intersecting.isEmpty())
            return foldSegments
         val newSegments = intersecting.map { segment -> calculateSegmentDifference(segment, face) }
                 .flatten().toSet()
         val nonIntersecting = foldSegments - intersecting
         return nonIntersecting + newSegments
      }
      /**
       * Calculates the rest of the segment not in the face (if any). For a convex polygon (which a Face always is) the result
       * might be at most, two segments. Zero if completely inside the face. One otherwise.
       */
      private fun calculateSegmentDifference(segment: LineSegment, face: Face): Set<LineSegment> {
         //either face fully intersected side to side or vertex to vertex or otherwise segment fully inside the face
         val segmentInsideFace = segment.endsAsList.all { face.contains(it) }
         if(segmentInsideFace)
            return emptySet()
         val segments = mutableSetOf<LineSegment>()
         val outsideEnds = findEndsOutsideFace(segment, face)
         val intersections = face.intersections(segment)
         for(end in outsideEnds){
            val closestIntersection = intersections.minBy { end.distance(it) }!!
            segments.add(LineSegment(end, closestIntersection))
         }
         return segments
      }
      private fun findEndsOutsideFace(segment: LineSegment, face: Face): Set<Point> {
         val firstEnd = segment.endsAsList[0]
         val secondEnd = segment.endsAsList[1]
         return when {
            face.contains(firstEnd) -> setOf(secondEnd)
            face.contains(secondEnd) -> setOf(firstEnd)
            else -> setOf(firstEnd, secondEnd)
         }
      }
   }
}
