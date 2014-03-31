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

package org.mapfish.print.config;

/**
 * Represents an error made in the config.yaml file.
 *
 * @author Jesse on 3/30/14.
 */
public class ConfigurationException extends RuntimeException {
    private Configuration configuration;

    /**
     * Constructor.
     * @param message the error message.
     */
    public ConfigurationException(final String message) {
        super(message);
    }


    /**
     * Constructor.
     *
     * @param message the error message.
     * @param cause an exception that is the true cause of the error.
     */
    public ConfigurationException(final String message, final Throwable cause) {
        super(message, cause);
    }


    public final Configuration getConfiguration() {
        return this.configuration;
    }

    public final void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }
}
