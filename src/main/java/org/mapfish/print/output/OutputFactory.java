/*
 * Copyright (C) 2013  Camptocamp
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

package org.mapfish.print.output;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.mapfish.print.config.Config;
import org.mapfish.print.utils.PJsonObject;
import org.springframework.beans.factory.annotation.Required;

/**
 * User: jeichar
 * Date: Oct 18, 2010
 * Time: 2:07:54 PM
 */
public class OutputFactory {
    private static final Logger LOGGER = Logger.getLogger(OutputFactory.class);
    private List<OutputFormatFactory> formatFactories = new ArrayList<OutputFormatFactory>();

    /**
     * For spring dependency injection
     *
     * @param formatFactories
     */
    @Required
    public void setFormatFactories(List<OutputFormatFactory> formatFactories) {
        this.formatFactories = formatFactories;
    }

    public OutputFormat create(Config config, PJsonObject spec) {
        String id = spec.optString("outputFormat", "pdf");

        for (OutputFormatFactory formatFactory : formatFactories) {
            String enablementMsg = formatFactory.enablementStatus();
            if(enablementMsg == null) {
                for (String supportedFormat : formatFactory.formats()) {
                    if(permitted(supportedFormat, config) && supportedFormat.equalsIgnoreCase(id)) {
                        final OutputFormat outputFormat = formatFactory.create(id);
                        LOGGER.info("OutputFormat chosen for " + id + " is " + (outputFormat.getClass().getSimpleName()));
                        return outputFormat;
                    }
                }
            } else {
                LOGGER.warn("OutputFormatFactory " + (formatFactory.getClass().getName()) + " is disabled: " + enablementMsg);
            }
        }

        if (id.equalsIgnoreCase("pdf")) {
            throw new Error("There must be a format that can output PDF");
        } else {
            StringBuilder allFormats = new StringBuilder();
            for (String format : getSupportedFormats(config)) {
                if(allFormats.length() > 0) allFormats.append(", ");
                allFormats.append(format.toLowerCase());
            }

            throw new IllegalArgumentException(id + " is not a supported format. Supported formats: "+allFormats);
        }
    }

    public Set<String> getSupportedFormats(Config config) {
        Set<String> supported = new HashSet<String>();
        for (OutputFormatFactory formatFactory : formatFactories) {
            if(formatFactory.enablementStatus() == null) {
                for (String format : formatFactory.formats()) {
                    if(permitted(format, config)) {
                        supported.add(format.toLowerCase());
                    }
                }
            }
        }

        return supported;
    }

    private boolean permitted(String supportedFormat, Config config) {
        TreeSet<String> configuredFormats = config.getFormats();
        if(configuredFormats.size() == 1 && configuredFormats.iterator().next().trim().equals("*")) {
            return true;
        }

        if(configuredFormats.isEmpty()) {
            return "pdf".equalsIgnoreCase(supportedFormat);
        }

        for (String configuredFormat : configuredFormats) {
            if(configuredFormat.equalsIgnoreCase(supportedFormat)) return true;
        }

        return false;
    }

}
