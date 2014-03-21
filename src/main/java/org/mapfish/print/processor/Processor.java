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

/**
 * Interface for processing input attributes
 * Created by Jesse on 2/21/14.
 */
public interface Processor extends ConfigurationObject {

    /**
     * Map the variable names to the processor inputs.
     */
    Map<String, String> getInputMapper();

    /**
     * @param values Actual values from attributes and the previous processor.
     * @return An id of the value for lookup in the output mapper?
     * @throws Exception
     */
    Map<String, Object> execute(Map<String, Object> values) throws Exception;

    /**
     * Map output from processor to the variable in the Jasper Report.
     */
    Map<String, String> getOutputMapper();
}
