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

import java.util.Map;
import java.util.Set;

/**
 * Indicates one or more properties are missing either from a config.yaml configuration file or from request json.
 * @author Jesse on 4/2/14.
 */
public final class MissingPropertyException extends RuntimeException {
    private final Map<String, Class<?>> missingProperties;
    private final Set<String> attributeNames;

    /**
     * Constructor.
     *
     * @param message the error message. A textual description of the missing properties (type and name) will be appended at the end.
     * @param missingProperties the properties that are missing
     * @param attributeNames all the allowed attribute names.
     */
    public MissingPropertyException(final String message, final Map<String, Class<?>> missingProperties, final Set<String> attributeNames) {
        super(createMessage(message, missingProperties, attributeNames));
        this.missingProperties = missingProperties;
        this.attributeNames = attributeNames;
    }

    private static String createMessage(final String message, final Map<String, Class<?>> missingProperties,
                                        final Set<String> attributeNames) {
        StringBuilder missingPropertyMessage = new StringBuilder(message).append("\n");
        for (Map.Entry<String, Class<?>> entry : missingProperties.entrySet()) {
            String type = entry.getValue().getName();
            if (entry.getValue().isArray()) {
                type = entry.getValue().getComponentType().getName() + "[]";
            }
            missingPropertyMessage.append("\n\t* ").append(type).append(" ").append(entry.getKey());
        }
        missingPropertyMessage.append("\n\nAll allowed properties are: \n");
        for (String attributeName : attributeNames) {
            missingPropertyMessage.append("\n\t* ").append(attributeName);
        }
        return missingPropertyMessage.toString();
    }

    public Map<String, Class<?>> getMissingProperties() {
        return this.missingProperties;
    }

    public Set<String> getAttributeNames() {
        return this.attributeNames;
    }
}
