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

package com.whitebeluga.origami.figure.creasemaking

import com.moduleforge.libraries.geometry._3d.LineSegment
import com.moduleforge.libraries.geometry._3d.Point
import com.moduleforge.libraries.geometry._3d.Vector
import com.whitebeluga.origami.figure.Bundle
import com.whitebeluga.origami.figure.Bundle.Companion.makeFaceToFacesAbove
import com.whitebeluga.origami.figure.Figure
import com.whitebeluga.origami.figure.component.Crease
import com.whitebeluga.origami.figure.component.Crease.Companion.creaseFromPointAndVertex
import com.whitebeluga.origami.figure.component.Crease.Companion.creaseFromPoints
import com.whitebeluga.origami.figure.component.Crease.Companion.creaseFromVertices
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.folding.SideOfFold
import com.whitebeluga.origami.figure.folding.SideOfFold.Companion.calculateSideOfFold
import com.whitebeluga.origami.figure.folding.SideOfFold.NEGATIVE
import com.whitebeluga.origami.figure.folding.SideOfFold.POSITIVE
import com.whitebeluga.origami.figure.folding.breakingup.bisectedfacefindingandfaceseparation.AMAPBisectedFaceFinderAndFaceSeparator
import com.whitebeluga.origami.figure.folding.breakingup.bisectedfacefindingandfaceseparation.FFBisectedFaceFinderAndFaceSeparator
import com.whitebeluga.origami.figure.folding.exceptions.InvalidFoldException
import com.whitebeluga.origami.figure.folding.exceptions.NoSideDefinedException

/**
 * In making a crease sometimes it's necessary to know the side in order to determine which flap are we increasing.
 *
 * In contrast with folding, when, in some cases, folding all possible layers still needs the side, this is not
 * the case with creasing. The side is only necessary in some cases of folding the most shallow flap.
 */
