package org.mapfish.print.processor.jasper;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapfish.print.attribute.Attribute;
import org.mapfish.print.attribute.DataSourceAttribute;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.CustomDependencies;
import org.mapfish.print.processor.Processor;
import org.mapfish.print.processor.ProcessorDependencyGraph;
import org.mapfish.print.processor.ProcessorDependencyGraphFactory;
import org.mapfish.print.processor.RequireAttributes;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;

import static org.mapfish.print.attribute.DataSourceAttribute.DataSourceAttributeValue;

/**
 * <p>A processor that will process a
 * {@link org.mapfish.print.attribute.DataSourceAttribute.DataSourceAttributeValue} and construct a single
 * Jasper DataSource from the input values in the
 * {@link org.mapfish.print.attribute.DataSourceAttribute.DataSourceAttributeValue} input object.</p>
 *
 * <p>The {@link org.mapfish.print.attribute.DataSourceAttribute.DataSourceAttributeValue} has an array of
 * maps, each map in the array equates to a row in the Jasper DataSource.</p>
 *
 * <p>The DataSourceProcessor can be configured with processors which will be used
 * to transform each map in the input array before constructing the final DataSource row.</p>
 *
 * <p>For example, each map in the array could be
 * {@link org.mapfish.print.attribute.map.MapAttribute.MapAttributeValues} and the DataSourceProcessor
 * could be configured with !createMap processor.  In this scenario each element in the array would be
 * transformed by the !createMap processor and thus each row of the resulting DataSource will contain the
 * map subreport created by the !createMap processor.</p>
 *
 * <p>An additional point to remember is that (as with the normal execution) in addition to the output of
 * the processors, the attributes in the input map will also be columns in the row.  This means that the
 * jasper report that makes use of the resulting DataSource will have access to both the results of the
 * processor as well as the input values (unless overwritten by the processor output).</p>
 *
 * <p>If the reportKey is defined (and reportTemplate) then a the reportTemplate jrxml file will be
 * compiled (as required by all jrxml files) and an additional column will be added to each row [reportKey]
 * : [compiled reportTemplate File]</p>
 *
 * <p>If reportKey is defined the reportTemplate must also be defined (and vice-versa).</p>
 *
 * <p>See also: <a href="attributes.html#!datasource">!datasource</a> attribute</p>
 * [[examples=verboseExample,datasource_dynamic_tables,datasource_many_dynamictables_legend,
 * datasource_multiple_maps,customDynamicReport,report]]
 */
