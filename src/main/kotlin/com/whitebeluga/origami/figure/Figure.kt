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

import com.moduleforge.libraries.geometry.Geometry.almostZero
import com.moduleforge.libraries.geometry._3d.LineSegment
import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Point
import com.moduleforge.libraries.geometry._3d.Vector
import com.whitebeluga.origami.figure.Bundle.Companion.makeFaceToFacesAbove
import com.whitebeluga.origami.figure.component.Crease
import com.whitebeluga.origami.figure.component.Crease.Companion.creaseFromPointsAndVertices
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.Vertex
import com.whitebeluga.origami.figure.creasemaking.CreaseMaker.Companion.makeAMAPCreaseMaker
import com.whitebeluga.origami.figure.creasemaking.CreaseMaker.Companion.makeFFCreaseMaker
import com.whitebeluga.origami.figure.folding.linefolding.LineFoldParameters
import com.whitebeluga.origami.figure.folding.linefolding.LineFolder.Companion.makeAMAPLineFolder
import com.whitebeluga.origami.figure.folding.linefolding.LineFolder.Companion.makeFFLineFolder
import java.awt.Color

/**
 * Immutable class.
 */
class Figure: FaceContainer {
   val center: Point
   val planes: Set<Plane>
   val bundles: Set<Bundle>
   val colors: Set<Color>

   constructor(bundle: Bundle): this(setOf(bundle))
   constructor (bundles: Set<Bundle>) : super(bundles.flatMap {it.faces}.toSet()) {
      this.bundles = bundles
      planes = bundles.map{it.plane}.toSet()
      center = calculateCenter(vertices)
      colors = (faces.map { it.backColor } + faces.map { it.frontColor }).toSet()
      //A figure is made of a paper that may have the same color or different colors on each face.
      // Therefore a figure ought to have one or two colors, but never more than two.
      assert(colors.size <= 2)
   }
   fun bundleOf(face: Face): Bundle = bundles.first { it.faces.contains(face) }
   fun bundlesConnectedTo(bundle: Bundle): Set<Bundle> {
      if(!bundles.contains(bundle))
         throw IllegalArgumentException()
      if(isMonoBundle())
         return emptySet()
      val facesConnectedToFacesInBundle = bundle.faces.flatMap { facesConnectedTo(it) }.toSet()
      val facesInOtherBundlesConnectedToBundle = facesConnectedToFacesInBundle - bundle.faces
      return facesInOtherBundlesConnectedToBundle.map{ bundleOf(it) }.toSet()
   }
   fun facesSharing(vertex: Vertex): Set<Face> {
      if(!vertices.contains(vertex))
         throw IllegalArgumentException()
      if(faces.size == 1)
         return faces
      return faces.filter { it.vertices.contains(vertex) }.toSet()
   }
   fun changeFrontColor(color: Color): Figure = changeFaceColor(color, true)
   fun changeBackColor(color: Color): Figure = changeFaceColor(color, false)
   private fun changeFaceColor(color: Color, isFront: Boolean): Figure {
      val originalPolygonsToRepainted: MutableMap<Face, Face> = mutableMapOf()
      faces.forEach {
                 if(isFront)
                    originalPolygonsToRepainted[it] = Face(it.vertices, color, it.backColor, it.creases)
                  else
                    originalPolygonsToRepainted[it] = Face(it.vertices, it.frontColor, color, it.creases)
      }
      val newLayerStacks = bundles.map { makeBundleFromReplacements(it, originalPolygonsToRepainted) }.toSet()
      return Figure(newLayerStacks)
   }
	fun translated(newCenter: Point): Figure {
      return if(center.epsilonEquals(newCenter))
            this
         else
            translated(center.vectorTo(newCenter))
   }
   fun translatedToOrigin(): Figure = translated(center.vectorToOrigin())
	private fun translated(vector: Vector): Figure {
      if(almostZero(vector.length()))
         throw IllegalArgumentException("Translation vector should have length.")
		val originalVerticesToTranslated: MutableMap<Vertex, Vertex> = mutableMapOf()
      val originalFacesToTranslated: MutableMap<Face, Face> = mutableMapOf()
		for (face in faces) {
         val translated = translateFace(face, originalVerticesToTranslated, vector)
         originalFacesToTranslated[face] = translated
		}
      val translatedBundles =
              bundles.map { makeBundleFromReplacements(it, originalFacesToTranslated, it.plane.shift(vector)) }
		return Figure(translatedBundles.toSet())
	}
   private fun translateFace(face: Face, originalVerticesToTranslated: MutableMap<Vertex, Vertex>, vector: Vector): Face {
      face.vertices.forEach {
         if (!originalVerticesToTranslated.containsKey(it))
            originalVerticesToTranslated[it] = Vertex(it.translate(vector))
      }
      val translatedVertices = face.vertices.map { originalVerticesToTranslated[it]!! }
      val translatedCreases = mutableSetOf<Crease>()
      for (crease in face.creases) {
         val creaseVertices = crease.vertices.map { originalVerticesToTranslated[it]!! }
         val creasePoints = crease.points.map { it.translate(vector) }
         val newCrease = creaseFromPointsAndVertices(creasePoints, creaseVertices)
         translatedCreases.add(newCrease)
      }
      return Face(translatedVertices, face.frontColor, face.backColor, translatedCreases)
   }
   private fun makeBundleFromReplacements(original: Bundle, replacements: Map<Face, Face>): Bundle =
           makeBundleFromReplacements(original, replacements, original.plane)
   private fun makeBundleFromReplacements(original: Bundle, replacements: Map<Face, Face>, newPlane: Plane): Bundle {
      val newFaces = original.faces.map {face -> replacements[face] ?: face }.toSet()
      val newFacesToFacesAbove = makeFaceToFacesAbove(original.facesToFacesAbove, replacements)
      return Bundle(newPlane, newFaces, facesToFacesAbove = newFacesToFacesAbove)
   }
   fun removeFace(face: Face): Figure {
      val newBundles = bundles.map { if(it.contains(face)) it.remove(face) else it }
      val notNull= newBundles.filterNotNull()
      return Figure(notNull.toSet())
   }
   fun isMonoBundle() = bundles.size == 1
   /**
    * A bit awkward name. I think it's better more understandable than lineFoldFirstFlap.
    * The rest of names follow the same pattern for consistency.
    */
   fun doFirstFlapLineFold(foldParameters: LineFoldParameters): Figure = makeFFLineFolder(this, foldParameters).fold()
   fun doAMAPLineFold(foldParameters: LineFoldParameters): Figure = makeAMAPLineFolder(this, foldParameters).fold()
   @JvmOverloads
   fun doFirstFlapCrease(crease: LineSegment, guiUserLookingDirection: Vector, pickedPointAsSideToCrease: Point? = null) =
      makeFFCreaseMaker(this, crease, guiUserLookingDirection, pickedPointAsSideToCrease).crease()
   fun doAMAPCrease(crease: LineSegment, guiUserLookingDirection: Vector) =
      makeAMAPCreaseMaker(this, crease, guiUserLookingDirection).crease()
   companion object {
      fun calculateCenter(points: Set<Point>): Point {
         fun midPoint(nums: List<Double>): Double = (nums.min()!! + nums.max()!!) / 2.0
         return Point(midPoint(points.map{it.x()}), midPoint(points.map{it.y()}), midPoint(points.map{it.z()}))
      }
   }
}