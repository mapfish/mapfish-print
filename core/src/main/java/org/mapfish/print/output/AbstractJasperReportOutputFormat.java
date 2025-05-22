package org.mapfish.print.output;

import com.google.common.annotations.VisibleForTesting;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import javax.xml.parsers.ParserConfigurationException;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.SimpleJasperReportsContext;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.renderers.Renderable;
import net.sf.jasperreports.repo.RepositoryService;
import org.locationtech.jts.util.AssertionFailedException;
import org.mapfish.print.Constants;
import org.mapfish.print.PrintException;
import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.config.WorkingDirectories;
import org.mapfish.print.http.MfClientHttpRequestFactoryImpl;
import org.mapfish.print.processor.Processor;
import org.mapfish.print.processor.ProcessorDependencyGraph;
import org.mapfish.print.processor.http.MfClientHttpRequestFactoryProvider;
import org.mapfish.print.processor.jasper.JasperReportBuilder;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** The AbstractJasperReportOutputFormat class. */
public abstract class AbstractJasperReportOutputFormat implements OutputFormat {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(AbstractJasperReportOutputFormat.class);

  @Autowired private ForkJoinPool forkJoinPool;

  @Autowired private WorkingDirectories workingDirectories;

  @Autowired private MfClientHttpRequestFactoryImpl httpRequestFactory;

  @Value("${httpRequest.fetchRetry.maxNumber}")
  private int httpRequestMaxNumberFetchRetry;

  @Value("${httpRequest.fetchRetry.intervalMillis}")
  private int httpRequestFetchRetryIntervalMillis;

  /**
   * Export the report to the output stream.
   *
   * @param outputStream the output stream to export to
   * @param print the report
   */
  protected abstract void doExport(OutputStream outputStream, Print print)
      throws JRException, IOException;

  @Override
  public final Processor.ExecutionContext print(
      @Nonnull final Map<String, String> mdcContext,
      final PJsonObject requestData,
      final Configuration config,
      final File configDir,
      final File taskDirectory,
      final OutputStream outputStream)
      throws Exception {
    final Print print = getJasperPrint(mdcContext, requestData, config, configDir, taskDirectory);

    if (Thread.currentThread().isInterrupted()) {
      throw new CancellationException();
    }

    doExport(outputStream, print);

    return print.executionContext;
  }

  private JasperFillManager getJasperFillManager(
      final MfClientHttpRequestFactoryProvider httpRequestFactoryProvider) {
    JasperReportsContext ctx = getJasperReportsContext(httpRequestFactoryProvider);
    return JasperFillManager.getInstance(ctx);
  }

