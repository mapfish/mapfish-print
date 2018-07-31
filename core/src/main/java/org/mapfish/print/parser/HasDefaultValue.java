package org.mapfish.print.parser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Marks a Processor input parameter object setter method as being optional.  If there is no value for the
 * property then no error will be thrown when populating the method in {@link
 * org.mapfish.print.processor.ProcessorUtils#populateInputParameter(org.mapfish.print.processor.Processor,
 * org.mapfish.print.output.Values)}
 */
@Target(value = ElementType.FIELD)
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface HasDefaultValue {
}
