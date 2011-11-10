package org.esa.beam.glob.ui.assistant;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.ui.assistant.AssistantPage;
import org.esa.beam.glob.core.timeseries.datamodel.ProductLocation;
import org.esa.beam.glob.ui.ProductLocationsPaneModel;

import javax.swing.JLabel;
import java.awt.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TimeSeriesAssistantPage_ReprojectingSources extends AbstractTimeSeriesAssistantPage {

    TimeSeriesAssistantPage_ReprojectingSources(TimeSeriesAssistantModel model) {
        super("Reproject Source Products", model);
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    public boolean canHelp() {
        // @todo
        return false;
    }

    @Override
    public boolean validatePage() {
        return super.validatePage();  //Todo change body of created method. Use File | Settings | File Templates to change
    }

    @Override
    public boolean hasNextPage() {
        return true;
    }

    @Override
    public AssistantPage getNextPage() {
        reprojectSourceProducts();
        return new TimeSeriesAssistantPage_VariableSelection(getAssistantModel());
    }

    private void reprojectSourceProducts() {
        final ProductLocationsPaneModel productLocationsModel = getAssistantModel().getProductLocationsModel();
        final List<ProductLocation> productLocations = productLocationsModel.getProductLocations();
        for (ProductLocation productLocation : productLocations) {
            final Map<String, Product> products = productLocation.getProducts();
            final Product crsReferenceProduct = getCrsReferenceProduct();
            for (Map.Entry<String, Product> productEntry : products.entrySet()) {
                final Product product = productEntry.getValue();
                if (!product.isCompatibleProduct(crsReferenceProduct, 0.1E-4f)) {
                    Product reprojectedProduct = createProjectedProduct(product, crsReferenceProduct);
                    productEntry.setValue(reprojectedProduct);
                }
            }
        }
    }

    private Product createProjectedProduct(Product toReproject, Product crsReference) {
        final Map<String, Product> productMap = getProductMap(toReproject, crsReference);
        final Map<String, Object> parameterMap = new HashMap<String, Object>();
        parameterMap.put("resamplingName", "Nearest");
        parameterMap.put("includeTiePointGrids", false);
        parameterMap.put("addDeltaBands", false);
        // @todo - generalise
        final Product reprojectedProduct = GPF.createProduct("Reproject", parameterMap, productMap);
        reprojectedProduct.setStartTime(toReproject.getStartTime());
        reprojectedProduct.setEndTime(toReproject.getEndTime());
        return reprojectedProduct;
    }


    private Map<String, Product> getProductMap(Product product, Product crsReference) {
        final Map<String, Product> productMap = new HashMap<String, Product>(2);
        productMap.put("source", product);
        productMap.put("collocateWith", crsReference);
        return productMap;
    }


    @Override
    protected Component createPageComponent() {
        return new JLabel("Da kommt noch was");
    }

    private Product getCrsReferenceProduct() {
        final List<ProductLocation> productLocations = getAssistantModel().getProductLocationsModel().getProductLocations();
        for (ProductLocation productLocation : productLocations) {
            for (Product product : productLocation.getProducts().values()) {
                if(product != null) {
                    return product;
                }
            }
        }
        return null;
    }
}
