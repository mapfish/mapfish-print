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

package org.mapfish.print.output;


import com.google.common.annotations.VisibleForTesting;
import jsr166y.ForkJoinPool;
import jsr166y.ForkJoinTask;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.Renderable;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.json.JSONException;
import org.mapfish.print.Constants;
import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.config.WorkingDirectories;
import org.mapfish.print.parser.MapfishParser;
import org.mapfish.print.processor.jasper.JasperReportBuilder;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * @author Jesse on 5/7/2014.
 */
public abstract class AbstractJasperReportOutputFormat implements OutputFormat {
    private static final Logger LOGGER = LoggerFactory.getLogger(JasperReportPDFOutputFormat.class);

    private static final String SUBREPORT_DIR = "SUBREPORT_DIR";
    private static final String SUBREPORT_TABLE_DIR = "SUBREPORT_TABLE_DIR";

    @Autowired
    private ForkJoinPool forkJoinPool;

    @Autowired
    private WorkingDirectories workingDirectories;

    @Autowired
    private ClientHttpRequestFactory httpRequestFactory;


    /**
     * Export the report to the output stream.
     *  @param outputStream the output stream to export to
     * @param print        the report
     */
    protected abstract void doExport(final OutputStream outputStream, final Print print) throws JRException, IOException;

    @Autowired
    private MapfishParser parser;

    @Override
    public final void print(final PJsonObject requestData, final Configuration config, final File configDir,
                            final File taskDirectory, final OutputStream outputStream)
            throws Exception {
        final Print print = getJasperPrint(requestData, config, configDir, taskDirectory);
        
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException();
        }
        
        doExport(outputStream, print);
    }

    /**
     * Renders the jasper report.
     *
     * @param requestData   the data from the client, required for writing.
     * @param config        the configuration object representing the server side configuration.
     * @param configDir     the directory that contains the configuration, used for resolving resources like images etc...
     * @param taskDirectory the temporary directory for this printing task.
     * @return a jasper print object which can be used to generate a PDF or other outputs.
     * @throws ExecutionException
     *
     * // CSOFF: RedundantThrows
     */
    @VisibleForTesting
    public final Print getJasperPrint(final PJsonObject requestData, final Configuration config,
                                      final File configDir, final File taskDirectory)
            throws JRException, SQLException, ExecutionException, JSONException {
        // CSON: RedundantThrows
        final String templateName = requestData.getString(Constants.JSON_LAYOUT_KEY);

        final Template template = config.getTemplate(templateName);
        if (template == null) {
            final String possibleTemplates = config.getTemplates().keySet().toString();
            throw new IllegalArgumentException("\nThere is no template with the name: " + templateName +
            ".\nAvailable templates: " + possibleTemplates);
        }
        final Values values = new Values(requestData, template, this.parser, taskDirectory, this.httpRequestFactory);

        double[] maxDpi = maxDpi(values);

        final File jasperTemplateFile = new File(configDir, template.getReportTemplate());
        final File jasperTemplateBuild = this.workingDirectories.getBuildFileFor(config, jasperTemplateFile,
                JasperReportBuilder.JASPER_REPORT_COMPILED_FILE_EXT, LOGGER);

        final File jasperTemplateDirectory = jasperTemplateBuild.getParentFile();

        values.put(SUBREPORT_DIR, jasperTemplateDirectory.getAbsolutePath());
        values.put(SUBREPORT_TABLE_DIR, taskDirectory.getAbsolutePath());

        final ForkJoinTask<Values> taskFuture = this.forkJoinPool.submit(template.getProcessorGraph().createTask(values));
        
        try {
            taskFuture.get();
        } catch (InterruptedException exc) {
            // if cancel() is called on the current thread, this exception will be thrown.
            // in this case, also properly cancel the task future.
            taskFuture.cancel(true);
            Thread.currentThread().interrupt();
            throw new CancellationException();
        }

        final JasperPrint print;
        if (template.getIterValue() != null) {
            if (!values.containsKey(template.getIterValue())) {
                throw new IllegalArgumentException(template.getIterValue() + " is missing.  It must either an attribute or a processor " +
                                                   "output");
            }

            final Object iterator = values.getObject(template.getIterValue(), Object.class);

            final JRDataSource jrDataSource;
            if (iterator instanceof Iterable) {
                Iterable iterable = (Iterable) iterator;

                final ForkJoinTask<List<Map<String, ?>>> iterTaskFuture =
                        this.forkJoinPool.submit(new ExecuteIterProcessorsTask(values, template));

                List<Map<String, ?>> dataSource;
                try {
                    dataSource = iterTaskFuture.get();
                } catch (InterruptedException exc) {
                    iterTaskFuture.cancel(true);
                    Thread.currentThread().interrupt();
                    throw new CancellationException();
                }

                jrDataSource = new JRMapCollectionDataSource(dataSource);
            } else {
                jrDataSource = new JREmptyDataSource();
            }
            print = JasperFillManager.fillReport(
                    jasperTemplateBuild.getAbsolutePath(),
                    values.getParameters(),
                    jrDataSource);
        } else if (template.getJdbcUrl() != null) {
            Connection connection;
            if (template.getJdbcUser() != null) {
                connection = DriverManager.getConnection(template.getJdbcUrl(), template.getJdbcUser(), template.getJdbcPassword());
            } else {
                connection = DriverManager.getConnection(template.getJdbcUrl());
            }
            print = JasperFillManager.fillReport(
                    jasperTemplateBuild.getAbsolutePath(),
                    values.getParameters(),
                    connection);
        } else if (template.getTableDataKey() != null) {
            final JRDataSource dataSource = values.getObject(template.getTableDataKey(), JRDataSource.class);
            print = JasperFillManager.fillReport(
                    jasperTemplateBuild.getAbsolutePath(),
                    values.getParameters(),
                    dataSource);
        } else {
            print = JasperFillManager.fillReport(
                    jasperTemplateBuild.getAbsolutePath(),
                    values.getParameters(),
                    new JREmptyDataSource());
        }
        print.setProperty(Renderable.PROPERTY_IMAGE_DPI, String.valueOf(Math.round(maxDpi[0])));
        return new Print(print, maxDpi[0], maxDpi[1]);
    }

    private double[] maxDpi(final Values values) {
        Map<String, MapAttribute.MapAttributeValues> maps = values.find(MapAttribute.MapAttributeValues.class);
        double maxDpi = Constants.PDF_DPI;
        double maxRequestorDpi = Constants.PDF_DPI;
        for (MapAttribute.MapAttributeValues attributeValues : maps.values()) {
            if (attributeValues.getDpi() > maxDpi) {
                maxDpi = attributeValues.getDpi();
            }

            if (attributeValues.getRequestorDPI() > maxRequestorDpi) {
                maxRequestorDpi = attributeValues.getRequestorDPI();
            }
        }
        return new double[]{maxDpi, maxRequestorDpi};
    }

    /**
     * The print information for doing the export.
     */
    public static final class Print {
        // CHECKSTYLE:OFF
        public final JasperPrint print;
        public final double dpi;
        public final double requestorDpi;
        // CHECKSTYLE:ON

        private Print(final JasperPrint print, final double dpi, final double requestorDpi) {
            this.print = print;
            this.dpi = dpi;
            this.requestorDpi = requestorDpi;
        }
    }
}
