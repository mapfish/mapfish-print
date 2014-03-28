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

import org.mapfish.print.config.ConfigurationObject;

import java.util.Map;
import javax.annotation.Nullable;

/**
 * Interface for processing input attributes.  A processor must <em>NOT</em> contain mutable state because a single processor
 * instance can be ran in multiple threads and one running processor must not interfere with the running of the other instance.
 *
 * @param <In>  A Java bean input parameter object of the execute method.
 *              Object is populated from the {@link org.mapfish.print.output.Values} object.
 * @author jesseeichar on 2/21/14.
 */
public interface Processor<In, Out> extends ConfigurationObject {

    Class<Out> getOutputType();

    /**
     * Map the variable names to the processor inputs.
     */
    @Nullable
    Map<String, String> getInputMapper();

    /**
     * Returns a <em>new/clean</em> instance of a parameter object.  This instance's will be inspected using reflection to
     * find its bean properties and the properties will be set from the {@link org.mapfish.print.output.Values} object.
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
     * set the value on the instance created by this method.
     * </li>
     * </ol>
     * <p/>
     * The populated instance will be passed to the execute method.  It is <em>imperative</em> that a new instance is created
     * each time because they will be used in a multi-threaded environment and thus the same processor instance may be ran
     * in multiple threads with different instances of the parameter object.
     */
    In createInputParameter();

    /**
     * Perform the process on the input attributes.
     *
     * @param values A Java bean whose properties are populated from the {@link org.mapfish.print.output.Values} object
     *               (which is used for transferring properties between processors).
     * @return a map of result
     */
    @Nullable
    Out execute(In values) throws Exception;

    /**
     * Map output from processor to the variable in the Jasper Report.
     */
    @Nullable
    Map<String, String> getOutputMapper();

}
