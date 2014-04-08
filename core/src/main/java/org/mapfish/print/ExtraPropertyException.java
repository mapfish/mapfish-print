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

package org.mapfish.print;

import java.util.Collection;
import java.util.Set;

/**
 * Indicates one or more properties are not used either in a config.yaml configuration file or in the request json.
 * @author Jesse on 4/2/14.
 */
public final class ExtraPropertyException extends RuntimeException {
    private final Collection<String> extraProperties;
    private final Set<String> attributeNames;

    /**
     * Constructor.
     *
     * @param message the error message. A textual description of the extra properties. List of names will be appended at the end
     * @param extraProperties the properties that are extra
     * @param attributeNames all the allowed attribute names.
     */
    public ExtraPropertyException(final String message, final Collection<String> extraProperties, final Set<String> attributeNames) {
        super(createMessage(message, extraProperties, attributeNames));
        this.extraProperties = extraProperties;
        this.attributeNames = attributeNames;
    }

    private static String createMessage(final String message, final Collection<String> extraProperties,
                                        final Set<String> attributeNames) {
        StringBuilder missingPropertyMessage = new StringBuilder(message).append("\n").append("Extra Properties: \n");
        for (String extraProperty : extraProperties) {
            missingPropertyMessage.append("\n\t* ").append(extraProperty);
        }
        missingPropertyMessage.append("\n\nAll allowed properties are: \n");
        for (String attributeName : attributeNames) {
            missingPropertyMessage.append("\n\t* ").append(attributeName);
        }
        return missingPropertyMessage.toString();
    }

    public Collection<String> getExtraProperties() {
        return this.extraProperties;
    }

    public Set<String> getAttributeNames() {
        return this.attributeNames;
    }
}
