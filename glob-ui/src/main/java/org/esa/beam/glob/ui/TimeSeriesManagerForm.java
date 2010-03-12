package org.esa.beam.glob.ui;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glob.GlobBoxManager;
import org.esa.beam.visat.VisatApp;

import javax.swing.JCheckBox;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import java.awt.Container;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;

class TimeSeriesManagerForm extends JPanel {

    private TimeSeriesManagerFormModel model;
    private JCheckBox showWorldMapChecker;
    private JCheckBox syncColorChecker;

    private JCheckBox useAlphaBlendingChecker;
    private JSlider timeSlider;

    TimeSeriesManagerForm(TimeSeriesManagerFormModel model) {
        this.model = model;
        createComponents(model.getPropertySet());
    }


    private void createComponents(PropertySet propertySet) {
        setPreferredSize(new Dimension(300, 200));
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setRowWeightY(4, 1.0);
        setLayout(tableLayout);

        showWorldMapChecker = new JCheckBox("Show world map layer");
        syncColorChecker = new JCheckBox("Synchronise colour information");
        useAlphaBlendingChecker = new JCheckBox("Use transparency blending");
        timeSlider = new JSlider(JSlider.HORIZONTAL);
        add(showWorldMapChecker);
        add(syncColorChecker);
        add(useAlphaBlendingChecker);
        add(timeSlider);
        add(tableLayout.createVerticalSpacer());

        final BindingContext context = new BindingContext(propertySet);
        context.bind(TimeSeriesManagerFormModel.PROPERTY_NAME_WORLDMAP, showWorldMapChecker);
        context.bind(TimeSeriesManagerFormModel.PROPERTY_NAME_SYNCCOLOR, syncColorChecker);
        context.bind(TimeSeriesManagerFormModel.PROPERTY_NAME_BLENDING, useAlphaBlendingChecker);

        final WorldMapHandler worldMapHandler = new WorldMapHandler();
        context.addPropertyChangeListener(TimeSeriesManagerFormModel.PROPERTY_NAME_WORLDMAP, worldMapHandler);
        context.addPropertyChangeListener(GlobBoxManager.CURRENT_VIEW_PROPERTY, worldMapHandler);

        final ColorSynchronizer colorSynchronizer = new ColorSynchronizer();
        context.addPropertyChangeListener(TimeSeriesManagerFormModel.PROPERTY_NAME_SYNCCOLOR, colorSynchronizer);
        context.addPropertyChangeListener(GlobBoxManager.CURRENT_VIEW_PROPERTY, colorSynchronizer);
    }

    private class WorldMapHandler implements PropertyChangeListener {

        private Layer layer;

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            ProductSceneView currentView = model.getCurrentView();
            if (currentView != null) {
                if (layer == null) {
                    final LayerType type = LayerTypeRegistry.getLayerType("BlueMarbleLayerType");
                    layer = type.createLayer(currentView, type.createLayerConfig(currentView));
                }
                if (LayerUtils.getChildLayerById(currentView.getRootLayer(), layer.getId()) != null) {
                    if (!model.isShowingWorldMapLayer()) {
                        currentView.getRootLayer().getChildren().remove(layer);
                    }
                } else {
                    if (model.isShowingWorldMapLayer()) {
                        layer.setVisible(true);
                        currentView.getRootLayer().getChildren().add(layer);
                    }
                }
            }
        }
    }

    private class ColorSynchronizer implements PropertyChangeListener {

        private TimeSeriesManagerForm.ColorSynchronizer.ImageInfoListener listener;
        private RasterDataNode currentRaster;

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            final ProductSceneView currentView = model.getCurrentView();
            if (currentView != null) {
                final List<RasterDataNode> rasterList = model.getCurrentRasterList();
                if (model.isSynchronizingColorInformation()) {
                    currentRaster = currentView.getRaster();
                    transferImageInfo(currentRaster, rasterList);
                    listener = new ImageInfoListener(currentRaster);
                    currentRaster.getProduct().addProductNodeListener(listener);
                } else {
                    if (currentRaster != null) {
                        currentRaster.getProduct().removeProductNodeListener(listener);
                        resetImageInfos(rasterList);
                    }
                }
            } else {
                if (currentRaster != null && listener != null) {
                    currentRaster.getProduct().removeProductNodeListener(listener);
                }
                currentRaster = null;
                listener = null;
            }
        }

        private void resetImageInfos(List<RasterDataNode> rasterList) {
            for (RasterDataNode raster : rasterList) {
                final ImageInfo imageInfo = raster.createDefaultImageInfo(null, ProgressMonitor.NULL);
                applyColorPaletteDef(raster, imageInfo.getColorPaletteDef());
            }
        }

        private void transferImageInfo(RasterDataNode referenceRaster, List<RasterDataNode> rasterList) {
            for (RasterDataNode raster : rasterList) {
                applyColorPaletteDef(raster, referenceRaster.getImageInfo().getColorPaletteDef());
            }
        }

        private void applyColorPaletteDef(RasterDataNode targetRaster, ColorPaletteDef refColorDef) {
            final ImageInfo targetImageInfo = targetRaster.getImageInfo(ProgressMonitor.NULL);
            final ColorPaletteDef.Point[] targetPoints = targetImageInfo.getColorPaletteDef().getPoints();

            if (!Arrays.equals(targetPoints, refColorDef.getPoints())) {
                if (isIndexCoded(targetRaster)) {
                    targetImageInfo.setColors(refColorDef.getColors());
                } else {
                    Stx stx = targetRaster.getStx(false, ProgressMonitor.NULL);
                    targetImageInfo.setColorPaletteDef(refColorDef,
                                                       targetRaster.scale(stx.getMin()),
                                                       targetRaster.scale(stx.getMax()),
                                                       false);
                }
                updateSceneView(targetRaster, targetImageInfo);
            }
        }

        // todo - not very intuitive code
        // the following code is need to synchronize the ColorManipulationForm
        // with the ImageInfo, why isn't it update automatically when the ImageInfo of the raster is changed
        private void updateSceneView(RasterDataNode targetRaster, ImageInfo targetImageInfo) {
            final VisatApp app = VisatApp.getApp();
            app.updateImages(new RasterDataNode[]{targetRaster});
            final JInternalFrame[] internalFrames = app.findInternalFrames(targetRaster, 1);
            for (JInternalFrame internalFrame : internalFrames) {
                final Container contentPane = internalFrame.getContentPane();
                if (contentPane instanceof ProductSceneView) {
                    ProductSceneView view = (ProductSceneView) contentPane;
                    view.setImageInfo(targetImageInfo.createDeepCopy());
                }
            }
        }

        private boolean isIndexCoded(RasterDataNode targetRaster) {
            return targetRaster instanceof Band && ((Band) targetRaster).getIndexCoding() != null;
        }

        private class ImageInfoListener extends ProductNodeListenerAdapter {

            private final RasterDataNode raster;

            ImageInfoListener(RasterDataNode raster) {
                this.raster = raster;
            }

            @Override
            public void nodeChanged(ProductNodeEvent event) {
                if (event.getSourceNode() == raster &&
                    event.getPropertyName().equals(RasterDataNode.PROPERTY_NAME_IMAGE_INFO)) {
                    transferImageInfo(raster, model.getCurrentRasterList());
                }

            }
        }
    }

}
