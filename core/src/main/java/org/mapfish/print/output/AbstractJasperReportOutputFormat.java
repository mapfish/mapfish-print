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
import com.google.common.collect.Lists;
import jsr166y.ForkJoinPool;
import jsr166y.ForkJoinTask;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.Renderable;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.util.LocalJasperReportsContext;
import net.sf.jasperreports.repo.RepositoryService;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

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

        JasperFillManager fillManager = getJasperFillManager(config);

        final JasperPrint print;
        if (template.getJdbcUrl() != null) {
            Connection connection;
            if (template.getJdbcUser() != null) {
                connection = DriverManager.getConnection(template.getJdbcUrl(), template.getJdbcUser(), template.getJdbcPassword());
            } else {
                connection = DriverManager.getConnection(template.getJdbcUrl());
            }

            print = fillManager.fill(
                    jasperTemplateBuild.getAbsolutePath(),
                    values.asMap(),
                    connection);

        } else if (template.getTableDataKey() != null) {
            final Object dataSourceObj = values.getObject(template.getTableDataKey(), Object.class);
            JRDataSource dataSource;
            if (dataSourceObj instanceof JRDataSource) {
                dataSource = (JRDataSource) dataSourceObj;
            } else if (dataSourceObj instanceof Iterable) {
                Iterable sourceObj = (Iterable) dataSourceObj;
                dataSource = toJRDataSource(sourceObj.iterator());
            }  else if (dataSourceObj instanceof Iterator) {
                Iterator sourceObj = (Iterator) dataSourceObj;
                dataSource = toJRDataSource(sourceObj);
            }  else if (dataSourceObj.getClass().isArray()) {
                Object[] sourceObj = (Object[]) dataSourceObj;
                dataSource = toJRDataSource(Arrays.asList(sourceObj).iterator());
            } else {
                throw new AssertionError("Objects of type: " + dataSourceObj.getClass() + " cannot be converted to a row in a " +
                                         "JRDataSource");
            }

            print = fillManager.fill(
                    jasperTemplateBuild.getAbsolutePath(),
                    values.asMap(),
                    dataSource);
        } else {
            print = fillManager.fill(
                    jasperTemplateBuild.getAbsolutePath(),
                    values.asMap(),
                    new JREmptyDataSource());
        }
        print.setProperty(Renderable.PROPERTY_IMAGE_DPI, String.valueOf(Math.round(maxDpi[0])));
        return new Print(print, maxDpi[0], maxDpi[1], getLocalJasperReportsContext(config));
    }

    private JasperFillManager getJasperFillManager(@Nonnull final Configuration configuration) {
        LocalJasperReportsContext ctx = getLocalJasperReportsContext(configuration);
        return JasperFillManager.getInstance(ctx);
    }

    private LocalJasperReportsContext getLocalJasperReportsContext(final Configuration configuration) {
        LocalJasperReportsContext ctx = new LocalJasperReportsContext(DefaultJasperReportsContext.getInstance());
        ctx.setClassLoader(getClass().getClassLoader());
        ctx.setExtensions(RepositoryService.class,
                Lists.newArrayList(new MapfishPrintRepositoryService(configuration, this.httpRequestFactory)));
        return ctx;
    }

    private JRDataSource toJRDataSource(@Nonnull final Iterator iterator) {
        List<Map<String, ?>> rows = new ArrayList<Map<String, ?>>();
        while (iterator.hasNext()) {
            Object next = iterator.next();
            if (next instanceof Values) {
                Values values = (Values) next;
                rows.add(values.asMap());
            } else if (next instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, ?> map = (Map<String, ?>) next;
                rows.add(map);
            } else {
                throw new AssertionError("Objects of type: " + next.getClass() + " cannot be converted to a row in a JRDataSource");
            }
        }
        return new JRMapCollectionDataSource(rows);
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
        @Nonnull public final JasperPrint print;
        @Nonnegative public final double dpi;
        @Nonnegative public final double requestorDpi;
        @Nonnull public final JasperReportsContext context;

        // CHECKSTYLE:ON

        private Print(@Nonnull final JasperPrint print,
                      @Nonnegative final double dpi,
                      @Nonnegative final double requestorDpi,
                      @Nonnull final JasperReportsContext context) {
            this.print = print;
            this.dpi = dpi;
            this.requestorDpi = requestorDpi;
            this.context = context;
        }
    }

}
