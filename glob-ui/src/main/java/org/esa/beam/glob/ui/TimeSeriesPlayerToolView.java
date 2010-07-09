package org.esa.beam.glob.ui;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneImage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glevel.BandImageMultiLevelSource;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Container;
import java.lang.reflect.Field;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 18.06.2010
 * Time: 15:48:31
 */
public class TimeSeriesPlayerToolView extends AbstractToolView {

    public static final String TIME_PROPERTY = "timeProperty";

    private static final String NEXT_IMAGE_LAYER = "nextImageLayer";

    private final SceneViewListener sceneViewListener;
    private final ProductNodeListener productNodeListener;

    private BandImageMultiLevelSource[] imageMultiLevelSources;
    private ProductSceneView currentView;
    private TimeSeriesPlayerForm form;

    public TimeSeriesPlayerToolView() {
        sceneViewListener = new SceneViewListener();
        productNodeListener = new TimeSeriesProductNodeListener();
    }

    @Override
    public void componentShown() {
        VisatApp.getApp().addInternalFrameListener(sceneViewListener);
    }

    @Override
    public void componentHidden() {
        VisatApp.getApp().removeInternalFrameListener(sceneViewListener);
    }

    @Override
    protected JComponent createControl() {
        form = new TimeSeriesPlayerForm();
        form.getTimeSlider().addChangeListener(new SliderChangeListener());
        ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view != null) {
            final String viewProductType = view.getProduct().getProductType();
            if (!view.isRGB() && viewProductType.equals(
                    AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE)) {
                setCurrentView(view);
            }
        }
        return form;
    }

    private void setCurrentView(ProductSceneView newView) {
        if (currentView != newView) {
            if (currentView != null) {
                currentView.getProduct().removeProductNodeListener(productNodeListener);
            }
            currentView = newView;
            form.setView(currentView);
            if (currentView != null) {
                form.setTimeSeries(TimeSeriesMapper.getInstance().getTimeSeries(currentView.getProduct()));
                final List<Band> bandList = form.getBandList(currentView.getRaster().getName());
                imageMultiLevelSources = new BandImageMultiLevelSource[bandList.size()];
                for (int i = 0; i < bandList.size(); i++) {
                    Band band = bandList.get(i);
                    imageMultiLevelSources[i] = BandImageMultiLevelSource.create(band, ProgressMonitor.NULL);
                }
                currentView.getProduct().addProductNodeListener(productNodeListener);
                exchangeRasterInProductSceneView(currentView.getRaster());
                reconfigureBaseImageLayer(currentView);
                form.configureTimeSlider(currentView.getRaster());
            } else {
                imageMultiLevelSources = null;
                form.setTimeSeries(null);
                form.configureTimeSlider(null);
                form.getTimer().stop();
            }
        }
    }

    // todo (mp) - The following should be done on ProductSceneView.setRasters()

    private void exchangeRasterInProductSceneView(RasterDataNode nextRaster) {
        // todo use a real ProgressMonitor
        final RasterDataNode currentRaster = currentView.getRaster();
        final ImageInfo imageInfoClone = (ImageInfo) currentRaster.getImageInfo(ProgressMonitor.NULL).clone();
        nextRaster.setImageInfo(imageInfoClone);
        currentView.setRasters(new RasterDataNode[]{nextRaster});
        currentView.setImageInfo(imageInfoClone.createDeepCopy());
        VisatApp.getApp().getSelectedInternalFrame().setTitle(nextRaster.getDisplayName());
    }

    private void reconfigureBaseImageLayer(ProductSceneView sceneView) {
        final Layer rootLayer = currentView.getRootLayer();
        final ImageLayer baseImageLayer = (ImageLayer) LayerUtils.getChildLayerById(rootLayer,
                                                                                    ProductSceneView.BASE_IMAGE_LAYER_ID);
        final List<Band> bandList = form.getBandList(currentView.getRaster().getName());
        final Band band = (Band) sceneView.getRaster();
        int nextIndex = bandList.indexOf(band) + 1;
        if (nextIndex >= bandList.size()) {
            nextIndex = 0;
        }

        if (!(baseImageLayer instanceof BlendImageLayer)) {
            final Band nextBand = bandList.get(nextIndex);
            MultiLevelSource nextLevelSource = BandImageMultiLevelSource.create(nextBand, ProgressMonitor.NULL);
            final BlendImageLayer blendLayer = new BlendImageLayer(baseImageLayer.getMultiLevelSource(),
                                                                   nextLevelSource);

            final List<Layer> children = rootLayer.getChildren();
            final int baseIndex = children.indexOf(baseImageLayer);
            children.remove(baseIndex);
            blendLayer.setId(ProductSceneView.BASE_IMAGE_LAYER_ID);
            blendLayer.setName(band.getDisplayName());
            blendLayer.setTransparency(0);
            children.add(baseIndex, blendLayer);
            configureSceneView(sceneView, blendLayer.getMultiLevelSource());
        }
    }

    // This is needed because sceneView must return correct ImageInfo

    private void configureSceneView(ProductSceneView sceneView, MultiLevelSource multiLevelSource) {
        try {
            final Field sceneImageField = ProductSceneView.class.getDeclaredField("sceneImage");
            sceneImageField.setAccessible(true);
            final Object sceneImage = sceneImageField.get(sceneView);
            final Field multiLevelSourceField = ProductSceneImage.class.getDeclaredField("bandImageMultiLevelSource");
            multiLevelSourceField.setAccessible(true);
            multiLevelSourceField.set(sceneImage, multiLevelSource);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private class SceneViewListener extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                ProductSceneView view = (ProductSceneView) contentPane;
                final RasterDataNode viewRaster = view.getRaster();
                final String viewProductType = viewRaster.getProduct().getProductType();
                if (currentView != view && !view.isRGB() && viewProductType.equals(
                        AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE)) {
                    setCurrentView(view);
                }
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (currentView == contentPane) {
                setCurrentView(null);
            }
        }
    }

    private class SliderChangeListener implements ChangeListener {

        private int value;

        @Override
        public void stateChanged(ChangeEvent e) {
            if (currentView == null) {
                return;
            }
            final int currentValue = form.getTimeSlider().getValue();
            MultiLevelSource newFirstLevelSource;
            MultiLevelSource newSecondLevelSource;
            if (currentView.getBaseImageLayer() instanceof BlendImageLayer) {
                BlendImageLayer blendLayer = (BlendImageLayer) currentView.getBaseImageLayer();
                int stepsPerTimespan = form.getStepsPerTimespan();

                final int secondBandIndex;
                final int firstBandIndex;

                final float transparency = (currentValue % stepsPerTimespan) / (float) stepsPerTimespan;
                blendLayer.setBlendFactor(transparency);

                if (currentValue == value) {
                    // nothing has changed -- do nothing
                    return;
                } else if (currentValue < value) {
                    // go backwards in time
                    value = currentValue;
                    firstBandIndex = MathUtils.floorInt(currentValue / (float) stepsPerTimespan);
                    secondBandIndex = currentValue / stepsPerTimespan;
                    newFirstLevelSource = imageMultiLevelSources[firstBandIndex];
                    newSecondLevelSource = blendLayer.getMultiLevelSource();
                } else {
                    // go forward in time
                    value = currentValue;
                    firstBandIndex = currentValue / stepsPerTimespan;
                    secondBandIndex = MathUtils.ceilInt(currentValue / (float) stepsPerTimespan);
                    newFirstLevelSource = blendLayer.getSecondMultiLevelSource();
                    newSecondLevelSource = imageMultiLevelSources[secondBandIndex];
                }

                if (secondBandIndex == firstBandIndex) {
                    final List<Band> bandList = form.getBandList(currentView.getRaster().getName());
                    final BlendImageLayer newBlendLayer = new BlendImageLayer(newFirstLevelSource,
                                                                              newSecondLevelSource);
                    newBlendLayer.setName(currentView.getRaster().getDisplayName());
                    newBlendLayer.setId(ProductSceneView.BASE_IMAGE_LAYER_ID);

                    final List<Layer> children = currentView.getRootLayer().getChildren();
                    int baseIndex = children.indexOf(blendLayer);
                    if (baseIndex == -1) {
                        baseIndex = children.indexOf(newBlendLayer);
                    }
//                    children.add(newBlendLayer);
                    children.set(baseIndex, newBlendLayer);
                    exchangeRasterInProductSceneView(bandList.get(firstBandIndex));

//                 todo why use view to fire property changes and not time series itself?
                    currentView.firePropertyChange(TIME_PROPERTY, -1, firstBandIndex);
                } else {
                    currentView.getLayerCanvas().repaint();
                }
            }
        }

    }

    private class TimeSeriesProductNodeListener extends ProductNodeListenerAdapter {

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            String propertyName = event.getPropertyName();
            if (propertyName.equals(AbstractTimeSeries.PROPERTY_PRODUCT_LOCATIONS) ||
                propertyName.equals(AbstractTimeSeries.PROPERTY_VARIABLE_SELECTION)) {
                form.configureTimeSlider(currentView.getRaster());
            }
        }
    }
}
