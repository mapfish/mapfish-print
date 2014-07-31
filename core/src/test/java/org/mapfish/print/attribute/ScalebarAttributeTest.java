package org.mapfish.print.attribute;

import static org.junit.Assert.*;

import java.awt.Dimension;
import java.util.List;

import org.junit.Test;
import org.mapfish.print.attribute.ScalebarAttribute.ScalebarAttributeValues;

import com.google.common.collect.Lists;

public class ScalebarAttributeTest {

    @Test
    public void testValidate() {
        ScalebarAttribute attribute = new ScalebarAttribute();
        List<Throwable> validationErrors = Lists.newArrayList();
        attribute.validate(validationErrors);
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

    private ScalebarAttributeValues getValues() {
        ScalebarAttribute attribute = new ScalebarAttribute();
        attribute.setWidth(300);
        attribute.setHeight(120);
        ScalebarAttributeValues values = attribute.createValue(null);
        return values;
    }
}
