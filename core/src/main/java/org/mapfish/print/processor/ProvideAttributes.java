package org.mapfish.print.processor;

import org.mapfish.print.attribute.Attribute;

import java.util.Map;

/**
 * Processor that provide attributes.
 */
public interface ProvideAttributes {
    /**
     * The provides attribute by the processor.
     *
     * @return the provided attributes
     */
    Map<String, Attribute> getAttributes();
}
