package org.mapfish.print;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunListener;
import org.locationtech.jts.util.Assert;
import org.locationtech.jts.util.AssertionFailedException;
import org.mapfish.print.servlet.MapPrinterServlet;
import org.mapfish.print.test.util.ImageSimilarity;
import org.mapfish.print.url.data.Handler;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.verapdf.core.EncryptedPdfException;
import org.verapdf.core.ModelParsingException;
import org.verapdf.core.ValidationException;
import org.verapdf.gf.model.GFModelParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.ValidationResult;
import org.verapdf.pdfa.validation.validators.ValidatorFactory;

/**
 * To run this test make sure that the test GeoServer is running:
 *
 * <p>./gradlew examples:farmRun
 *
 * <p>Or run the tests with the following task (which automatically starts the server):
 *
 * <p>./gradlew examples:geoserver
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {ExamplesTest.DEFAULT_SPRING_XML, ExamplesTest.TEST_SPRING_XML})
public class ExamplesTest {
  public static final String DEFAULT_SPRING_XML =
      "classpath:mapfish-spring-application-context.xml";
  public static final String TEST_SPRING_XML =
      "classpath:test-http-request-factory-application-context.xml";
  public static final String[] BITMAP_FORMATS = {"bmp", "png", "jpeg", "tiff", "jpg", "tif"};
  private static final Map<String, String> FORMAT_TO_CONTENT_TYPE =
      Map.of(
          "pdf", "application/pdf",
          "png", "image/png",
          "jpg", "image/jpeg",
          "jpeg", "image/jpeg",
          "tif", "image/tiff",
          "tiff", "image/tiff",
          "gif", "image/gif",
          "bmp", "image/bmp");
  private static final Logger LOGGER = LoggerFactory.getLogger(ExamplesTest.class);
  private static final String REQUEST_DATA_FILE = "requestData(-.*)?.json";
  private static final String CONFIG_FILE = "config.yaml";

  /**
   * If this system property is set then it will be interpreted as a regular expression and will be
   * used to filter the examples that are run.
   *
   * <p>For example: -Dexamples.filter=verbose.*
   *
   * <p>will run all examples starting with verbose.
   */
  private static final String FILTER_PROPERTY = "examples.filter";

  private static final Pattern REQUEST_MATCH_ALL = Pattern.compile(".*");
  private static final Pattern EXAMPLE_MATCH_ALL = Pattern.compile(".*");
  private static Pattern exampleFilter;
  private static Pattern requestFilter;

  static {
    Handler.configureProtocolHandler();
  }

  @Autowired MapPrinter mapPrinter;

  @BeforeClass
  public static void setUp() {
    final ClassLoader classLoader = AbstractApiTest.class.getClassLoader();
    final URL logfile = classLoader.getResource("logback.xml");
    final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    try {
      final JoranConfigurator configurator = new JoranConfigurator();
      configurator.setContext(loggerContext);
      // Call context.reset() to clear any previous configuration, e.g. default
      // configuration. For multi-step configuration, omit calling context.reset().
      loggerContext.reset();
      configurator.doConfigure(Objects.requireNonNull(logfile));
    } catch (JoranException je) {
      // StatusPrinter will handle this
    }
    StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);

    String filterProperty = System.getProperty(FILTER_PROPERTY);
    if (!StringUtils.isEmpty(filterProperty)) {
      String[] parts = filterProperty.split("/", 2);

      if (parts.length == 1) {
        requestFilter = REQUEST_MATCH_ALL;
      } else {
        requestFilter = Pattern.compile(parts[1]);
      }

      exampleFilter = Pattern.compile(parts[0]);

    } else {
      requestFilter = REQUEST_MATCH_ALL;
      exampleFilter = EXAMPLE_MATCH_ALL;
    }
  }

  private static File getFile(Class<?> testClass, String fileName) {
    final URL resource = testClass.getResource(fileName);
    if (resource == null) {
      throw new AssertionError("Unable to find test resource: " + fileName);
    }

    return new File(resource.getFile());
  }

  public static void main(String[] args) {
    JUnitCore junit = new JUnitCore();
    if (args.length < 1) {
      System.err.println(
          "This main is expected to have at least one parameter, it is a regular "
              + "expression for selecting the examples to run");
      System.exit(1);
    }
    if (args.length > 2) {
      System.err.println(
          "A maximum of 2 parameters are allowed.  param 1=example regex, param2 = "
              + "configRegexp");
      System.exit(1);
    }

    String filter = args[0];
    if (args.length == 2) {
      filter += "/" + args[1];
    }
    System.setProperty(FILTER_PROPERTY, filter);
    RunListener textListener = new TextListener(System.out);
    junit.addListener(textListener);
    junit.run(ExamplesTest.class);
  }

  @Test
  public void testExampleDirectoryNames() {
    final String namePattern = "[a-zA-Z0-9_]+";
    final File examplesDir = getFile(ExamplesTest.class, "/examples");
    StringBuilder errors = new StringBuilder();
    for (File example : Objects.requireNonNull(examplesDir.listFiles())) {
      if (example.isDirectory() && !examplesDir.getName().matches(namePattern)) {
        errors.append("\n    * ").append(examplesDir.getName());
      }
    }

    assertEquals(
        String.format(
            "All example directory names must match the pattern: '%s'.  "
                + "The following fail that test: %s",
            namePattern, errors),
        0,
        errors.length());
  }

  @Test
  public void testAllExamples() {
    Map<String, Throwable> errors = new HashMap<>();

    int testsRan = 0;
    final File examplesDir = getFile(ExamplesTest.class, "/examples");

    for (File example : Objects.requireNonNull(examplesDir.listFiles())) {
      if (example.isDirectory() && exampleFilter.matcher(example.getName()).matches()) {
        testsRan += runExample(example, errors);
      }
    }

    reportErrors(errors, testsRan);
  }

  @Test
  public void testPDFA() {
    final File examplesDir = getFile(ExamplesTest.class, "/examples");
    Map<String, Throwable> errors = new HashMap<>();
    runExample(new File(examplesDir, "pdf_a_compliant"), errors, true);
    reportErrors(errors, 1);
  }

  private void reportErrors(final Map<String, Throwable> errors, final int testsRan) {
    if (!errors.isEmpty()) {
      for (Map.Entry<String, Throwable> error : errors.entrySet()) {
        System.err.println("\nExample: '" + error.getKey() + "' failed with the error:");
        error.getValue().printStackTrace();
      }

      StringBuilder errorReport = new StringBuilder();
      errorReport.append("\n");
      errorReport.append(errors.size());
      errorReport.append(" errors encountered while running ");
      errorReport.append(testsRan);
      errorReport.append(" examples.\n");
      errorReport.append(
          "See Standard Error for the stack traces.  A summary is as follows...\n\n");
      for (Map.Entry<String, Throwable> error : errors.entrySet()) {
        StringBuilder exampleName = new StringBuilder();
        exampleName.append("The example ");
        exampleName.append(error.getKey());
        errorReport.append(exampleName);
        errorReport.append('\n');
        //noinspection ReplaceAllDot
        errorReport.append(exampleName.toString().replaceAll(".", "="));
        errorReport.append('\n');
        errorReport.append("Failed with the error:\n");
        final StringWriter sw = new StringWriter();
        error.getValue().printStackTrace(new PrintWriter(sw));
        errorReport.append(sw.toString());
        errorReport.append('\n');
      }
      errorReport.append("\n\n");
      fail(errorReport.toString());
    }
  }

  private int runExample(File example, Map<String, Throwable> errors) {
    return runExample(example, errors, false);
  }

  private int runExample(File example, Map<String, Throwable> errors, boolean pdfaValidation) {
    int testsRan = 0;
    try {
      final File configFile = new File(example, CONFIG_FILE);
      this.mapPrinter.setConfiguration(configFile);

      if (!hasRequestFile(example)) {
        throw new AssertionError(
            String.format(
                "Example: '%s' does not have any request data files.", example.getName()));
      }
      for (File requestFile : Objects.requireNonNull(example.listFiles())) {
        if (!requestFile.isFile() || !requestFilter.matcher(requestFile.getName()).matches()) {
          continue;
        }
        try {
          if (isRequestDataFile(requestFile)) {
            // WARN to be displayed in the Travis logs
            LOGGER.warn("Run example '{}' ({})", example.getName(), requestFile.getName());
            String requestData =
                new String(
                    java.nio.file.Files.readAllBytes(requestFile.toPath()),
                    Constants.DEFAULT_CHARSET);

            final PJsonObject jsonSpec = MapPrinter.parseSpec(requestData);

            testsRan++;
            String outputFormat = jsonSpec.getInternalObj().getString("outputFormat");
            byte[] data = requestData.getBytes(StandardCharsets.UTF_8);

            HttpURLConnection http =
                createHttpUrlConnection(example.getName(), outputFormat, data.length);
            http.connect();
            try (OutputStream os = http.getOutputStream()) {
              os.write(data);
            }
            int responseCode = http.getResponseCode();

            InputStream inputStr = http.getInputStream();
            if (responseCode != 200) {
              String encoding =
                  http.getContentEncoding() == null ? "UTF-8" : http.getContentEncoding();
              String response = IOUtils.toString(inputStr, encoding);
              Assert.isTrue(false, response);
            }

            Assert.equals(
                FORMAT_TO_CONTENT_TYPE.get(outputFormat), http.getHeaderField("Content-Type"));

            if (pdfaValidation) {
              pdfaValidate(errors, http, example.getName(), requestFile.getName());
            } else {
              compareImages(errors, http.getInputStream(), example, requestFile, outputFormat);
            }
          }
        } catch (RuntimeException e) {
          errors.put(String.format("%s (%s)", example.getName(), requestFile.getName()), e);
        }
      }
    } catch (IOException | RuntimeException e) {
      errors.put(example.getName(), e);
    }

    return testsRan;
  }

  private Map<String, Throwable> compareImages(
      Map<String, Throwable> errors,
      InputStream stream,
      File example,
      File requestFile,
      String outputFormat)
      throws IOException {
    BufferedImage image = ImageIO.read(stream);
    if (ArrayUtils.contains(BITMAP_FORMATS, outputFormat)) {
      File expectedOutputDir = new File(example, "expected_output");
      File expectedOutput = getExpectedOutput(outputFormat, requestFile, expectedOutputDir);
      if (!expectedOutput.exists()) {
        errors.put(
            example.getName() + " (" + requestFile.getName() + ")",
            new Exception("File not found: " + expectedOutput.toString()));
      }

      if (!"bmp".equals(outputFormat)) {
        // BMP is not supported by ImageIO
        new ImageSimilarity(expectedOutput).assertSimilarity(image);
      }
    }
    return errors;
  }

  private Map<String, Throwable> pdfaValidate(
      Map<String, Throwable> errors,
      HttpURLConnection http,
      String exampleName,
      String requestFileName) {
    PDFAFlavour flavour = PDFAFlavour.PDFA_1_A;
    PDFAValidator validator = ValidatorFactory.createValidator(flavour, false);
    try {
      GFModelParser parser = GFModelParser.createModelWithFlavour(http.getInputStream(), flavour);
      ValidationResult result = validator.validate(parser);
      LOGGER.warn("Example is PDF/A conform: {}", result.isCompliant());
      Assert.isTrue(result.isCompliant());
    } catch (EncryptedPdfException
        | ModelParsingException
        | ValidationException
        | IOException
        | AssertionFailedException e) {
      errors.put(String.format("%s (%s)", exampleName, requestFileName), e);
    }
    return errors;
  }

  private File getExpectedOutput(String outputFormat, File requestFile, File expectedOutputDir) {
    final String imageName = requestFile.getName().replace(".json", "." + outputFormat);
    return new File(expectedOutputDir, imageName);
  }

  private boolean hasRequestFile(File example) {
    for (File file : Objects.requireNonNull(example.listFiles())) {
      if (isRequestDataFile(file)) {
        return true;
      }
    }
    return false;
  }

  private boolean isRequestDataFile(File requestFile) {
    return requestFile.getName().matches(REQUEST_DATA_FILE);
  }

  private HttpURLConnection createHttpUrlConnection(
      String exampleName, String outputFormat, int contentLength) throws IOException {
    HttpURLConnection http = (HttpURLConnection) createUrlConnection(exampleName, outputFormat);
    http.setRequestMethod("POST");
    http.setDoInput(true);
    http.setDoOutput(true);
    http.setFixedLengthStreamingMode(contentLength);
    http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    return http;
  }

  private URLConnection createUrlConnection(String exampleName, String outputFormat)
      throws IOException {
    URL url =
        new URL(
            AbstractApiTest.PRINT_SERVER
                + "print/"
                + exampleName
                + MapPrinterServlet.CREATE_AND_GET_URL
                + "."
                + outputFormat);
    URLConnection connection = url.openConnection();
    connection.setRequestProperty("Referer", AbstractApiTest.PRINT_SERVER);
    connection.setRequestProperty("Cache-Control", "max-age=0");
    return connection;
  }
}
