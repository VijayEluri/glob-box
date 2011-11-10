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

package org.esa.beam.glob.core.timeseries.datamodel;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.util.ProductUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p><i>Note that this class is not yet public API. Interface may chhange in future releases.</i></p>
 *
 * @author Thomas Storm
 */
public class TimeSeriesFactory {

    private TimeSeriesFactory() {
    }

    /**
     * Creates a new TimeSeries from a given time series product. The given product has to be a time series product.
     * This method should only be called by the reader
     *
     * @param product a time series product
     *
     * @return a time series wrapping the given product
     */
    public static AbstractTimeSeries create(Product product) {
        final TimeSeriesImpl timeSeries = new TimeSeriesImpl(product);
        TimeSeriesMapper.getInstance().put(product, timeSeries);
        return timeSeries;
    }

    /**
     * Creates a new TimeSeries with a given name, a list of product locations and a list of variables (which are
     * placeholders for bands)
     *
     * @param timeSeriesName              a name for the time series
     * @param productLocations  locations where to find the data the time series is based on
     * @param variableNames     the variables the time series is based on
     *
     * @return a time series
     */
    public static AbstractTimeSeries create(String timeSeriesName,
                                            List<ProductLocation> productLocations,
                                            List<String> variableNames) {
        try {
            Assert.notNull(productLocations, "productLocations");
            Assert.argument(productLocations.size() > 0, "productLocations must contain at least one location.");
            Assert.notNull(variableNames, "variableNames");
            Assert.argument(variableNames.size() > 0, "variableNames must contain at least one variable name.");
            Assert.argument(timeSeriesName != null && timeSeriesName.trim().length() > 0, "timeSeriesName must not be null or empty.");

            if (noSourceProductsAvailable(productLocations)) {
                return null;
            }

            // todo - ts - get user-specified reference product
            final Product refProduct = productLocations.get(0).getProducts().values().iterator().next();
            final Product tsProduct = new Product(timeSeriesName, TimeSeriesImpl.TIME_SERIES_PRODUCT_TYPE,
                                                  refProduct.getSceneRasterWidth(),
                                                  refProduct.getSceneRasterHeight());
            tsProduct.setDescription("A time series product");
            ProductUtils.copyGeoCoding(refProduct, tsProduct);

            final AbstractTimeSeries timeSeries = new TimeSeriesImpl(tsProduct, productLocations, variableNames);
            TimeSeriesMapper.getInstance().put(tsProduct, timeSeries);
            return timeSeries;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean noSourceProductsAvailable(List<ProductLocation> productLocations) {
        final Map<String, Product> productList = new HashMap<String, Product>();
        for (ProductLocation productLocation : productLocations) {
            productList.putAll(productLocation.getProducts());
        }
        return productList.isEmpty();
    }

}
