package org.mapfish.print.processor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Marks a processor input parameter as input output value. To be used it he modify the value.
 */
@Target(value = ElementType.FIELD)
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface InputOutputValue {
}
