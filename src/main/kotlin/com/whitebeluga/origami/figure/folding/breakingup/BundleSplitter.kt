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

import com.moduleforge.libraries.geometry._3d.LineSegment
import com.whitebeluga.origami.figure.Bundle
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.folding.SideOfFold
import com.whitebeluga.origami.figure.folding.SideOfFold.NEGATIVE
import com.whitebeluga.origami.figure.folding.SideOfFold.POSITIVE

/**
 * Splits the bundle in two new bundles for a fold.
 */
internal class BundleSplitter(bundle: Bundle,
                              val sideToNonSplitFaces: Map<SideOfFold, Set<Face>>,
                              splitFaces: Set<SplitFace>,
                              val foldSegment: LineSegment) {
   private val plane = bundle.plane
   private val mapOfFaceToItsFacesAbove = bundle.facesToFacesAbove

   //"whole" means not bisected
   private val wholeFacesOfPositiveSide = sideToNonSplitFaces[POSITIVE]?: emptySet()
   private val wholeFacesOfNegativeSide = sideToNonSplitFaces[NEGATIVE]?: emptySet()

   private val mapOfBisectedFaceToItsPositivePart: Map<Face, Face> = splitFaces
           .associateBy( { it.originalFace}, {it.newFaces[POSITIVE]!!})
   private val mapOfBisectedFaceToItsNegativePart: Map<Face, Face> = splitFaces
           .associateBy( { it.originalFace}, {it.newFaces[NEGATIVE]!!})
   private val edgesOnTheFoldLineComingFromSplitFaces = splitFaces.map { it.connectingEdge }.toSet()

   fun splitBundle(): SplitBundle {
      val map = mapOf(
              POSITIVE to makeSplitBundlePart(wholeFacesOfPositiveSide, mapOfBisectedFaceToItsPositivePart),
              NEGATIVE to makeSplitBundlePart(wholeFacesOfNegativeSide, mapOfBisectedFaceToItsNegativePart) )
      val wholeFaces = sideToNonSplitFaces.values.flatten().toSet()
      val edgesOfWholeFaces = wholeFaces.map { it.edges }.flatten().toSet()
      val otherEdgesOnTheFoldLine = edgesOfWholeFaces.filter { foldSegment.contains(it) }
      val allEdgesOnTheFoldLine = edgesOnTheFoldLineComingFromSplitFaces + otherEdgesOnTheFoldLine
      assert(edgesOnTheFoldLineComingFromSplitFaces.intersect(otherEdgesOnTheFoldLine).isEmpty())
      return SplitBundle(map, allEdgesOnTheFoldLine)
   }
   private fun makeSplitBundlePart(wholeFaces: Set<Face>, bisectedFaceToFacePartOfSide: Map<Face, Face>): Bundle {
      val allFacesOfSide = wholeFaces + bisectedFaceToFacePartOfSide.values
      val facesToFacesAbove = makeFacesToFacesAbove(wholeFaces, bisectedFaceToFacePartOfSide)
      return Bundle(plane, allFacesOfSide, facesToFacesAbove)
   }
   private fun makeFacesToFacesAbove(wholeFaces: Set<Face>, faceToSplitFaceOfASide: Map<Face, Face>): Map<Face, Set<Face>> {
      val wholeFacesToFacesAbove = mutableMapOf<Face, Set<Face>>()
      for(face in wholeFaces) {
         val facesAbove = makeFacesToFacesAbove_FromWholeFace(face, wholeFaces, faceToSplitFaceOfASide)
         if(facesAbove.isEmpty())
            continue
         wholeFacesToFacesAbove[face] = facesAbove
      }
      val splitFacesToFacesAbove = mutableMapOf<Face, Set<Face>>()
      for((originalFace, facePart) in faceToSplitFaceOfASide){
         val facesAbove = makeFacesToFacesAbove_FromSplitFace(originalFace, facePart, wholeFaces, faceToSplitFaceOfASide)
         if(facesAbove.isEmpty())
            continue
         splitFacesToFacesAbove[facePart] = facesAbove
      }
      return wholeFacesToFacesAbove + splitFacesToFacesAbove
   }
   private fun makeFacesToFacesAbove_FromWholeFace( face: Face, allWholeFacesOfASide: Set<Face>,
                                                    faceToSplitFaceOfASide: Map<Face, Face>): Set<Face> {
      val facesAbove = mapOfFaceToItsFacesAbove[face] ?: return emptySet()
      val wholeFacesAboveFace = facesAbove.intersect(allWholeFacesOfASide)
      val splitFacesAboveFace = facesAbove.intersect(faceToSplitFaceOfASide.keys)
              .map { faceToSplitFaceOfASide[it]!! }.toSet()
      return (wholeFacesAboveFace + splitFacesAboveFace).filter { it.overlaps(face) }.toSet()
   }
   private fun makeFacesToFacesAbove_FromSplitFace( originalFace: Face, facePart: Face, allWholeFacesOfASide: Set<Face>,
                                                    faceToSplitFaceOfASide: Map<Face, Face>): Set<Face> {
      val facesAbove = mapOfFaceToItsFacesAbove[originalFace] ?: return emptySet()
      val wholeFacesAboveFace = facesAbove.intersect(allWholeFacesOfASide)
      val splitFacesAboveFace = facesAbove.intersect(faceToSplitFaceOfASide.keys)
              .map { faceToSplitFaceOfASide[it]!! }.toSet()
      return (wholeFacesAboveFace + splitFacesAboveFace).filter { it.overlaps(facePart) }.toSet()
   }
   companion object {
      fun fromSingleFaceBundle(bundle: Bundle, splitFace: SplitFace, foldSegment: LineSegment): BundleSplitter {
         assert(bundle.faces == setOf(splitFace.originalFace))
         return BundleSplitter(bundle, mapOf(), setOf(splitFace), foldSegment)
      }
   }
}