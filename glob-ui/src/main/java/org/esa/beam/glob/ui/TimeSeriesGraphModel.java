package org.esa.beam.glob.ui;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.math.Histogram;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;


class TimeSeriesGraphModel {
    private static final String NO_DATA_MESSAGE = "No data to display";
    private static final Stroke PIN_STROKE = new BasicStroke(
            1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f}, 0.0f);

    private final Map<AbstractTimeSeries, DisplayModel> displayModelMap;
    private final XYPlot timeSeriesPlot;

    private final List<List<Band>> variableBands;
    private final List<TimeSeriesCollection> pinDatasets;
    private final List<TimeSeriesCollection> cursorDatasets;
    private DisplayModel displayModel;
    private TimeSeriesUpdater updater;


    TimeSeriesGraphModel(XYPlot plot) {
        timeSeriesPlot = plot;
        variableBands = new ArrayList<List<Band>>();
        displayModelMap = new WeakHashMap<AbstractTimeSeries, DisplayModel>();
        pinDatasets = new ArrayList<TimeSeriesCollection>();
        cursorDatasets = new ArrayList<TimeSeriesCollection>();
        initPlot();
    }

    private void initPlot() {
        final ValueAxis domainAxis = timeSeriesPlot.getDomainAxis();
        domainAxis.setAutoRange(true);
        XYItemRenderer renderer = timeSeriesPlot.getRenderer();
        if (renderer instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer xyRenderer = (XYLineAndShapeRenderer) renderer;
            xyRenderer.setBaseShapesVisible(true);
            xyRenderer.setBaseShapesFilled(true);
        }
        timeSeriesPlot.setNoDataMessage(NO_DATA_MESSAGE);
    }

    void adaptToTimeSeries(AbstractTimeSeries timeSeries) {
        variableBands.clear();

        if (timeSeries != null) {
            displayModel = displayModelMap.get(timeSeries);
            if (displayModel == null) {
                displayModel = new DisplayModel(timeSeries);
                displayModelMap.put(timeSeries, displayModel);
            } else {
                displayModel.adaptTo(timeSeries);
            }
            for (String variableName : displayModel.variablesToDisplay) {
                variableBands.add(timeSeries.getBandsForVariable(variableName));
            }
        } else {
            displayModel = null;
        }
        updatePlot(timeSeries != null);
    }

    private void updatePlot(boolean hasData) {
        for (int i = 0; i < timeSeriesPlot.getDatasetCount(); i++) {
            timeSeriesPlot.setDataset(i, null);
        }
        timeSeriesPlot.clearRangeAxes();
        pinDatasets.clear();
        cursorDatasets.clear();

        if (hasData) {
            List<String> variablesToDisplay = displayModel.getVariablesToDisplay();
            int numVariables = variablesToDisplay.size();
            for (int i = 0; i < numVariables; i++) {

                String variableName = variablesToDisplay.get(i);
                List<Band> bandList = variableBands.get(i);

                Paint paint = displayModel.getVariablename2colorMap().get(variableName);
                String axisLabel = getAxisLabel(variableName, bandList.get(0).getUnit());
                NumberAxis valueAxis = new NumberAxis(axisLabel);
                valueAxis.setAutoRange(true);
                valueAxis.setRange(computeYAxisRange(bandList));
                valueAxis.setAxisLinePaint(paint);
                valueAxis.setLabelPaint(paint);
                valueAxis.setTickLabelPaint(paint);
                valueAxis.setTickMarkPaint(paint);
                timeSeriesPlot.setRangeAxis(i, valueAxis);

                TimeSeriesCollection cursorDataset = new TimeSeriesCollection();
                timeSeriesPlot.setDataset(i, cursorDataset);
                cursorDatasets.add(cursorDataset);

                TimeSeriesCollection pinDataset = new TimeSeriesCollection();
                timeSeriesPlot.setDataset(i + numVariables, pinDataset);
                pinDatasets.add(pinDataset);

                timeSeriesPlot.mapDatasetToRangeAxis(i, i);
                timeSeriesPlot.mapDatasetToRangeAxis(i + numVariables, i);

                XYLineAndShapeRenderer cursorRenderer = new XYLineAndShapeRenderer(true, true);
                cursorRenderer.setSeriesPaint(0, paint);

                XYLineAndShapeRenderer pinRenderer = new XYLineAndShapeRenderer(true, true);
                pinRenderer.setSeriesPaint(0, paint);
                pinRenderer.setSeriesStroke(0, PIN_STROKE);

                timeSeriesPlot.setRenderer(i, cursorRenderer, true);
                timeSeriesPlot.setRenderer(i + numVariables, pinRenderer, true);
            }
        }
    }

    private String getAxisLabel(String variableName, String unit) {
        if (StringUtils.isNotNullAndNotEmpty(unit)) {
            return String.format("%s (%s)", variableName, unit);
        } else {
            return variableName;
        }
    }

    private static Range computeYAxisRange(List<Band> bands) {
        Range result = null;
        for (Band band : bands) {
            Stx stx = band.getStx();
            Histogram histogram = new Histogram(stx.getHistogramBins(), stx.getMin(), stx.getMax());
            org.esa.beam.util.math.Range rangeFor95Percent = histogram.findRangeFor95Percent();
            Range range = new Range(band.scale(rangeFor95Percent.getMin()), band.scale(rangeFor95Percent.getMax()));
            if (result == null) {
                result = range;
            } else {
                result = Range.combine(result, range);
            }
        }
        return result;
    }

    private static double getValue(RasterDataNode raster, int pixelX, int pixelY, int currentLevel) {
        final RenderedImage image = raster.getGeophysicalImage().getImage(currentLevel);
        final Rectangle pixelRect = new Rectangle(pixelX, pixelY, 1, 1);
        final Raster data = image.getData(pixelRect);
        final RenderedImage validMask = raster.getValidMaskImage().getImage(currentLevel);
        final Raster validMaskData = validMask.getData(pixelRect);
        final double value;
        if (validMaskData.getSample(pixelX, pixelY, 0) > 0) {
            value = data.getSampleDouble(pixelX, pixelY, 0);
        } else {
            value = Double.NaN;
        }
        return value;
    }

    List<TimeSeries> computeTimeSeries(String title, int pixelX, int pixelY, int currentLevel) {
        List<String> variableNames = displayModel.getVariablesToDisplay();
        List<TimeSeries> result = new ArrayList<TimeSeries>(variableNames.size());
        for (int i = 0; i < variableNames.size(); i++) {
            String variableName = variableNames.get(i);
            List<Band> bandList = variableBands.get(i);
            result.add(computeTimeSeries(title + "_" + variableName, bandList, pixelX, pixelY, currentLevel));
        }
        return result;
    }

    private static TimeSeries computeTimeSeries(String title, final List<Band> bandList, int pixelX, int pixelY,
                                                int currentLevel) {
        TimeSeries timeSeries = new TimeSeries(title);
        for (Band band : bandList) {
            final ProductData.UTC startTime = band.getTimeCoding().getStartTime();
            final Millisecond timePeriod = new Millisecond(startTime.getAsDate(),
                    ProductData.UTC.UTC_TIME_ZONE,
                    Locale.getDefault());

            final double value = getValue(band, pixelX, pixelY, currentLevel);
            timeSeries.add(new TimeSeriesDataItem(timePeriod, value));
        }
        return timeSeries;
    }

    void addSelectedPinSeries(Placemark pin, ProductSceneView view) {
        PixelPos position = pin.getPixelPos();

        final Viewport viewport = view.getViewport();
        final ImageLayer baseLayer = view.getBaseImageLayer();
        final int currentLevel = baseLayer.getLevel(viewport);
        final AffineTransform levelZeroToModel = baseLayer.getImageToModelTransform();
        final AffineTransform modelToCurrentLevel = baseLayer.getModelToImageTransform(currentLevel);
        final Point2D modelPos = levelZeroToModel.transform(position, null);
        final Point2D currentPos = modelToCurrentLevel.transform(modelPos, null);

        List<TimeSeries> pinTimeSeries = computeTimeSeries("pin", (int) currentPos.getX(), (int) currentPos.getY(), currentLevel);
        removeTimeSeries(false);
        addTimeSeries(pinTimeSeries, false);
    }

    void addTimeSeries(List<TimeSeries> timeSeries, boolean cursor) {
        List<TimeSeriesCollection> collections = getDatasets(cursor);
        for (int i = 0; i < timeSeries.size(); i++) {
            collections.get(i).addSeries(timeSeries.get(i));
        }
    }

    void removeTimeSeries(boolean cursor) {
        List<TimeSeriesCollection> collections = getDatasets(cursor);
        for (TimeSeriesCollection dataset : collections) {
            dataset.removeAllSeries();
        }
    }

    private List<TimeSeriesCollection> getDatasets(boolean cursor) {
        return cursor ? cursorDatasets : pinDatasets;
    }

    void updateTimeAnnotation(RasterDataNode raster) {
        removeTimeAnnotation();

        final ProductData.UTC startTime = raster.getTimeCoding().getStartTime();
        final Millisecond timePeriod = new Millisecond(startTime.getAsDate(),
                ProductData.UTC.UTC_TIME_ZONE,
                Locale.getDefault());

        double millisecond = timePeriod.getFirstMillisecond();
        Range valueRange = null;
        for (int i = 0; i < timeSeriesPlot.getRangeAxisCount(); i++) {
            valueRange = Range.combine(valueRange, timeSeriesPlot.getRangeAxis(i).getRange());
        }
        if (valueRange != null) {
            XYLineAnnotation xyla = new XYLineAnnotation(millisecond, valueRange.getLowerBound(), millisecond, valueRange.getUpperBound());
            timeSeriesPlot.addAnnotation(xyla, true);
        }
    }

    void removeTimeAnnotation() {
        timeSeriesPlot.clearAnnotations();
    }

    void doit(int pixelX, int pixelY, int currentLevel, boolean cursor) {
        if (updater == null || updater.isDone()) {
            updater = new TimeSeriesUpdater(pixelX, pixelY, currentLevel, cursor);
            updater.execute();
        }
    }

    private class TimeSeriesUpdater extends SwingWorker<List<TimeSeries>, Void> {

        private final int pixelX;
        private final int pixelY;
        private final int currentLevel;
        private final boolean cursor;

        TimeSeriesUpdater(int pixelX, int pixelY, int currentLevel, boolean cursor) {
            this.pixelX = pixelX;
            this.pixelY = pixelY;
            this.currentLevel = currentLevel;
            this.cursor = cursor;
        }

        @Override
        protected List<TimeSeries> doInBackground() throws Exception {
            return computeTimeSeries("cursor", pixelX, pixelY, currentLevel);
        }

        @Override
        protected void done() {
//                getTimeSeriesPlot().removeAnnotation(loadingMessage);
            removeTimeSeries(cursor);
            try {
                addTimeSeries(get(), cursor);
            } catch (InterruptedException ignore) {
            } catch (ExecutionException ignore) {
            }
        }
    }

    private static class DisplayModel {
        private final Map<String, Paint> variablename2colorMap;
        private final List<String> variablesToDisplay;
        private int maxColorIndex;

        private DisplayModel(AbstractTimeSeries timeSeries) {
            variablesToDisplay = new ArrayList<String>();
            variablename2colorMap = new HashMap<String, Paint>();
            for (String variableName : timeSeries.getTimeVariables()) {
                if (timeSeries.isVariableSelected(variableName)) {
                    variablesToDisplay.add(variableName);
                    variablename2colorMap.put(variableName, getNextPaint());
                }
            }
        }

        public Map<String, Paint> getVariablename2colorMap() {
            return variablename2colorMap;
        }

        public List<String> getVariablesToDisplay() {
            return variablesToDisplay;
        }

        void adaptTo(AbstractTimeSeries timeSeries) {
            for (String variableName : timeSeries.getTimeVariables()) {
                if (timeSeries.isVariableSelected(variableName)) {
                    if (!variablesToDisplay.contains(variableName)) {
                        variablesToDisplay.add(variableName);
                    }
                    if (!variablename2colorMap.containsKey(variableName)) {
                        variablename2colorMap.put(variableName, getNextPaint());
                    }
                } else {
                    variablesToDisplay.remove(variableName);
                }
            }
        }

        private Paint getNextPaint() {
            int numColors = DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE.length;
            return DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE[maxColorIndex++ % numColors];
        }
    }
}
