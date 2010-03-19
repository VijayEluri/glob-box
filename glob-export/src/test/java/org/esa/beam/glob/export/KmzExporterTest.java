package org.esa.beam.glob.export;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ImageUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.geometry.BoundingBox;

import javax.media.jai.PlanarImage;
import javax.media.jai.SourcelessOpImage;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static junit.framework.Assert.*;

public class KmzExporterTest {

    @Test
    public void testExporter() throws IOException {
        final KmzExporter kmzExporter = new KmzExporter("description", "name");
//        RenderedImage layer = new DummyTestOpImage(10, 10);
        final BoundingBox boundBox = new ReferencedEnvelope(0, 20, 70, 30, DefaultGeographicCRS.WGS84);
        KmlLayer unTimedLayer = new KmlLayer("layerName", null, boundBox);
        kmzExporter.addLayer(unTimedLayer);

        assertEquals(kmzExporter.getLayerCount(), 1);

        KmlLayer timedLayer = new TimedKmlLayer("timedLayerName", null, boundBox, new ProductData.UTC(),
                                                new ProductData.UTC());
        kmzExporter.addLayer(timedLayer);

        assertEquals(kmzExporter.getLayerCount(), 2);

        final OutputStream outStream = createOutputStream();
//        kmzExporter.export(outStream, ProgressMonitor.NULL);
    }

    private OutputStream createOutputStream() {
        return new BufferedOutputStream(new ByteArrayOutputStream());
    }

    class DummyTestOpImage extends SourcelessOpImage {

        DummyTestOpImage(int width, int height) {
            super(ImageManager.createSingleBandedImageLayout(DataBuffer.TYPE_BYTE, width, height, width, height),
                  null,
                  ImageUtils.createSingleBandedSampleModel(DataBuffer.TYPE_BYTE, width, height),
                  0, 0, width, height);
        }

        @Override
        public void computeRect(PlanarImage[] sources, WritableRaster dest, Rectangle destRect) {
            super.computeRect(sources, dest, destRect);
        }

    }


}
