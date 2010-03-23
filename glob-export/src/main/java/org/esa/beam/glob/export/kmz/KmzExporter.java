package org.esa.beam.glob.export.kmz;

import com.bc.ceres.core.ProgressMonitor;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class KmzExporter {

    private static final String OVERLAY_KML = "overlay.kml";
    private static final String IMAGE_TYPE = "PNG";
    private RenderedImage legend;

    private final List<KmlLayer> kmlLayers;
    private List<KmlPlacemark> placemarks;
    private String legendName;
    private boolean isTimeSeries;
    private final String description;
    private final String name;


    public KmzExporter(final String description, final String name) {
        kmlLayers = new ArrayList<KmlLayer>();
        placemarks = new ArrayList<KmlPlacemark>();
        this.description = description;
        this.name = name;
    }

    public void export(final OutputStream outStream, final ProgressMonitor pm) throws IOException {

        int workload = kmlLayers.size() + 1 + (legend != null ? 1 : 0);
        pm.beginTask("Exporting KMZ", workload);

        ZipOutputStream zipStream = new ZipOutputStream(outStream);
        try {
            for (KmlLayer kmlLayer : kmlLayers) {
                zipStream.putNextEntry(new ZipEntry(kmlLayer.getName()));
                ImageEncoder encoder = ImageCodec.createImageEncoder(IMAGE_TYPE, outStream, null);
                encoder.encode(kmlLayer.getOverlay());
                pm.worked(1);
            }

            if (legend != null) {
                zipStream.putNextEntry(new ZipEntry(legendName + ".png"));
                ImageEncoder encoder = ImageCodec.createImageEncoder(IMAGE_TYPE, outStream, null);
                encoder.encode(legend);
            }


            zipStream.putNextEntry(new ZipEntry(OVERLAY_KML));

            final StringBuilder kmlContent = new StringBuilder();
            kmlContent.append(KmlFormatter.createHeader());
            if (placemarks != null) {
                kmlContent.append(KmlFormatter.createPlacemarks(placemarks));
            }
            kmlContent.append(KmlFormatter.createOverlays(kmlLayers, isTimeSeries));
            if (legend != null) {
                kmlContent.append(KmlFormatter.createLegend(legendName));
            }
            kmlContent.append(KmlFormatter.createFooter());

            outStream.write(kmlContent.toString().getBytes());
            pm.worked(1);

        } finally {
            zipStream.close();
        }

    }

    public void addLayer(KmlLayer layer) {
        kmlLayers.add(layer);
    }

    public void setLegend(String name, RenderedImage imageLegend) {
        legendName = name;
        legend = imageLegend;
    }

    public int getLayerCount() {
        return kmlLayers.size();
    }

    public void setTimeSeries(boolean timeSeries) {
        isTimeSeries = timeSeries;
    }
}
