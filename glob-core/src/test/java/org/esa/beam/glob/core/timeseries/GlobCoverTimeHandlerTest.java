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

package org.esa.beam.glob.core.timeseries;

import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;

import static junit.framework.Assert.*;

public class GlobCoverTimeHandlerTest {

    private GlobCoverTimeHandler timeHandler;

    @Before
    public void setUp() {
        timeHandler = new GlobCoverTimeHandler();
    }

    @Test
    public void fileNameParsingTestTif() throws ParseException {
        final ProductData.UTC[] dates = timeHandler.parseTimeFromFileName("GLOBCOVER_200412_200606" +
                                                                          "_V2.2_Global_CLA.tif");
        final ProductData.UTC startTime = ProductData.UTC.parse("12 2004", "MM yyyy");
        final ProductData.UTC endTime = ProductData.UTC.parse("06 2006", "MM yyyy");

        assertEquals(startTime.getMicroSecondsFraction(), dates[0].getMicroSecondsFraction());
        assertEquals(endTime.getMicroSecondsFraction(), dates[1].getMicroSecondsFraction());
    }

    @Test
    public void fileNameParsingTestHdf() throws ParseException {
        final ProductData.UTC[] dates = timeHandler.parseTimeFromFileName("GLOBCOVER-L3_MOSAIC_1982_V2.3_" +
                                                                          "ANNUAL_H[00]V[71].hdf");
        final ProductData.UTC startTime = ProductData.UTC.parse("1982", "yyyy");

        assertEquals(startTime.getMicroSecondsFraction(), dates[0].getMicroSecondsFraction());
    }

    @Test
    public void fileNameParsingTestHdfWithStartAndEndDate() throws ParseException {
        final ProductData.UTC[] dates = timeHandler.parseTimeFromFileName("GLOBCOVER-L3_MOSAIC_198207" +
                                                                          "-201003_V2.3_ANNUAL_H[00]V[71].hdf");
        final ProductData.UTC startTime = ProductData.UTC.parse("07 1982", "MM yyyy");
        final ProductData.UTC endTime = ProductData.UTC.parse("03 2010", "MM yyyy");

        assertEquals(startTime.getMicroSecondsFraction(), dates[0].getMicroSecondsFraction());
        assertEquals(endTime.getMicroSecondsFraction(), dates[1].getMicroSecondsFraction());
    }

    @Test
    public void fileNameParsingTestOfWrongFile() throws ParseException {
        final ProductData.UTC[] dates = timeHandler.parseTimeFromFileName("someProductWhichIsNoGlobcOVErProduct.tif");

        assertEquals(null, dates);
    }
}
