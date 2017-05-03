package org.mapfish.print.processor;

import org.mapfish.print.attribute.Attribute;

/**
 * Processor that requires attribute.
 */
public interface RequireAttributes {
    /**
     * Set the attribute.
     *
     * @param name the attribute name
     * @param attribute the attribute
     */
    void setAttribute(String name, Attribute attribute);
}
