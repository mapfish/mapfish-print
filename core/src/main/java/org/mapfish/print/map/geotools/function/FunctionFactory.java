package org.mapfish.print.map.geotools.function;

import com.google.common.collect.Lists;
import org.geotools.filter.FunctionExpressionImpl;
import org.opengis.feature.type.Name;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A factory for building all the mapfish print functions.
 *
 * @author Jesse on 11/7/2014.
 */
public class FunctionFactory implements org.geotools.filter.FunctionFactory {
    private List<? extends FunctionExpressionImpl> functions = Lists.newArrayList(
            new MultiplicationFunction()
    );

    @Override
    public List<FunctionName> getFunctionNames() {
        return Lists.transform(this.functions, new com.google.common.base.Function<Function, FunctionName>() {
            @Nullable
            @Override
            public FunctionName apply(@Nonnull Function input) {
                return input.getFunctionName();
            }
        });
    }

    @Override
    public Function function(String name, List<Expression> args, Literal fallback) {
        for (FunctionExpressionImpl template : this.functions) {
            if (template.getName().equals(name)) {
                try {
                    final FunctionExpressionImpl function = template.getClass().newInstance();
                    function.setParameters(args);
                    return function;
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    @Override
    public Function function(Name name, List<Expression> args, Literal fallback) {
        return function(name.getLocalPart(), args, fallback);
    }
}
