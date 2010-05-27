package org.esa.beam.glob.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 29.03.2010
 * Time: 15:57:55
 */
public class TimeSeries {

    private List<TimedRaster> rasterList;

    private ProductData.UTC startTime;

    private ProductData.UTC endTime;

    private TimedRaster refRaster;

    private GeoCoding geoCoding;

    public TimeSeries(final List<TimedRaster> rasterList, final TimedRaster refRaster, final ProductData.UTC startTime,
                      final ProductData.UTC endTime) {
        this.endTime = endTime;
        this.rasterList = rasterList;
        this.refRaster = refRaster;
        this.startTime = startTime;
        validateRasterList();
        applyGeoCoding(refRaster.getGeoCoding());
    }

    public void applyGeoCoding(final GeoCoding gc) {
        // TODO ts dummy
        setGeoCoding(gc);
        for (TimedRaster tr : rasterList) {
            tr.getRaster().setGeoCoding(gc);
        }
    }

    public void setGeoCoding(final GeoCoding geoCoding) {
        this.geoCoding = geoCoding;
        applyGeoCoding(geoCoding);
    }

    public List<TimedRaster> getRasterList() {
        return rasterList;
    }

    private void validateRasterList() {
        timeValidateRasterList();
        if (geoCoding == null) {
            geoCoding = refRaster.getGeoCoding();
        }
        applyGeoCoding(geoCoding);
    }

    private void timeValidateRasterList() {
        List<TimedRaster> result = new ArrayList<TimedRaster>();
        for (TimedRaster tr : rasterList) {
            final Date rasterStartTime = tr.getTimeCoding().getStartTime().getAsDate();
            final Date rasterEndTime = tr.getTimeCoding().getEndTime().getAsDate();
            final boolean isValidStartTime = rasterStartTime.equals(startTime.getAsDate())
                                             || rasterStartTime.after(startTime.getAsDate());
            final boolean isValidEndTime = rasterEndTime.equals(endTime.getAsDate())
                                           || rasterEndTime.before(endTime.getAsDate());
            if (isValidStartTime && isValidEndTime) {
                result.add(tr);
            }
        }
        this.rasterList = result;
    }

    public ProductData.UTC getEndTime() {
        return endTime;
    }

    public ProductData.UTC getStartTime() {
        return startTime;
    }

    public void setEndTime(final ProductData.UTC endTime) {
        this.endTime = endTime;
        validateRasterList();
    }

    public void setStartTime(final ProductData.UTC startTime) {
        this.startTime = startTime;
        validateRasterList();
    }

    public TimedRaster getRefRaster() {
        return refRaster;
    }

    public void remove(final RasterDataNode raster) {
        TimedRaster markedForDeletion = null;
        for (TimedRaster tr : rasterList) {
            if (tr.getRaster().equals(raster)) {
                markedForDeletion = tr;
            }
        }
        if (markedForDeletion != null) {
            rasterList.remove(markedForDeletion);
        }
        validateRasterList();
    }

    public void add(final RasterDataNode raster, final TimeCoding timeCoding) {
        final TimedRaster timedRaster = new TimedRaster(raster, timeCoding);
        if (!rasterList.contains(timedRaster)) {
            rasterList.add(timedRaster);
        }
        validateRasterList();
    }
}