package org.mapfish.print.parser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated field or one of the others in the same OneOf group is required.
 * @author Jesse on 4/9/2014.
 * @see org.mapfish.print.parser.CanSatisfyOneOf
 */
@Target(value = ElementType.FIELD)
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface OneOf {
    /**
     * The choice group id.  One of the options in the choice group must be present in the parsed JSON.
     */
    String value();
}
