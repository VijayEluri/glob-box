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
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.dataio.netcdf.util.MetadataUtils;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Guardian;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import ucar.ma2.Array;
import ucar.ma2.ArrayShort;
import ucar.ma2.Index1D;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;

/**
 * The class <code>BinnedProductReader</code> provides a dataio for the Binned products
 * generated by the GlobColour processor.
 * <p/>
 * Instances of this class are created only by the <code>createReaderInstance()</code>
 * method of a {@link BinnedProductReaderPlugIn} object.
 * <p/>
 * This implementation overrides the <code>readProductNodes()</code> and <code>readBandData()</code>
 * methods of the {@link AbstractProductReader}.
 *
 * @author Ralf Quast
 * @version $Revision: 3721 $ $Date: 2008-11-19 11:43:49 +0100 (Mi, 19. Nov 2008) $
 * @see IsinGrid
 * @see BinnedProductReaderPlugIn
 */
public class BinnedProductReader extends AbstractProductReader {

    private volatile NetcdfFile ncFile;
    private volatile EquirectGrid equirectGrid;
    private volatile IsinGridStorageInfo storageInfo;

    /**
     * Constructs an instance of this class.
     *
     * @param productReaderPlugIn the plug-in which creates this dataio instance.
     */
    public BinnedProductReader(BinnedProductReaderPlugIn productReaderPlugIn) {
        super(productReaderPlugIn);
    }

    /**
     * Factory method for creating a {@link Product} by reading the metadata from
     * the underlying netCDF file.
     * <p/>
     * This method is called only once during the lifetime of this dataio instance.
     * It is invoked as the last step in the <code>readProductNodes()</code> method
     * of the {@link AbstractProductReader} base class.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File file = new File(getInput().toString());
        ncFile = NetcdfFile.open(file.getPath());

        final Group ncroot = ncFile.getRootGroup();

        final double minLat = getDoubleValue(ncroot.findAttributeIgnoreCase(ProductAttributes.MIN_LAT), -90.0);
        final double maxLat = getDoubleValue(ncroot.findAttributeIgnoreCase(ProductAttributes.MAX_LAT), 90.0);
        final double minLon = getDoubleValue(ncroot.findAttributeIgnoreCase(ProductAttributes.MIN_LON), -180.0);
        final double maxLon = getDoubleValue(ncroot.findAttributeIgnoreCase(ProductAttributes.MAX_LON), 180.0);
        final double latitudeOfTrueScale = getDoubleValue(ncroot.findAttributeIgnoreCase(ProductAttributes.SITE_LAT),
                                                          0.0);

        equirectGrid = createEquirectGrid(minLat, maxLat, minLon, maxLon, latitudeOfTrueScale);

        final Product product = new Product(file.getName(),
                                            ReaderConstants.BINNED_GLOBAL,
                                            equirectGrid.getColCount(),
                                            equirectGrid.getRowCount(),
                                            this);

        final Attribute title = ncroot.findAttributeIgnoreCase(ProductAttributes.TITLE);
        if (title != null && title.isString()) {
            product.setDescription(title.getStringValue());
        }

        product.setFileLocation(file);
        MetadataUtils.readNetcdfMetadata(ncFile, product.getMetadataRoot());

        final Dimension bin = ncroot.findDimension(ReaderConstants.BIN);
        final List<Variable> variableList = findVariables(ncroot, bin);
        addBands(product, variableList);

        setGeoCoding(product, equirectGrid);
        ProductUtilities.extend(product);

        if (ProductUtilities.isDiagnosticDataSet(product)) {
            product.setProductType(ReaderConstants.BINNED_DDS);
        }

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
     *
     * @throws IOException if an I/O error occurs
     * @see #readBandRasterData
     * @see #getSubsetDef
     */
    @Override
    protected synchronized void readBandRasterDataImpl(int sourceOffsetX,
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
        Guardian.assertTrue("sourceStepX != 1", sourceStepX == 1);
        Guardian.assertTrue("sourceStepY != 1", sourceStepY == 1);
        Guardian.assertTrue("sourceWidth != targetWidth", sourceWidth == targetWidth);
        Guardian.assertTrue("sourceHeight != targetHeight", sourceHeight == targetHeight);

        final Variable col = ncFile.findVariable(ReaderConstants.COL);
        final Variable var = ncFile.findVariable(targetBand.getName());
        final double noDataValue = targetBand.getNoDataValue();

        pm.beginTask(MessageFormat.format("Resampling data from band ''{0}''", targetBand.getName()), targetHeight);
        try {
            if (storageInfo == null) {
                storageInfo = createStorageInfo(ReaderConstants.IG.getRow(equirectGrid.getMinLat()),
                                                equirectGrid.getRowCount());
            }

            final int[] start = new int[1];
            final int[] shape = new int[1];

            for (int i = 0; i < targetHeight; ++i) {
                final int y = sourceOffsetY + i;

                start[0] = storageInfo.getOffset(y);
                shape[0] = storageInfo.getBinCount(y);
                final int row = storageInfo.getRow(y);

                Array cols = null;
                Array data = null;
                Index1D index = null;

                if (shape[0] > 0) {
                    cols = col.read(start, shape);
                    data = var.read(start, shape);
                    index = new Index1D(shape);
                }

                row:
                for (int j = 0, k = 0; j < targetWidth; ++j) {
                    final int x = sourceOffsetX + j;
                    // calculate the ISIN grid column corresponding to (x, y)
                    final int z = ReaderConstants.IG.getCol(row, equirectGrid.getLon(x));

                    for (; k < shape[0]; ++k) {
                        index.set(k);
                        final short c = cols.getShort(index);

                        if (c == z) {
                            targetBuffer.setElemDoubleAt(i * targetWidth + j, data.getDouble(index));
                            continue row;
                        }
                        if (c > z) {
                            break;
                        }
                    }
                    targetBuffer.setElemDoubleAt(i * targetWidth + j, noDataValue);
                }

                pm.worked(1);

                if (pm.isCanceled()) {
                    throw new IOException("Process terminated by user.");
                }
            }
        } catch (InvalidRangeException e) {
            throw new IOException(e.getMessage());
        } finally {
            pm.done();
        }
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
        if (ncFile != null) {
            storageInfo = null;
            equirectGrid = null;
            ncFile.close();
            ncFile = null;
        }
        super.close();
    }

