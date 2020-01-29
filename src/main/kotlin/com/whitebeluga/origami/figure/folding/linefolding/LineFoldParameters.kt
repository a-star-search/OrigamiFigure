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

import com.moduleforge.libraries.geometry._3d.LineSegment
import com.moduleforge.libraries.geometry._3d.Point
import com.moduleforge.libraries.geometry._3d.Vector
import java.lang.Math.PI

data class LineFoldParameters
 @JvmOverloads constructor(val foldSegment: LineSegment,
                           val normalOfValleySide: Vector,
                           val guiUserLookingDirection: Vector,
                           val segmentEndsDockedToVertices: List<Point> = listOf(),
                           val angle: Double = FLAT_FOLD_ANGLE,
                           /**
                            *
                            * The side to rotate is a more complex case that may seem at first.
                            *
                            * - The user may choose a side to rotate, even when the end result is the same, just
                            * because it makes more sense for them to tell the application which side should
                            * rotate, for example to see the animation of the fold or any similar reason.
                            *
                            * This capability is of secondary importance and if there no chosen side, a reasonable
                            * heuristic is used to choose one (for example the smallest area side)
                            *
                            * - There is another reason to choose a side and it's that sometimes there is ambiguity.
                            * This can only happen with the kind of fold that folds a single flap, though.
                            *
                            * An example of this would be trying to fold a flap of the waterbomb base.
                            * While a complete fold will result in four flaps folded together, a flap fold with a side
                            * chosen will result in the same or in three flaps on one side and one flap on the other
                            * side, depending on the chosen side.
                            *
                            * In cases like this it is mandatory for the user to choose a side.
                            *
                            * Now the question is, can the user choose a flap to fold when there is ambiguity
                            * but want to see how the other side is rotated? Maybe, but it is unlikely,
                            * as in this case we want to fold a single flap, the rest of the figure is probably
                            * bigger by any chosen metric, which means it probably make more sense to see the flap
                            * rotated. In any case, as I mentioned early, it doesn't make any difference as far as
                            * the final figure is concerned.
                            *
                            * In short, in this case choosing a side to avoid ambiguity also means choosing a side for
                            * the interface to rotate.
                            *
                            */
                           val pickedPointAsSideToRotate: Point? = null){
   companion object {
      const val FLAT_FOLD_ANGLE: Double = PI
   }
}