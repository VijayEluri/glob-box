package org.esa.beam.glob.core.insitu.csv;

public interface Header {

    /**
     * @return {@code true}, if records that conform to this header return location values (see {@link Record#getLocation()}).
     */
    boolean hasLocation();

    /**
     * @return {@code true}, if records that conform to this header return time values (see {@link Record#getTime()}).
     */
    boolean hasTime();

    /**
     * @return The array of parameter names.
     */
    String[] getParameterNames();

    /**
     * @return The array of column names.
     */
    String[] getColumnNames();
}