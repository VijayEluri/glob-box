package org.esa.beam.glob.core.insitu;

/**
 * A record source allows retrieving records and their respective {@link org.esa.beam.glob.core.insitu.Header}.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @author Thomas Storm
 */
public interface RecordSource {
    /**
     * @return The header of the record source.
     */
    Header getHeader();

    /**
     * Gets the records.
     *
     * @return The records.
     */
    Iterable<Record> getRecords();

    /**
     * Closes the sources.
     */
    void close();
}
