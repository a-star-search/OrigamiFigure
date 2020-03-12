Module for an origami figure and all its different components.

Part of the backend of the origami application.

An important concept in this project is that of 'plane' and its direction, as a polygon face is on a
plane with direction. A polygon face can have a different color on each side.

If a "Plane" object is defined from a list of three points, if a viewer see that point arrangement to be
 anticlockwise, then the normal of the plane points to the viewer.

Let's define it in a different way to be clear about this concept: a clockwise rotation as perceived by an observer
determines that the observer's looking direction (vector) as forward.

It can also be said that the viewer is on the positive side of the plane defined by the such point list perceived
 to be in anticlockwise order.

Coordinate system:
==================
The coordinate system used in this project is one where an observer on the Z+ part of space would see
X growing to his right and Y growing upwards, just as cartesian coordinate system are usually represented.

This is important because this project is to be used by a view module that may or may not rely on libraries
in which the Z axis grows in the opposite direction when the observer sees the x growing to the right and the
y growing upward. In other words, keep this in mind because not all 3d libraries have the same axis directions.

Figure Component System:
========================
The vertices of a polygon are represented by 3D point objects that are never equals if they are different objects,
even if they are at the same exact spatial position.

This makes it easier to represent different vertices in the same spatial position, which is a common occurrence in
an origami figure (just a theoretical one, of course).

A segment or edge is equal to another if, and only if, the end points are equal between them (irrespective of order).
Again, there can be more than one segment occupying the same position in space.

Faces
=====
Every de-facto polygon of an origami figure should be represented by one, and exactly one, polygon type object.
The different algorithms that comprise a fold should be careful to keep this constraint.

This means, for example, that there cannot be two polygons connected together on the same plane unless they have been
folded one on top of the other.

The consequence of this is that sometimes it is necessary to do "unfoldings" when flat-folding along the connection of two
faces, the resulting faces need to be joined back together.

Note that the faces resulting of the process of folding a square are always convex. This has some relevance as certain
algorithms are different depending of the polygons being concave or convex (and usually much simpler in the latter case).

The visible part of a face, though, can be convex. But that is of little or no concern in this module. It might of
concern for the GUI or the rendering part of the application.

Bundles
=======
A figure is created by a set of "Bundles". A Bundle is a stack of layers of faces that share the same
plane. There is no such thing as a "Layer" class in the application though, as the information of the ordering of the faces
is just that, a mapping of faces to those which are below it and those which are over it for every other face that
overlaps a given face.

Unit test names:
================
I'm not satisfied with many of the unit tests names. It's challenging to give descriptive and not especially long
names to complex and specific geometric operations.

Sometimes it's easier to make a drawing and reference it in the
javadoc of the test case which will be given a generic name. It seems maintainable, as far as I can see...

If I find a better way to name unit tests, I should revisit them.

About the "side" to fold or crease
==================================
This is explained in the code, but since it is part of the interface of important methods such as folding and
creasing, I should discuss it here too.

The need for this piece of information is quite unintuitive at first, but it makes perfect sense.

When the need for it in the case of "flap" folding is understood it still requires some effort to understand that
in some cases (although uncommon) it is also needed to complete the AMAP fold! (or crease)

About the side in "first flap" folding:
Including the side of fold as parameter might look superfluous on an intuitive level
but bear in mind that a single "flap" of the right side is not the same as one of the left side!

Simply think of a square paper that has been folded vertically down the middle. Now consider a second fold, parallel
to the first along the new middle. One flap to fold will be the part joined: two faces that have to be folded together.
The other "flap" is a single face. In each case we end up with a different figure after the fold.

Creases
=======
Creases are line segments that belong to faces (not bundles). Although in real origami a crease can sometimes
not fully bisect a face, in this application it will always bisect the face it belongs to. It goes from a vertex
or point of an edge to another vertex or edge and it cannot lay exactly on an edge.

If one or both ends of a crease coincide with vertices of the face it belongs to, then both objects vertex of the face
and end  of the crease should be equal.

The folding, translation and rotation algorithms should ensure no duplication of crease ends that are vertices as well
as the vertices of the face.

There is no promise for the ends of a crease that lay on edges though. And creases on different or the same face that
share the same end position may have the same or different Point objects.

-----


/**
Miscellaneous utility functions for this project
 */
object Util {
//	@JvmStatic fun <T : Any> cycle(xs: List<T>): Sequence<T> {
//		var i = 0
//		return generateSequence { xs[i++ % xs.size] }
//	}
	/** can be used for logging */
//	inline fun methodName(): String = "fun ${Thread.currentThread().stackTrace[1].methodName}( )"
//	@JvmStatic fun asString(pol: Face): String = pol.vertices.map { toString(it) }.toString().replace(",", "")
//	@JvmStatic fun asString(points: List<Point>): String = points.map { toString(it) }.toString().replace(",", "")
//	@JvmStatic fun asString(xs: Collection<Face>): String =
//			  xs.map { it -> it.vertices.map { toString(it) } }.toString().replace(",", "")
	private fun trimDecimal(num: Double) = DecimalFormat("#.##", DecimalFormatSymbols(US)).format(num)
//	private fun toString(point: Point) = "(${trimDecimal(point.x())}|${trimDecimal(point.y())}|${trimDecimal(point.z())})"
}