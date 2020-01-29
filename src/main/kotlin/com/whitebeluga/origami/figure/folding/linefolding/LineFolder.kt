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

package com.whitebeluga.origami.figure.folding.linefolding

import com.moduleforge.libraries.geometry._3d.Line
import com.moduleforge.libraries.geometry._3d.LineSegment
import com.moduleforge.libraries.geometry._3d.Point
import com.moduleforge.libraries.geometry._3d.Vector
import com.whitebeluga.origami.figure.Bundle
import com.whitebeluga.origami.figure.Figure
import com.whitebeluga.origami.figure.folding.SideOfFold
import com.whitebeluga.origami.figure.folding.SideOfFold.Companion.calculateSideOfFold
import com.whitebeluga.origami.figure.folding.breakingup.FigureBreaker
import com.whitebeluga.origami.figure.folding.breakingup.bisectedfacefindingandfaceseparation.AMAPBisectedFaceFinderAndFaceSeparator
import com.whitebeluga.origami.figure.folding.breakingup.bisectedfacefindingandfaceseparation.FFBisectedFaceFinderAndFaceSeparator
import com.whitebeluga.origami.figure.folding.breakingup.bisectedfacefindingandfaceseparation.parameters.SeparatedFaces
import com.whitebeluga.origami.figure.folding.rotating.Rotator.Companion.makeRotator

/**
 * Crease folding:
 *
 * This line-folding algorithm is able to fold any crease of a face that means the crease does not have to go from one
 * point to the boundary to another, it can be completely internal to the face or just one of the ends be internal.
 *
 * This kind of partial creases are more uncommon than complete creases but they are contemplated by this algorithm.
 */
internal abstract class LineFolder(protected val figure: Figure,
                      protected val foldSegment: LineSegment,
                      protected val angle: Double,
                      /**
                      It can be demonstrated that the vector that indicates in which side the valley fold takes place
                      also indicates the rotation direction for any of the two sides demarcated by the fold line.

                       It's a normal and perpendicular to the plane.
                       */
                      private val normalOfValleySide: Vector,
                      protected val segmentEndsDockedToVertices: List<Point>,
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
                      protected val guiUserLookingDirection: Vector,
                      pickedPointAsSideToRotate: Point? ) {
   protected val bundleToFold: Bundle
   private val foldLine: Line = foldSegment.line
   private val isThereChosenSideToRotate: Boolean
   protected val sideToRotate: SideOfFold?

   init {
      /*
      bundle to fold is the bundle for which there is at least one face fully intersected by
      the fold segment
      */
      val bundleIntersectedByFoldSegment = figure.bundles.firstOrNull {bundle ->
         bundle.faces.any { it.intersectedAcrossBy(foldSegment) }
       }
      bundleToFold = bundleIntersectedByFoldSegment ?: throw RuntimeException("No bundle is intersected by the fold segment.")
      isThereChosenSideToRotate = pickedPointAsSideToRotate != null
      sideToRotate = if(isThereChosenSideToRotate)
            calculateSideOfFold(foldLine, pickedPointAsSideToRotate!!, bundleToFold.plane)
         else null
   }
   companion object {
      fun makeAMAPLineFolder(figure: Figure, params: LineFoldParameters): LineFolder = AMAPLineFolder(figure, params)
      fun makeFFLineFolder(figure: Figure, params: LineFoldParameters): LineFolder = FFLineFolder(figure, params)
   }
   fun fold(): Figure {
      val facesBisectedAndSeparated = bisectAndSeparate()
      val figureBreaker = FigureBreaker(bundleToFold, facesBisectedAndSeparated, foldSegment, sideToRotate)
      val brokenUpFigure = figureBreaker.breakUpFigure()
      val rotator = makeRotator(brokenUpFigure, foldSegment, normalOfValleySide, angle)
      val folded = rotator.rotate()
      return folded
   }
   protected abstract fun bisectAndSeparate(): SeparatedFaces

   /**
   First flap line folder.

   Because sometimes we don't want to fold all the layers through a line, but rather a single "flap", for lack of a
   better word, the side to fold is of importance. That side is given by a point picked by the user from the GUI.

   The side is thus, optional, as when all layers are folded through, the end figure is the same.
    */
   private class FFLineFolder: LineFolder {
      private constructor(figure: Figure,
                          foldSegment: LineSegment,
                          angle: Double,
                          /** normal and perpendicular */
                          normalOfValleySide: Vector,
                          segmentEndsDockedToVertices: List<Point>,
                          guiUserLookingDirection: Vector,
                          pickedPointAsSideToRotate: Point?):
              super(figure, foldSegment, angle, normalOfValleySide, segmentEndsDockedToVertices, guiUserLookingDirection, pickedPointAsSideToRotate)
      constructor(figure: Figure, foldParameters: LineFoldParameters):
              this(   figure,
                      foldParameters.foldSegment,
                      foldParameters.angle,
                      foldParameters.normalOfValleySide,
                      foldParameters.segmentEndsDockedToVertices,
                      foldParameters.guiUserLookingDirection,
                      foldParameters.pickedPointAsSideToRotate)
      override fun bisectAndSeparate(): SeparatedFaces =
              FFBisectedFaceFinderAndFaceSeparator(figure, bundleToFold, foldSegment, guiUserLookingDirection, sideToRotate)
                      .findBisectedAndSeparateTheRest()
   }
   /**
   Fold that goes side to side or side to vertex or vertex to vertex and comprises one or more faces and all other
   faces connected to them that must too be folded.

   This class encapsulates an algorithm to fold as many layers as possible given a fold segment. It stops at partially
   intersected faces and all connected to them.
    */
   private class AMAPLineFolder: LineFolder {
      private constructor(figure: Figure,
                          foldSegment: LineSegment,
                          angle: Double,
                          normalOfValleySide: Vector,
                          segmentEndsDockedToVertices: List<Point>,
                          guiUserLookingDirection: Vector,
                          pickedPointAsSideToRotate: Point?):
              super(figure, foldSegment, angle, normalOfValleySide, segmentEndsDockedToVertices, guiUserLookingDirection, pickedPointAsSideToRotate)

      constructor(figure: Figure, foldParameters: LineFoldParameters):
              this(   figure,
                      foldParameters.foldSegment,
                      foldParameters.angle,
                      foldParameters.normalOfValleySide,
                      foldParameters.segmentEndsDockedToVertices,
                      foldParameters.guiUserLookingDirection,
                      foldParameters.pickedPointAsSideToRotate)
      override fun bisectAndSeparate(): SeparatedFaces =
         AMAPBisectedFaceFinderAndFaceSeparator(figure, bundleToFold, foldSegment, guiUserLookingDirection, sideToRotate)
                 .findBisectedAndSeparateTheRest()
   }

}