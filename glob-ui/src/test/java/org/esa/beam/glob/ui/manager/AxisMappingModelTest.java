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

package org.esa.beam.glob.ui.manager;

import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Thomas Storm
 */
public class AxisMappingModelTest {

    private AxisMappingModel axisMappingModel;

    @Before
    public void setUp() throws Exception {
        axisMappingModel = new AxisMappingModel();
    }

    @Test
    public void testGetRasterNames() throws Exception {
        axisMappingModel.addRasterName("algal", "algal_1");
        axisMappingModel.addRasterName("algal", "algal_2");

        final Set<String> rasterNames = axisMappingModel.getRasterNames("algal");

        final Set<String> expectedRasterNames = new HashSet<String>(2);
        expectedRasterNames.add("algal_2");
        expectedRasterNames.add("algal_1");
        assertEquals(expectedRasterNames, rasterNames);
    }

    @Test
    public void testGetInsituNames() throws Exception {
        axisMappingModel.addInsituName("chl", "chl_1");
        axisMappingModel.addInsituName("chl", "chl2");

        final Set<String> insituNames = axisMappingModel.getInsituNames("chl");

        final Set<String> expectedInsituNames = new HashSet<String>(2);
        expectedInsituNames.add("chl2");
        expectedInsituNames.add("chl_1");
        assertEquals(expectedInsituNames, insituNames);
    }

    @Test
    public void testRemoveAlias() throws Exception {
        final String alias = "chl";
        axisMappingModel.addInsituName(alias, "chl_1");
        axisMappingModel.addInsituName(alias, "chl2");
        axisMappingModel.addRasterName(alias, "chl_a");
        axisMappingModel.addRasterName(alias, "chl_b");

        axisMappingModel.removeAlias(alias);

        assertTrue(axisMappingModel.getInsituNames(alias).isEmpty());
        assertTrue(axisMappingModel.getRasterNames(alias).isEmpty());

    }

    @Test
    public void testRemoveInsitu() throws Exception {
        axisMappingModel.addInsituName("chl", "chl_1");
        axisMappingModel.addInsituName("chl", "chl2");

        axisMappingModel.removeInsituName("chl", "chl2");
        final Set<String> insituNames = axisMappingModel.getInsituNames("chl");

        final Set<String> expectedInsituNames = new HashSet<String>(2);
        expectedInsituNames.add("chl_1");
        assertEquals(expectedInsituNames, insituNames);
    }

    @Test
    public void testRemoveRaster() throws Exception {
        axisMappingModel.addRasterName("algal", "algal_1");
        axisMappingModel.addRasterName("algal", "algal2");

        axisMappingModel.removeRasterName("algal", "algal2");
        final Set<String> rasterNames = axisMappingModel.getRasterNames("algal");

        final Set<String> expectedRasterNames = new HashSet<String>(2);
        expectedRasterNames.add("algal_1");
        assertEquals(expectedRasterNames, rasterNames);
    }

    @Test
    public void testGetAliasNames() throws Exception {
        axisMappingModel.addRasterName("ra", "rn");
        axisMappingModel.addInsituName("ia", "in");
        
        final Set<String> names = axisMappingModel.getAliasNames();

        assertTrue(names instanceof SortedSet);
        final HashSet<String> expectedNames = new HashSet<String>();
        expectedNames.add("ra");
        expectedNames.add("ia");
        assertEquals(expectedNames, names);
    }

    @Test
    public void testAddAlias() throws Exception {
        axisMappingModel.addAlias("chl");

        assertEquals("chl", axisMappingModel.getAliasNames().iterator().next());
    }

    @Test
    public void testNoAliasNamesAreAddedAsSideEffect() throws Exception {
        axisMappingModel.getRasterNames("alias");
        axisMappingModel.getInsituNames("alias");
        assertTrue(axisMappingModel.getAliasNames().isEmpty());
    }

    @Test
    public void testReplaceAlias() throws Exception {
        axisMappingModel.addRasterName("alias", "RName");
        axisMappingModel.addInsituName("alias", "IName");

        axisMappingModel.replaceAlias("alias", "replaced");

        final Set<String> aliasNames = axisMappingModel.getAliasNames();
        assertEquals(1, aliasNames.size());
        assertEquals("replaced", aliasNames.iterator().next());
        assertEquals("RName", axisMappingModel.getRasterNames("replaced").iterator().next());
        assertEquals("IName", axisMappingModel.getInsituNames("replaced").iterator().next());
    }
}

