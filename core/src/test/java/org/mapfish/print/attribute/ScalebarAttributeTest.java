package org.mapfish.print.attribute;

import com.google.common.collect.Lists;

import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.attribute.ScalebarAttribute.ScalebarAttributeValues;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.parser.MapfishParser;
import org.mapfish.print.processor.map.scalebar.Type;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.Dimension;
import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ScalebarAttributeTest extends AbstractMapfishSpringTest {

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private TestHttpClientFactory httpRequestFactory;

    @Test
    public void testValidate() {
        ScalebarAttribute attribute = new ScalebarAttribute();
        List<Throwable> validationErrors = Lists.newArrayList();
        Configuration configuration = new Configuration();
        attribute.validate(validationErrors, configuration);
        // errors: width and height is not set
        assertEquals(2, validationErrors.size());
    }

    @Test
    public void testCreateValue() throws Exception {
        ScalebarAttributeValues values = getValues();
        assertEquals(new Dimension(300, 120), values.getSize());
    }

    @Test
    public void testPostConstruct() throws Exception {
        ScalebarAttributeValues values = getValues();
        values.postConstruct();
        // passes.. ok
    }

    @Test(expected=IllegalArgumentException.class)
    public void testPostConstructInvalidColor() throws Exception {
        ScalebarAttributeValues values = getValues();

        values.backgroundColor = "sun-yellow";
        values.postConstruct();
    }

    @Test(expected=IllegalArgumentException.class)
    public void testPostConstructInvalidUnit() throws Exception {
        ScalebarAttributeValues values = getValues();

        values.unit = "light-years";
        values.postConstruct();
    }

    @Test(expected=IllegalArgumentException.class)
    public void testPostConstructInvalidIntervals() throws Exception {
        ScalebarAttributeValues values = getValues();

        values.intervals = 1;
        values.postConstruct();
    }

    @Test
    public void testAttributesFromJson() throws Exception {
        final File configFile = getFile(ScalebarAttributeTest.class, "scalebar/config.yaml");
        final Configuration config = configurationFactory.getConfig(configFile);
        final Template template = config.getTemplate("main");
        final PJsonObject pJsonObject = parseJSONObjectFromFile(ScalebarAttributeTest.class, "scalebar/requestData.json");
        final Values values = new Values(pJsonObject, template, new MapfishParser(), getTaskDirectory(), this.httpRequestFactory,
                new File("."));
        final ScalebarAttribute.ScalebarAttributeValues value = values.getObject("scalebar", ScalebarAttribute.ScalebarAttributeValues.class);

        assertEquals(Type.LINE.getLabel(), value.type);
        assertEquals("m", value.unit);
        assertTrue(value.geodetic);
        assertTrue(value.lockUnits);
        assertEquals("Liberation Sans", value.font);
        assertEquals("#cccccc", value.fontColor);
    }

    private ScalebarAttributeValues getValues() {
        ScalebarAttribute attribute = new ScalebarAttribute();
        attribute.setWidth(300);
        attribute.setHeight(120);
        ScalebarAttributeValues values = attribute.createValue(null);
        return values;
    }
}
