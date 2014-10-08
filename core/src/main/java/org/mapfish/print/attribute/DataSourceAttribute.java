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
import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.print.PrintException;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.parser.MapfishParser;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.PObject;
import org.mapfish.print.wrapper.yaml.PYamlArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
 *     contain all of the attributes required to satisfy the processors in the
 *     {@link org.mapfish.print.processor.jasper.DataSourceProcessor}.
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
public final class DataSourceAttribute implements Attribute {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceAttribute.class);

    private Map<String, Attribute> attributes = Maps.newHashMap();
    private String configName;
    private PYamlArray defaults;

    public void setDefault(final List<Object> defaultData) {
        this.defaults = new PYamlArray(null, defaultData, "dataSource");
    }

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
    public void printClientConfig(final JSONWriter json, final Template template) throws JSONException {
        try {
            json.key(ReflectiveAttribute.JSON_NAME).value(this.configName);
            json.key(ReflectiveAttribute.JSON_ATTRIBUTE_TYPE).value(DataSourceAttributeValue.class.getSimpleName());

            json.key(ReflectiveAttribute.JSON_CLIENT_PARAMS);
            json.object();
            json.key("attributes");
            json.array();
            for (Map.Entry<String, Attribute> entry : this.attributes.entrySet()) {
                Attribute attribute = entry.getValue();
                if (attribute.getClass().getAnnotation(InternalAttribute.class) == null) {
                    json.object();
                    json.key("name").value(entry.getKey());
                    attribute.printClientConfig(json, template);
                    json.endObject();
                }
            }
            json.endArray();
            json.endObject();

        } catch (Throwable e) {
            // Note: If this test fails and you just added a new attribute, make
            // sure to set defaults in AbstractMapfishSpringTest.configureAttributeForTesting
            throw new Error("Error printing the clientConfig of: " + DataSourceAttribute.class.getName(), e);
        }
    }

    @Override
    public void setConfigName(final String name) {
        this.configName = name;
    }

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        // no validation to be done
    }

    /**
     * Parser the attributes into the value object.
     * @param parser the parser
     * @param template the containing template
     * @param jsonValue the json
     */
    @SuppressWarnings("unchecked")
    public DataSourceAttributeValue parseAttribute(@Nonnull final MapfishParser parser,
                                                   @Nonnull final Template template,
                                                   @Nullable final PArray jsonValue) throws JSONException {
        final PArray pValue;

        if (jsonValue != null) {
            pValue = jsonValue;
        } else {
            pValue = this.defaults;
        }

        if (pValue == null) {
            throw new PrintException("Missing required attribute: " + this.configName);
        }

        final DataSourceAttributeValue value = new DataSourceAttributeValue();
        value.attributesValues = new Map[pValue.size()];
        for (int i = 0; i < pValue.size(); i++) {
            PObject rowData = pValue.getObject(i);
            final Values valuesForParsing = new Values();
            valuesForParsing.populateFromAttributes(template, parser, this.attributes, rowData);
            value.attributesValues[i] = valuesForParsing.asMap();
        }

        return value;
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
