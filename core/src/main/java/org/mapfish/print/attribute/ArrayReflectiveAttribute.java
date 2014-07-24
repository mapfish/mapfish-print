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
import org.mapfish.print.config.Template;
import org.mapfish.print.wrapper.yaml.PYamlArray;

import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;

/**
 * An attribute which is essentially a ReflectiveAttribute but rather than representing a single object it represents
 * an array of objects.
 *
 * @author Jesse on 4/10/2014.
 * @param <Value> the type of each element in the array
 */
public abstract class ArrayReflectiveAttribute<Value> implements Attribute {

    private volatile ReflectiveAttribute<Value> delegate;
    private PYamlArray defaults;
    private String configName;

    @PostConstruct
    private void init() {
        if (this.defaults == null) {
            this.defaults = new PYamlArray(null, Collections.<Object>emptyList(), this.configName);
        }
    }

    /**
     * The YAML config default values.
     *
     * @return the default values
     */
    public final PYamlArray getDefaultValue() {
        return this.defaults;
    }

    public final void setDefault(final List<Object> defaultValue) {
        this.defaults = new PYamlArray(null, defaultValue, this.configName);
    }

    @Override
    public final void setConfigName(final String configName) {
        this.configName = configName;
    }

    /**
     * Create an instance for each element in the array.
     * See {@link ReflectiveAttribute#createValue(org.mapfish.print.config.Template)} for
     * details on how the mechanism works.
     *
     * @param template the template for this attribute.
     */
    public abstract Value createValue(final Template template);

    @Override
    public final void printClientConfig(final JSONWriter json, final Template template) throws JSONException {
        getReflectiveAttribute().printClientConfig(json, template);
    }

    private ReflectiveAttribute<Value> getReflectiveAttribute() {
        if (this.delegate == null) {
            synchronized (this) {
                if (this.delegate == null) {
                    this.delegate = new ReflectiveAttribute<Value>() {

                        @Override
                        protected Class<? extends Value> getValueType() {
                            return ArrayReflectiveAttribute.this.delegate.getValueType();
                        }

                        @Override
                        public Value createValue(final Template template) {
                            return ArrayReflectiveAttribute.this.createValue(template);
                        }

                        @Override
                        public void validate(final List<Throwable> validationErrors) {
                            ArrayReflectiveAttribute.this.validate(validationErrors);
                        }
                    };
                }
            }
        }

        return this.delegate;
    }
}
