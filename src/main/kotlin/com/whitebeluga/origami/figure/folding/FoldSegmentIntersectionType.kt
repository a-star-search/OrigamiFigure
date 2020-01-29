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

package com.whitebeluga.origami.figure.folding

/**
 * Type of intersection between coplanar faces and line segments. Intended for line folds as defined by a line segment.
 *
 * Remember a "line fold" is defined by a line segment.
 *
 * This works only for convex polygons. Remember origami "faces" are always convex polygons
 *
 */
enum class FoldSegmentIntersectionType {
	/**
	 The fold segment goes along the edge and covers it completely, end to end
	 */
	COVERS_EDGE,
	PARTIAL_EDGE,
	/**
	 Crossing from one side to another or between a vertex and a side
	 but not along the edge
	 */
	ACROSS,
   /**
    * coplanar with the polygon, but it doesn't fully intersect it
    *
    * it needs to enter the polygon though (or be fully inside the polygon),
    * if the intersection point is one of the ends of the fold segment
    * (within the library's epsilon), then is not considered a partial intersection, but no intersection
    *
    * a partial intersection is also when it's completely inside the polygon, even if not touching
    * any side
    */
   PARTIAL,
   /**
    * If not on same plane or simply not intersected at all and not partially either
    *
    * It could intersect the face on one point by passing through in another plane (this is not the common scenario
    * this class is designed for, though) or it can touch the face's boundary.
    */
	NONE
}