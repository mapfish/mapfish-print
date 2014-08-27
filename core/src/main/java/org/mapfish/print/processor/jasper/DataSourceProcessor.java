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
import jsr166y.ForkJoinTask;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.mapfish.print.output.Values;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.InternalValue;
import org.mapfish.print.processor.Processor;
import org.mapfish.print.processor.ProcessorDependencyGraph;
import org.mapfish.print.processor.ProcessorDependencyGraphFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A processor that will run several other processors on a Iterable value and output a datasource object for
 * consumption by a jasper report or sub-report.
 *
 * @author Jesse on 8/26/2014.
 */
public final class DataSourceProcessor extends AbstractProcessor<DataSourceProcessor.Input, DataSourceProcessor.Output> {

    private String rowInputName;

    @Autowired
    private ProcessorDependencyGraphFactory processorGraphFactory;
    private ProcessorDependencyGraph processorGraph;

    /**
     * Constructor.
     */
    public DataSourceProcessor() {
        super(Output.class);
    }

    /**
     * The name to register the <em>datasource</em> value under in the values object passed to the processors.
     * <p/>
     * It is preferred that the <em>datasource</em> value is an Iterator &lt;Values> or Iteratable &lt;Values> in that case
     * inputName is not required and will be ignored.
     * <p/>
     * However if <em>datasource</em> refers to a single value or an iterable of a kind &lt;Values>, then it will be
     * put in a new values object with the key <em>inputName</em>.
     *
     * @param rowInputName the name to use when adding the row data to the values object.
     */
    public void setRowInputName(final String rowInputName) {
        this.rowInputName = rowInputName;
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

    @Nullable
    @Override
    public Input createInputParameter() {
        return new Input();
    }

    @Nullable
    @Override
    public Output execute(final Input input, final ExecutionContext context) throws Exception {

        Values values = input.values;
        final Object val = input.source;
        final Iterable<?> iterable;
        if (val != null) {
            if (val instanceof Iterable) {
                iterable = (Iterable<?>) val;
            } else if (val instanceof Iterator) {
                iterable = Lists.newArrayList((Iterator) val);
            } else {
                iterable = Collections.singleton(val);
            }
        } else {
            iterable = null;
        }

        JRDataSource jrDataSource = null;
        if (iterable != null) {
            jrDataSource = processInput(values, iterable);
        }

        if (jrDataSource == null) {
            jrDataSource = new JREmptyDataSource();
        }
        return new Output(jrDataSource);
    }

    private JRDataSource processInput(@Nonnull final Values values,
                                      @Nonnull final Iterable<?> iterable) {
        List<Values> dataSourceValues = Lists.newArrayList();
        for (Object o : iterable) {
            Values rowValues;
            if (o instanceof Values) {
                rowValues = (Values) o;
                rowValues.addRequiredValues(values);
            } else {
                if (this.rowInputName == null) {
                    String sourceName = getInputMapperBiMap().get("source");
                    if (sourceName == null) {
                        sourceName = "source";
                    }
                    throw new AssertionError("One of the values of '" + sourceName + "' does not refer to a 'Values' object " +
                                             "and there is no inputName defined");
                }

                rowValues = new Values(values);
                rowValues.put(this.rowInputName, o);
            }
            dataSourceValues.add(rowValues);
        }
        List<ForkJoinTask<Values>> futures = Lists.newArrayList();
        if (!dataSourceValues.isEmpty()) {
            for (Values dataSourceValue : dataSourceValues.subList(1, dataSourceValues.size())) {
                final ForkJoinTask<Values> taskFuture = this.processorGraph.createTask(dataSourceValue).fork();
                futures.add(taskFuture);
            }

            List<Map<String, ?>> rows = new ArrayList<Map<String, ?>>();

            Values firstRowData = this.processorGraph.createTask(dataSourceValues.get(0)).invoke();
            rows.add(firstRowData.asMap());

            for (ForkJoinTask<Values> future : futures) {
                final Values rowData = future.join();
                rows.add(rowData.asMap());
            }

            return new JRMapCollectionDataSource(rows);
        }
        return null;
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors) {
        if (this.processorGraph == null || this.processorGraph.getAllProcessors().isEmpty()) {
            validationErrors.add(new IllegalStateException("There are child processors for this processor"));
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
        public Values values;

        /**
         * The data that will be processed by this processor in order to create a Jasper DataSource object.
         */
        public Object source;

    }
    /**
     * Contains the datasource output.
     */
    public static final class Output {
        /**
         * The datasource to be assigned to a report or sub-report detail/table section.
         */
        public final JRDataSource datasource;

        /**
         * Constructor for setting the table data.
         * @param datasource the table data
         */
        public Output(@Nonnull final JRDataSource datasource) {
            this.datasource = datasource;
        }
    }
}
