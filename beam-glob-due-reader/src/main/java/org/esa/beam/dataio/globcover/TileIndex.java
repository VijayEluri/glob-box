/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.dataio.globcover;

import java.awt.Rectangle;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class TileIndex implements Comparable {

    public static final int TILE_SIZE = 1800;
    public static final int MAX_HORIZ_INDEX = 71;
    public static final int MAX_VERT_INDEX = 35;

    private final int horizIndex;
    private final int vertIndex;
    private final int index;
    private final Rectangle bounds;

    TileIndex(int horizIndex, int vertIndex) {
        this.horizIndex = horizIndex;
        this.vertIndex = vertIndex;
        index = horizIndex + MAX_HORIZ_INDEX * vertIndex;
        bounds = new Rectangle(horizIndex * TILE_SIZE, vertIndex * TILE_SIZE, TILE_SIZE, TILE_SIZE);
    }

    public Rectangle getBounds() {
        return bounds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TileIndex)) {
            return false;
        }

        TileIndex tileIndex = (TileIndex) o;

        return index == tileIndex.index;

    }

    @Override
    public String toString() {
        return String.format("TileIndex{horizIndex=%d, vertIndex=%d'}'", horizIndex, vertIndex);
    }

    @Override
    public int hashCode() {
        int result = horizIndex;
        result = 31 * result + vertIndex;
        result = 31 * result + index;
        return result;
    }

    @Override
    public int compareTo(Object o) {
        final TileIndex tileIndex = (TileIndex) o;
        return Integer.valueOf(this.index).compareTo(tileIndex.index);
    }

}
