/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.processor;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.parser.ParserUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import javax.annotation.Nonnull;

/**
 * Basic functionality of a processor.  Mostly utility methods.
 *
 * @param <In>  A Java bean input parameter object of the execute method.
 *              Object is populated from the {@link org.mapfish.print.output.Values} object.
 * @param <Out> A Java bean output/return object from the execute method.
 *              properties will be put into the {@link org.mapfish.print.output.Values} object so other processor can access the values.
 * @author Jesse
 */
public abstract class AbstractProcessor<In, Out> implements Processor<In, Out> {
    private BiMap<String, String> inputMapper = HashBiMap.create();
    private BiMap<String, String> outputMapper = HashBiMap.create();

    private final Class<Out> outputType;
    private String outputPrefix;

    /**
     * Constructor.
     *
     * @param outputType the type of the output of this processor.  Used to calculate processor dependencies.
     */
    protected AbstractProcessor(final Class<Out> outputType) {
        this.outputType = outputType;
    }

    @Override
    public final Class<Out> getOutputType() {
        return this.outputType;
    }

    @Override
    @Nonnull
    public final BiMap<String, String> getInputMapperBiMap() {
        return this.inputMapper;
    }

    /**
     * The prefix to apply to each output value.  This provides a simple way to make all output values have unique values.
     * @param prefix the new prefix
     */
    public final void setOutputPrefix(final String prefix) {
       this.outputPrefix = prefix;
    }

    @Override
    public final String getOutputPrefix() {
       return this.outputPrefix;
    }

    /**
     * The input mapper.
     *
     * @param inputMapper the values.
     */
    public final void setInputMapper(@Nonnull final Map<String, String> inputMapper) {
        this.inputMapper.putAll(inputMapper);
    }

    @Nonnull
    @Override
    public final BiMap<String, String> getOutputMapperBiMap() {
        return this.outputMapper;
    }

    /**
     * The output mapper.
     *
     * @param outputMapper the values.
     */
    public final void setOutputMapper(@Nonnull final Map<String, String> outputMapper) {
        this.outputMapper.putAll(outputMapper);
    }

    @Override
    public final void validate(final List<Throwable> errors, final Configuration configuration) {
        final In inputParameter = createInputParameter();
        final Set<String> allInputAttributeNames;
        if (inputParameter != null) {
            allInputAttributeNames = ParserUtils.getAllAttributeNames(inputParameter.getClass());
        } else {
            allInputAttributeNames = Collections.emptySet();
        }
        for (String inputAttributeName : this.inputMapper.values()) {
            if (!allInputAttributeNames.contains(inputAttributeName)) {
                errors.add(new ConfigurationException(inputAttributeName + " is not defined in processor '" + this + "'.  Check for " +
                                                        "typos. Options are " + allInputAttributeNames));
            }
        }

        Set<String> allOutputAttributeNames = ParserUtils.getAllAttributeNames(getOutputType());
        for (String outputAttributeName : this.outputMapper.keySet()) {
            if (!allOutputAttributeNames.contains(outputAttributeName)) {
                errors.add(new ConfigurationException(outputAttributeName + " is not defined in processor '" + this + "' as an output " +
                                                        "attribute.  Check for typos. Options are " + allOutputAttributeNames));
            }
        }

        extraValidation(errors, configuration);
    }

    /**
     * Perform any extra validation a subclass may need to perform.
     * @param validationErrors a list to add errors to so that all validation errors are reported as one.
     * @param configuration the containing configuration
     */
    protected abstract void extraValidation(final List<Throwable> validationErrors, final Configuration configuration);

    /**
     * Checks if the print was canceled and throws a
     * {@link CancellationException} if so.
     * 
     * @param context the execution context
     * @throws CancellationException
     */
    protected final void checkCancelState(final ExecutionContext context) {
        if (context.isCanceled()) {
            throw new CancellationException("task was canceled");
        }
    }
    
    // CHECKSTYLE:OFF
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
    // CHECKSTYLE:ON
    
    /**
     * Default implementation of {@link ExecutionContext}.
     */
    public static final class Context implements ExecutionContext {

        private volatile boolean canceled = false;
        
        /**
         * Sets the canceled flag.
         */
        public void cancel() {
            this.canceled = true;
        }
        
        @Override
        public boolean isCanceled() {
            return this.canceled;
        }
    }
}
