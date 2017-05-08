package org.mapfish.print.attribute.map;

import org.mapfish.print.attribute.AbstractAttributeTest;
import org.mapfish.print.attribute.Attribute;

public class BasicMapAttributeTest extends AbstractAttributeTest {

    @Override
    protected Attribute createAttribute() {
        final MapAttribute mapAttribute = new MapAttribute();
        mapAttribute.setHeight(123);
        mapAttribute.setWidth(321);
        mapAttribute.setMaxDpi(1232.0);
        return mapAttribute;
    }
}
