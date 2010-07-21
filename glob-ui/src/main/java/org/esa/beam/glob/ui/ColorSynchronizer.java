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

package org.esa.beam.glob.ui;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import javax.swing.JInternalFrame;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;

@Deprecated
class ColorSynchronizer implements PropertyChangeListener {

    private ImageInfoListener listener;
    private RasterDataNode currentRaster;

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
//        final ProductSceneView currentView = model.getCurrentView();
//        if (currentView != null) {
//            final List<RasterDataNode> rasterList = model.getCurrentRasterList();
//            if (model.isSynchronizingColorInformation()) {
//                currentRaster = currentView.getRaster();
//                transferImageInfo(currentRaster, rasterList);
//                listener = new ImageInfoListener(currentRaster);
//                currentRaster.getProduct().addProductNodeListener(listener);
//            } else {
//                if (currentRaster != null) {
//                    currentRaster.getProduct().removeProductNodeListener(listener);
//                }
//            }
//        } else {
//            if (currentRaster != null && listener != null) {
//                currentRaster.getProduct().removeProductNodeListener(listener);
//            }
//            currentRaster = null;
//            listener = null;
//        }
    }

    private void transferImageInfo(final RasterDataNode referenceRaster, final List<RasterDataNode> rasterList) {
//            @Override
//            protected Void doInBackground(ProgressMonitor pm) throws Exception {
//                    for (RasterDataNode raster : rasterList) {
//                        if (raster != referenceRaster) {
//                            // just to trigger computation
//                            raster.getImageInfo(new SubProgressMonitor(pm, 1));
//                            raster.getStx(false, new SubProgressMonitor(pm, 1));
//                            publish(raster);
//                        }
//                    }
//            }
//
//            @Override
//            protected void process(List<RasterDataNode> chunks) {
//                final ColorPaletteDef colorDef = referenceRaster.getImageInfo().getColorPaletteDef();
//                for (RasterDataNode raster : chunks) {
//                    if (raster != referenceRaster) {
//                        applyColorPaletteDef(raster, colorDef);
//                    }
//                }
//
//            }
//
//            @Override
//            protected void done() {
//                updateLayerCollection(referenceRaster);
//            }
//        };
    }

    private void updateLayerCollection(RasterDataNode referenceRaster) {
        // todo this block should be moved to CollectionLayer
//        final CollectionLayer collectionLayer = model.getLayerGroup();
//        if (collectionLayer != null) {
//            final List<Layer> children = collectionLayer.getChildren();
//            final ImageInfo imageInfo = referenceRaster.getImageInfo();
//            for (Layer child : children) {
//                if (child instanceof ImageLayer) {
//                    ImageLayer imageLayer = (ImageLayer) child;
//                    final MultiLevelSource source = imageLayer.getMultiLevelSource();
//                    if (source instanceof BandImageMultiLevelSource) {
//                        BandImageMultiLevelSource multiLevelSource = (BandImageMultiLevelSource) source;
//                        multiLevelSource.setImageInfo(imageInfo);
//                    }
//                }
//                child.regenerate();
//            }
//        }
    }

    private void applyColorPaletteDef(RasterDataNode targetRaster, ColorPaletteDef refColorDef) {
        final ImageInfo targetImageInfo = targetRaster.getImageInfo();
        final ColorPaletteDef.Point[] targetPoints = targetImageInfo.getColorPaletteDef().getPoints();

        if (!Arrays.equals(targetPoints, refColorDef.getPoints())) {
            if (isIndexCoded(targetRaster)) {
                targetImageInfo.setColors(refColorDef.getColors());
            } else {
                Stx stx = targetRaster.getStx();
                targetImageInfo.setColorPaletteDef(refColorDef,
                                                   targetRaster.scale(stx.getMin()),
                                                   targetRaster.scale(stx.getMax()),
                                                   false);
            }
            targetRaster.fireImageInfoChanged();
            updateSceneView(targetRaster, targetImageInfo);
        }
    }

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
//                transferImageInfo(raster, model.getCurrentRasterList());
            }

        }
    }
}
