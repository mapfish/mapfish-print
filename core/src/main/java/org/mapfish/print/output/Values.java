package org.mapfish.print.output;

import org.locationtech.jts.util.Assert;
import org.mapfish.print.ExtraPropertyException;
import org.mapfish.print.attribute.Attribute;
import org.mapfish.print.attribute.HttpRequestHeadersAttribute;
import org.mapfish.print.attribute.ReflectiveAttribute;
import org.mapfish.print.config.PDFConfig;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.ConfigFileResolvingHttpRequestFactory;
import org.mapfish.print.http.MfClientHttpRequestFactoryImpl;
import org.mapfish.print.processor.http.MfClientHttpRequestFactoryProvider;
import org.mapfish.print.servlet.MapPrinterServlet;
import org.mapfish.print.wrapper.ObjectMissingException;
import org.mapfish.print.wrapper.PObject;
import org.mapfish.print.wrapper.json.PJsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
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
     * The key that is used to store
     * {@link org.mapfish.print.processor.http.MfClientHttpRequestFactoryProvider}.
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

    /**
     * The key for the locale.
     */
    public static final String LOCALE_KEY = "REPORT_LOCALE";

    private final Map<String, Object> values = new ConcurrentHashMap<>();

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
     * @param taskDirectory the temporary directory for this printing task.
     * @param httpRequestFactory a factory for making http requests.
     * @param jasperTemplateBuild the directory where the jasper templates are compiled to
     */
    public Values(
            final String jobId,
            final PJsonObject requestData,
            final Template template,
            final File taskDirectory,
            final MfClientHttpRequestFactoryImpl httpRequestFactory,
            final File jasperTemplateBuild) {
        this(jobId, requestData, template, taskDirectory, httpRequestFactory, jasperTemplateBuild, null);
    }

    /**
     * Construct from the json request body and the associated template.
     *
     * @param jobId the job ID
     * @param requestData the json request data
     * @param template the template
     * @param taskDirectory the temporary directory for this printing task.
     * @param httpRequestFactory a factory for making http requests.
     * @param jasperTemplateBuild the directory where the jasper templates are compiled to
     * @param outputFormat the output format
     */
    //CHECKSTYLE:OFF
    public Values(
            final String jobId,
            final PJsonObject requestData,
            final Template template,
            final File taskDirectory,
            final MfClientHttpRequestFactoryImpl httpRequestFactory,
            final File jasperTemplateBuild,
            final String outputFormat) {
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

        Map<String, Attribute> attributes = new HashMap<>(template.getAttributes());
        populateFromAttributes(template, attributes, jsonAttributes);

        this.values.put(JOB_ID_KEY, jobId);

        this.values.put(VALUES_KEY, this);

        Locale locale = Locale.getDefault();
        if (requestData.has("lang")) {
            String[] localeSplit = requestData.getString("lang").split("_");
            if (localeSplit.length == 1) {
                locale = new Locale(localeSplit[0]);
            } else if (localeSplit.length == 2) {
                locale = new Locale(localeSplit[0], localeSplit[1]);
            } else if (localeSplit.length > 2) {
                locale = new Locale(localeSplit[0], localeSplit[1], localeSplit[2]);
            }
        }
        this.values.put(LOCALE_KEY, locale);
    }

    /**
     * Create a new instance and copy the required elements from the other values object. (IE working
     * directory, http client factory, etc...)
     *
     * @param values the values containing the required elements
     */
    public Values(@Nonnull final Values values) {
        addRequiredValues(values);
    }

    /**
     * Process the requestJsonAttributes using the attributes and the MapfishParser and add all resulting
     * values to this values object.
     *
     * @param template the template of the current request.
     * @param attributes the attributes that will be used to add values to this values object
     * @param requestJsonAttributes the json data for populating the attribute values
     */
    public void populateFromAttributes(
            @Nonnull final Template template,
            @Nonnull final Map<String, Attribute> attributes,
            @Nonnull final PObject requestJsonAttributes) {
        if (requestJsonAttributes.has(JSON_REQUEST_HEADERS) &&
                requestJsonAttributes.getObject(JSON_REQUEST_HEADERS).has(JSON_REQUEST_HEADERS) &&
                !attributes.containsKey(JSON_REQUEST_HEADERS)) {
            attributes.put(JSON_REQUEST_HEADERS, new HttpRequestHeadersAttribute());
        }
        for (Map.Entry<String, Attribute> attribute: attributes.entrySet()) {
            try {
                put(attribute.getKey(),
                    attribute.getValue().getValue(template, attribute.getKey(), requestJsonAttributes));
            } catch (ObjectMissingException | IllegalArgumentException e) {
                throw e;
            } catch (Throwable e) {
                String templateName = "unknown";
                for (Map.Entry<String, Template> entry: template.getConfiguration().getTemplates()
                        .entrySet()) {
                    if (entry.getValue() == template) {
                        templateName = entry.getKey();
                        break;
                    }
                }

                String defaults = "";

                if (attribute instanceof ReflectiveAttribute<?>) {
                    ReflectiveAttribute<?> reflectiveAttribute = (ReflectiveAttribute<?>) attribute;
                    defaults = "\n\n The attribute defaults are: " + reflectiveAttribute.getDefaultValue();
                }

                String errorMsg = "An error occurred when creating a value from the '" + attribute.getKey() +
                        "' attribute for the '" +
                        templateName + "' template.\n\nThe JSON is: \n" + requestJsonAttributes + defaults +
                        "\n" +
                        e.toString();

                throw new AttributeParsingException(errorMsg, e);
            }
        }

        if (template.getConfiguration().isThrowErrorOnExtraParameters()) {
            final List<String> extraProperties = new ArrayList<>();
            for (Iterator<String> it = requestJsonAttributes.keys(); it.hasNext(); ) {
                final String attributeName = it.next();
                if (!attributes.containsKey(attributeName)) {
                    extraProperties.add(attributeName);
                }
            }

            if (!extraProperties.isEmpty()) {
                throw new ExtraPropertyException("Extra properties found in the request attributes",
                                                 extraProperties, attributes.keySet());
            }
        }
    }

    /**
     * Add the elements that all values objects require from the provided values object.
     *
     * @param sourceValues the values object containing the required elements
     */
    public void addRequiredValues(@Nonnull final Values sourceValues) {
        Object taskDirectory = sourceValues.getObject(TASK_DIRECTORY_KEY, Object.class);
        MfClientHttpRequestFactoryProvider requestFactoryProvider =
                sourceValues.getObject(CLIENT_HTTP_REQUEST_FACTORY_KEY,
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
        this.values.put(LOCALE_KEY, sourceValues.getObject(LOCALE_KEY, Locale.class));
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
            throw new IllegalArgumentException(
                    "A null value was attempted to be put into the values object under key: " + key);
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
        return (Map<String, T>) this.values.entrySet().stream()
                .filter(input -> valueTypeToFind.isInstance(input.getValue()))
                .collect(
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public String toString() {
        Map<String, Object> display = new HashMap<>(this.values);
        display.remove(VALUES_KEY);
        return display.toString();
    }
}
