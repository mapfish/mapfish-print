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
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;

import java.util.List;

/**
 * A type of attribute whose value is a primitive type.
 * <ul>
 * <li>{@link java.lang.String}</li>
 * <li>{@link java.lang.Integer}</li>
 * <li>{@link java.lang.Float}</li>
 * <li>{@link java.lang.Double}</li>
 * <li>{@link java.lang.Short}</li>
 * <li>{@link java.lang.Boolean}</li>
 * <li>{@link java.lang.Character}</li>
 * <li>{@link java.lang.Byte}</li>
 * <li>{@link java.lang.Enum}</li>
 * </ul>
 *
 * @param <Value> The value type of the attribute
 * @author Jesse on 4/9/2014.
 */
public abstract class PrimitiveAttribute<Value> implements Attribute {
    private Class<Value> valueClass;
    private Value defaultValue;

    private String configName;
    /**
     * Constructor.
     *
     * @param valueClass the type of the value of this attribute
     */
    public PrimitiveAttribute(final Class<Value> valueClass) {
        this.valueClass = valueClass;
    }

    public final Class<Value> getValueClass() {
        return this.valueClass;
    }

    public final void setDefault(final Value value) {
        this.defaultValue = value;
    }

    public final Value getDefault() {
        return this.defaultValue;
    }

    @Override
    public final void setConfigName(final String configName) {
        this.configName = configName;
    }

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        // no checks required
    }

    @Override
    public final void printClientConfig(final JSONWriter json, final Template template) throws JSONException {
        json.key(ReflectiveAttribute.JSON_NAME).value(this.configName);
        json.key(ReflectiveAttribute.JSON_ATTRIBUTE_TYPE).value(clientConfigTypeDescription());
    }

    /**
     * Returns a string that is a technical description of the type.  In other words, a string that the client software
     * (user of the capabilities response) can use to create a request or UI.
     * CSOFF: DesignForExtension
     */
    protected String clientConfigTypeDescription() {
        //CSON: DesignForExtension
        return this.valueClass.getSimpleName();
    }
}
