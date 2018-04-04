package org.mapfish.print.output;


import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.util.AssertionFailedException;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.util.LocalJasperReportsContext;
import net.sf.jasperreports.renderers.Renderable;
import net.sf.jasperreports.repo.RepositoryService;
import org.json.JSONException;
import org.mapfish.print.Constants;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.config.WorkingDirectories;
import org.mapfish.print.http.MfClientHttpRequestFactoryImpl;
import org.mapfish.print.processor.http.MfClientHttpRequestFactoryProvider;
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
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


/**
 * The AbstractJasperReportOutputFormat class.
 */
public abstract class AbstractJasperReportOutputFormat implements OutputFormat {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJasperReportOutputFormat.class);

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
     * @param print the report
     */
    protected abstract void doExport(final OutputStream outputStream, final Print print)
            throws JRException, IOException;

    @Override
    public final void print(final String jobId, final PJsonObject requestData, final Configuration config,
                            final File configDir, final File taskDirectory, final OutputStream outputStream)
            throws Exception {
        final Print print = getJasperPrint(jobId, requestData, config, configDir, taskDirectory);

        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException();
        }

        doExport(outputStream, print);
    }

    private JasperFillManager getJasperFillManager(
            final MfClientHttpRequestFactoryProvider httpRequestFactoryProvider) {
        LocalJasperReportsContext ctx = getLocalJasperReportsContext(httpRequestFactoryProvider);
        return JasperFillManager.getInstance(ctx);
    }

    /**
     * Renders the jasper report.
     *
     * @param jobId the job ID
     * @param requestData the data from the client, required for writing.
     * @param config the configuration object representing the server side configuration.
     * @param configDir the directory that contains the configuration, used for resolving resources like images etc...
     * @param taskDirectory the temporary directory for this printing task.
     * @return a jasper print object which can be used to generate a PDF or other outputs.
     * @throws ExecutionException
     */
    @VisibleForTesting
    public final Print getJasperPrint(final String jobId, final PJsonObject requestData,
                                      final Configuration config, final File configDir, final File taskDirectory)
            throws JRException, SQLException, ExecutionException, JSONException {
        final String templateName = requestData.getString(Constants.JSON_LAYOUT_KEY);

        final Template template = config.getTemplate(templateName);
        if (template == null) {
            final String possibleTemplates = config.getTemplates().keySet().toString();
            throw new IllegalArgumentException(String.format(
                    "\nThere is no template with the name: %s.\nAvailable templates: %s",
                    templateName, possibleTemplates));
        }
        final File jasperTemplateFile = new File(configDir, template.getReportTemplate());
        final File jasperTemplateBuild = this.workingDirectories.getBuildFileFor(config, jasperTemplateFile,
                JasperReportBuilder.JASPER_REPORT_COMPILED_FILE_EXT, LOGGER);

        final Values values = new Values(jobId, requestData, template, taskDirectory,
                this.httpRequestFactory, jasperTemplateBuild.getParentFile());

        double maxDpi = maxDpi(values);

        final ForkJoinTask<Values> taskFuture = this.forkJoinPool.submit(
                template.getProcessorGraph().createTask(values));

        try {
            taskFuture.get();
        } catch (InterruptedException exc) {
            // if cancel() is called on the current thread, this exception will be thrown.
            // in this case, also properly cancel the task future.
            taskFuture.cancel(true);
            Thread.currentThread().interrupt();
            throw new CancellationException();
        }

        // Fill the locale
        String localeRef = requestData.optString("lang");
        Locale locale = Locale.getDefault();
        if (localeRef != null) {
            String[] localeSplit = localeRef.split("_");
            if (localeSplit.length == 1) {
                locale = new Locale(localeSplit[0]);
            } else if (localeSplit.length == 2) {
                locale = new Locale(localeSplit[0], localeSplit[1]);
            } else if (localeSplit.length > 2) {
                locale = new Locale(localeSplit[0], localeSplit[1], localeSplit[2]);
            }
        }
        values.put("REPORT_LOCALE", locale);

        // Fill the resource bundle
        String resourceBundle = config.getResourceBundle();
        if (resourceBundle != null) {
            values.put("REPORT_RESOURCE_BUNDLE", ResourceBundle.getBundle(
                    resourceBundle, locale, new ResourceBundleClassLoader(configDir)));
        }

        ValuesLogger.log(templateName, template, values);
        JasperFillManager fillManager = getJasperFillManager(
                values.getObject(
                        Values.CLIENT_HTTP_REQUEST_FACTORY_KEY, MfClientHttpRequestFactoryProvider.class));

        checkRequiredValues(config, values, template.getReportTemplate());

        final JasperPrint print;
        if (template.getJdbcUrl() != null) {
            Connection connection;
            if (template.getJdbcUser() != null) {
                connection = DriverManager.getConnection(
                        template.getJdbcUrl(), template.getJdbcUser(), template.getJdbcPassword());
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
                    throw new AssertionError(
                            String.format("Objects of type: %s cannot be converted to a row in a " +
                                    "JRDataSource", dataSourceObj.getClass()));
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
        print.setProperty(Renderable.PROPERTY_IMAGE_DPI, String.valueOf(Math.round(maxDpi)));
        return new Print(getLocalJasperReportsContext(
                values.getObject(
                    Values.CLIENT_HTTP_REQUEST_FACTORY_KEY, MfClientHttpRequestFactoryProvider.class)),
                print, values, maxDpi);
    }

    private void checkRequiredFields(
            final Configuration configuration, final JRDataSource dataSource, final String reportTemplate) {
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
                                wrongType.append("\t* ").append(name).append(" : ")
                                        .append(record.getClass().getName());
                                wrongType.append(" expected type: ").append(type).append("\n");
                            }
                        } else {
                            LOGGER.warn(String.format(
                                    "The field %s in %s is not available in at least one" +
                                    " of the rows in the datasource.  This may not be an error.",
                                    name, reportTemplate));
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
                        append(".  The class attribute in the template xml does not match the class of the " +
                                "actual object.").
                        append("\nEither change the declaration in the jasper template or update the " +
                                "configuration so that the parameters have the correct type.\n\n").
                        append(wrongType);
            }

            if (finalError.length() > 0) {
                throw new AssertionFailedException(finalError.toString());
            }
        }
    }

    private void checkRequiredValues(
            final Configuration configuration, final Values values, final String reportTemplate) {
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
                    if (param.getElementsByTagName("defaultValueExpression").getLength() == 0) {
                        missing.append("\t* ").append(name).append("\n");
                    }
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
                    append(".  The class attribute in the template xml does not match the class of the " +
                            "actual object.").
                    append("\nEither change the declaration in the jasper template or update the " +
                            "configuration so that the parameters have the correct type.\n\n").
                    append(wrongType);
        }

        if (finalError.length() > 0) {
            throw new AssertionFailedException(finalError.toString());
        }
    }

    private LocalJasperReportsContext getLocalJasperReportsContext(
            final MfClientHttpRequestFactoryProvider httpRequestFactoryProvider) {
        LocalJasperReportsContext ctx = new LocalJasperReportsContext(DefaultJasperReportsContext.getInstance());
        ctx.setClassLoader(getClass().getClassLoader());
        ctx.setExtensions(RepositoryService.class,
                Lists.newArrayList(new MapfishPrintRepositoryService(httpRequestFactoryProvider.get())));
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
                throw new AssertionError(String.format(
                        "Objects of type: %s cannot be converted to a row in a JRDataSource",
                        next.getClass()));
            }
        }
        return new JRMapCollectionDataSource(rows);
    }

    private double maxDpi(final Values values) {
        Map<String, MapAttribute.MapAttributeValues> maps = values.find(MapAttribute.MapAttributeValues.class);
        double maxDpi = Constants.PDF_DPI;
        for (MapAttribute.MapAttributeValues attributeValues : maps.values()) {
            if (attributeValues.getDpi() > maxDpi) {
                maxDpi = attributeValues.getDpi();
            }
        }
        return maxDpi;
    }

    /**
     * The print information for doing the export.
     */
    public static final class Print {
        // CHECKSTYLE:OFF
        @Nonnull public final JasperPrint print;
        @Nonnegative public final double dpi;
        @Nonnull public final JasperReportsContext context;
        @Nonnull public final Values values;

        // CHECKSTYLE:ON

        private Print(@Nonnull final JasperReportsContext context, @Nonnull final JasperPrint print,
                      @Nonnull final Values values, @Nonnegative final double dpi) {
            this.print = print;
            this.context = context;
            this.values = values;
            this.dpi = dpi;
        }
    }

}
