package org.esa.beam.glob.core;

import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.ProductData;

/**
 * Abstract class representing a time-coding. A time-coding is defined by a start and an end time and thus represents
 * a time span. It maps time information to pixel-positions.
 */
public abstract class TimeCoding {

    private ProductData.UTC startTime;

    private ProductData.UTC endTime;

    /**
     * Constructor creates a new TimeCoding-instance with a given start and end time.
     *
     * @param startTime the start time of the time span represented by the time-coding
     * @param endTime   the end time of the time span represented by the time-coding
     */
    protected TimeCoding(ProductData.UTC startTime, ProductData.UTC endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Allows to retrieve time information for a given pixel.
     *
     * @param pos the pixel position to retrieve time information for
     *
     * @return the time at the given pixel position, can be {@code null} if time can not be determined.
     */
    public abstract ProductData.UTC getTime(final PixelPos pos);

    /**
     * Getter for the start time
     *
     * @return the start time, may be {@code null}
     */
    public ProductData.UTC getStartTime() {
        return startTime;
    }

    /**
     * Getter for the end time
     *
     * @return the end time, may be {@code null}
     */
    public ProductData.UTC getEndTime() {
        return endTime;
    }

    /**
     * Setter for the start time
     *
     * @param startTime the start time to set
     */
    public void setStartTime(ProductData.UTC startTime) {
        this.startTime = startTime;
    }

    /**
     * Setter for the end time
     *
     * @param endTime the end time to set
     */
    public void setEndTime(ProductData.UTC endTime) {
        this.endTime = endTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TimeCoding that = (TimeCoding) o;

        boolean startEqual = areEqual(startTime, that.startTime);
        boolean endEqual = areEqual(endTime, that.endTime);
        return startEqual && endEqual;
    }

    private boolean areEqual(ProductData.UTC time1, ProductData.UTC time2) {
        if (time1 == null && time2 == null) {
            return true;
        }

        if (time1 == null || time2 == null) {
            return false;
        }

        return time1.getAsDate().getTime() == time2.getAsDate().getTime();

    }

    @Override
    public int hashCode() {
        int result = startTime != null ? startTime.hashCode() : 0;
        result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
        return result;
    }
}