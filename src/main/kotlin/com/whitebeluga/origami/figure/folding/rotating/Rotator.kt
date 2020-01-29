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

import com.moduleforge.libraries.geometry.Geometry.epsilonEquals
import com.moduleforge.libraries.geometry._3d.Line
import com.moduleforge.libraries.geometry._3d.LineSegment
import com.moduleforge.libraries.geometry._3d.Vector
import com.whitebeluga.origami.figure.Bundle
import com.whitebeluga.origami.figure.Figure
import com.whitebeluga.origami.figure.folding.breakingup.BrokenUpFigure
import com.whitebeluga.origami.figure.folding.rotating.bundlerotation.BundleRotator.Companion.makePIRotator
import com.whitebeluga.origami.figure.folding.rotating.bundlerotation.BundleRotator.Companion.makeRotator
import java.lang.Math.PI

internal abstract class Rotator(
        protected val brokenUp: BrokenUpFigure,
        protected val line: Line,
        protected val rotationDirection: Vector) {
   protected val sideToRotate = brokenUp.sideToRotate
   protected val stationarySide = sideToRotate.opposite()
   protected val allVerticesOnFoldSegment = brokenUp.edgesOnTheFoldLine.map { it.vertices }.flatten().toSet()
   /**
    * If the angle is close enough (within geometry library precision) to a 180 angle it will do a flat fold
    *
    * Well, we could not check and allow a bundle to rotate as close to another bundle as we wished instead of
    * "docking" and joining the bundle but I don't think it makes a lot of sense in the context of the origami
    * application.
    */
   abstract fun rotate(): Figure
   companion object {
      fun makeRotator(brokenUp: BrokenUpFigure, foldSegment: LineSegment, rotationDirection: Vector, angle: Double): Rotator {
         val isFlatFold = epsilonEquals(angle, PI)
         return if(isFlatFold)
               PIRotator(brokenUp, foldSegment, rotationDirection)
            else
               RegularRotator(brokenUp, foldSegment.line, rotationDirection, angle)
      }
      fun makePIRotator(brokenUp: BrokenUpFigure, foldSegment: LineSegment, rotationDirection: Vector): Rotator =
              PIRotator(brokenUp, foldSegment, rotationDirection)
   }

   private class PIRotator(brokenUp: BrokenUpFigure, val foldSegment: LineSegment, rotationDirection: Vector): Rotator(brokenUp, foldSegment.line, rotationDirection) {
      override fun rotate(): Figure {
         val flatFolded = rotateBundles()
         val stationaryPartOfBisectedBundle = flatFolded.stationaryPartOfBisectedBundle
         val rotatedPartOfBisectedBundle = flatFolded.rotatedPartOfBisectedBundle
         val foldedBundle = stationaryPartOfBisectedBundle
                 .joinRotatedPartOfBundleOnItself(rotatedPartOfBisectedBundle, rotationDirection, foldSegment)

         val allBundles = flatFolded.restOfRotatedBundles + flatFolded.restOfStationaryBundles + foldedBundle
         return Figure(allBundles)
      }
      private fun rotateBundles(): FlatFoldedBrokenUpFigure {
         val rotatedSide = rotateSide()
         val rotatedPartOfSplitBundle= rotatedSide.first
         val restOfRotatedBundles = rotatedSide.second
         val stationaryPartOfBisectedBundle = brokenUp.mapOfSideToSplitBundlePart[stationarySide]!!
         val restOfStationaryBundles = brokenUp.mapOfSideToWholeBundles[stationarySide] ?: emptySet()
         return FlatFoldedBrokenUpFigure( stationaryPartOfBisectedBundle, rotatedPartOfSplitBundle, restOfStationaryBundles,
                 restOfRotatedBundles, rotationDirection)
      }
      /** returns a pair, first element is the rotated part of the split bundle,
       * second element are all the other bundles at the side to rotate*/
      private fun rotateSide(): Pair<Bundle, Set<Bundle>> {
         val bundlePartToBeRotated = brokenUp.mapOfSideToSplitBundlePart[sideToRotate]!!
         val restOfBundlesToBeRotated = brokenUp.mapOfSideToWholeBundles[sideToRotate] ?: emptySet()
         val bundleRotator = makePIRotator(bundlePartToBeRotated, restOfBundlesToBeRotated, line,
                 rotationDirection, allVerticesOnFoldSegment)
         return bundleRotator.rotate()
      }
   }
   /**
    * RegularRotator class for non-flat folds. Takes a "broken up" figure and rotates the bundles of the side that needs
    * rotating while avoiding point duplication.
    *
    * Then returns the new figure, made of the stationary (unchanged) bundles and the new rotated ones.
    *
    * Note that the rotated bundles are the only new stuff needed to make the new figure as long the fold
    * is not flat. Flat folds however require an extra step, having to do with the merging of what formerly
    * was a single bundle.
    */
   private class RegularRotator(brokenUp: BrokenUpFigure, line: Line, rotationDirection: Vector, val angle: Double):
           Rotator(brokenUp, line, rotationDirection) {
      /**
       * This function simply rotates the different parts or "bundles" of a "broken up" figure,
       * being careful not to duplicate any point in the process.
       */
      override fun rotate(): Figure {
         assertIsNotFlatFold()
         val toBeRotated = brokenUp.mapOfSideToBundles[sideToRotate]!!
         val bundleRotator = makeRotator(toBeRotated, line, rotationDirection, angle, allVerticesOnFoldSegment)
         val rotated = bundleRotator.rotateBundles()
         val stationary = brokenUp.mapOfSideToBundles[stationarySide]!!
         return Figure(rotated + stationary)
      }
      private fun assertIsNotFlatFold() {
         val isFlatFold = epsilonEquals(angle, PI)
         val isNotFlatFold = !isFlatFold
         //the type of result is different for flat folds than for non flat ones.
         assert(isNotFlatFold)
      }
   }
}