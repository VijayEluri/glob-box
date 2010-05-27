package org.esa.beam.glob.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.RasterDataNode;

/**
 * User: Thomas Storm
 * Date: 31.03.2010
 * Time: 10:08:36
 */
public class TimedRaster {

    private TimeCoding timeCoding;

    private final RasterDataNode raster;

    public TimedRaster(RasterDataNode raster, TimeCoding timeCoding) {
        this.raster = raster;
        this.timeCoding = timeCoding;
    }

    public TimeCoding getTimeCoding() {
        return timeCoding;
    }

    public void setTimeCoding(TimeCoding timeCoding) {
        this.timeCoding = timeCoding;
    }

    public RasterDataNode getRaster() {
        return raster;
    }

    public GeoCoding getGeoCoding() {
        return raster.getGeoCoding();
    }

    public String getName() {
        return raster.getName();
    }

    public int getWidth() {
        return raster.getRasterWidth();
    }

    public int getHeight() {
        return raster.getRasterHeight();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TimedRaster && ((TimedRaster) obj).getName().equals(
                this.getName()) && ((TimedRaster) obj).getRaster().equals(
                getRaster()) && ((TimedRaster) obj).getTimeCoding().getStartTime().equals(
                this.getTimeCoding().getStartTime()) && ((TimedRaster) obj).getTimeCoding().getEndTime().equals(
                this.getTimeCoding().getEndTime());

    }

    @Override
    public int hashCode() {
        return getName().hashCode() * timeCoding.getStartTime().hashCode() * timeCoding.getEndTime().hashCode();
    }
}