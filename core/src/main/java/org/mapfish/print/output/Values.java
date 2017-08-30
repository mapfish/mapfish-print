package org.mapfish.print.output;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.util.Assert;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapfish.print.attribute.Attribute;
import org.mapfish.print.attribute.DataSourceAttribute;
import org.mapfish.print.attribute.HttpRequestHeadersAttribute;
import org.mapfish.print.attribute.PrimitiveAttribute;
import org.mapfish.print.attribute.ReflectiveAttribute;
import org.mapfish.print.config.PDFConfig;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.ConfigFileResolvingHttpRequestFactory;
import org.mapfish.print.http.MfClientHttpRequestFactoryImpl;
import org.mapfish.print.parser.MapfishParser;
import org.mapfish.print.processor.http.MfClientHttpRequestFactoryProvider;
import org.mapfish.print.servlet.MapPrinterServlet;
import org.mapfish.print.wrapper.ObjectMissingException;
import org.mapfish.print.wrapper.PObject;
import org.mapfish.print.wrapper.json.PJsonArray;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.mapfish.print.wrapper.multi.PMultiObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.mapfish.print.servlet.MapPrinterServlet.JSON_REQUEST_HEADERS;

/**
 * Values that go into a processor from previous processors in the processor processing graph.
 */
public final class Values {
    /**
     * The key that is used to store the task directory in the values map.
     */
    public static final String TASK_DIRECTORY_KEY = "tempTaskDirectory";
    /**
     * The key that is used to store {@link org.mapfish.print.processor.http.MfClientHttpRequestFactoryProvider}.
     */
    public static final String CLIENT_HTTP_REQUEST_FACTORY_KEY = "clientHttpRequestFactoryProvider";
    /**
     * The key that is used to store {@link org.mapfish.print.config.Template}.
     */
    public static final String TEMPLATE_KEY = "template";
    /**
     * The key for the values object for the {@link org.mapfish.print.config.PDFConfig} object.
     */
    public static final String PDF_CONFIG_KEY = "pdfConfig";
    /**
     * The key for the output format.
     */
    public static final String OUTPUT_FORMAT_KEY = "outputFormat";
    /**
     * The key for the values object for the subreport directory.
     */
    public static final String SUBREPORT_DIR_KEY = "SUBREPORT_DIR";
    /**
     * The key for the reference ID.
     */
    public static final String JOB_ID_KEY = "jobId";
    /**
     * The key for the values object of it self.
     */
    public static final String VALUES_KEY = "values";

    private final Map<String, Object> values = new ConcurrentHashMap<String, Object>();

    /**
     * Constructor.
     *
     * @param values initial values.
     */
    public Values(final Map<String, Object> values) {
        this.values.putAll(values);
    }

    /**
     * Constructor.
     */
    public Values() {
        // nothing to do
    }

    /**
     * Construct from the json request body and the associated template.
     *
     * @param jobId the job ID
     * @param requestData the json request data
     * @param template the template
     * @param parser the parser to use for parsing the request data.
     * @param taskDirectory the temporary directory for this printing task.
     * @param httpRequestFactory a factory for making http requests.
     * @param jasperTemplateBuild the directory where the jasper templates are compiled to
     */
    public Values(final String jobId,
                  final PJsonObject requestData,
                  final Template template,
                  final MapfishParser parser,
                  final File taskDirectory,
                  final MfClientHttpRequestFactoryImpl httpRequestFactory,
                  final File jasperTemplateBuild) throws JSONException {
        this(jobId, requestData, template, parser, taskDirectory, httpRequestFactory, jasperTemplateBuild, null);
    }

    /**
     * Construct from the json request body and the associated template.
     *
     * @param jobId the job ID
     * @param requestData the json request data
     * @param template the template
     * @param parser the parser to use for parsing the request data.
     * @param taskDirectory the temporary directory for this printing task.
     * @param httpRequestFactory a factory for making http requests.
     * @param jasperTemplateBuild the directory where the jasper templates are compiled to
     * @param outputFormat the output format
     */
    //CHECKSTYLE:OFF
    public Values(final String jobId,
                  final PJsonObject requestData,
                  final Template template,
                  final MapfishParser parser,
                  final File taskDirectory,
                  final MfClientHttpRequestFactoryImpl httpRequestFactory,
                  final File jasperTemplateBuild,
                  final String outputFormat) throws JSONException {
        //CHECKSTYLE:ON
        Assert.isTrue(!taskDirectory.mkdirs() || taskDirectory.exists());

        // add task dir. to values so that all processors can access it
        this.values.put(TASK_DIRECTORY_KEY, taskDirectory);
        this.values.put(CLIENT_HTTP_REQUEST_FACTORY_KEY,
                new MfClientHttpRequestFactoryProvider(new ConfigFileResolvingHttpRequestFactory(
                        httpRequestFactory, template.getConfiguration(), jobId)));
        this.values.put(TEMPLATE_KEY, template);
        this.values.put(PDF_CONFIG_KEY, template.getPdfConfig());
        if (jasperTemplateBuild != null) {
            this.values.put(SUBREPORT_DIR_KEY, jasperTemplateBuild.getAbsolutePath());
        }
        if (outputFormat != null) {
            this.values.put(OUTPUT_FORMAT_KEY, outputFormat);
        }

        final PJsonObject jsonAttributes = requestData.getJSONObject(MapPrinterServlet.JSON_ATTRIBUTES);

        Map<String, Attribute> attributes = Maps.newHashMap(template.getAttributes());
        populateFromAttributes(template, parser, attributes, jsonAttributes);

        this.values.put(JOB_ID_KEY, jobId);

        this.values.put(VALUES_KEY, this);
    }