    private IsinGridStorageInfo createStorageInfo(final int minRow, int rowCount)
            throws IOException, InvalidRangeException {
        final Variable row = ncFile.findVariable(ReaderConstants.ROW);

        final int[] offsets = new int[rowCount];
        int binCount = 0;

        indexing:
        for (int rowIndex = minRow; binCount < row.getShape()[0];) {
            final ArrayShort.D1 chunk = (ArrayShort.D1) readChunk(row, binCount);

            for (final int chunkFront = binCount; binCount - chunkFront < chunk.getShape()[0];) {
                if (rowIndex > chunk.get(binCount - chunkFront)) {
                    ++binCount;
                    continue;
                }
                if (rowCount == 0) {
                    break indexing;
                }

                offsets[--rowCount] = binCount;
                if (rowIndex == chunk.get(binCount - chunkFront)) {
                    ++binCount;
                }
                ++rowIndex;
            }
        }
        while (rowCount > 0) {
            offsets[--rowCount] = binCount;
        }

        return new IsinGridStorageInfo(minRow, binCount, offsets);
    }

    private static Array readChunk(final Variable var, final int offset)
            throws IOException, InvalidRangeException {
        final int[] start = new int[1];
        final int[] shape = new int[1];

        start[0] = offset;
        shape[0] = Math.min(50000, var.getShape()[0] - start[0]);

        return var.read(start, shape);
    }

