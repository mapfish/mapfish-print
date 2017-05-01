package org.mapfish.print.processor;

import org.mapfish.print.attribute.Attribute;

import java.util.Map;

/**
 * Processor that provide attributes.
 */
public interface ProvideAttributes {
    Map<String, Attribute> getAttributes();
}
