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

import org.mapfish.print.output.Values;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Basic functionality of a processor.  Mostly utility methods.
 * 
 * @author Jesse
 */
public abstract class AbstractProcessor implements Processor {
    private Map<String, String> outputMapper;

    //    private static final Pattern FULL_VARIABLE_REGEXP = Pattern.compile("^\\$\\{([^}]+)\\}$");
    private static final Pattern VARIABLE_REGEXP = Pattern.compile("\\$\\{([^}]+)\\}");

    /*
        Integer getInteger(String config, Values values) {
            Matcher matcher = FULL_VARIABLE_REGEXP.matcher(config);
            if (matcher.find()) {
                return values.getInteger(matcher.group(1));
            }
            else {
                return Integer.parseInt(config);
            }
        }

        Double getDouble(String config, Values values) {
            Matcher matcher = FULL_VARIABLE_REGEXP.matcher(config);
            if (matcher.find()) {
                return values.getDouble(matcher.group(1));
            }
            else {
                return Double.parseDouble(config);
            }
        }

        Object getObject(String config, Values values) {
            Matcher matcher = FULL_VARIABLE_REGEXP.matcher(config);
            if (matcher.find()) {
                return values.getDouble(matcher.group(1));
            }
            else {
                return config;
            }
        }
    */
    final String getString(final String config, final Values values) {
        String actualValue = config;
        StringBuffer result = new StringBuffer();
        while (true) {
            Matcher matcher = VARIABLE_REGEXP.matcher(actualValue);
            if (matcher.find()) {
                result.append(actualValue.substring(0, matcher.start()));
                result.append(values.getString(matcher.group(1)));
                actualValue = actualValue.substring(matcher.end());
            } else {
                break;
            }
        }
        return result.toString();
    }

    public final Map<String, String> getOutputMapper() {
        return this.outputMapper;
    }

    public final void setOutputMapper(final Map<String, String> outputMapper) {
        this.outputMapper = outputMapper;
    }
}
