/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.arcbin;

import org.esa.beam.util.math.MathUtils;

import java.io.File;
import java.io.IOException;

/**
 * todo - add API doc
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class Main {

    public static void main(String[] args) throws IOException {
        File dir = new File(args[0]);
        if (!dir.exists()) {
            System.out.println("dir does not exist!!");
        }
        DblbndAdf dblbndAdf = DblbndAdf.create(new File(dir, DblbndAdf.FILE_NAME));
        HdrAdf hdrAdf = HdrAdf.create(new File(dir, HdrAdf.FILE_NAME));
        
        int pixels = MathUtils.floorInt((dblbndAdf.urx - dblbndAdf.llx) / hdrAdf.pixelSizeX);
        int lines = MathUtils.floorInt((dblbndAdf.ury - dblbndAdf.lly) / hdrAdf.pixelSizeY);
        System.out.println("pixels width " + pixels);
        System.out.println("rows height  " + lines);
        
        int numTiles = hdrAdf.tilesPerColumn * hdrAdf.tilesPerRow;
        System.out.println("numTiles  " + numTiles);
        TileIndex tileIndex = TileIndex.create(new File(dir, TileIndex.FILE_NAME), numTiles);
        
        System.out.println("sucess !!!!");
    }
}