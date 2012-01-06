package org.esa.beam.glob.ui.graph;

import com.bc.jexp.ParseException;
import org.esa.beam.glob.core.timeseries.datamodel.AxisMappingModel;
import org.junit.*;

import static org.junit.Assert.*;

public class TimeSeriesValidatorTest {

    private TimeSeriesValidator validator;
    private AxisMappingModel mappingModel;

    @Before
    public void setUp() throws Exception {
        validator = new TimeSeriesValidator();
        mappingModel = new AxisMappingModel();
        mappingModel.addRasterName("alias1", "raster1");
        mappingModel.addRasterName("alias2", "raster2");
        mappingModel.addRasterName("alias1", "raster3");
        mappingModel.addInsituName("alias1", "insitu1");
        mappingModel.addInsituName("alias1", "insitu2");
        mappingModel.addInsituName("alias2", "insitu3");
        validator.adaptTo("key1", mappingModel);
    }

    @Test
    public void testValidateWithDefaultExpression() throws Exception {
        assertTrue(validator.validate(Double.NaN, "insitu1", TimeSeriesType.INSITU));
    }

    @Test(expected = ParseException.class)
    public void testValidateWithWrongType() throws Exception {
        validator.setExpression("raster1", "raster1 > 5", TimeSeriesType.INSITU);
    }

    @Test
    public void testValidate() throws Exception {
        validator.setExpression("raster1", "r.raster1 > 5", TimeSeriesType.CURSOR);
        assertFalse(validator.validate(4.9, "raster1", TimeSeriesType.CURSOR));
        assertTrue(validator.validate(5.1, "raster1", TimeSeriesType.CURSOR));
    }

    @Test
    public void testThatValidatorIsCorrectlyInitialized() throws Exception {
        validator.setExpression("raster1", "r.raster1 > 5", TimeSeriesType.CURSOR);
    }

    @Test
    public void testRepeatedAdapting() throws Exception {
        validator.setExpression("raster1", "r.raster1 >5", TimeSeriesType.CURSOR);
        assertTrue( validator.validate(6, "raster1", TimeSeriesType.CURSOR));
        assertFalse(validator.validate(4, "raster1", TimeSeriesType.CURSOR));

        validator.adaptTo("key2", new AxisMappingModel());
        try {
            validator.setExpression("raster1", "r.raster1 > 5", TimeSeriesType.CURSOR);
            fail();
        } catch (ParseException expected) {

        }

        validator.adaptTo("key1", mappingModel);
        assertTrue( validator.validate(6, "raster1", TimeSeriesType.CURSOR));
        assertFalse(validator.validate(4, "raster1", TimeSeriesType.CURSOR));

        validator.setExpression("raster1", "r.raster1 < 3", TimeSeriesType.CURSOR);
        assertTrue( validator.validate(2, "raster1", TimeSeriesType.CURSOR));
        assertFalse(validator.validate(4, "raster1", TimeSeriesType.CURSOR));
    }
}
