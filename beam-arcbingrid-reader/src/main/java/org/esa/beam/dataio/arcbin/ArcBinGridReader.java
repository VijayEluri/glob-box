package org.esa.beam.dataio.arcbin;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.util.math.MathUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;


public class ArcBinGridReader extends AbstractProductReader {

    private RasterDataFile rasterDataFile;
    private static final String BAND_NAME = "classes";
    private static final String PRODUCT_TYPE = "ARC_INFO_BIN_GRID";

    protected ArcBinGridReader(ArcBinGridReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        File file = new File(String.valueOf(getInput()));
        File gridDir = file.getParentFile();

        GeorefBounds georefBounds = GeorefBounds.create(getCaseInsensitiveFile(gridDir, GeorefBounds.FILE_NAME));
        RasterStatistics rasterStatistics = RasterStatistics.create(
                getCaseInsensitiveFile(gridDir, RasterStatistics.FILE_NAME));
        File headerFile = getCaseInsensitiveFile(gridDir, Header.FILE_NAME);
        final Header header = Header.create(headerFile);
        final int width = MathUtils.floorInt((georefBounds.upperRightX - georefBounds.lowerLeftX) / header.pixelSizeX);
        final int height = MathUtils.floorInt((georefBounds.upperRightY - georefBounds.lowerLeftY) / header.pixelSizeY);
        int numTiles = header.tilesPerColumn * header.tilesPerRow;

        TileIndex tileIndex = TileIndex.create(getCaseInsensitiveFile(gridDir, TileIndex.FILE_NAME), numTiles);
        rasterDataFile = RasterDataFile.create(getCaseInsensitiveFile(gridDir, RasterDataFile.FILE_NAME));

        Product product = new Product(gridDir.getName(), PRODUCT_TYPE, width, height);
        product.setFileLocation(headerFile);
        final Dimension gridTileSize = new Dimension(header.tileXSize, header.tileYSize);
        int tileExtend = Math.max(header.tileXSize, header.tileYSize);
        final Dimension imageTileSize = new Dimension(tileExtend, tileExtend);
        product.setPreferredTileSize(imageTileSize);

        final AffineTransform i2m = createAffineTransform(georefBounds, header, height);
        product.setGeoCoding(createGeoCoding(width, height, product, i2m));

        int productDataType = getDataType(header, rasterStatistics);
        final Band band = product.addBand(BAND_NAME, productDataType);
        double nodataValue = getNodataValue(productDataType);
        band.setNoDataValue(nodataValue);
        band.setNoDataValueUsed(true);
        final int databufferType = ImageManager.getDataBufferType(productDataType);
        final GridTileProvider gridTileProvider;
        if (ProductData.isIntType(productDataType)) {
            gridTileProvider = new IntegerGridTileProvider(rasterDataFile, tileIndex, (int) nodataValue, gridTileSize,
                                                           productDataType);
        } else {
            final int tileLength = gridTileSize.width * gridTileSize.height;
            gridTileProvider = new FloatGridTileProvider(rasterDataFile, tileIndex, (float) nodataValue, tileLength,
                                                         productDataType);
        }
        final MultiLevelModel model = new DefaultMultiLevelModel(i2m, width, height);
        AbstractMultiLevelSource multiLevelSource = new AbstractMultiLevelSource(model) {
            @Override
            protected RenderedImage createImage(int level) {
                if (rasterDataFile != null) {
                    ResolutionLevel resolutionLevel = ResolutionLevel.create(model, level);
                    return new GridTileOpImage(width, height, imageTileSize, databufferType, resolutionLevel, header,
                                               gridTileSize, gridTileProvider);
                } else {
                    throw new IllegalStateException("rasterDataFile is closed");
                }
            }
        };
        MultiLevelImage image = new DefaultMultiLevelImage(multiLevelSource);
        band.setSourceImage(image);

        File colorPaletteFile = ColorPalette.findColorPaletteFile(gridDir);
        if (colorPaletteFile != null) {
            ColorPaletteDef colorPaletteDef = ColorPalette.createColorPalette(colorPaletteFile, rasterStatistics);
            if (colorPaletteDef != null) {
                band.setImageInfo(new ImageInfo(colorPaletteDef));
                final Map<Integer, String> descriptionMap = LegendFile.createDescriptionMap(gridDir);
                IndexCoding indexCoding = ColorPalette.createIndexCoding(colorPaletteDef, descriptionMap);
                product.getIndexCodingGroup().add(indexCoding);
                band.setSampleCoding(indexCoding);
            }
        }

        MetadataElement metadataRoot = product.getMetadataRoot();
        metadataRoot.addElement(MetaDataHandler.createHeaderElement(header));
        metadataRoot.addElement(MetaDataHandler.createGeorefBoundsElement(georefBounds));
        if (rasterStatistics != null) {
            metadataRoot.addElement(MetaDataHandler.createRasterStatisticsElement(rasterStatistics));
        }

        return product;
    }

