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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.PObject;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Attribute representing the headers from the request.
 *
 * This is an internal attribute and is added to the system automatically.  It does not need to
 * be added in the config.yaml file.
 *
 * @author Jesse on 6/26/2014.
 */
@InternalAttribute
public final class HttpRequestHeadersAttribute extends ReflectiveAttribute<HttpRequestHeadersAttribute.Value> {
    /**
     * Constructor that calls init.
     */
    public HttpRequestHeadersAttribute() {
        init();
    }

    @Override
    protected Class<Value> getValueType() {
        return Value.class;
    }

    @Override
    public Value createValue(final Template template) {
        return new Value();
    }

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        // nothing to do
    }

    /**
     * The object containing the attribute data.
     */
    public static final class Value {
        /**
         * The headers from the request.
         */
        public PObject requestHeaders;

        /**
         * Get all the headers in map form.
         */
        public Map<String, List<String>> getHeaders() {
            Map<String, List<String>> headerMap = Maps.newHashMap();

            final Iterator<String> keys = this.requestHeaders.keys();

            while (keys.hasNext()) {
                List<String> valuesAsList = Lists.newArrayList();

                String headerName = keys.next();
                final PArray values = this.requestHeaders.optArray(headerName);
                if (values != null) {
                    for (int i = 0; i < values.size(); i++) {
                        valuesAsList.add(values.getString(i));
                    }
                } else {
                    valuesAsList.add(this.requestHeaders.getString(headerName));
                }

                headerMap.put(headerName, valuesAsList);
            }

            return headerMap;
        }
    }
}
