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

import com.bc.ceres.glayer.Layer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

@Deprecated
class WorldMapHandler implements PropertyChangeListener {

    private Layer layer;

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
//        ProductSceneView currentView = model.getCurrentView();
//        if (currentView != null) {
//            if (layer == null) {
//                final LayerType type = LayerTypeRegistry.getLayerType("BlueMarbleLayerType");
//                layer = type.createLayer(currentView, type.createLayerConfig(currentView));
//            }
//            if (LayerUtils.getChildLayerById(currentView.getRootLayer(), layer.getId()) != null) {
//                if (!model.isShowingWorldMapLayer()) {
//                    currentView.getRootLayer().getChildren().remove(layer);
//                }
//            } else {
//                if (model.isShowingWorldMapLayer()) {
//                    layer.setVisible(true);
//                    currentView.getRootLayer().getChildren().add(layer);
//                }
//            }
//        }
    }
}
