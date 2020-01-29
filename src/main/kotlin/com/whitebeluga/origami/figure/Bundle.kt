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

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Preconditions.checkArgument
import com.moduleforge.libraries.geometry._3d.Line
import com.moduleforge.libraries.geometry._3d.LineSegment
import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Plane.planeFromOrderedPoints
import com.moduleforge.libraries.geometry._3d.Polygon.Companion.calculateUnionArea
import com.moduleforge.libraries.geometry._3d.Vector
import com.moduleforge.util.Util.addNewValueToEntryOfMap
import com.moduleforge.util.Util.addNewValuesToEntryOfMap
import com.whitebeluga.origami.figure.Bundle.Side.BOTTOM
import com.whitebeluga.origami.figure.Bundle.Side.TOP
import com.whitebeluga.origami.figure.Bundle.Unfolder.Companion.areUnfoldedFaces
import com.whitebeluga.origami.figure.component.Edge
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.Vertex

/**
 * A bundle is a stack of co-planar faces.
 *
 * The faces that are farther in the direction of the plane normal are considered to be at the "top" of the network
 * of nodes.
 *
 * The reason for this denomination is rather arbitrary, but it makes some sense if you think of the faces being in
 * a bundle that exists on a plane, and that plane has two faces that we can distinguish thanks to its normal.
 * The "up" side of the plane is the one which has the normal looking at us when we look at it.
 *
 * Although normally all faces that share the same plane in a figure belong to the same bundle, it isn't necessarily
 * always the case. In some fringe cases different layer stacks can share the same plane. Think two limbs of
 * a figure and at the end of the limbs: the paper is squashed forming some kind of feet. They theoretically
 * occupy the same plane, but it wouldn't make sense to consider them the same bundle of faces, since they are not
 * connected ( or rather they are connected through faces in different planes).
 *
 */
class Bundle: FaceContainer {
   val plane: Plane
   val normal: Vector
   /**
    * It's a normal vector, perpendicular to the stack's plane,
    * going from the bottom to the top.
    *
    * Same value as the normal.
    */
   val upwards: Vector
   /**
    * It's a normal vector, perpendicular to the stack's plane,
    * going from the top to the bottom
    */
   val downwards: Vector
   /**
    * Above is to be congruent with the direction of the normal of the plane that the network of nodes is in. So,
    * for example
    * a face 'A' further along said normal than a face 'B' is said to be above B.
    *
    * For a face to be above or below another, they have to be intersected (ie, intersection area not nil). Otherwise
    * we simply don't care about the relationship.
    *
    * It also doesn't matter if there are other faces in between, the only thing that matters
    * to be an above or below face is that the intersection area is not zero. Regardless of whether the faces are
    * directly "touching" each other or not.
    *
    * ---
    *
    * If a face has no faces above, then it has no entry on the map. It's important to ensure that since users
    * of this class may rely on this condition.
    *
    * Obviously the same goes for the faces below map, but for faces that have no faces below.
    */
   val facesToFacesAbove: Map<Face, Set<Face>>
   val facesToFacesBelow: Map<Face, Set<Face>>