    /**
     * Process the requestJsonAttributes using the attributes and the MapfishParser and add all resulting values to this values object.
     *
     * @param template the template of the current request.
     * @param parser the parser to use for parsing the request data.
     * @param attributes the attributes that will be used to add values to this values object
     * @param requestJsonAttributes the json data for populating the attribute values
     * @throws JSONException
     */
    public void populateFromAttributes(@Nonnull final Template template,
                                       @Nonnull final MapfishParser parser,
                                       @Nonnull final Map<String, Attribute> attributes,
                                       @Nonnull final PObject requestJsonAttributes) throws JSONException {
        if (requestJsonAttributes.has(JSON_REQUEST_HEADERS) &&
                requestJsonAttributes.getObject(JSON_REQUEST_HEADERS).has(JSON_REQUEST_HEADERS) &&
                !attributes.containsKey(JSON_REQUEST_HEADERS)) {
            attributes.put(JSON_REQUEST_HEADERS, new HttpRequestHeadersAttribute());
        }
        for (String attributeName : attributes.keySet()) {
            final Attribute attribute = attributes.get(attributeName);
            final Object value;
            try {
                if (attribute instanceof PrimitiveAttribute) {
                    PrimitiveAttribute<?> pAtt = (PrimitiveAttribute<?>) attribute;
                    Object defaultVal = pAtt.getDefault();
                    PObject jsonToUse = requestJsonAttributes;
                    if (defaultVal != null) {
                        final JSONObject obj = new JSONObject();
                        obj.put(attributeName, defaultVal);
                        PObject[] pValues = new PObject[]{requestJsonAttributes, new PJsonObject(obj, "default_" + attributeName)};
                        jsonToUse = new PMultiObject(pValues);
                    }
                    value = parser.parsePrimitive(attributeName, pAtt, jsonToUse);
                } else if (attribute instanceof DataSourceAttribute) {
                    DataSourceAttribute dsAttribute = (DataSourceAttribute) attribute;
                    value = dsAttribute.parseAttribute(parser, template, requestJsonAttributes.optArray(attributeName));
                } else if (attribute instanceof ReflectiveAttribute) {
                    boolean errorOnExtraParameters = template.getConfiguration().isThrowErrorOnExtraParameters();
                    ReflectiveAttribute<?> rAtt = (ReflectiveAttribute<?>) attribute;
                    value = rAtt.createValue(template);
                    PObject pValue = requestJsonAttributes.optObject(attributeName);

                    if (pValue != null) {
                        PObject[] pValues = new PObject[]{pValue, rAtt.getDefaultValue()};
                        pValue = new PMultiObject(pValues);
                    } else {
                        final Object valueOpt = requestJsonAttributes.opt(attributeName);
                        if (valueOpt != null) {
                            String valueAsString;
                            if (valueOpt instanceof PJsonArray) {
                                valueAsString = ((PJsonArray) valueOpt).getInternalArray().toString(2);
                            } else if (valueOpt instanceof JSONArray) {
                                valueAsString = ((JSONArray) valueOpt).toString(2);
                            } else {
                                valueAsString = valueOpt.toString();
                            }
                            final String message = "Expected a JSON Object as the value for the element with the path: '" +
                                                   requestJsonAttributes.getPath(attributeName) + "' but instead "
                                                   + "got a '" + valueOpt.getClass().toString() +
                                                   "'.\nThe value is: \n" + valueAsString;
                            throw new IllegalArgumentException(message);
                        }
                        pValue = rAtt.getDefaultValue();
                    }
                    parser.parse(errorOnExtraParameters, pValue, value);
                } else {
                    throw new IllegalArgumentException("Unsupported attribute type: " + attribute);
                }
                put(attributeName, value);
            } catch (ObjectMissingException e) {
                throw e;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Throwable e) {
                String templateName = "unknown";
                for (Map.Entry<String, Template> entry : template.getConfiguration().getTemplates().entrySet()) {
                    if (entry.getValue() == template) {
                        templateName = entry.getKey();
                    }
                }

                String defaults = "";

                if (attribute instanceof ReflectiveAttribute<?>) {
                    ReflectiveAttribute<?> reflectiveAttribute = (ReflectiveAttribute<?>) attribute;
                    defaults = "\n\n The attribute defaults are: " + reflectiveAttribute.getDefaultValue();
                }

                String errorMsg = "An error occurred when creating a value from the '" + attributeName + "' attribute for the '" +
                                  templateName + "' template.\n\nThe JSON is: \n" + requestJsonAttributes + defaults;

                throw new AttributeParsingException(errorMsg, e);
            }
        }
    }

