package org.mapfish.print.test.util;

import org.mapfish.print.attribute.Attribute;
import org.mapfish.print.attribute.NorthArrowAttribute;
import org.mapfish.print.attribute.ScalebarAttribute;
import org.mapfish.print.attribute.map.GenericMapAttribute;

/**
 * Support for testing attributes. This is in main jar because it might be needed across module
 * boundaries and that can be difficult if it is in testing jar.
 */
public final class AttributeTesting {

  private AttributeTesting() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * A few attributes will throw exceptions if not initialized this method can be called when an
   * attribute needs testing but the test is generic and does not necessarily want or need to know
   * the specific type of attribute and its properties.
   */
  public static void configureAttributeForTesting(final Attribute att) {
    if (att instanceof GenericMapAttribute genericMapAttribute) {
      genericMapAttribute.setWidth(500);
      genericMapAttribute.setHeight(500);
      genericMapAttribute.setMaxDpi(400.0);
    } else if (att instanceof ScalebarAttribute scalebarAttribute) {
      scalebarAttribute.setWidth(300);
      scalebarAttribute.setHeight(120);
    } else if (att instanceof NorthArrowAttribute northArrowAttribute) {
      northArrowAttribute.setSize(50);
    }
  }
}
