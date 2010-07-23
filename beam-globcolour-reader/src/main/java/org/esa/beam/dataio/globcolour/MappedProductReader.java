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
package org.esa.beam.dataio.globcolour;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.netcdf.NetCdfReader;
import org.esa.beam.dataio.netcdf.NetCdfReaderPlugIn;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.IOException;

/**
 * The class <code>MappedProductReader</code> provides a dataio for the Mapped products
 * generated by the GlobColour processor.
 * <p/>
 * Instances of this class are created only by the <code>createReaderInstance()</code>
 * method of a {@link MappedProductReaderPlugIn} object.
 * <p/>
 * This implementation overrides the <code>readProductNodes()</code> and <code>readBandData()</code>
 * methods of the {@link AbstractProductReader}.
 *
 * @author Ralf Quast
 * @version $Revision: 3720 $ $Date: 2008-11-19 10:44:16 +0100 (Mi, 19. Nov 2008) $
 */
public class MappedProductReader extends AbstractProductReader {
    
    private final ProductReader delegateReader;

    /**
     * Constructs an instance of this class.
     *
     * @param productReaderPlugIn the plug-in which creates this dataio instance.
     */
    public MappedProductReader(MappedProductReaderPlugIn productReaderPlugIn) {
        super(productReaderPlugIn);
        delegateReader = new NetCdfReader(new NetCdfReaderPlugIn(), MappedProductReaderPlugIn.CF_PROFILE);
    }

    /**
     * Factory method for creating a {@link Product} by reading the metadata from
     * the underlying netCDF file.
     * <p/>
     * This method is called only once during the lifetime of this dataio instance.
     * It is invoked as the last step in the <code>readProductNodes()</code> method
     * of the {@link AbstractProductReader} base class.
     *
     * @throws java.io.IOException if an I/O error occurs.
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        final Product product = delegateReader.readProductNodes(getInput(), getSubsetDef());

        if (ProductUtilities.isDiagnosticDataSet(product)) {
            product.setProductType(ReaderConstants.MAPPED_DDS);
        } else {
            product.setProductType(ReaderConstants.MAPPED_GLOBAL);
        }

        product.setProductReader(this);
        ProductUtilities.extend(product);
        product.setModified(false);

        return product;
    }

    /**
     * The template method which is called by the method after an optional spatial subset has been applied to the input
     * parameters.
     * <p/>
     * <p>The destination band, buffer and region parameters are exactly the ones passed to the original  call. Since
     * the <code>destOffsetX</code> and <code>destOffsetY</code> parameters are already taken into acount in the
     * <code>sourceOffsetX</code> and <code>sourceOffsetY</code> parameters, an implementor of this method is free to
     * ignore them.
     *
     * @param sourceOffsetX the absolute X-offset in source raster co-ordinates
     * @param sourceOffsetY the absolute Y-offset in source raster co-ordinates
     * @param sourceWidth   the width of region providing samples to be read given in source raster co-ordinates
     * @param sourceHeight  the height of region providing samples to be read given in source raster co-ordinates
     * @param sourceStepX   the sub-sampling in X direction within the region providing samples to be read
     * @param sourceStepY   the sub-sampling in Y direction within the region providing samples to be read
     * @param targetBand    the target band which identifies the data source from which to read the sample values
     * @param targetOffsetX the X-offset in the band's raster co-ordinates
     * @param targetOffsetY the Y-offset in the band's raster co-ordinates
     * @param targetWidth   the width of region to be read given in the band's raster co-ordinates
     * @param targetHeight  the height of region to be read given in the band's raster co-ordinates
     * @param targetBuffer  the destination buffer which receives the sample values to be read
     * @param pm            a monitor to inform the user about progress
     * @throws IOException if an I/O error occurs
     * @see #readBandRasterData
     * @see #getSubsetDef
     */
    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX,
                                          int sourceOffsetY,
                                          int sourceWidth,
                                          int sourceHeight,
                                          int sourceStepX,
                                          int sourceStepY,
                                          Band targetBand,
                                          int targetOffsetX,
                                          int targetOffsetY,
                                          int targetWidth,
                                          int targetHeight,
                                          ProductData targetBuffer,
                                          ProgressMonitor pm)
            throws IOException {
        delegateReader.readBandRasterData(targetBand, targetOffsetX, targetOffsetY, targetWidth, targetHeight,
                                         targetBuffer, pm);
    }

    /**
     * Closes the access to all currently opened resources such as file input streams and all resources of this children
     * directly owned by this dataio. Its primary use is to allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>close()</code> are undefined.
     * <p/>
     * <p>Overrides of this method should always call <code>super.close();</code> after disposing this instance.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (delegateReader != null) {
            delegateReader.close();
        }
        super.close();
    }
}