    /**
     * Create a new instance and copy the required elements from the other values object.
     * (IE working directory, http client factory, etc...)
     *
     * @param values the values containing the required elements
     */
    public Values(@Nonnull final Values values) {
        addRequiredValues(values);
    }

    /**
     * Add the elements that all values objects require from the provided values object.
     *
     * @param sourceValues the values object containing the required elements
     */
    public void addRequiredValues(@Nonnull final Values sourceValues) {
        Object taskDirectory = sourceValues.getObject(TASK_DIRECTORY_KEY, Object.class);
        MfClientHttpRequestFactoryProvider requestFactoryProvider = sourceValues.getObject(CLIENT_HTTP_REQUEST_FACTORY_KEY,
                MfClientHttpRequestFactoryProvider.class);
        Template template = sourceValues.getObject(TEMPLATE_KEY, Template.class);
        PDFConfig pdfConfig = sourceValues.getObject(PDF_CONFIG_KEY, PDFConfig.class);
        String subReportDir = sourceValues.getString(SUBREPORT_DIR_KEY);

        this.values.put(TASK_DIRECTORY_KEY, taskDirectory);
        this.values.put(CLIENT_HTTP_REQUEST_FACTORY_KEY, requestFactoryProvider);
        this.values.put(TEMPLATE_KEY, template);
        this.values.put(PDF_CONFIG_KEY, pdfConfig);
        this.values.put(SUBREPORT_DIR_KEY, subReportDir);
        this.values.put(VALUES_KEY, this);
        this.values.put(JOB_ID_KEY, sourceValues.getString(JOB_ID_KEY));
    }

    /**
     * Put a new value in map.
     *
     * @param key id of the value for looking up.
     * @param value the value.
     */
    public void put(final String key, final Object value) {
        if (TASK_DIRECTORY_KEY.equals(key) && this.values.keySet().contains(TASK_DIRECTORY_KEY)) {
            // ensure that no one overwrites the task directory
            throw new IllegalArgumentException("Invalid key: " + key);
        }

        if (value == null) {
            throw new IllegalArgumentException("A null value was attempted to be put into the values object under key: " + key);
        }
        this.values.put(key, value);
    }

    /**
     * Get all parameters.
     */
    public Map<String, Object> asMap() {
        return this.values;
    }

    /**
     * Get a value as a string.
     *
     * @param key the key for looking up the value.
     */
    public String getString(final String key) {
        return (String) this.values.get(key);
    }

    /**
     * Get a value as a double.
     *
     * @param key the key for looking up the value.
     */
    public Double getDouble(final String key) {
        return (Double) this.values.get(key);
    }

    /**
     * Get a value as a integer.
     *
     * @param key the key for looking up the value.
     */
    public Integer getInteger(final String key) {
        return (Integer) this.values.get(key);
    }

    /**
     * Get a value as a string.
     *
     * @param key the key for looking up the value.
     * @param type the type of the object
     * @param <V> the type
     */
    public <V> V getObject(final String key, final Class<V> type) {
        final Object obj = this.values.get(key);
        return type.cast(obj);
    }

    /**
     * Return true if the identified value is present in this values.
     *
     * @param key the key to check for.
     */
    public boolean containsKey(final String key) {
        return this.values.containsKey(key);
    }

    /**
     * Get a boolean value from the values or null.
     *
     * @param key the look up key of the value
     */
    @Nullable
    public Boolean getBoolean(@Nonnull final String key) {
        return (Boolean) this.values.get(key);
    }

    /**
     * Remove a value from this object.
     *
     * @param key key of entry to remove.
     */
    public void remove(final String key) {
        this.values.remove(key);
    }

    /**
     * Find all the values of the requested type.
     *
     * @param valueTypeToFind the type of the value to return.
     * @param <T> the type of the value to find.
     * @return the key, value pairs found.
     */
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> find(final Class<T> valueTypeToFind) {
        final Map<String, Object> filtered = Maps.filterEntries(this.values, new Predicate<Map.Entry<String, Object>>() {
            @Override
            public boolean apply(@Nullable final Map.Entry<String, Object> input) {
                return input != null && valueTypeToFind.isInstance(input.getValue());
            }
        });

        return (Map<String, T>) filtered;
    }

    @Override
    public String toString() {
        Map<String, Object> display = new HashMap<String, Object>(this.values);
        display.remove(VALUES_KEY);
        return display.toString();
    }
}