   constructor(plane: Plane, face: Face): this(plane, setOf(face), emptyMap())
   /**
    * In the map there can't be empty sets as values. When that is the case, the key just shouldn't be there
    */
   constructor(plane: Plane, faces: Set<Face>, facesToFacesAbove: Map<Face, Set<Face>>): super(faces)  {
      this.plane = plane
      this.facesToFacesAbove = facesToFacesAbove.toMap()
      validateFaceToFacesAboveMap(faces, facesToFacesAbove)
      normal = plane.normal
      upwards = plane.normal
      downwards = plane.normal.negate()
      facesToFacesBelow = calculateFacesToFacesBelow(facesToFacesAbove)
   }
   private constructor(plane: Plane, faces: Set<Face>, facesToFacesAbove: Map<Face, Set<Face>>,
                       facesToFacesBelow: Map<Face, Set<Face>>): super(faces) {
      this.plane = plane
      this.facesToFacesAbove = facesToFacesAbove.toMap()
      this.facesToFacesBelow = facesToFacesBelow.toMap()
      normal = plane.normal
      upwards = plane.normal
      downwards = plane.normal.negate()
   }
   @VisibleForTesting
   internal fun unfold(unfoldedFace: Face, toReplace: Pair<Face, Face>): Bundle {
      val newFacesToFacesAbove = facesToFacesAbove.toMutableMap()
      val facesToReplace = toReplace.toList()
      facesToReplace.forEach { newFacesToFacesAbove.remove(it)}
      val facesAboveFirst = facesToFacesAbove[toReplace.first] ?: emptySet()
      val facesAboveSecond = facesToFacesAbove[toReplace.second] ?: emptySet()
      val facesAboveUnfolded = facesAboveFirst + facesAboveSecond
      if(facesAboveUnfolded.isNotEmpty())
         newFacesToFacesAbove[unfoldedFace] = facesAboveUnfolded
      val valuesWithFacesToReplace =
              newFacesToFacesAbove.filterValues { it.intersect(facesToReplace).isNotEmpty() }
      for((key, value) in valuesWithFacesToReplace) {
         val newValue = value - facesToReplace + unfoldedFace
         newFacesToFacesAbove[key] = newValue
      }
      val newFaces = faces - facesToReplace + unfoldedFace
      return Bundle(plane, newFaces, newFacesToFacesAbove)

   }
   internal fun reverse(): Bundle = Bundle(plane, faces, facesToFacesAbove = facesToFacesBelow,
           facesToFacesBelow = facesToFacesAbove)
   /**
    * Note that this function doesn't "reverse" a bundle itself (however you want to understand 'reversing' a bundle)
    *
    * What it does is reversing the FRAME OF REFERENCE so that what was 'up' is now 'down' and vice versa. It is,
    * however, essentially the same object.
    *
    * This is useful in 180 folds, where the two parts rejoin but one of the bundle parts has to adopt the reference frame
    * (the normal direction) of the other, and therefore, for its faces, what was considered upwards is now downwards
    * and etc. But otherwise nothing else changes in the object.
    */
   internal fun switchDirection(): Bundle =
      Bundle(plane.facingTheOtherWay(), faces, facesToFacesAbove = facesToFacesBelow, facesToFacesBelow = facesToFacesAbove)
   /**
    *  The map passed as parameter is a map of the new bottom faces to their faces above, not only in the bottom
    *  bundle but also those in the top bundle
    */
   @VisibleForTesting
   internal fun addNewBottomFaces(newBottomFaces: Set<Face>, newFacesToFacesAbove: Map<Face, Set<Face>>): Bundle =
      //there are no conflicts of keys, can just add the maps
      Bundle(plane, faces + newBottomFaces,facesToFacesAbove + newFacesToFacesAbove)
   /**
    * Why a function "join part of bundle on itself" instead of just "join" with another bundle?
    *
    * Because it allows us to assert that the normals point in opposite directions as it is always the case
    * on both parts to join, where one of the parts has been rotated
    *
    * We need to know from which side is the rotated part coming, one way to do it is by passing the original fold
    * direction that applied to the pre-fold original bundle
    *
    * ---
    * Why is the fold segment is a necessary parameter?
    *
    * For two reasons:
    *
    * - The fold line is necessary when a fold is done, sometimes an "unfold" can happen (picture a flap being folded
    * over to the other side)
    * unfolds require joining two faces. While the fold foldLine may not be strictly necessary it's useful for
    * detecting these faces and the connection points shared by them along the foldLine.
    *
    * - The folded part has to go "inside" sometimes. That is, it not always "wraps" over the whole bundle.
    * The fold segment tells us which faces are partially intersected and therefore cannot be "wrapped" by the fold.
    *
    */
   fun joinRotatedPartOfBundleOnItself(rotatedPart: Bundle, originalRotationDirection: Vector, foldSegment: LineSegment): Bundle {
      val joiner = BundleJoiner(this, rotatedPart, originalRotationDirection, foldSegment)
      return joiner.join()
   }
   fun contains(face: Face): Boolean = faces.contains(face)
   fun removeAll(facesToRemove: Set<Face>): Bundle? {
      if(facesToRemove == faces)
         return null
      var facesRemoved: Bundle? = this
      facesToRemove.forEach {
         facesRemoved = facesRemoved?.remove(it)
      }
      return facesRemoved
   }
   fun remove(faceToRemove: Face): Bundle? {
      if(!contains(faceToRemove))
         throw IllegalArgumentException()
      if(faces.size == 1)
         return null
      val newFacesToFacesAbove = mutableMapOf<Face, Set<Face>>()
      for((face, facesAbove) in facesToFacesAbove){
         if(face == faceToRemove)
            continue
         if(facesAbove == setOf(faceToRemove))
            continue
         newFacesToFacesAbove[face] = facesAbove - faceToRemove
      }
      return Bundle(plane, faces - faceToRemove, newFacesToFacesAbove)
   }
   fun insertFace(newFace: Face, facesAbove: Set<Face>, facesBelow: Set<Face>): Bundle {
      assert(!faces.contains(newFace))
      assert( facesAbove.isEmpty() || facesAbove.all { this.contains(it) } )
      assert( facesBelow.isEmpty() || facesBelow.all { this.contains(it) } )
      assert(facesAbove.isEmpty() || facesAbove.all { newFace.overlaps(it)} )
      assert(facesBelow.isEmpty() || facesBelow.all { newFace.overlaps(it)} )
      val restOfFaces = faces - (facesAbove + facesBelow)
      assert(restOfFaces.isEmpty() || restOfFaces.none { newFace.overlaps(it)} )
      val newFacesToFacesAbove =
              if(facesAbove.isEmpty()) mutableMapOf() else mutableMapOf(newFace to facesAbove)
      for( faceBelow in facesBelow ) {
         val previousFacesAbove = facesToFacesAbove[faceBelow] ?: emptySet()
         val newFacesAbove = previousFacesAbove + newFace
         newFacesToFacesAbove[faceBelow] = newFacesAbove
      }
      for((existingFace, existingFacesAbove) in facesToFacesAbove )
         if( !newFacesToFacesAbove.containsKey(existingFace))
            newFacesToFacesAbove[existingFace] = existingFacesAbove
      return Bundle(this.plane, this.faces + newFace, newFacesToFacesAbove)
   }
   fun facesBelow(face: Face): Set<Face> {
      if(!faces.contains(face))
         throw IllegalArgumentException("Face not contained in the bundle.")
      return facesToFacesBelow[face] ?: emptySet()
   }
   fun facesAbove(face: Face): Set<Face> {
      if(!faces.contains(face))
         throw IllegalArgumentException("Face not contained in the bundle.")
      return facesToFacesAbove[face] ?: emptySet()
   }
   /** Calculates the area of the bundle */
   fun calculateArea(): Double = calculateUnionArea(faces)
   /**
    * Returns the faces that don't have any other face on top and are at the top side of the bundle
    */
   fun topFaces(): Set<Face> = faces.filter { facesAbove(it).isEmpty() }.toSet()
   fun faceConnectedByEdge(edge: Edge, side: Side): Face  = mapOfSideToFaceConnectedByEdge(edge)[side]!!
   /**
    * Of the two faces articulated by the edge returns the top-most one.
    */
   fun topFaceConnectedByEdge(edge: Edge): Face  =
           mapOfSideToFaceConnectedByEdge(edge)[TOP]!!
   fun bottomFaceConnectedByEdge(edge: Edge): Face =
      mapOfSideToFaceConnectedByEdge(edge)[BOTTOM]!!
   private fun mapOfSideToFaceConnectedByEdge(edge: Edge): Map<Side, Face> {
      checkArgument(edges.contains(edge))
      checkArgument(isAConnectingEdge(edge))
      val faces = facesConnectedBy(edge).toList()
      val first = faces[0]
      val second = faces[1]
      val top = if(facesAbove(first).contains(second)) second else first
      val bottom = (faces - top).first()
      return mapOf(Side.TOP to top, Side.BOTTOM to bottom)
   }
   /**
    * Returns the faces that don't have any other face on top and are at the bottom side of the bundle
    */
   fun bottomFaces(): Set<Face> = faces.filter { facesBelow(it).isEmpty() }.toSet()
   fun isMonoFace(): Boolean = faces.size == 1
   fun isBiFace(): Boolean = faces.size == 2
   companion object {
      private fun validateFaceToFacesAboveMap(allFaces: Set<Face>, facesToFacesAbove: Map<Face, Set<Face>>) {
         if(allFaces.size == 1) {
            assert(facesToFacesAbove.isEmpty())
            return
         }
         val listOfFaces = allFaces.toList()
         for((index, face) in listOfFaces.dropLast(1).withIndex())
            for(anotherFace in listOfFaces.drop(index + 1) ) {
               //for all pairs of faces
               if(face.overlaps(anotherFace))
                  assert(facesToFacesAbove.containsKey(face) || facesToFacesAbove.containsKey(anotherFace))
            }
         if(facesToFacesAbove.isEmpty()) return
         assert(allFaces.containsAll(facesToFacesAbove.keys))
         assert(facesToFacesAbove.values.none{it.isEmpty()} )
         assert(facesToFacesAbove.values.all{allFaces.containsAll(it)} )
      }
      private fun calculateFacesToFacesBelow(facesToFacesAbove: Map<Face, Set<Face>>): Map<Face, Set<Face>> {
         val facesToFacesBelow = mutableMapOf<Face, MutableSet<Face>>()
         for((f, facesAbove) in facesToFacesAbove.entries)
            for(faceAbove in facesAbove)
               addNewValueToEntryOfMap(facesToFacesBelow, faceAbove, f)
         return facesToFacesBelow
      }
      /**
       * Some functions (color change, translation, rotation and creasing) transform the faces of a figure without
       * modifying the structure of the bundles.
       *
       * In order to construct the face order of the new bundles, we take the original face order (map of faces to
       * faces above) and the mapping of faces to transformed faces, and we return the map of faces to faces above
       * for the transformed faces.
       */
      internal fun makeFaceToFacesAbove(originalFaceToFacesAbove: Map<Face, Set<Face>>,
                                        originalFaceToTransformedFace: Map<Face, Face>): Map<Face, Set<Face>> {
         val getTransformedFace = { face: Face -> originalFaceToTransformedFace[face] ?: face }
         val getTransformedFaces = { faces: Set<Face> -> faces.map { it -> getTransformedFace(it) }.toSet() }
         return originalFaceToFacesAbove
                 .map { (face, facesAbove) -> Pair(getTransformedFace(face) , getTransformedFaces(facesAbove)) }
                 .toMap()
      }
   }
   enum class Side {
      TOP {
         override fun opposite(): Side = Side.BOTTOM
      },
      BOTTOM {
         override fun opposite(): Side = Side.TOP
      };
      abstract fun opposite(): Side
   }
   /**
   When two bundles are being joined it is implied that is in process of folding the bundle on itself.

   The two bundles come from the same one after being split, but the normals should have rotated an angle PI with
   respect to one another and be pointing in opposite directions.

   Notice that the "rotated" part is a part of the original bundle that is already rotated! And that in a bundle
   rotation, as is to be expected, not only are the faces rotated, but the bundle normal should also be pointing in the
   opposite direction as the original bundle of which it was a part. In other words it comes rotated and it is not
   our task to rotate it.

   A third parameter (that is, one other than the two bundles), the direction of the fold, is needed to establish
   whether the incoming ("incoming" can also be understood as "rotating") pushes the layers of the stationary part
   bundle from the bottom -if the fold direction vector is opposite to the stationary bundle's normal-
   or, if the fold direction is the same as the stationary part's normal, the incoming part is added at the top of the
   bundle, which is the same as appending at the end of the layers' list.

   If you have difficulty picturing all this, then "picture" it literally, by drawing it. It should all become
   very clear

    */
   @VisibleForTesting
   internal class BundleJoiner(
           private val stationaryPart: Bundle,
           private val incomingPart: Bundle,
           /**
            * The rotation direction as it would appear depicted on the original pre-fold bundle
            *
            * Don't mistake it with the incoming direction of the incoming part, which is the opposite
            * vector
            */
           originalRotationDirection: Vector,
           /**
            * You might ask, why the fold line: it is in order to do "unfolds", that is, two faces whose connecting
            * segment goes exactly along the fold line and when the fold is done should become one face.
            */
           private val foldSegment: LineSegment) {
      private val foldLine: Line = foldSegment.line
      /**
       * End of the stationary part at which the rotating bundle part is coming
       * ie If top, the rotating bundle comes downwards to meet the top part of the stationary part bundle.
       */
      private val rotatingBundleComesTowards: Side
      /**
       * This variable was hard to name, so it definitely needs an explanation:
       *
       * When faces are folded, as they are rotated, other faces might end up below (I do not mean below with
       * respect to the arbitrary top and bottom of a bundle, obviously) and wrapped by these faces.
       *
       * If a face that is partially intersected by the fold line -and thus cannot be folded- or fully intersected but not being folded anyway (that would happen only with
       * the first-flap fold) and that face is in the direction of the fold from the faces being folded,
       * then those faces will establish the limit for which the faces being folded can 'sink' to.
       *
       * TThis "go-through" faces or faces that are not wrapped by the folded faces always belong to the stationary bundle
       *
       * IMPORTANT:
       * This only applies to flat folding, where both sides of broken up faces and the rest of not folded
       * faces of a bundle end up as faces of the new bundle.
       *
       * This variable is irrelevant for folds of less than PI rad.
       */
      private val goThrough: Set<Face>

      init {
         val stationaryAndIncomingDotProduct = stationaryPart.normal.dot(incomingPart.normal)
         assert(stationaryAndIncomingDotProduct < 0) //the vectors face away from each other
         val dotProduct = originalRotationDirection.dot(stationaryPart.normal)
         val facingTheSameWay = dotProduct > 0
         rotatingBundleComesTowards = if(facingTheSameWay) TOP else BOTTOM
         goThrough = makeGoThroughFaces()
      }
      private fun makeGoThroughFaces(): Set<Face> {
         val stationaryPartFaces = stationaryPart.faces
         val incomingPartFaces = incomingPart.faces

         val incomingPartSegments = incomingPartFaces.flatMap { it.edges }.toSet()
         val stationaryPartFacesArticulatingIncomingPart =
                 stationaryPartFaces.filter { it.edges.intersect(incomingPartSegments).isNotEmpty() }.toSet()
         val relevantMap = if(rotatingBundleComesTowards == TOP)
            stationaryPart.facesToFacesAbove
         else
            stationaryPart.facesToFacesBelow

         val stationaryPartFacesThatCouldBeInTheWayOfIncomingFaces =
                 stationaryPartFacesArticulatingIncomingPart.flatMap { relevantMap[it] ?: emptySet() }.toSet()


         val overlappingStationaryFaces = stationaryPartFacesThatCouldBeInTheWayOfIncomingFaces.filter { stationaryFace ->
               incomingPartFaces.any { incomingFace -> stationaryFace.overlaps(incomingFace) }
            }.toSet()
         val overlappingPartiallyIntersectedStationaryFaces = overlappingStationaryFaces
                 .filter { it.partiallyIntersectedBy(foldSegment) }
                 .toSet()
         val result = overlappingPartiallyIntersectedStationaryFaces.toMutableSet()
         for(overlappingPartiallyIntersectedStationaryFace in overlappingPartiallyIntersectedStationaryFaces) {
            val toAdd = (relevantMap[overlappingPartiallyIntersectedStationaryFace]?.toSet() ?: emptySet())
                    .intersect(overlappingStationaryFaces)
            result.addAll(toAdd)
         }
         return result
      }
      fun join(): Bundle =
              when(rotatingBundleComesTowards){
                 TOP -> appendIncomingPartOnTopOfStationaryPart()
                 BOTTOM -> pushIncomingPartToBottomOfStationaryPart()
              }
      /**
       * the bottom layer of the incoming bundle should be the new top of the result bundle
       */
      private fun appendIncomingPartOnTopOfStationaryPart(): Bundle {
         val toPrepend = incomingPart.switchDirection()
         return join(toPrepend, stationaryPart, foldLine)
      }
      /**
       * the top layer of the incoming bundle should be the new bottom of the result bundle
       */
      private fun pushIncomingPartToBottomOfStationaryPart(): Bundle {
         val toAppend = incomingPart.switchDirection()
         return join(stationaryPart, toAppend, foldLine)
      }
      /** Join the two bundles, "sinking" and merging the second chunk into the first.
       *
       * More precisely, the top of the second comes into the bottom of the first
       *
       * The order of the faces of the second chunk is the same as the first: the first of the second goes after the
       * last of the first.
       *
       * Using this convention makes it easier to visualize the algorithm.
       *
       * Unfolds:
       *
       * Unfolds complicate the bundle joining algorithm. An unfold is a connection that existed along the fold segment
       * and when folding, the faces connected become a single face again and we need to adjust the layers
       *
       * There is another twist: when joining bundles, there are some faces that we can pass "through" ie.
       * they don't stop a face. The explanation to this variable is a bit lengthy but basically it is a
       * consequence of folding faces towards the "inside" of the bundle instead of wrapping the whole
       * bundle as they are rotated.
       */
      private fun join(topChunk: Bundle, bottomChunk: Bundle, foldLine: Line): Bundle {
         if(bottomChunk.faces.isEmpty())
            return topChunk
         if(topChunk.faces.isEmpty())
            return bottomChunk
         return joinWhenBothBundlesNotEmpty(topChunk, bottomChunk, foldLine)
      }
      private fun joinWhenBothBundlesNotEmpty(topChunk: Bundle, bottomChunk: Bundle, foldLine: Line): Bundle {
         assert(topChunk.plane.approximatelyFacingTheSameWay(bottomChunk.plane))
         val newFaceToFacesAbove = mutableMapOf<Face, Set<Face>>()
         val facesOfTopChunkThatCanBeOnTop = topChunk.faces - goThrough
         val facesOfBottomChunkThatCanHaveFacesOnTop = bottomChunk.faces - goThrough
         for(bottomFace in facesOfBottomChunkThatCanHaveFacesOnTop) {
            newFaceToFacesAbove[bottomFace] = facesOfTopChunkThatCanBeOnTop
                    .filter { it.overlaps(bottomFace) }
                    .toSet()
         }
         val goThroughOfTopChunk = topChunk.faces.intersect(goThrough)
         for(goThroughFace in goThroughOfTopChunk) {
            newFaceToFacesAbove[goThroughFace] = bottomChunk.faces
                    .filter { it.overlaps(goThroughFace) }
                    .toSet()
         }
         val goThroughOfBottomChunk = bottomChunk.faces.intersect(goThrough)
         for(topFace in topChunk.faces - goThrough) {
            newFaceToFacesAbove[topFace] = goThroughOfBottomChunk
                    .filter { it.overlaps(topFace) }
                    .toSet()
         }
         val totalFaceToFacesAbove = addMaps(topChunk.facesToFacesAbove, bottomChunk.facesToFacesAbove, newFaceToFacesAbove)
         val totalFaceToFacesAbove_NoEmptyValues = totalFaceToFacesAbove.filterValues { it.isNotEmpty() }
         val allFaces = topChunk.faces + bottomChunk.faces
         val resultMinusUnfoldedFaces = Bundle(topChunk.plane, allFaces, totalFaceToFacesAbove_NoEmptyValues)
         val toBeUnfolded = makePairsOfFacesToUnfold(topChunk, bottomChunk, foldLine)
         return makeUnfolds(resultMinusUnfoldedFaces, toBeUnfolded)
      }
      companion object {
         private fun addMaps(vararg arrayOfFaceToFaces: Map<Face, Set<Face>>): Map<Face, Set<Face>> {
            val result = mutableMapOf<Face, MutableSet<Face>>()
            for(map in arrayOfFaceToFaces)
               for((face, facesAbove) in map)
                  addNewValuesToEntryOfMap(result, face, facesAbove)
            return result
         }
         /**
          * The order of the bundle parameters is irrelevant
          */
         private fun makePairsOfFacesToUnfold(b1: Bundle, b2: Bundle, foldLine: Line): Set<Pair<Face, Face>> {
            val toBeUnfolded = mutableSetOf<Pair<Face, Face>>()
            for(f1 in b1.faces)
               for(f2 in b2.faces) {
                  val areUnfolded = areUnfoldedFaces(f1, f2, foldLine)
                  if (areUnfolded)
                     toBeUnfolded.add(Pair(f1, f2))
               }
            return toBeUnfolded
         }
         private fun makeUnfolds(joinedBundleButNotUnfolded: Bundle, toBeUnfolded: Set<Pair<Face, Face>>): Bundle {
            var result = joinedBundleButNotUnfolded
            for (pair in toBeUnfolded) {
               val unfolded = Unfolder(pair).joinUnfoldedFaces()
               result = result.unfold(unfolded, pair)
            }
            return result
         }
      }
   }
   /**
    * The purpose of this class is to take two faces and join them together.
    *
    * An "unfolding" is a case that can happen
    * during folding (think of a when a flap is flipped to the other side, a fold disappears)
    *
    * Assume the two faces passed as parameters are being "unfolded" and no parameter validation is needed.
    *
    * This is a class instead of an object to have some common variables as class members be used by any
    * method (better readibility)
    */
   @VisibleForTesting
   internal class Unfolder(private val face1: Face, private val face2: Face) {
      private val commonVerticesSet = face1.vertices.intersect(face2.vertices)
      private val commonVertices = commonVerticesSet.toList()
      constructor(faces: Pair<Face, Face>): this(faces.first, faces.second)
      /**
       * The assumption here is that the faces are "unfolded" ie, can be rejoined
       * No need to check anything, it has already been checked before in the algorithm
       */
      fun joinUnfoldedFaces(): Face {
         val verticesFace1 = getVerticesFromVertexOnFoldLineToVertexOnFoldLine(face1)
         val verticesFace2 = getVerticesFromVertexOnFoldLineToVertexOnFoldLine(face2)
         //from the vertices from one of the faces, remove all vertices on the fold line
         val verticesFace1VerticesOnFoldLineRemoved  = verticesFace1.toMutableList()
         verticesFace1VerticesOnFoldLineRemoved.removeAll(commonVertices)
         val toBeDiscarded = commonVertices.filter { canVertexOnFoldLineBeDiscarded(it) }
         //from the vertices on the other face, remove only those vertices on the line that should be discarded
         val verticesFace2DiscardedRemoved = verticesFace2.toMutableList()
         verticesFace2DiscardedRemoved.removeAll(toBeDiscarded)
         val newVertices = verticesFace1VerticesOnFoldLineRemoved + verticesFace2DiscardedRemoved
         return Face(newVertices, face1.colors)
      }
      /**
       * Returns the vertices of a face but from one of the vertices on the fold line to the other one
       * on the fold line.
       *
       * Such a list is needed to join both list from both faces and created the list of vertices of
       * the joined resulting face.
       */
      private fun getVerticesFromVertexOnFoldLineToVertexOnFoldLine(face: Face): List<Vertex> {
         val first = commonVertices[0]
         val second = commonVertices[1]
         val firstVertexOfFace = if(face.nextVertex(first) == second) second else first
         return face.verticesStartingWith(firstVertexOfFace) as List<Vertex>
      }
      /**
       * If the vertex is on the same line as vertices from each face, then it is not needed in the new
       * joined face
       */
      private fun canVertexOnFoldLineBeDiscarded(vertexOnFoldLine: Vertex): Boolean {
         val previousOfVertexInFace1 = getPreviousVertex(vertexOnFoldLine, face1)
         val previousOfVertexInFace2 = getPreviousVertex(vertexOnFoldLine, face2)
         val segmentBetweenFaces =  LineSegment(previousOfVertexInFace1, previousOfVertexInFace2)
         return segmentBetweenFaces.contains(vertexOnFoldLine)
      }
      /**
       * What I mean here by "previous vertex" is, for one of the two vertices on the fold line of an "unfolded" face,
       * get the adjacent vertex that is not the other vertex on the fold line.
       *
       * This vertex will be used to determine if the vertex on the fold line is redundant or not when joining the
       * two faces.
       *
       * The vertices on the fold line parameter may be a set or a list instead of a pair for simplicity, but
       * it should be always a pair, and in fact, it should have been checked/asserted already as part of the
       * algorithm
       */
      private fun getPreviousVertex(vertexOnFoldLine: Vertex, face: Face): Vertex {
         val next = face.nextVertex(vertexOnFoldLine) as Vertex
         val previous = face.previousVertex(vertexOnFoldLine)  as Vertex
         return when {
            commonVertices.contains(next) -> previous
            commonVertices.contains(previous) -> next
            else -> throw RuntimeException("Shouldn't happen.")
         }
      }
      companion object {
         fun areUnfoldedFaces(face1: Face, face2: Face, foldLine: Line):  Boolean {
            val vertices1 = face1.vertices
            val vertices2 = face2.vertices
            val commonVertices = vertices1.intersect(vertices2)
            if(commonVertices.size < 2)
               return false
            assert(commonVertices.size == 2) //it cannot be three or more
            val onTheFoldLine = foldLine.containsAll(commonVertices)
            if(!onTheFoldLine)
               return false
            val atSameSideOfLine = !atDifferentSidesOfLine(vertices1, vertices2)
            if(atSameSideOfLine)
               return false
            //at this point, the faces have to be a pair of unfolded ones,
            // assert that they are facing the same way, as they should
            ensureSameOrientation(vertices1, vertices2)
            return true
         }
         /**
          * true if vertices are at different sides of the fold line
          */
         private fun atDifferentSidesOfLine(vertices1: List<Vertex>, vertices2: List<Vertex>): Boolean {
            val commonVertices = vertices1.intersect(vertices2)
            val vertices1_NotOnTheLine = vertices1 - commonVertices
            val vertices2_NotOnTheLine = vertices2 - commonVertices
            val commonVerticesList = commonVertices.toList()
            val line = Line.linePassingBy(commonVerticesList[0], commonVerticesList[1])
            for (vertex1 in vertices1_NotOnTheLine) {
               val closestToVertex1 = line.closestPoint(vertex1)
               for (vertex2 in vertices2_NotOnTheLine) {
                  val closestToVertex2 = line.closestPoint(vertex2)
                  val v1 = vertex1.vectorTo(closestToVertex1)
                  val v2 = vertex2.vectorTo(closestToVertex2)
                  val atSameSideOfLine = v1.dot(v2) > 0
                  if (atSameSideOfLine)
                     return false
               }
            }
            return true
         }
         private fun ensureSameOrientation(vertices1: List<Vertex>, vertices2: List<Vertex>) {
            val plane1 = planeFromOrderedPoints(vertices1.take(3))
            val plane2 = planeFromOrderedPoints(vertices2.take(3))
            assert(plane1.approximatelyFacingTheSameWay(plane2))
         }
      }
   }
}