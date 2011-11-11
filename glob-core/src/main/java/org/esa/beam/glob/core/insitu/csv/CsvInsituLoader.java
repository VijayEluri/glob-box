/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.glob.core.insitu.csv;

import com.bc.ceres.core.Assert;
import org.esa.beam.glob.core.insitu.InsituLoader;

import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;

/**
 * TODO fill out or delete
 *
 * @author Thomas Storm
 */
public class CsvInsituLoader implements InsituLoader {

    private Reader reader;
    private DateFormat dateFormat;

    @Override
    public RecordSource loadSource() throws IOException {
        Assert.state(reader != null, "reader != null");
        Assert.state(dateFormat != null, "dateFormat != null");
        return new CsvRecordSource(reader, dateFormat);
    }

    public void setCsvReader(Reader reader) {
        this.reader = reader;
    }

    public void setDateFormat(DateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }
}
