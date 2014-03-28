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

import com.google.common.collect.Maps;

import java.util.Map;
import javax.annotation.Nonnull;

/**
 * Basic functionality of a processor.  Mostly utility methods.
 *
 * @author Jesse
 * @param <In>  A Java bean input parameter object of the execute method.
 *              Object is populated from the {@link org.mapfish.print.output.Values} object.
 * @param <Out> A Java bean output/return object from the execute method.
 *              properties will be put into the {@link org.mapfish.print.output.Values} object so other processor can access the values.
 */
public abstract class AbstractProcessor<In, Out> implements Processor<In, Out> {
    private Map<String, String> inputMapper = Maps.newHashMap();
    private Map<String, String> outputMapper = Maps.newHashMap();

    private final Class<Out> outputType;

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
    public final Map<String, String> getInputMapper() {
        return this.inputMapper;
    }

    public final void setInputMapper(@Nonnull final Map<String, String> inputMapper) {
        this.inputMapper = inputMapper;
    }

    @Nonnull
    @Override
    public final Map<String, String> getOutputMapper() {
        return this.outputMapper;
    }

    public final void setOutputMapper(@Nonnull final Map<String, String> outputMapper) {
        this.outputMapper = outputMapper;
    }

    // CHECKSTYLE:OFF
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
    // CHECKSTYLE:ON
}
