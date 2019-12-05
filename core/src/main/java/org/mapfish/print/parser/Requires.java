package org.mapfish.print.parser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicates that if one field in a value/param object, then one or more other attributes are required.
 *
 * Note: If the field with the {@link org.mapfish.print.parser.Requires} annotation is NOT in the json then
 * the required are not required as long as they have the {@link org.mapfish.print.parser.HasDefaultValue}
 * annotation.
 */
@Target(value = ElementType.FIELD)
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Requires {
    /**
     * The names of the required fields if this field is present.
     */
    String[] value();
}
