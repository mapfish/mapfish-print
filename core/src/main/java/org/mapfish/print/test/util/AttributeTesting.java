package org.mapfish.print.test.util;

import org.mapfish.print.attribute.Attribute;
import org.mapfish.print.attribute.NorthArrowAttribute;
import org.mapfish.print.attribute.ScalebarAttribute;
import org.mapfish.print.attribute.map.GenericMapAttribute;

/**
 * Support for testing attributes.  This is in main jar because it might be needed across module boundaries
 * and that can be difficult if it is in testing jar.
 * <p>
 * CHECKSTYLE:OFF
 */
public class AttributeTesting {
    /**
     * A few attributes will throw exceptions if not initialized this method can be called when an attribute
     * needs testing but the test is generic and does not necessarily want or need to know the specific type
     * of attribute and its properties.
     */
    public static void configureAttributeForTesting(Attribute att) {
        if (att instanceof GenericMapAttribute) {
            GenericMapAttribute genericMapAttribute = (GenericMapAttribute) att;
            genericMapAttribute.setWidth(500);
            genericMapAttribute.setHeight(500);
            genericMapAttribute.setMaxDpi(400.0);
        } else if (att instanceof ScalebarAttribute) {
            ScalebarAttribute scalebarAttribute = (ScalebarAttribute) att;
            scalebarAttribute.setWidth(300);
            scalebarAttribute.setHeight(120);
        } else if (att instanceof NorthArrowAttribute) {
            NorthArrowAttribute northArrowAttribute = (NorthArrowAttribute) att;
            northArrowAttribute.setSize(50);
        }
    }
}
