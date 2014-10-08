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
import com.vividsolutions.jts.util.AssertionFailedException;
import jsr166y.ForkJoinPool;
import jsr166y.ForkJoinTask;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.Renderable;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.util.LocalJasperReportsContext;
import net.sf.jasperreports.repo.RepositoryService;
import org.json.JSONException;
import org.mapfish.print.Constants;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.config.WorkingDirectories;
import org.mapfish.print.http.MfClientHttpRequestFactoryImpl;
import org.mapfish.print.parser.MapfishParser;
import org.mapfish.print.processor.jasper.JasperReportBuilder;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * @author Jesse on 5/7/2014.
 */
public abstract class AbstractJasperReportOutputFormat implements OutputFormat {
    private static final Logger LOGGER = LoggerFactory.getLogger(JasperReportPDFOutputFormat.class);

    @Autowired
    private ForkJoinPool forkJoinPool;

    @Autowired
    private WorkingDirectories workingDirectories;

    @Autowired
    private MfClientHttpRequestFactoryImpl httpRequestFactory;


    /**
     * Export the report to the output stream.
     *
     * @param outputStream the output stream to export to
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

    private JasperFillManager getJasperFillManager(@Nonnull final Configuration configuration) {
        LocalJasperReportsContext ctx = getLocalJasperReportsContext(configuration);
        return JasperFillManager.getInstance(ctx);
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
     */
     // CSOFF: RedundantThrows
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
        final File jasperTemplateFile = new File(configDir, template.getReportTemplate());
        final File jasperTemplateBuild = this.workingDirectories.getBuildFileFor(config, jasperTemplateFile,
                JasperReportBuilder.JASPER_REPORT_COMPILED_FILE_EXT, LOGGER);

        final Values values = new Values(requestData, template, this.parser, taskDirectory, this.httpRequestFactory,
                jasperTemplateBuild.getParentFile());

        double[] maxDpi = maxDpi(values);

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

