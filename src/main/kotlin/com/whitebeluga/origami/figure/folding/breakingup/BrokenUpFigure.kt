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

import com.whitebeluga.origami.figure.Bundle
import com.whitebeluga.origami.figure.component.Edge
import com.whitebeluga.origami.figure.folding.SideOfFold

/**
 * Represents the state of the figure after breaking up along a fold line and deciding a side to rotate (if none
 * was given).
 *
 * The next step is doing the rotation of the bundles and for the case of flat folds, merge bundles and
 * merge together some faces in certain cases.
 */
internal class BrokenUpFigure(
        val splitBundle: SplitBundle,
        val mapOfSideToWholeBundles: Map<SideOfFold, Set<Bundle>>,
        val sideToRotate: SideOfFold) {
   val mapOfSideToSplitBundlePart: Map<SideOfFold, Bundle> = splitBundle.mapOfSideToBundlePart
   /**
    * Edges that fall exactly on the edge of the line.
    *
    * Normally these are created when the fold is done and faces are split by the fold segment.
    *
    * However it can also happen that some segments of the bundle to fold, fall exactly on the line.
    */
   val edgesOnTheFoldLine: Set<Edge> = splitBundle.edgesOnFoldLine
   /**
    * Addition of the split bundle map and the rest of bundles map.
    */
   val mapOfSideToBundles: Map<SideOfFold, Set<Bundle>> = joinMaps(mapOfSideToSplitBundlePart, mapOfSideToWholeBundles)

   companion object {
      fun joinMaps(splitBundle: Map<SideOfFold, Bundle>, restOfBundles: Map<SideOfFold, Set<Bundle>>):
              Map<SideOfFold, Set<Bundle>> =
         restOfBundles.toMutableMap().also { result ->
           splitBundle.entries.forEach { (k, v) ->
              result[k] = if (result[k] == null) setOf(v) else result[k]!! + v } }
   }
}