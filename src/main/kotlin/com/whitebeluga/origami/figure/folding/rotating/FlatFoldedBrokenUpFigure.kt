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

import com.moduleforge.libraries.geometry._3d.Vector
import com.whitebeluga.origami.figure.Bundle

/**
 * This class represents the broken up figure, after the rotation of the side that needs be rotated.
 *
 * Such a figure is not actually the finished figure for flat folds (that is, 180 deg folds).
 * It is, in general, the finished figure for non-flat folds. But for "flat" folding,
 * sometimes there needs to be face merging.
 *
 * Face merging are "unfolds" which only happen under certain circumstances.
 *
 * Check the documentation to understand what is an "unfold" and how can it happen.
 *
 */
internal data class FlatFoldedBrokenUpFigure(
        val stationaryPartOfBisectedBundle: Bundle,
        val rotatedPartOfBisectedBundle: Bundle,
        val restOfStationaryBundles: Set<Bundle>,
        val restOfRotatedBundles: Set<Bundle>,
        /**
         * Should be a normal vector perpendicular to the plane.
         *
         * It is necessary in order to correctly merge both parts of the folded bundle
         */
        val rotationDirection: Vector)