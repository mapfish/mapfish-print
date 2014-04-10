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

package org.mapfish.print.attribute;

import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.print.config.ConfigurationObject;
import org.mapfish.print.config.Template;

/**
 * Represents an attribute passed in from a web-client to be used to populate the report.  It reads a value from the request data
 * <p/>
 *
 * @author jesseeichar on 2/21/14.
 */
public interface Attribute extends ConfigurationObject {

    /**
     * Write this attribute out the the json writer so that clients can know what attributes are expected.
     *
     * @param json the json writer to write to
     * @param template the template that this attribute is part of
     * @throws JSONException
     */
    void printClientConfig(JSONWriter json, Template template) throws JSONException;
}
