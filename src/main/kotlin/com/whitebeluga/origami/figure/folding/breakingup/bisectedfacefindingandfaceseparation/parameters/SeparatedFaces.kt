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

package com.whitebeluga.origami.figure.folding.breakingup.bisectedfacefindingandfaceseparation.parameters

import com.whitebeluga.origami.figure.Bundle
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.folding.SideOfFold

/**
 * Represents all the faces at each side of a fold.
 *
 * It is useful for the rotation of the fold. A fold breaks up some faces along its line, but then all the faces of the
 * figure have to be separated into two groups in order for the fold rotation to be made.
 *
 * This class is internal to the module, any other module shouldn't care about the inner workings of a fold, only the
 * fold method needs to be exposed.
 *
 * This can be used for any kind of line fold: either as many as possible or first flap
 */
internal class SeparatedFaces(
   /** Faces bisected by the fold segment. They all belong to the same bundle. */
   val bisected: Set<Face>,
   /**
    * Division of faces of the bundle by the side they occupy with respect to the fold segment.
    *
    * If there are no faces on one of the sides, then there is no entry in the map.
    */
   val mapOfSideToWholeFaces: Map<SideOfFold, Set<Face>>,
   /**
    * The rest of the bundles of the figure, each of them has to be at one of the sides
    *
    * If there are no faces on one of the sides, then there is no entry in the map.
    */
   val mapOfSideToRestOfBundles: Map<SideOfFold, Set<Bundle>>) {
   init {
      //no empty value set; if there are no values then the key shouldn't be in the map at all
      //this is arbitrary, just to enforce one of two possibilities: no entry or entry with empty result
      //reduces the chance of making an error
      assert(mapOfSideToWholeFaces.values.none { it.isEmpty() })
      //same constraint but for bundles
      assert(mapOfSideToRestOfBundles.values.none { it.isEmpty() })
   }
   /** No other bundles. Only bisected and non bisected faces in the bundle to fold. */
   private constructor(bisected: Set<Face>, notBisected: Map<SideOfFold, Set<Face>>):
           this(bisected, notBisected, mapOf())
   /** No non-bisected faces and no other bundles. Only bisected and one bundle. */
   private constructor(bisected: Set<Face>): this(bisected, mapOf(), mapOf())
   private constructor(bisected: Face): this(setOf(bisected), mapOf(), mapOf())

   companion object {
      /**
       * when there are only bisected faces and nothing else
       */
      fun fromBisectedFaces(bisected: Set<Face>) = SeparatedFaces(bisected)
      /**
       * just one face being bisected
       */
      fun fromBisectedFace(bisected: Face) = SeparatedFaces(bisected)
      /**
       * when there are only bisected faces and not bisected and no more bundles
       */
      fun fromBisectedAndNonBisectedFaces(bisected: Set<Face>, nonBisected: Map<SideOfFold, Set<Face>>) =
              SeparatedFaces(bisected, nonBisected)
   }
   override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false
      other as SeparatedFaces
      if (bisected != other.bisected) return false
      if (mapOfSideToWholeFaces != other.mapOfSideToWholeFaces) return false
      if (mapOfSideToRestOfBundles != other.mapOfSideToRestOfBundles) return false
      return true
   }
   override fun hashCode(): Int {
      var result = bisected.hashCode()
      result = 31 * result + mapOfSideToWholeFaces.hashCode()
      result = 31 * result + mapOfSideToRestOfBundles.hashCode()
      return result
   }
}