package org.mapfish.print.processor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Marks a processor output parameter as debug value. If no mapping is defined for this value, and if there
 * would be a conflict with an other value that has the same name, a mapping is created automatically with a
 * random name.
 */
@Target(value = ElementType.FIELD)
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface InternalValue {
}