    private static EquirectGrid createEquirectGrid(final double minLat,
                                                   final double maxLat,
                                                   final double minLon,
                                                   final double maxLon,
                                                   final double latitudeOfTrueScale) {
        final int maxRowCount = ReaderConstants.IG.getRowCount() - 1;

        final int minRow = Math.min(ReaderConstants.IG.getRow(minLat), maxRowCount);
        final int maxRow = Math.min(ReaderConstants.IG.getRow(maxLat), maxRowCount);
        final int rowOfTrueScale = Math.min(ReaderConstants.IG.getRow(latitudeOfTrueScale), maxRowCount);

        final int maxColCount = ReaderConstants.IG.getColCount(rowOfTrueScale) - 1;

        final int minCol = Math.max(0, Math.min(ReaderConstants.IG.getCol(rowOfTrueScale, minLon), maxColCount));
        final int maxCol = Math.max(0, Math.min(ReaderConstants.IG.getCol(rowOfTrueScale, maxLon), maxColCount));

        final int rowCount = maxRow - minRow + 1;
        final int colCount = maxCol - minCol + 1;

        return new EquirectGrid(rowCount, colCount,
                                ReaderConstants.IG.getLatSouth(minRow),
                                ReaderConstants.IG.getLonWest(rowOfTrueScale, minCol),
                                ReaderConstants.IG.getLatStep(),
                                ReaderConstants.IG.getLonStep(rowOfTrueScale));
    }

    @SuppressWarnings("unchecked")
    private static List<Variable> findVariables(final Group ncgroup, final Dimension ncdim) {
        final List<Variable> variableList = ncgroup.getVariables();

        final Iterator<Variable> iter = variableList.iterator();
        while (iter.hasNext()) {
            final Variable variable = iter.next();
            if (variable.getRank() != 1 || variable.getDimension(0) != ncdim) {
                iter.remove();
            }
        }

        return variableList;
    }

    private static void addBands(final Product product, final List<Variable> variableList) {
        final int rasterWidth = product.getSceneRasterWidth();
        final int rasterHeight = product.getSceneRasterHeight();

        for (final Variable variable : variableList) {
            final String variableName = variable.getShortName();
            final int rasterDataType = DataTypeUtils.getRasterDataType(variable.getDataType(),
                                                                           variable.isUnsigned());
            final Band band = new Band(variableName, rasterDataType, rasterWidth, rasterHeight);

            double fillValue;
            if (ReaderConstants.ROW.equals(variableName) || ReaderConstants.COL.equals(variableName)) {
                fillValue = -1.0;
            } else {
                fillValue = getDoubleValue(variable.findAttribute(ProductAttributes.FILL_VALUE), 0.0);
            }

            band.setDescription(variable.getDescription());
            band.setUnit(variable.getUnitsString());
            band.setNoDataValue(fillValue);
            band.setNoDataValueUsed(true);

            product.addBand(band);
        }
    }

    private static void setGeoCoding(final Product product, final EquirectGrid grid) {
        AffineTransform i2m = new AffineTransform();
        i2m.translate(grid.getMinLon(), grid.getMinLat());
        i2m.scale(grid.getLonStep(), -grid.getLatStep());
        i2m.translate(0, -grid.getRowCount());

        Rectangle imageBounds = new Rectangle(grid.getColCount(), grid.getRowCount());
        try {
            final DefaultGeographicCRS crs = DefaultGeographicCRS.WGS84;
            CrsGeoCoding coding = new CrsGeoCoding(crs, imageBounds, i2m);
            product.setGeoCoding(coding);
        } catch (FactoryException ignored) {
        } catch (TransformException ignored) {
        }
    }

    private static double getDoubleValue(final Attribute attr, final double defaultValue) {
        Number value = defaultValue;

        if (attr != null) {
            value = attr.getNumericValue();
            if (value == null) {
                value = defaultValue;
            }
        }

        return value.doubleValue();
    }
}
