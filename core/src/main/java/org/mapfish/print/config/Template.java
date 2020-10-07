package org.mapfish.print.config;

import com.google.common.collect.Sets;
import org.geotools.styling.Style;
import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.print.attribute.Attribute;
import org.mapfish.print.attribute.InternalAttribute;
import org.mapfish.print.config.access.AccessAssertion;
import org.mapfish.print.config.access.AlwaysAllowAssertion;
import org.mapfish.print.config.access.RoleAccessAssertion;
import org.mapfish.print.map.style.StyleParser;
import org.mapfish.print.processor.Processor;
import org.mapfish.print.processor.ProcessorDependencyGraph;
import org.mapfish.print.processor.ProcessorDependencyGraphFactory;
import org.mapfish.print.processor.map.CreateMapProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;

import static org.mapfish.print.OptionalUtils.or;

/**
 * Represents a report template configuration.
 */
public class Template implements ConfigurationObject, HasConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(Template.class);
    @Autowired
    private ProcessorDependencyGraphFactory processorGraphFactory;
    @Autowired
    private ClientHttpRequestFactory httpRequestFactory;
    @Autowired
    private StyleParser styleParser;


    private String reportTemplate;
    private Map<String, Attribute> attributes = new HashMap<>();
    private List<Processor> processors = new ArrayList<>();
    private boolean mapExport;
    private boolean pdfA = false;

    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;
    private Set<String> jdbcDrivers = Sets.newHashSet();
    private volatile ProcessorDependencyGraph processorGraph;
    private Map<String, String> styles = new HashMap<>();
    private Configuration configuration;
    private AccessAssertion accessAssertion = AlwaysAllowAssertion.INSTANCE;
    private PDFConfig pdfConfig = new PDFConfig();
    private String tableDataKey;
    private String outputFilename;

    public final String getOutputFilename() {
        return this.outputFilename;
    }

    /**
     * The default output file name of the report (takes precedence over {@link
     * Configuration#setOutputFilename(String)}).  This can be overridden by the outputFilename parameter in
     * the request JSON.
     * <p>
     * This can be a string and can also have a date section in the string that will be filled when the report
     * is created for example a section with ${&lt;dateFormatString&gt;} will be replaced with the current
     * date formatted in the way defined by the &lt;dateFormatString&gt; string.  The format rules are the
     * rules in
     * <a href="http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html">
     * java.text.SimpleDateFormat</a> (do a google search if the link above is broken).
     * </p>
     * <p>
     * Example: <code>outputFilename: print-${dd-MM-yyyy}</code> should output:
     * <code>print-22-11-2014.pdf</code>
     * </p>
     * <p>
     * Note: the suffix will be appended to the end of the name.
     * </p>
     *
     * @param outputFilename default output file name of the report.
     */
    public final void setOutputFilename(final String outputFilename) {
        this.outputFilename = outputFilename;
    }

    /**
     * Get the merged configuration between this template and the configuration's template.  The settings in
     * the template take priority over the configurations settings but if not set in the template then the
     * default will be the configuration's options.
     */
    public PDFConfig getPdfConfig() {
        return this.pdfConfig.getMergedInstance(this.configuration.getPdfConfig());
    }

    /**
     * Configure various properties related to the reports generated as PDFs.
     *
     * @param pdfConfig the pdf configuration
     */
    public final void setPdfConfig(final PDFConfig pdfConfig) {
        this.pdfConfig = pdfConfig;
    }

    /**
     * Print out the template information that the client needs for performing a request.
     *
     * @param json the writer to write the information to.
     */
    public final void printClientConfig(final JSONWriter json) throws JSONException {
        json.key("attributes");
        json.array();
        for (Map.Entry<String, Attribute> entry: this.attributes.entrySet()) {
            Attribute attribute = entry.getValue();
            if (attribute.getClass().getAnnotation(InternalAttribute.class) == null) {
                json.object();
                attribute.printClientConfig(json, this);
                json.endObject();
            }
        }
        json.endArray();
    }

    public final Map<String, Attribute> getAttributes() {
        return this.attributes;
    }

    /**
     * Set the attributes for this template.
     *
     * @param attributes the attribute map
     */
    public final void setAttributes(final Map<String, Attribute> attributes) {
        for (Map.Entry<String, Attribute> entry: attributes.entrySet()) {
            Object attribute = entry.getValue();
            if (!(attribute instanceof Attribute)) {
                final String msg =
                        "Attribute: '" + entry.getKey() + "' is not an attribute. It is a: " + attribute;
                LOGGER.error("Error setting the Attributes: {}", msg);
                throw new IllegalArgumentException(msg);
            } else {
                ((Attribute) attribute).setConfigName(entry.getKey());
            }
        }
        this.attributes = attributes;
    }

    public final String getReportTemplate() {
        return this.reportTemplate;
    }

    public final void setReportTemplate(final String reportTemplate) {
        this.reportTemplate = reportTemplate;
    }

    public final List<Processor> getProcessors() {
        return this.processors;
    }

    /**
     * Set the normal processors.
     *
     * @param processors the processors to set.
     */
    public final void setProcessors(final List<Processor> processors) {
        assertProcessors(processors);
        this.processors = processors;
    }

    private void assertProcessors(final List<Processor> processorsToCheck) {
        for (Processor entry: processorsToCheck) {
            if (!(entry instanceof Processor)) {
                final String msg = "Processor: " + entry + " is not a processor.";
                LOGGER.error("Error setting the Attributes: {}", msg);
                throw new IllegalArgumentException(msg);
            }
        }

    }

    /**
     * Set the key of the data that is the datasource for the main table in the report.
     *
     * @param tableData the key of the data that is the datasource for the main table in the report.
     */
    public final void setTableData(final String tableData) {
        this.tableDataKey = tableData;
    }

    public final String getTableDataKey() {
        return this.tableDataKey;
    }

    public final String getJdbcUrl() {
        return this.jdbcUrl;
    }

    public final void setJdbcUrl(final String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public final Set<String> getJdbcDrivers() {
        return this.jdbcDrivers;
    }

    public final void setJdbcDrivers(final Set<String> jdbcDrivers) {
        this.jdbcDrivers = jdbcDrivers;
    }

    public final String getJdbcUser() {
        return this.jdbcUser;
    }

    public final void setJdbcUser(final String jdbcUser) {
        this.jdbcUser = jdbcUser;
    }

    public final String getJdbcPassword() {
        return this.jdbcPassword;
    }

    public final void setJdbcPassword(final String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }

    /**
     * Get the processor graph to use for executing all the processors for the template.
     *
     * @return the processor graph.
     */
    public final ProcessorDependencyGraph getProcessorGraph() {
        if (this.processorGraph == null) {
            synchronized (this) {
                if (this.processorGraph == null) {
                    final Map<String, Class<?>> attcls = new HashMap<>();
                    for (Map.Entry<String, Attribute> attribute: this.attributes.entrySet()) {
                        attcls.put(attribute.getKey(), attribute.getValue().getValueType());
                    }
                    this.processorGraph = this.processorGraphFactory.build(this.processors, attcls);
                }
            }
        }
        return this.processorGraph;
    }

    /**
     * Set the named styles defined in the configuration for this.
     *
     * @param styles set the styles specific for this template.
     */
    public final void setStyles(final Map<String, String> styles) {
        this.styles = styles;
    }

    /**
     * Look for a style in the named styles provided in the configuration.
     *
     * @param styleName the name of the style to look for.
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public final java.util.Optional<Style> getStyle(final String styleName) {
        final String styleRef = this.styles.get(styleName);
        Optional<Style> style;
        if (styleRef != null) {
            style = (Optional<Style>) this.styleParser
                    .loadStyle(getConfiguration(), this.httpRequestFactory, styleRef);
        } else {
            style = Optional.empty();
        }
        return or(style, this.configuration.getStyle(styleName));
    }

    public final Configuration getConfiguration() {
        return this.configuration;
    }

    @Override
    public final void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public final void validate(final List<Throwable> validationErrors, final Configuration config) {
        this.accessAssertion.validate(validationErrors, config);
        int numberOfTableConfigurations = this.tableDataKey == null ? 0 : 1;
        numberOfTableConfigurations += this.jdbcUrl == null ? 0 : 1;

        if (numberOfTableConfigurations > 1) {
            validationErrors.add(new ConfigurationException(
                    "Only one of 'iterValue' or 'tableData' or 'jdbcUrl' should be defined."));
        }

        for (Attribute attribute: this.attributes.values()) {
            attribute.validate(validationErrors, config);
        }

        ProcessorDependencyGraphFactory.fillProcessorAttributes(this.processors, this.attributes);
        for (Processor processor: this.processors) {
            processor.validate(validationErrors, config);
        }

        try {
            getProcessorGraph();
        } catch (Throwable t) {
            validationErrors.add(t);
        }

        for (String jdbcDriver : getJdbcDrivers()) {
            try {
               Class.forName(jdbcDriver);
            } catch (ClassNotFoundException e) {
               validationErrors.add(new ConfigurationException(
                        "Unable to load JDBC driver: " + jdbcDriver +
                                " ensure that the web application has the jar on its classpath"));
            }



        }
        if (getJdbcUrl() != null) {

            Connection connection = null;
            try {
                if (getJdbcUser() != null) {
                    connection = DriverManager.getConnection(getJdbcUrl(), getJdbcUser(), getJdbcPassword());
                } else {
                    connection = DriverManager.getConnection(getJdbcUrl());
                }
            } catch (SQLException e) {
                validationErrors.add(e);
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        validationErrors.add(e);
                    }
                }
            }
        }

        if (this.mapExport) {
            int count = 0;
            for (Processor<?, ?> processor: getProcessors()) {
                if (processor instanceof CreateMapProcessor) {
                    count++;
                }
                if (count > 1) {
                    break;
                }
            }
            if (count != 1) {
                validationErrors.add(new ConfigurationException(
                        "When using MapExport, exactly one CreateMapProcessor should be defined."));
            }
        }
    }

    final void assertAccessible(final String name) {
        this.accessAssertion.assertAccess("Template '" + name + "'", this);
    }


    /**
     * The roles required to access this template.  If empty or not set then it is a <em>public</em> template.
     * If there are many roles then a user must have one of the roles in order to access the template.
     *
     * The security (how authentication/authorization is done) is configured in the
     * /WEB-INF/classes/mapfish-spring-security.xml
     * <p>
     * Any user without the required role will get an error when trying to access the template and the
     * template will not be visible in the capabilities requests.
     * </p>
     *
     * @param access the roles needed to access this
     */
    public final void setAccess(final List<String> access) {
        final RoleAccessAssertion assertion = new RoleAccessAssertion();
        assertion.setRequiredRoles(access);
        this.accessAssertion = assertion;
    }

    public final AccessAssertion getAccessAssertion() {
        return this.accessAssertion;
    }

    public final boolean isMapExport() {
        return this.mapExport;
    }

    public final void setMapExport(final boolean mapExport) {
        this.mapExport = mapExport;
    }

    public final boolean isPdfA() {
        return pdfA;
    }

    /**
     * If set to true (defaults to false), the generated maps, scalebar and north arrow will not contain any
     * transparent images.
     * <p>
     * This is needed in case you want to output PDF/A-1a reports.
     * <p>
     * In pdfA mode, all layers are merged into a single JPEG layer at the requested
     * resolution: WMTS tiles will be downscaled and vector layers will be rendered as bitmaps.
     *
     * @param pdfA the value
     */
    public void setPdfA(final boolean pdfA) {
        this.pdfA = pdfA;
    }
}
