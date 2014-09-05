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

import com.google.common.collect.Maps;
import org.mapfish.print.config.Template;
import org.mapfish.print.parser.MapfishParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * This attribute represents a collection of attributes which can be used as the data source of a Jasper report's
 * table/detail section.
 * <p>
 *     For example consider the case where the report should contain multiple tables or charts but the number of reports
 *     may change depending on the request.  In this case the client will post a datasource attribute json object containing an array
 *     of all the table attribute objects.  The {@link org.mapfish.print.processor.jasper.DataSourceProcessor} will process
 *     the datasource attribute and create a Jasper datasource that contains all the tables.
 * </p>
 * <p>
 *     This datasource must be used in tandem with the {@link org.mapfish.print.processor.jasper.DataSourceProcessor} processor.
 * </p>
 * <p>
 *     The json data of this attribute is special since it represents an array of attributes, each element in the array must
 *     be all of the attributes required to satisfy the processors in the {@link org.mapfish.print.processor.jasper.DataSourceProcessor}.
 * </p>
 * <p>
 * Example configuration:
 * <pre><code>
 * datasource: !datasource
 *   table: !table
 *   map: !map
 *     width: 200
 *     height: 100
 * </code></pre>
 * </p>
 * <p>
 * Example request data:
 * <pre><code>
 * datasource: [
 *   {
 *       table: {
 *           ... // normal table attribute data
 *       },
 *       map: {
 *           ... // normal map attribute data
 *       }
 *   }, {
 *       table: {
 *           ... // normal table attribute data
 *       },
 *       map: {
 *           ... // normal map attribute data
 *       }
 *   }
 * ]
 * </code></pre>
 * </p>
 *
 * @author Jesse on 9/5/2014.
 */
public final class DataSourceAttribute extends ReflectiveAttribute<DataSourceAttribute.DataSourceAttributeValue> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceAttribute.class);


    private Map<String, Attribute> attributes = Maps.newHashMap();

    /**
     * The attributes that are acceptable by this dataSource.  The format is the same as the template attributes section.
     *
     * @param attributes the attributes
     */
    public void setAttributes(final Map<String, Attribute> attributes) {
        for (Map.Entry<String, Attribute> entry : attributes.entrySet()) {
            Object attribute = entry.getValue();
            if (!(attribute instanceof Attribute)) {
                final String msg = "Attribute: '" + entry.getKey() + "' is not an attribute. It is a: " + attribute;
                LOGGER.error("Error setting the Attributes: " + msg);
                throw new IllegalArgumentException(msg);
            } else {
                ((Attribute) attribute).setConfigName(entry.getKey());
            }
        }
        this.attributes = attributes;
    }

    @Override
    protected Class<? extends DataSourceAttributeValue> getValueType() {
        return DataSourceAttributeValue.class;
    }

    @Override
    public DataSourceAttributeValue createValue(final Template template) {
        return new DataSourceAttributeValue();
    }

    @Override
    public void validate(final List<Throwable> validationErrors) {
        // no validation to be done
    }

    /**
     * The value class for the {@link org.mapfish.print.attribute.DataSourceAttribute}.
     */
    public static final class DataSourceAttributeValue {
        /**
         * The array of attribute data.  Each element in the array is the attribute data for one row in the resulting
         * datasource (as processed by {@link org.mapfish.print.processor.jasper.DataSourceProcessor})
         */
        public Map<String, Object>[] attributesValues;
    }
}