    private GeoCoding createGeoCoding(int width, int height, Product product, AffineTransform i2m) {
        // TODO parse projection from prj.adf file. For now we assume WGS84 (applicable for GlobToolBox products) (mz, 2010-02-24)
        //CoordinateReferenceSystem crs = ProjectionReader.parsePrjFile(getCaseInsensitiveFile(dir, "prj.adf"));
        Rectangle imageBounds = new Rectangle(width, height);
        try {
            final DefaultGeographicCRS crs = DefaultGeographicCRS.WGS84;
            return new CrsGeoCoding(crs, imageBounds, i2m);
        } catch (FactoryException ignored) {
        } catch (TransformException ignored) {
        }
        return null;
    }

    private AffineTransform createAffineTransform(GeorefBounds georefBounds, Header header, int height) {
        AffineTransform i2m = new AffineTransform();
        i2m.translate(georefBounds.lowerLeftX, georefBounds.lowerLeftY);
        i2m.scale(header.pixelSizeX, -header.pixelSizeY);
        i2m.translate(0, -height);
        return i2m;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        throw new IOException("ArcBinGridReader.readBandRasterDataImpl itentionally not implemented");
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (rasterDataFile != null) {
            rasterDataFile.close();
            rasterDataFile = null;
        }
    }

    private int getDataType(Header header, RasterStatistics rasterStatistics) throws ProductIOException {
        int cellType = header.cellType;
        if (cellType == ArcBinGridConstants.CELL_TYPE_INT) {
            if (rasterStatistics != null && rasterStatistics.min >= 0 && rasterStatistics.max <= 254) {
                return ProductData.TYPE_UINT8;
            } else if (rasterStatistics != null && rasterStatistics.min >= -32767 && rasterStatistics.max <= 32767) {
                return ProductData.TYPE_INT16;
            } else {
                return ProductData.TYPE_INT32;
            }
        } else if (cellType == ArcBinGridConstants.CELL_TYPE_FLOAT) {
            return ProductData.TYPE_FLOAT32;
        } else {
            throw new ProductIOException("Unsupported data type: " + cellType);
        }
    }

    private double getNodataValue(int dataType) throws ProductIOException {
        if (dataType == ProductData.TYPE_FLOAT32) {
            return ArcBinGridConstants.NODATA_VALUE_FLOAT;
        } else if (dataType == ProductData.TYPE_UINT8) {
            return 255;
        } else if (dataType == ProductData.TYPE_INT16) {
            return Short.MIN_VALUE;
        } else if (dataType == ProductData.TYPE_INT32) {
            return -2147483647; // taken from gdal
        } else {
            throw new ProductIOException("Unsupported data type: " + dataType);
        }
    }

    static File getCaseInsensitiveFile(File dir, String lowerCaseName) {
        File lowerCaseFile = new File(dir, lowerCaseName);
        if (lowerCaseFile.exists()) {
            return lowerCaseFile;
        }
        File upperCaseFile = new File(dir, lowerCaseName.toUpperCase());
        if (upperCaseFile.exists()) {
            return upperCaseFile;
        }
        return null;
    }
}
