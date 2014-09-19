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

package org.mapfish.print.processor.jasper;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jsr166y.ForkJoinTask;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapfish.print.attribute.Attribute;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.parser.MapfishParser;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.InternalValue;
import org.mapfish.print.processor.Processor;
import org.mapfish.print.processor.ProcessorDependencyGraph;
import org.mapfish.print.processor.ProcessorDependencyGraphFactory;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.mapfish.print.attribute.DataSourceAttribute.DataSourceAttributeValue;

/**
 * A processor that will run several other processors on a Iterable value and output a datasource object for
 * consumption by a jasper report or sub-report.
 *
 * @author Jesse on 8/26/2014.
 */
public final class DataSourceProcessor extends AbstractProcessor<DataSourceProcessor.Input, DataSourceProcessor.Output> {

    private Map<String, Attribute> attributes = Maps.newHashMap();

    @Autowired
    private ProcessorDependencyGraphFactory processorGraphFactory;
    private ProcessorDependencyGraph processorGraph;
    @Autowired
    private MapfishParser parser;
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

    /**
     * The path to the report template used to render each row of the data.  This is only required if a subreport needs to be
     * compiled and is referenced in the containing report's detail section.
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
     * subreport will be added to each row in the datasource with this value as the key.  This allows the containing report to
     * reference the subreport in each row.
     *
     * @param reportKey the key/name to use when putting the path to the compiled subreport in each row of the datasource.
     */
    public void setReportKey(final String reportKey) {
        this.reportKey = reportKey;
    }

    /**
     * All the processors that will executed for each value retrieved from the {@link org.mapfish.print.output.Values} object
     * with the datasource name.  All output values from the processor graph will be the datasource values.
     * <p/>
     * <p>
     * Each value retrieved from values with the datasource name will be the input of the processor graph
     * and all the output values for that execution will be the values of a single row in the datasource.
     * The Jasper template can use any of the values in its detail band.
     * </p>
     *
     * @param processors the processors which will be ran to create the datasource
     */
    public void setProcessors(final List<Processor> processors) {
        this.processorGraph = this.processorGraphFactory.build(processors);
    }

    /**
     * All the attributes needed either by the processors for each datasource row or by the jasper template.
     *
     * @param attributes the attributes.
     */
    public void setAttributes(final Map<String, Attribute> attributes) {
        this.attributes = attributes;
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

    //CSOFF:RedundantThrows
    private JRDataSource processInput(@Nonnull final Input input)
            throws JSONException, JRException {
        //CSON:RedundantThrows
        List<Values> dataSourceValues = Lists.newArrayList();
        for (Map<String, Object> o : input.datasource.attributesValues) {
            Values rowValues = new Values(input.values);
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
        dataSourceValue.populateFromAttributes(template, this.parser, this.attributes,
                new PJsonObject(new JSONObject(), "DataSourceProcessorAttributes"));
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors, final Configuration configuration) {
        if (this.processorGraph == null || this.processorGraph.getAllProcessors().isEmpty()) {
            validationErrors.add(new ConfigurationException("There are child processors for this processor"));
        }

        if (this.reportTemplate != null && this.reportKey == null || this.reportTemplate == null && this.reportKey != null) {
            validationErrors.add(new ConfigurationException("'reportKey' and 'reportTemplate' must either both be null or both" +
                                                            " be non-null.  reportKey: " + this.reportKey +
                                                            " reportTemplate: " + this.reportTemplate));
        }
        for (Attribute attribute : this.attributes.values()) {
            attribute.validate(validationErrors, configuration);
        }
    }

    /**
     * Contains the datasource input.
     */
    public static final class Input {
        /**
         * The values object with all values.  This is required in order to run sub-processor graph
         */
        @InternalValue
        public Template template;
        /**
         * The values object with all values.  This is required in order to run sub-processor graph
         */
        @InternalValue
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