        checkRequiredValues(config, values, template.getReportTemplate());

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

        } else {
            JRDataSource dataSource;
            if (template.getTableDataKey() != null) {
                final Object dataSourceObj = values.getObject(template.getTableDataKey(), Object.class);
            if (dataSourceObj instanceof JRDataSource) {
                dataSource = (JRDataSource) dataSourceObj;
            } else if (dataSourceObj instanceof Iterable) {
                Iterable sourceObj = (Iterable) dataSourceObj;
                dataSource = toJRDataSource(sourceObj.iterator());
                } else if (dataSourceObj instanceof Iterator) {
                Iterator sourceObj = (Iterator) dataSourceObj;
                dataSource = toJRDataSource(sourceObj);
                } else if (dataSourceObj.getClass().isArray()) {
                Object[] sourceObj = (Object[]) dataSourceObj;
                dataSource = toJRDataSource(Arrays.asList(sourceObj).iterator());
            } else {
                throw new AssertionError("Objects of type: " + dataSourceObj.getClass() + " cannot be converted to a row in a " +
                                         "JRDataSource");
            }
        } else {
                dataSource = new JREmptyDataSource();
            }
            checkRequiredFields(config, dataSource, template.getReportTemplate());
            print = fillManager.fill(
                    jasperTemplateBuild.getAbsolutePath(),
                    values.asMap(),
                    dataSource);
        }
        print.setProperty(Renderable.PROPERTY_IMAGE_DPI, String.valueOf(Math.round(maxDpi[0])));
        return new Print(getLocalJasperReportsContext(config), print, values, maxDpi[0], maxDpi[1]);
    }

    private void checkRequiredFields(final Configuration configuration, final JRDataSource dataSource, final String reportTemplate) {
        if (dataSource instanceof JRRewindableDataSource) {
            JRRewindableDataSource source = (JRRewindableDataSource) dataSource;
            StringBuilder wrongType = new StringBuilder();
            try {
                while (source.next()) {
                    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    factory.setValidating(false);
                    final DocumentBuilder documentBuilder = factory.newDocumentBuilder();
                    final byte[] bytes = configuration.loadFile(reportTemplate);
                    final Document document = documentBuilder.parse(new ByteArrayInputStream(bytes));
                    final NodeList parameters = document.getElementsByTagName("field");
                    JRDesignField field = new JRDesignField();
                    for (int i = 0; i < parameters.getLength(); i++) {
                        final Element param = (Element) parameters.item(i);
                        final String name = param.getAttribute("name");
                        field.setName(name);
                        Object record = dataSource.getFieldValue(field);
                        if (record != null) {
                            final String type = param.getAttribute("class");
                            Class<?> clazz = Class.forName(type);

                            if (!clazz.isInstance(record)) {
                                wrongType.append("\t* ").append(name).append(" : ").append(record.getClass().getName());
                                wrongType.append(" expected type: ").append(type).append("\n");
                            } else {
                                LOGGER.warn("The field " + name + " in " + reportTemplate + " is not available in at least one of the " +
                                            "rows in the datasource.  This may not be an error.");
                            }
                        }
                    }
                }
                source.moveFirst();
            } catch (Throwable e) {
                throw ExceptionUtils.getRuntimeException(e);
            }

            StringBuilder finalError = new StringBuilder();

            if (wrongType.length() > 0) {
                finalError.append("The following parameters are declared in ").append(reportTemplate).
                        append(".  The class attribute in the template xml does not match the class of the actual object.").
                        append("\nEither change the declaration in the jasper template or update the configuration so that the ").
                        append("parameters have the correct type.\n\n").
                        append(wrongType);
            }

            if (finalError.length() > 0) {
                throw new AssertionFailedException(finalError.toString());
            }

        }
    }

    private void checkRequiredValues(final Configuration configuration, final Values values, final String reportTemplate) {
        StringBuilder missing = new StringBuilder();
        StringBuilder wrongType = new StringBuilder();
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            final DocumentBuilder documentBuilder = factory.newDocumentBuilder();
            final byte[] bytes = configuration.loadFile(reportTemplate);
            final Document document = documentBuilder.parse(new ByteArrayInputStream(bytes));
            final NodeList parameters = document.getElementsByTagName("parameter");
            for (int i = 0; i < parameters.getLength(); i++) {
                final Element param = (Element) parameters.item(i);
                final String name = param.getAttribute("name");
                if (!values.containsKey(name)) {
                    missing.append("\t* ").append(name).append("\n");
                } else {
                    final String type = param.getAttribute("class");
                    Class<?> clazz = Class.forName(type);
                    Object value = values.getObject(name, Object.class);
                    if (!clazz.isInstance(value)) {
                        wrongType.append("\t* ").append(name).append(" : ").append(value.getClass().getName());
                        wrongType.append(" expected type: ").append(type).append("\n");
                    }
                }
            }
        } catch (Throwable e) {
            throw ExceptionUtils.getRuntimeException(e);
        }

        StringBuilder finalError = new StringBuilder();
        if (missing.length() > 0) {
            finalError.append("The following parameters are declared in ").append(reportTemplate).
                    append(" but are not output values of processors or attributes.").
                    append("\nEither remove the references or update the configuration so that all the ").
                    append("parameters are available for the report.\n\n").
                    append(missing);
        }
        if (wrongType.length() > 0) {
            finalError.append("The following parameters are declared in ").append(reportTemplate).
                    append(".  The class attribute in the template xml does not match the class of the actual object.").
                    append("\nEither change the declaration in the jasper template or update the configuration so that the ").
                    append("parameters have the correct type.\n\n").
                    append(wrongType);
        }

        if (finalError.length() > 0) {
            throw new AssertionFailedException(finalError.toString());
        }
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
        @Nonnull public final Values values;

        // CHECKSTYLE:ON

        private Print(@Nonnull final JasperReportsContext context, @Nonnull final JasperPrint print,
                      @Nonnull final Values values, @Nonnegative final double dpi,
                      @Nonnegative final double requestorDpi) {
            this.print = print;
            this.context = context;
            this.values = values;
            this.dpi = dpi;
            this.requestorDpi = requestorDpi;
        }
    }

}
