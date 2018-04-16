package org.mapfish.print.attribute;

import com.google.common.collect.Maps;
import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.print.PrintException;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
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
 * <p>
 *     This attribute represents a collection of attributes which can be used as the data source of a Jasper report's
 *     table/detail section.
 * </p>
 * <p>
 *     For example consider the case where the report should contain multiple tables or charts but the number of reports
 *     may change depending on the request.  In this case the client will post a datasource attribute json object containing an array
 *     of all the table attribute objects.  The {@link org.mapfish.print.processor.jasper.DataSourceProcessor} will process
 *     the datasource attribute and create a Jasper datasource that contains all the tables.
 * </p>
 * <p>
 *     This datasource must be used in tandem with the {@link org.mapfish.print.processor.jasper.DataSourceProcessor} processor
 *     (see <a href="processors.html#!createDataSource">!createDataSource</a> processor).
 * </p>
 * <p>
 *     The json data of this attribute is special since it represents an array of attributes, each element in the array must
 *     contain all of the attributes required to satisfy the processors in the
 *     {@link org.mapfish.print.processor.jasper.DataSourceProcessor}.
 * </p>
 * <p>
 * Example configuration:
 * </p>
 * <pre><code>
 * datasource: !datasource
 *   table: !table
 *   map: !map
 *     width: 200
 *     height: 100
 * </code></pre>
 * <p>
 * Example request data:
 * </p>
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
 * [[examples=verboseExample,datasource_dynamic_tables,datasource_many_dynamictables_legend,
 * datasource_multiple_maps,customDynamicReport,report]]
 */
public final class DataSourceAttribute implements Attribute {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceAttribute.class);

    private Map<String, Attribute> attributes = Maps.newHashMap();
    private String configName;
    private PYamlArray defaults;

    /**
     * <p>Default values for this attribute. Example:</p>
     * <pre><code>
     *     attributes:
     *       datasource: !datasource
     *         attributes:
     *           name: !string {}
     *           count: !integer {}
     *         default:
     *           - name: "name"
     *           - count: 3</code></pre>
     * @param defaultData The default values.
     */
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
                LOGGER.error("Error setting the Attributes: {}", msg);
                throw new IllegalArgumentException(msg);
            } else {
                ((Attribute) attribute).setConfigName(entry.getKey());
            }
        }
        this.attributes = attributes;
    }

    /**
     * Gets the attributes.
     *
     * @return the attributes
     */
    public Map<String, Attribute> getAttributes() {
        return this.attributes;
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
     * @param template the containing template
     * @param jsonValue the json
     */
    @SuppressWarnings("unchecked")
    public DataSourceAttributeValue parseAttribute(@Nonnull final Template template,
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
            valuesForParsing.populateFromAttributes(template, this.attributes, rowData);
            value.attributesValues[i] = valuesForParsing.asMap();
        }

        return value;
    }

    @Override
    public Class getValueType() {
        return DataSourceAttributeValue.class;
    }

    @Override
    public Object getValue(@Nonnull final Template template,
                           @Nonnull final String attributeName, @Nonnull final PObject requestJsonAttributes) {
        return this.parseAttribute(template, requestJsonAttributes.optArray(attributeName));
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
