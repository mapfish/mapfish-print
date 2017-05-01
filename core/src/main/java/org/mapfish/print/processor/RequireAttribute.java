package org.mapfish.print.processor;

import org.mapfish.print.attribute.Attribute;

/**
 * Processor that requires attribute.
 */
public interface RequireAttribute {
    /**
     * Set the attribute
     */
    void setAttribute(String name, Attribute attribute);
}
