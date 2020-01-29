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

import com.moduleforge.libraries.geometry._3d.ColorCombination
import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Plane.planeFromOrderedPoints
import com.moduleforge.libraries.geometry._3d.Point
import com.whitebeluga.origami.figure.component.Face
import com.whitebeluga.origami.figure.component.Vertex
import java.awt.Color
import java.awt.Color.WHITE
import java.lang.Math.*

/*
 * This class is not covered by unit tests for two reasons:
 * - It would much more harder to test than what can be seen with the naked eye (and these figures' purpose is to be
 * displayed on a GUI)
 * - This is code that will not change, that is, refactored, only bugs will be fixed. An origami base is an origami
 * base and always will be the same.
 *
 * By convention we use, for all bases, a plane whose normal goes into z- (z negative) part of the space.
 *
 * When creating a base, the last layer added is the one further into z-. If we usually picture a user looking from z+
 * into a figure centered around the coordinate axis origin, we might want to create a figure that would look right
 * to such a user.
 */
enum class OrigamiBase {
   SQUARE {
      private fun makeVerticesOfSquare(sideLength: Double, center: Point): List<Vertex> {
         val x = center.x(); val y = center.y(); val z = center.z()
         val halfLength = sideLength / 2.0
         return listOf(Vertex(x - halfLength, y - halfLength, z), Vertex(x + halfLength, y - halfLength, z),
                 Vertex(x + halfLength, y + halfLength, z), Vertex(x - halfLength, y + halfLength, z))
      }
      /**
      Vertices in counter Clockwise order when viewed from z+ part of space
      Figure parallel to the XY plane
      Layer's plane normal vertices to z+ part of space
       */
      override fun make(sideLength: Double, colors: ColorCombination, center: Point): Figure {
         val square = Face(makeVerticesOfSquare(sideLength, center), colors)
         val plane = planeParallelToXYAndZNegDirection(center.z())
         val layerStack = Bundle(plane, square)
         return Figure(layerStack)
      }
   },
   BIRD {
      override fun make(sideLength: Double, colors: ColorCombination, center: Point): Figure {
         val halfFigureWidth = sideLength * tan(PI / 8.0) / 2.0
         val halfFigureHeight = sideLength / 2.0
         val x = center.x(); val y = center.y(); val z = center.z()
         val bottomLeft = Vertex(x, y - halfFigureHeight, z)
         val bottomRight = Vertex(bottomLeft)
         val top = Vertex(x, y + halfFigureHeight, z)
         val frontLeft = Vertex(x - halfFigureWidth, y, z)
         val frontRight = Vertex(x + halfFigureWidth, y, z)
         val frontLeftLongTriangle = Face(top, frontLeft, bottomLeft, colors=colors)
         val frontRightLongTriangle = Face(top, bottomRight, frontRight, colors=colors)
         val longUpperTriangle = Face(frontRight, frontLeft, top)
         val tipOfCentralPyramid = Vertex(x, y + halfFigureWidth, z)//for lack of a better word
         val shortUpperTriangle = Face(tipOfCentralPyramid, frontLeft, frontRight)
         val rightFirstInnerTriangle = Face(tipOfCentralPyramid, frontRight, bottomRight)
         val leftFirstInnerTriangle = Face(tipOfCentralPyramid, bottomLeft, frontLeft)
         val behindLeft = Vertex(frontLeft)
         val behindRight = Vertex(frontRight)
         val rightSecondInnerTriangle = Face(tipOfCentralPyramid, bottomRight, behindRight)
         val leftSecondInnerTriangle = Face(tipOfCentralPyramid, behindLeft, bottomLeft)
         val behindShortUpperTriangle = Face(tipOfCentralPyramid, behindRight, behindLeft)
         val behindTop = Vertex(top)
         val behindLongUpperTriangle = Face(behindTop, behindLeft, behindRight)
         val behindLeftLongTriangle = Face(behindTop, bottomLeft, behindLeft, colors=colors)
         val behindRightLongTriangle = Face(behindTop, behindRight, bottomRight, colors=colors)
         val faces = setOf(
                 frontLeftLongTriangle, frontRightLongTriangle,
                 longUpperTriangle, shortUpperTriangle,
                 rightFirstInnerTriangle, rightSecondInnerTriangle, leftFirstInnerTriangle, leftSecondInnerTriangle,
                 behindShortUpperTriangle, behindLongUpperTriangle,
                 behindLeftLongTriangle, behindRightLongTriangle)
         val facesToFacesAbove = mapOf(
              //front faces
              frontLeftLongTriangle to setOf(
                      longUpperTriangle, shortUpperTriangle,
                      leftFirstInnerTriangle, leftSecondInnerTriangle,
                      behindShortUpperTriangle, behindLongUpperTriangle,
                      behindLeftLongTriangle ),
              frontRightLongTriangle to setOf(
                      longUpperTriangle, shortUpperTriangle,
                      rightFirstInnerTriangle, rightSecondInnerTriangle,
                      behindShortUpperTriangle, behindLongUpperTriangle,
                      behindRightLongTriangle ),
              //first pair of upper triangles
              longUpperTriangle to setOf(shortUpperTriangle,
                      rightFirstInnerTriangle, rightSecondInnerTriangle,
                      leftFirstInnerTriangle, leftSecondInnerTriangle,
                      behindShortUpperTriangle, behindLongUpperTriangle,
                      behindLeftLongTriangle, behindRightLongTriangle),
              shortUpperTriangle to setOf(
                      rightFirstInnerTriangle, rightSecondInnerTriangle,
                      leftFirstInnerTriangle, leftSecondInnerTriangle,
                      behindShortUpperTriangle, behindLongUpperTriangle,
                      behindLeftLongTriangle, behindRightLongTriangle),
              //second pair of upper triangles
              behindShortUpperTriangle to setOf(behindLongUpperTriangle, behindLeftLongTriangle, behindRightLongTriangle),
              behindLongUpperTriangle to setOf(behindLeftLongTriangle, behindRightLongTriangle),
              //inner triangles
              rightFirstInnerTriangle to setOf(rightSecondInnerTriangle,
                      behindShortUpperTriangle, behindLongUpperTriangle, behindRightLongTriangle),
              rightSecondInnerTriangle to setOf(behindShortUpperTriangle, behindLongUpperTriangle, behindRightLongTriangle),
              leftFirstInnerTriangle to setOf(leftSecondInnerTriangle,
                      behindShortUpperTriangle, behindLongUpperTriangle, behindLeftLongTriangle),
              leftSecondInnerTriangle to setOf(behindShortUpperTriangle, behindLongUpperTriangle, behindLeftLongTriangle) )
         val plane = planeParallelToXYAndZNegDirection(z)
         return Figure(Bundle(plane, faces, facesToFacesAbove))
      }
   },
   BIRD_BLINTZ{
      override fun make(sideLength: Double, colors: ColorCombination, center: Point): Figure {
         val kitePoints = kitePoints(sideLength) //top, right, bottom, left
         val figureWidth= sqrt_2*sideLength/2.0 //half diagonal
         val halfWidth = figureWidth/2.0
         //vertices:
         val kiteTop = kitePoints[0]
         val kiteRight = kitePoints[1]
         val kiteBottom = kitePoints[2]
         val kiteLeft = kitePoints[3]
         val backKiteRight = Vertex(kiteRight)
         val backKiteLeft = Vertex(kiteLeft)
         val backKiteBottom = Vertex(kiteBottom)
         val frontRightWingVertex = Vertex(halfWidth, kiteRight.y(), 0.0)
         val frontLeftWingVertex = Vertex(-halfWidth, kiteRight.y(), 0.0)
         val backRightWingVertex = Vertex(halfWidth, kiteRight.y(), 0.0)
         val backLeftWingVertex = Vertex(-halfWidth, kiteRight.y(), 0.0)
         val innerRightBottom = Vertex(kiteBottom)
         val innerLeftBottom = Vertex(kiteBottom)
         val innerLeftFront = Vertex(0.0, kiteRight.y(), 0.0)
         val innerRightFront = Vertex(innerLeftFront)
         val innerLeftBack = Vertex(innerLeftFront)
         val innerRightBack = Vertex(innerLeftFront)
         //end vertices
         //first layer
         val frontKite = Face(kiteTop, kiteRight, kiteBottom, kiteLeft, colors=colors)
         //second layer
         val frontFrontSmallRightTriangle = Face(kiteRight, innerRightFront, kiteBottom, colors=colors)
         val frontFrontSmallLeftTriangle = Face(kiteLeft, kiteBottom, innerLeftFront, colors=colors)
         //third layer
         val frontFrontBigRightTriangle = Face(innerRightFront, frontRightWingVertex, kiteBottom, colors=colors)
         val frontFrontBigLeftTriangle = Face(frontLeftWingVertex, innerLeftFront, kiteBottom, colors=colors)
         //fourth layer
         val frontBackBigRightTriangle = Face(frontRightWingVertex, innerRightFront, innerRightBottom, colors=colors)
         val frontBackBigLeftTriangle = Face(innerLeftFront, frontLeftWingVertex, innerLeftBottom, colors=colors)
         //fifth layer
         val frontBackSmallRightTriangle = Face(innerRightFront, kiteRight, innerRightBottom, colors=colors)
         val frontBackSmallLeftTriangle = Face(innerLeftBottom, kiteLeft, innerLeftFront, colors=colors)
         //sixth layer
         val firstRightHalfKite = Face(kiteTop, innerRightBottom, kiteRight, colors=colors)
         val firstLeftHalfKite = Face(kiteTop, kiteLeft, innerLeftBottom, colors=colors)
         //seventh layer
         val secondRightHalfKite = Face(kiteTop, backKiteRight, innerRightBottom, colors=colors)
         val secondLeftHalfKite = Face(kiteTop, innerLeftBottom, backKiteLeft, colors=colors)
         //eighth layer
         val backFrontSmallRightTriangle = Face(backKiteRight, innerRightBack, innerRightBottom, colors=colors)
         val backFrontSmallLeftTriangle = Face(innerLeftBack, backKiteLeft, innerLeftBottom, colors=colors)
         //ninth layer
         val backFrontBigRightTriangle = Face(innerRightBack, backRightWingVertex, innerRightBottom, colors=colors)
         val backFrontBigLeftTriangle = Face(backLeftWingVertex, innerLeftBack, innerLeftBottom, colors=colors)
         //tenth layer
         val backBackBigRightTriangle = Face(backRightWingVertex, innerRightBack, backKiteBottom, colors=colors)
         val backBackBigLeftTriangle = Face(innerLeftBack, backLeftWingVertex, backKiteBottom, colors=colors)
         //eleventh layer
         val backBackSmallRightTriangle = Face(innerRightBack, backKiteRight, backKiteBottom, colors=colors)
         val backBackSmallLeftTriangle = Face(backKiteLeft, innerLeftBack, backKiteBottom, colors=colors)
         //last layer
         val backKite = Face(backKiteLeft, backKiteBottom, backKiteRight, kiteTop, colors=colors)
         val faces = setOf(frontKite,
                 frontFrontSmallRightTriangle,
                 frontFrontSmallLeftTriangle,
                 frontFrontBigRightTriangle,
                 frontFrontBigLeftTriangle,
                 frontBackBigRightTriangle,
                 frontBackBigLeftTriangle,
                 frontBackSmallRightTriangle,
                 frontBackSmallLeftTriangle,
                 firstRightHalfKite,
                 firstLeftHalfKite,
                 secondRightHalfKite,
                 secondLeftHalfKite,
                 backFrontSmallRightTriangle,
                 backFrontSmallLeftTriangle,
                 backFrontBigRightTriangle,
                 backFrontBigLeftTriangle,
                 backBackBigRightTriangle,
                 backBackBigLeftTriangle,
                 backBackSmallRightTriangle,
                 backBackSmallLeftTriangle,
                 backKite)
         val facesToFacesAbove = mapOf(
                 frontKite to setOf(frontFrontSmallRightTriangle,
                         frontFrontSmallLeftTriangle,
                         frontFrontBigRightTriangle,
                         frontFrontBigLeftTriangle,
                         frontBackBigRightTriangle,
                         frontBackBigLeftTriangle,
                         frontBackSmallRightTriangle,
                         frontBackSmallLeftTriangle,
                         firstRightHalfKite,
                         firstLeftHalfKite,
                         secondRightHalfKite,
                         secondLeftHalfKite,
                         backFrontSmallRightTriangle,
                         backFrontSmallLeftTriangle,
                         backFrontBigRightTriangle,
                         backFrontBigLeftTriangle,
                         backBackBigRightTriangle,
                         backBackBigLeftTriangle,
                         backBackSmallRightTriangle,
                         backBackSmallLeftTriangle,
                         backKite),
                 frontFrontSmallRightTriangle to setOf(
                         frontFrontBigRightTriangle,
                         frontBackBigRightTriangle,
                         frontBackSmallRightTriangle,
                         firstRightHalfKite,
                         secondRightHalfKite,
                         backFrontSmallRightTriangle,
                         backFrontBigRightTriangle,
                         backBackBigRightTriangle,
                         backBackSmallRightTriangle,
                         backKite),
                 frontFrontSmallLeftTriangle to setOf(
                         frontFrontBigLeftTriangle,
                         frontBackBigLeftTriangle,
                         frontBackSmallLeftTriangle,
                         firstLeftHalfKite,
                         secondLeftHalfKite,
                         backFrontSmallLeftTriangle,
                         backFrontBigLeftTriangle,
                         backBackBigLeftTriangle,
                         backBackSmallLeftTriangle,
                         backKite),
                 frontFrontBigRightTriangle to setOf(
                         frontBackBigRightTriangle,
                         frontBackSmallRightTriangle,
                         firstRightHalfKite,
                         secondRightHalfKite,
                         backFrontSmallRightTriangle,
                         backFrontBigRightTriangle,
                         backBackBigRightTriangle,
                         backBackSmallRightTriangle,
                         backKite),
                 frontFrontBigLeftTriangle to setOf(
                         frontBackBigLeftTriangle,
                         frontBackSmallLeftTriangle,
                         firstLeftHalfKite,
                         secondLeftHalfKite,
                         backFrontSmallLeftTriangle,
                         backFrontBigLeftTriangle,
                         backBackBigLeftTriangle,
                         backBackSmallLeftTriangle,
                         backKite),
                 frontBackBigRightTriangle to setOf(
                         frontBackSmallRightTriangle,
                         firstRightHalfKite,
                         secondRightHalfKite,
                         backFrontSmallRightTriangle,
                         backFrontBigRightTriangle,
                         backBackBigRightTriangle,
                         backBackSmallRightTriangle,
                         backKite),
                 frontBackBigLeftTriangle to setOf(
                         frontBackSmallLeftTriangle,
                         firstLeftHalfKite,
                         secondLeftHalfKite,
                         backFrontSmallLeftTriangle,
                         backFrontBigLeftTriangle,
                         backBackBigLeftTriangle,
                         backBackSmallLeftTriangle,
                         backKite),
                 frontBackSmallRightTriangle to setOf(
                         firstRightHalfKite,
                         secondRightHalfKite,
                         backFrontSmallRightTriangle,
                         backFrontBigRightTriangle,
                         backBackBigRightTriangle,
                         backBackSmallRightTriangle,
                         backKite),
                 frontBackSmallLeftTriangle to setOf(
                         firstLeftHalfKite,
                         secondLeftHalfKite,
                         backFrontSmallLeftTriangle,
                         backFrontBigLeftTriangle,
                         backBackBigLeftTriangle,
                         backBackSmallLeftTriangle,
                         backKite),
                 firstRightHalfKite to setOf(
                         secondRightHalfKite,
                         backFrontSmallRightTriangle,
                         backFrontBigRightTriangle,
                         backBackBigRightTriangle,
                         backBackSmallRightTriangle,
                         backKite),
                 firstLeftHalfKite to setOf(
                         secondLeftHalfKite,
                         backFrontSmallLeftTriangle,
                         backFrontBigLeftTriangle,
                         backBackBigLeftTriangle,
                         backBackSmallLeftTriangle,
                         backKite),
                 secondRightHalfKite to setOf(
                          backFrontSmallRightTriangle,
                          backFrontBigRightTriangle,
                          backBackBigRightTriangle,
                          backBackSmallRightTriangle,
                          backKite),
                 secondLeftHalfKite to setOf(
                         backFrontSmallLeftTriangle,
                         backFrontBigLeftTriangle,
                         backBackBigLeftTriangle,
                         backBackSmallLeftTriangle,
                         backKite),
                 backFrontSmallRightTriangle to setOf(
                         backFrontBigRightTriangle,
                         backBackBigRightTriangle,
                         backBackSmallRightTriangle,
                         backKite),
                 backFrontSmallLeftTriangle to setOf(
                         backFrontBigLeftTriangle,
                         backBackBigLeftTriangle,
                         backBackSmallLeftTriangle,
                         backKite),
                 backFrontBigRightTriangle to setOf(backBackBigRightTriangle, backBackSmallRightTriangle, backKite),
                 backFrontBigLeftTriangle to setOf(backBackBigLeftTriangle, backBackSmallLeftTriangle, backKite),
                 backBackBigRightTriangle to setOf(backBackSmallRightTriangle, backKite),
                 backBackBigLeftTriangle to setOf(backBackSmallLeftTriangle, backKite),
                 backBackSmallRightTriangle to setOf(backKite),
                 backBackSmallLeftTriangle to setOf(backKite)
         )
         val plane = planeParallelToXYAndZNegDirection(center.z())
         val base = Figure(Bundle(plane, faces, facesToFacesAbove))
         //need to center this particular base! (the calculation was easier with no centering)
         return base.translated(center)
      }
      //return in this order: top, right, bottom, left
      //for simplicity we make the top point be at (0, 0, 0)
      private fun kitePoints(sideLength: Double): List<Vertex> {
         val shortSideOfKite = (sin(PI / 8.0) * sideLength) / (sin(5.0 * PI / 8.0) * 2.0)
         val rightX = (sqrt_2/2.0) * shortSideOfKite
         val crossY  = -rightX
         val right = Vertex(rightX, crossY, 0.0)
         val left = Vertex(-rightX, crossY, 0.0)
         val top = Vertex(0.0, 0.0, 0.0)
         val bottom = Vertex(0.0, -sideLength / 2.0, 0.0)
         return listOf(top, right, bottom, left)
      }
   },
   BLINTZ {
      override fun make(sideLength: Double, colors: ColorCombination, center: Point): Figure {
         val x = center.x(); val y = center.y(); val z = center.z()
         val halfFigureWidth = sideLength/2.0
         val leftPoint = Vertex(x - halfFigureWidth, y, z)
         val bottomPoint = Vertex(x, y - halfFigureWidth, z)
         val rightPoint = Vertex(x + halfFigureWidth, y, z)
         val topPoint = Vertex(x, y + halfFigureWidth, z)
         //if it were folded from a square paper, this part of the paper would have stayed put, so the order of the
         // vertices is counterclockwise, just as we do with the square paper
         val bottomFace = Face(leftPoint, bottomPoint, rightPoint, topPoint, colors=colors)
         //the vertices on these flaps go in clockwise direction
         val leftTopFlap = Face(leftPoint, topPoint, Vertex(center), colors=colors)
         val leftBottomFlap = Face(leftPoint, Vertex(center), bottomPoint, colors=colors)
         val rightBottomFlap = Face(Vertex(center), rightPoint, bottomPoint, colors=colors)
         val rightTopFlap = Face(Vertex(center), topPoint, rightPoint, colors=colors)
         val faces = setOf(bottomFace, leftTopFlap, leftBottomFlap, rightBottomFlap, rightTopFlap)
         val facesToFacesAbove = mapOf(
                 bottomFace to setOf(leftTopFlap, leftBottomFlap, rightBottomFlap, rightTopFlap) )
         val plane = planeParallelToXYAndZNegDirection(center.z())
         return Figure(Bundle(plane, faces, facesToFacesAbove))
      }
   },
   BOAT {
      override fun make(sideLength: Double, colors: ColorCombination, center: Point): Figure {
         val halfFigureWidth = sideLength / 2.0
         val halfFigureHeight = sideLength / 4.0
         val x = center.x(); val y = center.y(); val z = center.z()

         val centerVertexForRightHandFaces = Vertex(center)
         val centerVertexForLeftHandFaces = Vertex(center)
         val right = Vertex(x + halfFigureWidth, y, z)
         val left = Vertex(x - halfFigureWidth, y, z)
         val rightVertexForUpperFaces = Vertex(right)
         val rightVertexForLowerFaces = Vertex(right)
         val leftVertexForUpperFaces = Vertex(left)
         val leftVertexForLowerFaces = Vertex(left)
         val upperRightVertex = Vertex(x + halfFigureWidth / 2.0, y + halfFigureHeight, z)
         val upperLeftVertex = Vertex(x - halfFigureWidth / 2.0, y + halfFigureHeight, z)
         val lowerRightVertex = Vertex(x + halfFigureWidth / 2.0, y - halfFigureHeight, z)
         val lowerLeftVertex = Vertex(x - halfFigureWidth / 2.0, y - halfFigureHeight, z)

         //the faces I call "deck" are because if we think of the base as a boat, this faces at the front would form the sides of the "deck" of the boat
         val upperDeck = Face(leftVertexForUpperFaces, upperLeftVertex, upperRightVertex, rightVertexForUpperFaces, colors=colors)
         val lowerDeck = Face(leftVertexForLowerFaces, rightVertexForLowerFaces, lowerRightVertex, lowerLeftVertex, colors=colors)
         val upperRightTriangle = Face(upperRightVertex, centerVertexForRightHandFaces, rightVertexForUpperFaces, colors=colors)
         val upperLeftTriangle = Face(upperLeftVertex, leftVertexForUpperFaces, centerVertexForLeftHandFaces, colors=colors)

         val lowerRightTriangle = Face(centerVertexForRightHandFaces, lowerRightVertex, rightVertexForLowerFaces, colors=colors)
         val lowerLeftTriangle = Face(leftVertexForLowerFaces, lowerLeftVertex, centerVertexForLeftHandFaces, colors=colors)
         val hiddenRightPocket = Face(centerVertexForRightHandFaces, upperRightVertex, lowerRightVertex, colors=colors)
         val hiddenLeftPocket = Face(centerVertexForLeftHandFaces, lowerLeftVertex, upperLeftVertex, colors=colors)

         val squareBase = Face(upperLeftVertex, lowerLeftVertex, lowerRightVertex, upperRightVertex, colors=colors)

         val faces = setOf(upperDeck, lowerDeck,
                 upperRightTriangle, upperLeftTriangle, lowerRightTriangle, lowerLeftTriangle,
                 hiddenRightPocket, hiddenLeftPocket,
                 squareBase)
         val facesToFacesAbove = mapOf(
                 squareBase to faces - squareBase,
                 hiddenRightPocket to setOf(upperRightTriangle, lowerRightTriangle, upperDeck, lowerDeck),
                 hiddenLeftPocket to setOf(upperLeftTriangle, lowerLeftTriangle, upperDeck, lowerDeck),
                 upperRightTriangle to setOf(upperDeck),
                 upperLeftTriangle to setOf(upperDeck),
                 lowerRightTriangle to setOf(lowerDeck),
                 lowerLeftTriangle to setOf(lowerDeck) )
         val plane = planeParallelToXYAndZNegDirection(center.z())
         return Figure(Bundle(plane, faces, facesToFacesAbove))
      }
   },
   DIAMOND {
      /**
      |                      -m:
      |                     :+s:/
      |                    :/ s :/
      |                   :/  s  :/
      |                  :/   s   -/
      |           V1 -------> s    -/ <----- V2
      |                :/    `y     -+
      |               /:     /s\ <----------- V3
      |              /:.  :/  s  -/.  :-+
      |             /+ /:     s     \./oo
      |            /y-        s        `oo
      |            -+         s         /:
      |             .o        s        /:
      |              .o       s       /-
      |               .o      s      +-
      |                .o     s     +-
      |                 .o    s    +-
      |                  .o   s   +-
      |                   `o  s  +.
      |                    `o s +.
      |                     `oyo.
      |                      `d.
       */
      override fun make(sideLength: Double, colors: ColorCombination, center: Point): Figure {
         val x = center.x(); val y = center.y(); val z = center.z()
         val figureWidth = tan(PI / 8.0) * sideLength * sqrt_2 //trust me, this is the formula
         val halfFigureHeight = sideLength / sqrt_2
         val top = Vertex(x, y + halfFigureHeight, z)
         val bottom = Vertex(x, y - halfFigureHeight, z)
         val left = Vertex(x - figureWidth/2.0, y, z)
         val right = Vertex(x + figureWidth/2.0, y, z)

         val base = Face(top, left, bottom, right, colors=colors)

         //take a look at the javadoc above to understand where these points are
         val v1_Right = Vertex(x, y + sideLength - halfFigureHeight, z)
         val v1_Left = Vertex(v1_Right)
         val deltaX_OfV2 = tan(PI / 8.0) * (sqrt_2 * sideLength - sideLength)
         val v2_Right = Vertex(x + deltaX_OfV2, v1_Right.y(), z)
         val v2_Left = Vertex(x - deltaX_OfV2, v1_Right.y(), z)

         //h is the distance from the top to the corners of the flaps in the middle (take a look at the diagram above)
         val h = (sideLength * sqrt_2 - sideLength) / cos(PI / 4.0)
         val v3_Right = Vertex(x, y + halfFigureHeight - h, z)
         val hiddenRight = Face(right, v2_Right, v3_Right, colors=colors)
         val v3_Left = Vertex(v3_Right)
         val hiddenLeft = Face(v2_Left, left, v3_Left, colors=colors)
         val upperRight = Face(top, right, v3_Right, colors=colors)
         val upperLeft = Face(top, v3_Left, left, colors=colors)
         val lowerRight = Face(v1_Right, v2_Right, right, bottom, colors=colors)
         val lowerLeft = Face(v1_Left, bottom, left, v2_Left, colors=colors)
         val faces = setOf(base,
                 upperLeft, upperRight,
                 hiddenLeft, hiddenRight, lowerLeft, lowerRight)
         val facesToFacesAbove = mapOf(
                 base to faces - base,
                 lowerRight to setOf(hiddenRight, upperRight),
                 lowerLeft to setOf(hiddenLeft, upperLeft),
                 hiddenRight to setOf(upperRight),
                 hiddenLeft to setOf(upperLeft)
         )
         val plane = planeParallelToXYAndZNegDirection(center.z())
         return Figure(Bundle(plane, faces, facesToFacesAbove))
      }
   },
   FISH {
      /**
       * Crease pattern of fish base:
      |
      |                       :. top point
      |                     :yoh+.
      |                   :/-o -/:+.
      |                 :/` s   +- :+.
      |               :/`  o.    o`  :+.
      |             :/`   /:      s    :+.
      |           :/`    -+       .o     :o.
      |         :+o     `o         :/    `o:+.   upper right
      |       :/` /-    s`          +.   o`  -+.
      |     :/`    o`  +.            s` .+     -+.
      |   :/`       s /:             `s s        -+.
      | :/`         .s+               .y/          -+.
      |-y/:::::::::::s.                s::::::::::::ss  right ext.
      |  :/`          s`              +.          :+.
      |    :/.        `s             ::         :+.
      |      :/.       -+           .o        :+.
      |        :/.      /:         `s       :+.
      |          :/.     o.        o`     :+.            |
      |            :/.    s`      +-    :+.              |
      |              :/.  `o     :/   :+.
      |                :/. -+   .o  :+.
      |                  :/./-  s :+.
      |                    :/y.o/+.
      |                      :ys.  bottom
      |                       `
      Folded:
      |                      -m:
      |                     :+s:/
      |                    :/ s :/
      |                   :/  s  :/
      |                  :/   s   -/
      |                 :/    y`   -/
      |                :/  `//y:+.  -+
      |               /: `//` s  -+- -+
      |              /:./:    s    -+:-+
      |             /+/:      s      ./oo
      |            /y-        s        `oo
      |            -+         s         /:
      |             .o        s        /:
      |              .o       s       /-
      |               .o      s      +-
      |                .o     s     +-
      |                 .o    s    +-
      |                  .o   s   +-
      |                   `o  s  +.
      |                    `o s +.
      |                     `oyo.
      |                      `d.
       */
      override fun make(sideLength: Double, colors: ColorCombination, center: Point): Figure {
         val x = center.x(); val y = center.y(); val z = center.z()
         val figureWidth = tan(PI / 8.0) * sideLength * sqrt_2 //trust me, this is the formula
         val top = Vertex(x, y + sideLength/sqrt_2, z)
         val bottom = Vertex(x, y - sideLength/sqrt_2, z)
         val left = Vertex(x - figureWidth/2.0, y, z)
         val right = Vertex(x + figureWidth/2.0, y, z)
         val middleOnRightPart = Vertex(center)
         val middleOnLeftPart = Vertex(center)
         //if it were folded from a square paper, this part of the paper would have stayed put, so the order of the
         // vertices is counterclockwise, just as we do with the square paper
         val bottomFace = Face( top, left, bottom, right, colors=colors)
         //clockwise
         val upperLeftFace = Face(top, middleOnLeftPart, left)
         val upperRightFace = Face(top, right, middleOnRightPart)

         val cornerRightFlap = Vertex(x, y + sideLength * ( 1.0 - (1.0/sqrt_2)), z)
         val cornerLeftFlap = Vertex(x, y + sideLength * ( 1.0 - (1.0/sqrt_2)), z)

         //counter clockwise
         val hiddenLeftFace = Face(cornerLeftFlap, left, middleOnLeftPart)
         val hiddenRightFace = Face(cornerRightFlap, middleOnRightPart, right)
         //clockwise
         val topLeftFace = Face(cornerLeftFlap, bottom, left)
         val topRightFace = Face(cornerRightFlap, right, bottom)
         val faces = setOf(bottomFace, topLeftFace, topRightFace, hiddenLeftFace, hiddenRightFace,
                 upperLeftFace, upperRightFace)
         val facesToFacesAbove = mapOf(
                 bottomFace to faces - bottomFace,
                 upperLeftFace to setOf(hiddenLeftFace, topLeftFace),
                 upperRightFace to setOf(hiddenRightFace, topRightFace),
                 hiddenLeftFace to setOf(topLeftFace),
                 hiddenRightFace to setOf(topRightFace) )
         val plane = planeParallelToXYAndZNegDirection(center.z())
         return Figure(Bundle(plane, faces, facesToFacesAbove))
      }
   },
   FROG {
      override fun make(sideLength: Double, colors: ColorCombination, center: Point): Figure {
         //TODO ...
         return super.make(sideLength, colors, center)
      }
   },
   KITE {
      override fun make(sideLength: Double, colors: ColorCombination, center: Point): Figure {
         val x = center.x(); val y = center.y(); val z = center.z()
         val top = Vertex(x, y + sideLength/sqrt_2, z)
         val bottom = Vertex(x, y - sideLength/sqrt_2, z)
         val halfFigureWidth = tan(PI/8.0) * sideLength //trust me, this is the formula
         val right = Vertex(x + halfFigureWidth, y + sideLength - sideLength/sqrt_2, z)
         val left = Vertex(x - halfFigureWidth, y + sideLength - sideLength/sqrt_2, z)
         val bottomFace = Face(top, left, bottom, right, colors=colors)
         val crossPoint = Vertex(x, y + sideLength - sideLength/sqrt_2, z)
         val leftFlapPolygon = Face(left, crossPoint, bottom, colors=colors)
         val rightFlapPolygon = Face(Vertex(crossPoint), right, bottom, colors=colors)
         val faces = setOf(bottomFace, rightFlapPolygon, leftFlapPolygon)
         val plane = planeParallelToXYAndZNegDirection(center.z())
         val facesToFacesAbove = mapOf(
                 bottomFace to setOf(leftFlapPolygon, rightFlapPolygon))
         return Figure(Bundle(plane, faces, facesToFacesAbove))
      }
   },
   PRELIMINARY {
      override fun make(sideLength: Double, colors: ColorCombination, center: Point): Figure {

         val halfFigureWidth = sideLength * (sqrt_2 / 4.0)
         val halfFigureHeight = sideLength * (sqrt_2 / 4.0)
         val x = center.x(); val y = center.y(); val z = center.z()

         val topVertex = Vertex(x, y + halfFigureHeight, z)
         val bottomAtFront = Vertex(x, y - halfFigureHeight, z)
         val bottomAtBack = Vertex(bottomAtFront)
         val bottomInnerAtRightSide = Vertex(bottomAtFront)
         val bottomInnerAtLeftSide = Vertex(bottomAtFront)
         val rightAtFront = Vertex(x + halfFigureWidth, y, z)
         val rightAtBack = Vertex(rightAtFront)
         val leftAtFront = Vertex(x - halfFigureWidth, y, z)
         val leftAtBack = Vertex(leftAtFront)

         val back = Face(topVertex, rightAtBack, bottomAtBack, leftAtBack, colors=colors)
         val innerLeft1 = Face(topVertex, leftAtBack, bottomInnerAtLeftSide, colors=colors)
         val innerLeft2 = Face(topVertex, bottomInnerAtLeftSide, leftAtFront, colors=colors)
         val innerRight1 = Face(topVertex, bottomInnerAtRightSide, rightAtBack, colors=colors)
         val innerRight2 = Face(topVertex, rightAtFront, bottomInnerAtRightSide, colors=colors)
         val front = Face(topVertex, leftAtFront, bottomAtFront, rightAtFront, colors=colors)

         val faces = setOf(back, innerLeft1, innerLeft2, innerRight1, innerRight2, front)
         val plane = planeParallelToXYAndZNegDirection(center.z())
         val facesToFacesAbove = mapOf(
                 back to setOf(front, innerLeft1, innerLeft2, innerRight1, innerRight2),
                 innerLeft1 to setOf(front, innerLeft2),
                 innerRight1 to setOf(front, innerRight2),
                 innerLeft2 to setOf(front),
                 innerRight2 to setOf(front) )
         return Figure(Bundle(plane, faces, facesToFacesAbove))
      }
   },
   WATERBOMB {
      override fun make(sideLength: Double, colors: ColorCombination, center: Point): Figure {
         val halfFigureWidth = sideLength/2.0
         val halfFigureHeight = sideLength/4.0
         val x = center.x(); val y = center.y(); val z = center.z()
         val top = Vertex(x, y + halfFigureHeight, z)
         val rightFront = Vertex(x + halfFigureWidth, y - halfFigureHeight, z)
         val leftFront = Vertex(x - halfFigureWidth, y - halfFigureHeight, z)
         val front = Face(top, leftFront, rightFront, colors=colors)
         val middleBottomLeftPart = Vertex(x, y - halfFigureHeight, z)
         val middleBottomRightPart = Vertex(middleBottomLeftPart)
         val innerLeftTriangle1 = Face(top, middleBottomLeftPart, leftFront, colors=colors)
         val innerRightTriangle1 = Face(top, rightFront, middleBottomRightPart, colors=colors)
         val leftBack = Vertex(leftFront)
         val innerLeftTriangle2 = Face(top, leftBack, middleBottomLeftPart, colors=colors)
         val rightBack = Vertex(rightFront)
         val innerRightTriangle2 = Face(top, middleBottomRightPart, rightBack, colors=colors)
         val backTriangle = Face(top, rightBack, leftBack, colors=colors)
         val faces = setOf(front,
                 backTriangle, innerLeftTriangle1, innerRightTriangle1,
                 innerLeftTriangle2, innerRightTriangle2)
         val plane = planeParallelToXYAndZNegDirection(center.z())
         val facesToFacesAbove = mapOf(
                 front to setOf(backTriangle, innerLeftTriangle1, innerRightTriangle1, innerLeftTriangle2, innerRightTriangle2),
                 innerLeftTriangle1 to setOf(innerLeftTriangle2, backTriangle),
                 innerRightTriangle1 to setOf(innerRightTriangle2, backTriangle),
                 innerLeftTriangle2 to setOf(backTriangle),
                 innerRightTriangle2 to setOf(backTriangle) )
         return Figure(Bundle(plane, faces, facesToFacesAbove))
      }
   };
   private val origin = Point(0, 0, 0)
   protected val sqrt_2 = sqrt(2.0)
   @JvmOverloads
   fun make(sideLength: Int, frontColor: Color=WHITE, backColor: Color=WHITE, center: Point=origin): Figure =
           make(sideLength, ColorCombination(frontColor, backColor), center)
   @JvmOverloads
   fun make(sideLength: Int, colors: ColorCombination, center: Point = origin): Figure = make(sideLength.toDouble(), colors, center)
   //using "open" and providing a default implementation is only so I can have overloading, which is not allowed for abstract methods
   @JvmOverloads
   open fun make(sideLength: Double, frontColor: Color=WHITE, backColor: Color=WHITE, center: Point=origin): Figure =
           make(sideLength, ColorCombination(frontColor, backColor), center)
   @JvmOverloads
   open fun make(sideLength: Double, colors: ColorCombination, center: Point=origin): Figure = SQUARE.make(sideLength, colors, center)
   protected fun planeParallelToXYAndZNegDirection(z: Double): Plane =
           planeFromOrderedPoints(Point(0, 0, z), Point(1, 1, z), Point(1, 0, z))
}