public final class DataSourceProcessor
        extends AbstractProcessor<DataSourceProcessor.Input, DataSourceProcessor.Output>
        implements RequireAttributes, CustomDependencies {

    private Map<String, Attribute> internalAttributes = Maps.newHashMap();
    private Map<String, Attribute> allAttributes = Maps.newHashMap();

    @Autowired
    private ProcessorDependencyGraphFactory processorGraphFactory;
    private ProcessorDependencyGraph processorGraph;
    private List<Processor> processors;
    private List<String> copyAttributes = Lists.newArrayList();
    @Autowired
    private JasperReportBuilder jasperReportBuilder;

    private String reportTemplate;
    private String reportKey;

    /**
     * Constructor.
     */
    public DataSourceProcessor() {
        super(Output.class);
    }

    @PostConstruct
    private void init() {
        // default to no processors
        this.processorGraph  = this.processorGraphFactory.build(Collections.<Processor>emptyList(),
                Collections.<String, Class<?>>emptyMap());
    }

    /**
     * The path to the report template used to render each row of the data.  This is only required if a
     * subreport needs to be compiled and is referenced in the containing report's detail section.
     * <p>
     *     The path should be relative to the configuration directory
     * </p>
     * @param reportTemplate the path to the report template.
     */
    public void setReportTemplate(final String reportTemplate) {
        this.reportTemplate = reportTemplate;
    }

    /**
     * The key/name to use when putting the path to the compiled subreport in each row of the datasource.
     * This is required if {@link #reportTemplate} has been set.  The path to the compiled
     * subreport will be added to each row in the datasource with this value as the key.  This allows the
     * containing report to reference the subreport in each row.
     *
     * @param reportKey the key/name to use when putting the path to the compiled subreport in each row of
     *      the datasource.
     */
    public void setReportKey(final String reportKey) {
        this.reportKey = reportKey;
    }

    /**
     * All the processors that will executed for each value retrieved from the
     * {@link org.mapfish.print.output.Values} object with the datasource name.  All output values from the
     * processor graph will be the datasource values.
     * <p></p>
     * <p>
     * Each value retrieved from values with the datasource name will be the input of the processor graph
     * and all the output values for that execution will be the values of a single row in the datasource.
     * The Jasper template can use any of the values in its detail band.
     * </p>
     *
     * @param processors the processors which will be ran to create the datasource
     */
    public void setProcessors(final List<Processor> processors) {
        this.processors = processors;
    }

    /**
     * All the attributes needed either by the processors for each datasource row or by the jasper template.
     *
     * @param attributes the attributes.
     */
    public void setAttributes(final Map<String, Attribute> attributes) {
        this.internalAttributes = attributes;
        this.allAttributes.putAll(attributes);
    }

    /**
     * The attributes that will be copied from the previous level.
     *
     * @param copyAttributes the attributes name
     */
    public void setCopyAttributes(final List<String> copyAttributes) {
        this.copyAttributes = copyAttributes;
    }


    @Nonnull
    @Override
    public Collection<String> getDependencies() {
        final ArrayList<String> result = new ArrayList<>(this.copyAttributes);
        result.add(Values.TASK_DIRECTORY_KEY);
        result.add(Values.CLIENT_HTTP_REQUEST_FACTORY_KEY);
        result.add(Values.TEMPLATE_KEY);
        result.add(Values.PDF_CONFIG_KEY);
        result.add(Values.SUBREPORT_DIR_KEY);
        result.add(Values.VALUES_KEY);
        result.add(Values.JOB_ID_KEY);
        return result;
    }

    /**
     * All the sub-level attributes.
     *
     * @param name the attribute name.
     * @param attribute the attribute.
     */
    public void setAttribute(final String name, final Attribute attribute) {
        if (name.equals("datasource")) {
            this.allAttributes.putAll(((DataSourceAttribute) attribute).getAttributes());
        } else if (this.copyAttributes.contains(name)) {
            this.allAttributes.put(name, attribute);
        }
    }

    @Nullable
    @Override
    public Input createInputParameter() {
        return new Input();
    }

    @Nullable
    @Override
    public Output execute(final Input input, final ExecutionContext context) throws Exception {

        JRDataSource jrDataSource = processInput(input);

        if (jrDataSource == null) {
            jrDataSource = new JREmptyDataSource();
        }
        return new Output(jrDataSource);
    }

    private JRDataSource processInput(@Nonnull final Input input)
            throws JSONException, JRException {
        List<Values> dataSourceValues = Lists.newArrayList();
        for (Map<String, Object> o : input.datasource.attributesValues) {
            // copy only the required values
            Values rowValues = new Values(input.values);
            for (String attributeName: this.copyAttributes) {
                rowValues.put(attributeName, input.values.getObject(attributeName, Object.class));
            }
            for (Map.Entry<String, Object> entry : o.entrySet()) {
                rowValues.put(entry.getKey(), entry.getValue());
            }

            dataSourceValues.add(rowValues);
        }

        List<ForkJoinTask<Values>> futures = Lists.newArrayList();
        if (!dataSourceValues.isEmpty()) {
            for (Values dataSourceValue : dataSourceValues) {
                addAttributes(input.template, dataSourceValue);
                final ForkJoinTask<Values> taskFuture = this.processorGraph.createTask(dataSourceValue).fork();
                futures.add(taskFuture);
            }
            final File reportFile;
            if (this.reportTemplate != null) {
                final Configuration configuration = input.template.getConfiguration();
                final File file = new File(configuration.getDirectory(), this.reportTemplate);
                reportFile = this.jasperReportBuilder.compileJasperReport(configuration, file);
            } else {
                reportFile = null;
            }
            List<Map<String, ?>> rows = new ArrayList<Map<String, ?>>();

            for (ForkJoinTask<Values> future : futures) {
                final Values rowData = future.join();
                if (reportFile != null) {
                        rowData.put(this.reportKey, reportFile.getAbsolutePath());
                }
                rows.add(rowData.asMap());
            }

            return new JRMapCollectionDataSource(rows);
        }
        return null;
    }

    private void addAttributes(@Nonnull final Template template,
                               @Nonnull final Values dataSourceValue) throws JSONException {
        dataSourceValue.populateFromAttributes(template, this.internalAttributes,
                new PJsonObject(new JSONObject(), "DataSourceProcessorAttributes"));
    }

    @Override
    public void toString(@Nonnull final StringBuilder builder, final int indent, final String parent) {
        super.toString(builder, indent, parent);
        this.processorGraph.toString(builder, indent + 1, this.toString());
    }

    @Override
    protected void extraValidation(
            final List<Throwable> validationErrors,
            final Configuration configuration) {
        if (this.reportTemplate != null && this.reportKey == null ||
                this.reportTemplate == null && this.reportKey != null) {
            validationErrors.add(new ConfigurationException("'reportKey' and 'reportTemplate' must ither " +
                    "both be null or both be non-null.  reportKey: " + this.reportKey + " reportTemplate: "
                    + this.reportTemplate));
        }

        for (Attribute attribute : this.internalAttributes.values()) {
            attribute.validate(validationErrors, configuration);
        }

        ProcessorDependencyGraphFactory.fillProcessorAttributes(this.processors, this.allAttributes);
        for (Processor processor : this.processors) {
            processor.validate(validationErrors, configuration);
        }

        final Map<String, Class<?>> attcls = new HashMap<String, Class<?>>();
        for (String attributeName: this.allAttributes.keySet()) {
            attcls.put(attributeName, this.allAttributes.get(attributeName).getValueType());
        }
        try {
            this.processorGraph = this.processorGraphFactory.build(this.processors, attcls);
        } catch (IllegalArgumentException e) {
            validationErrors.add(e);
        }

        if (this.processorGraph == null) {
            validationErrors.add(new ConfigurationException(
                    "There are no child processors for this processor"));
        } else {
            final Set<Processor<?, ?>> allProcessors = this.processorGraph.getAllProcessors();
            for (Processor<?, ?> processor : allProcessors) {
                processor.validate(validationErrors, configuration);
            }
        }
    }

    /**
     * Contains the datasource input.
     */
    public static final class Input {
        /**
         * The values object with all values.  This is required in order to run sub-processor graph
         */
        public Template template;

        /**
         * The values object with all values.  This is required in order to run sub-processor graph
         */
        public Values values;

        /**
         * The data that will be processed by this processor in order to create a Jasper DataSource object.
         */
        public DataSourceAttributeValue datasource;

    }

    /**
     * Contains the datasource output.
     */
    public static final class Output {
        /**
         * The datasource to be assigned to a report or sub-report detail/table section.
         */
        public final JRDataSource jrDataSource;

        /**
         * Constructor for setting the table data.
         *
         * @param datasource the table data
         */
        public Output(@Nonnull final JRDataSource datasource) {
            this.jrDataSource = datasource;
        }
    }
}