internal abstract class CreaseMaker(protected val figure: Figure,
                           protected val creaseSegment: LineSegment,
                           /**
                            * Information sent by the controller. This tells us which side the user is looking at when
                            * defining a fold segment, which allows this class to find out the faces that are supposed to be
                            * folded.
                            *
                            * The vector should be "cleaned" by the controller module, meaning that an actual looking
                            * direction vector is not passed, but rather a normal and perpendicular to the plane of the
                            * bundle to fold where this vector will make a angle of less than pi/2 with the actual looking
                            * direction.
                            */
                           protected val guiUserLookingDirection: Vector) {
   protected val bundleToCrease: Bundle

   init {
      /*
      bundle to crease is the bundle for which there is at least one face fully intersected by
      the fold segment
      */
      val bundleIntersectedByCreaseSegment = figure.bundles.firstOrNull { bundle ->
         bundle.faces.any { it.intersectedAcrossBy(creaseSegment) }
      }
      bundleToCrease = bundleIntersectedByCreaseSegment ?: throw RuntimeException("No bundle is intersected by the crease segment.")
   }
   companion object {
      fun makeAMAPCreaseMaker(figure: Figure, creaseSegment: LineSegment, guiUserLookingDirection: Vector): CreaseMaker =
              AMAPCreaseMaker(figure, creaseSegment, guiUserLookingDirection)
      fun makeFFCreaseMaker(figure: Figure, creaseSegment: LineSegment, guiUserLookingDirection: Vector,
                            pickedPointAsSideToCrease: Point?): CreaseMaker =
              FFCreaseMaker(figure, creaseSegment, guiUserLookingDirection, pickedPointAsSideToCrease)
   }
   fun crease(): Figure {
      val facesToCrease = findFacesToCrease()
      val creasedBundle = creaseBundle(facesToCrease)
      val creasedFigureBundles = figure.bundles - bundleToCrease + creasedBundle
      return Figure(creasedFigureBundles)
   }
   protected abstract fun findFacesToCrease(): Set<Face>
   private fun creaseBundle(facesToCrease: Set<Face>): Bundle {
      val restOfFaces = bundleToCrease.faces - facesToCrease
      val creasedFaces = creaseFaces(facesToCrease)
      return creaseBundle(creasedFaces, restOfFaces)
   }
   private fun creaseBundle(originalFaceToCreasedFace: Map<Face, Face>, restOfFaces: Set<Face>): Bundle {
      val newFacesToFacesAbove = makeFaceToFacesAbove(bundleToCrease.facesToFacesAbove, originalFaceToCreasedFace)
      val allFacesOfCreasedBundle = (originalFaceToCreasedFace.values + restOfFaces).toSet()
      return Bundle(bundleToCrease.plane, allFacesOfCreasedBundle, newFacesToFacesAbove)
   }
   private fun creaseFaces(faces: Set<Face>): Map<Face, Face> =
      faces.map { it to creaseFace(it) }.toMap()
   private fun creaseFace(face: Face): Face {
      val crease = makeCrease(face)
      return face.addCrease(crease)
   }
   private fun makeCrease(face: Face): Crease {
      val vertices = face.vertices
      val intersectedVertices = vertices.filter { vertex -> creaseSegment.contains(vertex) }
      if(intersectedVertices.size == 2)
         return creaseFromVertices(intersectedVertices[0], intersectedVertices[1])
      /*
      We are only considering here full intersections.
      If the GUI contemplates partial intersections of a face by a crease, I should modify this implementation.
       */
      val intersectingSegment = face.intersectingSegment(creaseSegment)!!
      if(intersectedVertices.isEmpty()) {
         val points = intersectingSegment.endsAsList
         return creaseFromPoints(points[0], points[1])
      }
      val v = intersectedVertices.first()
      val p = intersectingSegment.endsAsList.first { !it.epsilonEquals(v) }
      return creaseFromPointAndVertex(p, v)
   }
   protected fun findFacesToCrease(facesOfFlaps: Set<Face>) =
           facesOfFlaps
              .filter { it.intersectedAcrossBy(creaseSegment) }
              .toSet()
   private class AMAPCreaseMaker(figure: Figure, creaseSegment: LineSegment, guiUserLookingDirection: Vector) :
           CreaseMaker(figure, creaseSegment, guiUserLookingDirection) {
      override fun findFacesToCrease(): Set<Face> {
         val faces = bundleToCrease.faces
         val bisectedFaces = faces.filter { it.intersectedAcrossBy(creaseSegment) }
         val areAllFacesBisected = faces.size == bisectedFaces.size
         return if(areAllFacesBisected)
            faces
         else {
            val aMAPBisectedFaceFinder =
                    AMAPBisectedFaceFinderAndFaceSeparator(figure, bundleToCrease, creaseSegment, guiUserLookingDirection)
            //To be honest I'm not sure why this function needs a side. I think it shouldn't need it
            // I'll pass a random one
            val facesOfFlaps = aMAPBisectedFaceFinder.findAllFacesOfFlaps(POSITIVE)
            return findFacesToCrease(facesOfFlaps)
         }
      }
   }
   private class FFCreaseMaker(figure: Figure, creaseSegment: LineSegment, guiUserLookingDirection: Vector,
                               pickedPointAsSideToCrease: Point?): CreaseMaker(figure, creaseSegment, guiUserLookingDirection) {
      private val isThereChosenSideToRotate: Boolean = pickedPointAsSideToCrease != null
      private val sideOfFlapToCrease: SideOfFold?
      init {
         sideOfFlapToCrease = if(isThereChosenSideToRotate)
               calculateSideOfFold(creaseSegment.line, pickedPointAsSideToCrease!!, bundleToCrease.plane)
            else
               null
      }
      override fun findFacesToCrease(): Set<Face> =
              if(sideOfFlapToCrease == null)
                 findFacesToCreaseWhenNoSideToFoldSpecified()
              else
                 findFacesToCrease(sideOfFlapToCrease)
      private fun findFacesToCreaseWhenNoSideToFoldSpecified(): Set<Face> {
         val toCreasePositiveSide = findFacesToCrease(POSITIVE)
         val thereAreNoFacesToCreaseWhenPositiveSide = toCreasePositiveSide.isEmpty()
         val toCreaseNegativeSide = findFacesToCrease(NEGATIVE)
         val thereAreNoFacesToCreaseWhenNegativeSide = toCreaseNegativeSide.isEmpty()
         if(thereAreNoFacesToCreaseWhenPositiveSide && thereAreNoFacesToCreaseWhenNegativeSide)
            throw InvalidFoldException("No faces to crease were found.")
         if(thereAreNoFacesToCreaseWhenPositiveSide)
            return toCreaseNegativeSide
         if(thereAreNoFacesToCreaseWhenNegativeSide)
            return toCreasePositiveSide
         val sideMakesADifference = toCreaseNegativeSide != toCreasePositiveSide
         if(sideMakesADifference)
            throw NoSideDefinedException("Side needs to be specified.")
         return toCreasePositiveSide //return any of the two
      }
      private fun findFacesToCrease(sideOfFlapToCrease: SideOfFold): Set<Face> {
         val bisectedFaceFinder = FFBisectedFaceFinderAndFaceSeparator(figure, bundleToCrease, creaseSegment,
                 guiUserLookingDirection, sideOfFlapToCrease)
         val facesOfFlaps = bisectedFaceFinder.findAllFacesOfFlapsToFold(sideOfFlapToCrease)
         return findFacesToCrease(facesOfFlaps)
      }
   }
}