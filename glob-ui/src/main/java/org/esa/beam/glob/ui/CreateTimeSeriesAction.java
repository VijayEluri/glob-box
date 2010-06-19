package org.esa.beam.glob.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.glob.core.TimeSeriesProductBuilder;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import java.awt.Dimension;

public class CreateTimeSeriesAction extends AbstractVisatAction {

    @Override
    public void actionPerformed(CommandEvent event) {
        final VisatApp app = VisatApp.getApp();
        final ProductNode node = app.getSelectedProductNode();
        if (node instanceof RasterDataNode) {
            RasterDataNode raster = (RasterDataNode) node;
            final Model model = new Model(raster);
            PropertyContainer propertyContainer = model.createPropertyContainer();
            PropertyPane timeSeriesPane = new PropertyPane(propertyContainer);
            if (showDialog(app, timeSeriesPane)) {
                final String timeSeriesName = model.getName();

                final Product tsProduct = TimeSeriesProductBuilder.createTimeSeriesProduct(timeSeriesName,
                                                                                           raster,
                                                                                           VisatApp.getApp().getProductManager());
                app.getProductManager().addProduct(tsProduct);
            }
        }
    }

    @Override
    public void updateState() {
        final VisatApp app = VisatApp.getApp();
        final ProductNode node = app.getSelectedProductNode();
        setEnabled(node instanceof RasterDataNode);
    }

    private boolean showDialog(VisatApp app, final PropertyPane timeSeriesPane) {
        ModalDialog modalDialog = new ModalDialog(app.getMainFrame(), "Create time series",
                                                  ModalDialog.ID_OK_CANCEL_HELP, null) {
            @Override
            protected boolean verifyUserInput() {
                return !timeSeriesPane.getBindingContext().hasProblems();
            }
        };
        modalDialog.setContent(timeSeriesPane.createPanel());
        final int status = modalDialog.show();
        modalDialog.getJDialog().setMinimumSize(new Dimension(300, 80));
        modalDialog.getJDialog().dispose();
        return status == ModalDialog.ID_OK;
    }

    public static class Model {

        private static final String PROPERTY_NAME_NAME = "name";

        private String name;

        private transient RasterDataNode referenceRaster;

        public Model(RasterDataNode raster) {
            referenceRaster = raster;
            this.name = "TimeSeries_" + referenceRaster.getName();
        }

        public String getName() {
            return name;
        }

        public PropertyContainer createPropertyContainer() {
            final PropertyContainer propertyContainer = PropertyContainer.createObjectBacked(this);
            final PropertyDescriptor nameDescriptor = propertyContainer.getDescriptor(Model.PROPERTY_NAME_NAME);
            nameDescriptor.setValidator(new Validator() {
                @Override
                public void validateValue(Property property, Object value) throws ValidationException {
                    if (value instanceof String) {
                        String name = (String) value;
                        if (!Product.isValidNodeName(name)) {
                            throw new ValidationException("Name of time series is not valid");
                        }
                    }
                }
            });
            return propertyContainer;
        }
    }

}
