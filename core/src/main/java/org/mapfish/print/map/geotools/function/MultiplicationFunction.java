package org.mapfish.print.map.geotools.function;

import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.opengis.filter.capability.FunctionName;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

/**
 * A Function that multiplies the two values.
 */
public final class MultiplicationFunction extends FunctionExpressionImpl {

    /**
     * The name of this function.
     */
    public static final FunctionName NAME = new FunctionNameImpl("multiplication",
                                                                 parameter("result", Double.class),
                                                                 parameter("value1", Double.class),
                                                                 parameter("value2", Double.class));

    /**
     * Default constructor.
     */
    public MultiplicationFunction() {
        super(NAME);
    }

    @Override
    public Object evaluate(final Object feature) {
        double value1;
        double value2;

        try { // attempt to get value and perform conversion
            value1 = (getExpression(0).evaluate(feature, Double.class));
        } catch (Exception e) {
            // probably a type error
            throw new IllegalArgumentException(
                    "Filter Function problem for function abs argument #0 - expected type double");
        }
        try { // attempt to get value and perform conversion
            value2 = (getExpression(1).evaluate(feature, Double.class));
        } catch (Exception e) {
            // probably a type error
            throw new IllegalArgumentException(
                    "Filter Function problem for function abs argument #1 - expected type double");
        }

        return value1 * value2;
    }
}
