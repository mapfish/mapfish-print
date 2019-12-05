package org.mapfish.print.parser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated field or can satisfy the {@link org.mapfish.print.parser.OneOf} requirements
 * or can co-exist with that requirement.
 *
 * If this annotation is present then {@link org.mapfish.print.parser.HasDefaultValue} is not required.
 *
 * @see org.mapfish.print.parser.OneOf
 */
@Target(value = ElementType.FIELD)
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface CanSatisfyOneOf {
    /**
     * The choice group id.  One of the options in the choice group must be present in the parsed JSON.
     */
    String value();
}