  /**
   * Renders the jasper report.
   *
   * @param mdcContext the MDC context for the current print job.
   * @param requestData the data from the client, required for writing.
   * @param config the configuration object representing the server side configuration.
   * @param configDir the directory that contains the configuration, used for resolving resources
   *     like images etc...
   * @param taskDirectory the temporary directory for this printing task.
   * @return a jasper print object which can be used to generate a PDF or other outputs.
   * @throws ExecutionException
   */
  @VisibleForTesting
  public final Print getJasperPrint(
      @Nonnull final Map<String, String> mdcContext,
      final PJsonObject requestData,
      final Configuration config,
      final File configDir,
      final File taskDirectory)
      throws JRException, SQLException, ExecutionException {
    final String templateName = requestData.getString(Constants.JSON_LAYOUT_KEY);

    final Template template = config.getTemplate(templateName);
    final File jasperTemplateFile = new File(configDir, template.getReportTemplate());
    final File jasperTemplateBuild =
        this.workingDirectories.getBuildFileFor(
            config,
            jasperTemplateFile,
            JasperReportBuilder.JASPER_REPORT_COMPILED_FILE_EXT,
            LOGGER);

    final Values values =
        new Values(
            mdcContext,
            requestData,
            template,
            taskDirectory,
            this.httpRequestFactory,
            jasperTemplateBuild.getParentFile(),
            httpRequestMaxNumberFetchRetry,
            httpRequestFetchRetryIntervalMillis);

    double maxDpi = maxDpi(values);

    final ProcessorDependencyGraph.ProcessorGraphForkJoinTask task =
        template.getProcessorGraph().createTask(values);
    final ForkJoinTask<Values> taskFuture = this.forkJoinPool.submit(task);

    try {
      taskFuture.get();
    } catch (InterruptedException exc) {
      // if cancel() is called on the current thread, this exception will be thrown.
      // in this case, also properly cancel the task future.
      taskFuture.cancel(true);
      Thread.currentThread().interrupt();
      throw new CancellationException();
    }

    // Fill the resource bundle
    String resourceBundle = config.getResourceBundle();
    if (resourceBundle != null) {
      values.put(
          "REPORT_RESOURCE_BUNDLE",
          ResourceBundle.getBundle(
              resourceBundle,
              values.getObject(Values.LOCALE_KEY, Locale.class),
              new ResourceBundleClassLoader(configDir)));
    }

    ValuesLogger.log(templateName, template, values);
    JasperFillManager fillManager =
        getJasperFillManager(
            values.getObject(
                Values.CLIENT_HTTP_REQUEST_FACTORY_KEY, MfClientHttpRequestFactoryProvider.class));

    checkRequiredValues(config, values, template.getReportTemplate());

    final JasperPrint print;

    for (String jdbcDriver : template.getJdbcDrivers()) {
      try {
        Class.forName(jdbcDriver);
      } catch (ClassNotFoundException e) {
        throw new AssertionError(
            String.format(
                "Unable to load JDBC driver: "
                    + jdbcDriver
                    + " ensure that the web application has the jar on its classpath"));
      }
    }
    if (template.getJdbcUrl() != null) {
      Connection connection = null;
      try {
        if (template.getJdbcUser() != null) {
          connection =
              DriverManager.getConnection(
                  template.getJdbcUrl(), template.getJdbcUser(), template.getJdbcPassword());
        } else {
          connection = DriverManager.getConnection(template.getJdbcUrl());
        }

        print = fillManager.fill(jasperTemplateBuild.getAbsolutePath(), values.asMap(), connection);
      } finally {
        if (connection != null && !connection.isClosed()) {
          connection.close();
        }
      }
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
              String.format(
                  "Objects of type: %s cannot be converted to a row in a " + "JRDataSource",
                  dataSourceObj.getClass()));
        }
      } else {
        dataSource = new JREmptyDataSource();
      }
      checkRequiredFields(config, dataSource, template.getReportTemplate());
      print = fillManager.fill(jasperTemplateBuild.getAbsolutePath(), values.asMap(), dataSource);
    }
    print.setProperty(Renderable.PROPERTY_IMAGE_DPI, String.valueOf(Math.round(maxDpi)));
    return new Print(
        getJasperReportsContext(
            values.getObject(
                Values.CLIENT_HTTP_REQUEST_FACTORY_KEY, MfClientHttpRequestFactoryProvider.class)),
        print,
        values,
        maxDpi,
        task.getExecutionContext());
  }

  private void checkRequiredFields(
      final Configuration configuration,
      final JRDataSource dataSource,
      final String reportTemplate) {
    if (dataSource instanceof JRRewindableDataSource) {
      JRRewindableDataSource source = (JRRewindableDataSource) dataSource;
      StringBuilder wrongType = new StringBuilder();
      try {
        while (source.next()) {
          final Document document = parseXML(configuration, reportTemplate);
          final NodeList parameters = document.getElementsByTagName("field");
          JRDesignField field = new JRDesignField();
          for (int i = 0; i < parameters.getLength(); i++) {
            final Element param = (Element) parameters.item(i);
            final String name = param.getAttribute("name");
            field.setName(name);
            Object fieldValue = dataSource.getFieldValue(field);
            if (fieldValue != null) {
              final String type = param.getAttribute("class");
              Class<?> clazz = Class.forName(type);
              if (!clazz.isInstance(fieldValue)) {
                wrongType
                    .append("\t* ")
                    .append(name)
                    .append(": ")
                    .append(fieldValue.getClass().getName());
                wrongType.append(" expected type: ").append(type).append("\n");
              }
            } else {
              LOGGER.warn(
                  "The field {} in {} is not available in at least one of the rows in the"
                      + " datasource.  This may not be an error.",
                  name,
                  reportTemplate);
            }
          }
        }
        source.moveFirst();
      } catch (JRException
          | ParserConfigurationException
          | IOException
          | ClassNotFoundException
          | SAXException e) {
        throw new PrintException("Checking required fields failed", e);
      }

      StringBuilder finalError = new StringBuilder();
      assertNoError(reportTemplate, wrongType, finalError);
    }
  }

  private void checkRequiredValues(
      final Configuration configuration, final Values values, final String reportTemplate) {
    StringBuilder missing = new StringBuilder();
    StringBuilder wrongType = new StringBuilder();
    try {

      final Document document = parseXML(configuration, reportTemplate);
      final NodeList parameters = document.getElementsByTagName("parameter");
      for (int i = 0; i < parameters.getLength(); i++) {
        final Element param = (Element) parameters.item(i);
        final String name = param.getAttribute("name");
        if (!param.getParentNode().getNodeName().equals("jasperReport")) {
          continue;
        }
        if (!values.containsKey(name)) {
          if (param.getElementsByTagName("defaultValueExpression").getLength() == 0) {
            missing.append("\t* ").append(name).append("\n");
          }
        } else {
          final String type = param.getAttribute("class");
          Class<?> clazz = Class.forName(type);
          Object value = values.getObject(name, Object.class);
          if (!clazz.isInstance(value)) {
            wrongType.append("\t* ").append(name).append(": ").append(value.getClass().getName());
            wrongType.append(" expected type: ").append(type).append("\n");
          }
        }
      }
    } catch (ParserConfigurationException | IOException | SAXException | ClassNotFoundException e) {
      throw new PrintException("Checking required values failed", e);
    }

    StringBuilder finalError = new StringBuilder();
    if (missing.length() > 0) {
      finalError
          .append("The following parameters are declared in ")
          .append(reportTemplate)
          .append(" but are not output values of processors or attributes.")
          .append("\nEither remove the references or update the configuration so that all the ")
          .append("parameters are available for the report.\n\n")
          .append(missing);
    }
    assertNoError(reportTemplate, wrongType, finalError);
  }

  private void assertNoError(
      final String reportTemplate, final StringBuilder wrongType, final StringBuilder finalError) {
    if (wrongType.length() > 0) {
      finalError
          .append("The following parameters are declared in ")
          .append(reportTemplate)
          .append(
              ".  The class attribute in the template xml does not match the class of the "
                  + "actual object.")
          .append(
              "\nEither change the declaration in the jasper template or update the "
                  + "configuration so that the parameters have the correct type.\n\n")
          .append(wrongType);
    }

    if (finalError.length() > 0) {
      throw new AssertionFailedException(finalError.toString());
    }
  }

  private Document parseXML(final Configuration configuration, final String reportTemplate)
      throws ParserConfigurationException, IOException, SAXException {
    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setValidating(false);
    final DocumentBuilder documentBuilder = factory.newDocumentBuilder();
    final byte[] bytes = configuration.loadFile(reportTemplate);
    return documentBuilder.parse(new ByteArrayInputStream(bytes));
  }

  private JasperReportsContext getJasperReportsContext(
      final MfClientHttpRequestFactoryProvider httpRequestFactoryProvider) {
    SimpleJasperReportsContext ctx =
        new SimpleJasperReportsContext(DefaultJasperReportsContext.getInstance());
    ctx.setExtensions(
        RepositoryService.class,
        Collections.singletonList(
            new MapfishPrintRepositoryService(httpRequestFactoryProvider.get())));
    return ctx;
  }

  private JRDataSource toJRDataSource(@Nonnull final Iterator iterator) {
    List<Map<String, ?>> rows = new ArrayList<>();
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
        throw new AssertionError(
            String.format(
                "Objects of type: %s cannot be converted to a row in a JRDataSource",
                next.getClass()));
      }
    }
    return new JRMapCollectionDataSource(rows);
  }

  private double maxDpi(final Values values) {
    Map<String, MapAttribute.MapAttributeValues> maps =
        values.find(MapAttribute.MapAttributeValues.class);
    double maxDpi = Constants.PDF_DPI;
    for (MapAttribute.MapAttributeValues attributeValues : maps.values()) {
      if (attributeValues.getDpi() > maxDpi) {
        maxDpi = attributeValues.getDpi();
      }
    }
    return maxDpi;
  }

  /** The print information for doing the export. */
  public static final class Print {
    /** The print information for Jasper. */
    @Nonnull public final JasperPrint print;

    /** The print DPI. */
    @Nonnegative public final double dpi;

    /** The execution context for the print job. */
    @Nonnull public final Processor.ExecutionContext executionContext;

    /** The JasperReports context for the print job. */
    @Nonnull public final JasperReportsContext context;

    /** The values used to do the print. */
    @Nonnull public final Values values;

    private Print(
        @Nonnull final JasperReportsContext context,
        @Nonnull final JasperPrint print,
        @Nonnull final Values values,
        @Nonnegative final double dpi,
        @Nonnull final Processor.ExecutionContext executionContext) {
      this.print = print;
      this.context = context;
      this.values = values;
      this.dpi = dpi;
      this.executionContext = executionContext;
    }
  }
}
