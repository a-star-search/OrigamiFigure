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
import com.whitebeluga.origami.figure.component.Vertex
import com.whitebeluga.origami.figure.folding.SideOfFold

/**
 * A bisected bundle can be separated in the following way:
 *
 * Faces that are connected to the vertices of the bisected faces on one side.
 *
 * Vertices of the bisected faces separated by side.
 *
 * This class is useful for the bundle divider.
 */
internal data class SeparatedBisectedBundle(
        val originalBundle: Bundle,
        val nonBisectedFacesOfBisectedBundle: Map<SideOfFold, Set<Face>>,
        val sideToPointsOfBisectedFaces: Map<SideOfFold, Set<Vertex>>)