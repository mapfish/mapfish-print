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
import org.mapfish.print.config.ConfigurationObject;

import javax.annotation.Nullable;

/**
 * Interface for processing input attributes.  A processor must <em>NOT</em> contain mutable state because a single processor
 * instance can be ran in multiple threads and one running processor must not interfere with the running of the other instance.
 *
 * @param <In>  A Java DTO input parameter object of the execute method.  The object properties are resolved by looking at the
 *              public fields in the object and setting those fields.  Only fields in the object itself will be inspected.
 *              Object is populated from the {@link org.mapfish.print.output.Values} object.
 * @param <Out> A Java DTO output/return object from the execute method.
 *             properties will be put into the {@link org.mapfish.print.output.Values} object so other processor can access the values.
 *
 * @author jesseeichar on 2/21/14.
 */
public interface Processor<In, Out> extends ConfigurationObject {

    /**
     * Get the class of the output type.  This is used when determining the outputs this processor produces.
     * <p/>
     * The <em>public fields</em> of the Processor will be the output of the processor and thus can be mapped to inputs
     * of another processor.
     */
    Class<Out> getOutputType();

    /**
     * Map the variable names to the processor inputs.
     */
    @Nullable
    BiMap<String, String> getInputMapperBiMap();

    /**
     * Returns a <em>new/clean</em> instance of a parameter object.  This instance's will be inspected using reflection to
     * find its public fields and the properties will be set from the {@link org.mapfish.print.output.Values} object.
     * <p/>
     * The way the properties will be looked up is to
     * <ol>
     * <li>
     * take the bean property name
     * </li>
     * <li>
     * map it using the input mapper, (if the input mapper does not have a mapping for the property then the unmapped
     * property name is used)
     * </li>
     * <li>
     * Look up the property value in the {@link org.mapfish.print.output.Values} object using the mapped property name
     * </li>
     * <li>
     * set the value on the instance created by this method.  If the value is null an exception will be thrown <em>UNLESS</em>
     * the {@link org.mapfish.print.parser.HasDefaultValue} annotation is on the field for the property.
     * </li>
     * </ol>
     * <p/>
     * The populated instance will be passed to the execute method.  It is <em>imperative</em> that a new instance is created
     * each time because they will be used in a multi-threaded environment and thus the same processor instance may be ran
     * in multiple threads with different instances of the parameter object.
     * <p/>
     * It is important to realize that super classes will also be analyzed, so care must be had with inheritance.
     */
    @Nullable
    In createInputParameter();

    /**
     * Perform the process on the input attributes.
     *
     * @param values A Java object whose <em>public fields</em> are populated from the {@link org.mapfish.print.output.Values} object
     *               (which is used for transferring properties between processors).
     * @param context The execution context for a print task.
     * @return A Java object whose <em>public fields</em> will be put into the {@link org.mapfish.print.output.Values} object.  The
     *         key in the {@link org.mapfish.print.output.Values} object is the name of the field or if there is a mapping in the
     *         {@link #getOutputMapperBiMap()} map, the mapped name.  The key is determined in a similar way as for the input object.
     */
    @Nullable
    Out execute(In values, ExecutionContext context) throws Exception;

    /**
     * Map output from processor to the variable in the Jasper Report.
     */
    @Nullable
    BiMap<String, String> getOutputMapperBiMap();

    /**
     * Get the prefix to apply to each output value.  This provides a simple way to make all output values have unique values.
     * <p>
     *     If output prefix is non-null and non-empty (whitespace is removed) then the prefix will be prepended to the normal name
     *     of the output value.
     * </p>
     * <p>
     *     When a prefix is appended the normal name will be capitalized.  For example: if the normal name is: <em>map</em>
     *     and the prefix is <em>page1</em> then the final name will be <em>page1Map</em>.
     * </p>
     * <p>
     *     Note: If a mapping is in the {@link #getOutputMapperBiMap()} then the prefix will be ignored for that value and the
     *     un-prefixed name from the output mapper will be used directly.
     * </p>
     * <p>
     *     Note: If a prefix has white space at the start or end it will be removed.
     * </p>
     */
    String getOutputPrefix();

    /**
     * An execution context for a specific print task. 
     */
    public interface ExecutionContext {
        /**
         * @return Was the print task canceled?
         */
        boolean isCanceled();
    }
}
