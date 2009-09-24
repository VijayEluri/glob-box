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

import static com.bc.ceres.binio.TypeBuilder.*;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.SequenceData;

import org.esa.beam.framework.dataio.ProductIOException;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 * Contains the Header
 */
class HdrAdf {
    
    public static final String FILE_NAME = "hdr.adf";
    
    private static final String MAGIC = "GRID1.2\0";
    
    private static final CompoundType TYPE = 
        COMPOUND("Header", 
                 MEMBER("HMagic", SEQUENCE(BYTE, 8)), 
                 MEMBER("unknown1", SEQUENCE(BYTE, 8)), 
                 MEMBER("HCellType", INT), 
                 MEMBER("unknown2", SEQUENCE(BYTE, 236)), 
                 MEMBER("HPixelSizeX", DOUBLE), 
                 MEMBER("HPixelSizeY", DOUBLE), 
                 MEMBER("XRef", DOUBLE), 
                 MEMBER("YRef", DOUBLE), 
                 MEMBER("HTilesPerRow", INT), 
                 MEMBER("HTilesPerColumn", INT),
                 MEMBER("HTileXSize", INT),
                 MEMBER("unknown3", INT),
                 MEMBER("HTileYSize", INT)
        );

    final int cellType;
    final double pixelSizeX;
    final double pixelSizeY;
    final double xRef;
    final double yRef;
    final int tilesPerRow;
    final int tilesPerColumn;
    final int tileXSize;
    final int tileYSize;
    
    private HdrAdf(int cellType, 
                   double pixelSizeX, double pixelSizeY, 
                   double xRef, double yRef, 
                   int tilesPerRow, int tilesPerColumn,
                   int tileXSize, int tileYSize) {
        this.cellType = cellType;
        this.pixelSizeX = pixelSizeX;
        this.pixelSizeY = pixelSizeY;
        this.xRef = xRef;
        this.yRef = yRef;
        this.tilesPerRow = tilesPerRow;
        this.tilesPerColumn = tilesPerColumn;
        this.tileXSize = tileXSize;
        this.tileYSize = tileYSize;
    }

    public static HdrAdf create(File file) throws IOException {
        DataFormat dataFormat = new DataFormat(TYPE, ByteOrder.BIG_ENDIAN);
        DataContext context = dataFormat.createContext(file, "r");
        CompoundData data = context.createData();
        
        SequenceData magic = data.getSequence("HMagic");
        if (checkMagicString(magic)) {
            HdrAdf hdrAdf = new HdrAdf(data.getInt("HCellType"), data.getDouble("HPixelSizeX"), data.getDouble("HPixelSizeX"),
                              data.getDouble("XRef"), data.getDouble("YRef"), data.getInt("HTilesPerRow"),
                              data.getInt("HTilesPerColumn"), data.getInt("HTileXSize"), data.getInt("HTileYSize"));
            context.dispose();
            return hdrAdf;
        } else {
            context.dispose();
            throw new ProductIOException("Wrong magic string in 'hdr.adf' file.");
        }
    }

    private static boolean checkMagicString(SequenceData magic) throws IOException {
        byte[] magicBytes = MAGIC.getBytes();
        for (int i = 0; i < MAGIC.length(); i++) {
            if (magicBytes[i] != magic.getByte(i)) {
                return false;
            }
        }
        return true;
    }
}
