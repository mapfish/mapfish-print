package org.mapfish.print;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

import org.apache.commons.lang.ArrayUtils;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunListener;
import org.mapfish.print.servlet.MapPrinterServlet;
import org.mapfish.print.servlet.oldapi.OldAPIRequestConverter;
import org.mapfish.print.test.util.ImageSimilarity;
import org.mapfish.print.url.data.Handler;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mapfish.print.servlet.MapPrinterServlet.JSON_ATTRIBUTES;
import static org.mapfish.print.servlet.MapPrinterServlet.JSON_REQUEST_HEADERS;

/**
 * To run this test make sure that the test GeoServer is running:
 * <p></p>
 * ./gradlew examples:farmRun
 * <p></p>
 * Or run the tests with the following task (which automatically starts the server):
 * <p></p>
 * ./gradlew examples:farmIntegrationTest
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        ExamplesTest.DEFAULT_SPRING_XML,
        ExamplesTest.TEST_SPRING_XML
})
public class ExamplesTest {
    static {
        Handler.configureProtocolHandler();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ExamplesTest.class);

    public static final String DEFAULT_SPRING_XML = "classpath:mapfish-spring-application-context.xml";
    public static final String TEST_SPRING_XML = "classpath:test-http-request-factory-application-context.xml";

    private static final String REQUEST_DATA_FILE = "requestData(-.*)?.json";
    private static final String OLD_API_REQUEST_DATA_FILE = "oldApi-requestData(-.*)?.json";
    private static final String CONFIG_FILE = "config.yaml";
    /**
     * If this system property is set then it will be interpreted as a regular expression and will be used
     * to filter the examples that are run.
     *
     * For example:
     * -Dexamples.filter=verbose.*
     *
     * will run all examples starting with verbose.
     */
    private static final String FILTER_PROPERTY = "examples.filter";
    private static final Pattern REQUEST_MATCH_ALL = Pattern.compile(".*");
    private static final Pattern EXAMPLE_MATCH_ALL = Pattern.compile(".*");
    @Autowired
    MapPrinter mapPrinter;

    private static Pattern exampleFilter;
    private static Pattern requestFilter;

    @BeforeClass
    public static void setUp() throws Exception {
        final ClassLoader classLoader = AbstractApiTest.class.getClassLoader();
        final URL logfile = classLoader.getResource("logback.xml");
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            final JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(loggerContext);
            // Call context.reset() to clear any previous configuration, e.g. default
            // configuration. For multi-step configuration, omit calling context.reset().
            loggerContext.reset();
            configurator.doConfigure(logfile);
        } catch (JoranException je) {
            // StatusPrinter will handle this
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);

        String filterProperty = System.getProperty(FILTER_PROPERTY);
        if (!Strings.isNullOrEmpty(filterProperty)) {
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

    @Test
    public void testExampleDirectoryNames() throws Exception {
        final String namePattern = "[a-zA-Z0-9_]+";
        final File examplesDir = getFile(ExamplesTest.class, "/examples");
        StringBuilder errors = new StringBuilder();
        for (File example : Files.fileTreeTraverser().children(examplesDir)) {
            if (example.isDirectory() && !examplesDir.getName().matches(namePattern)) {
                errors.append("\n    * ").append(examplesDir.getName());
            }
        }

        assertEquals(String.format("All example directory names must match the pattern: '%s'.  " +
                "The following fail that test: %s", namePattern, errors), 0, errors.length());
    }
    @Test
    public void testAllExamples() throws Exception {
        Map<String, Throwable> errors = Maps.newHashMap();

        int testsRan = 0;
        final File examplesDir = getFile(ExamplesTest.class, "/examples");

        for (File example : Files.fileTreeTraverser().children(examplesDir)) {
            if (example.isDirectory() && exampleFilter.matcher(example.getName()).matches()) {
                testsRan += runExample(example, errors);
            }
        }

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
            errorReport.append("See Standard Error for the stack traces.  A summary is as follows...\n\n");
            for (Map.Entry<String, Throwable> error : errors.entrySet()) {
                StringBuilder exampleName = new StringBuilder();
                exampleName.append("The example ");
                exampleName.append(error.getKey());
                errorReport.append(exampleName);
                errorReport.append('\n');
                errorReport.append(exampleName.toString().replaceAll(".", "="));
                errorReport.append('\n');
                errorReport.append("Failed with the error:\n");
                errorReport.append(error.getValue());
                errorReport.append('\n');
            }
            errorReport.append("\n\n");
            fail(errorReport.toString());
        }
    }

    private int runExample(File example, Map<String, Throwable> errors) {
        int testsRan = 0;
        try {
            final File configFile = new File(example, CONFIG_FILE);
            this.mapPrinter.setConfiguration(configFile);

            if (!hasRequestFile(example)) {
                throw new AssertionError(String.format(
                        "Example: '%s' does not have any request data files.", example.getName()));
            }
            for (File requestFile : Files.fileTreeTraverser().children(example)) {
                if (!requestFile.isFile() || !requestFilter.matcher(requestFile.getName()).matches()) {
                    continue;
                }
                try {
                    if (isRequestDataFile(requestFile)) {
                        // WARN to be displayed in the Travis logs
                        LOGGER.warn("Run example '{}' ({})", example.getName(), requestFile.getName());
                        String requestData = Files.asCharSource(requestFile,
                                Charset.forName(Constants.DEFAULT_ENCODING)).read();

                        final PJsonObject jsonSpec;
                        if (requestFile.getName().matches(OLD_API_REQUEST_DATA_FILE)) {
                            PJsonObject oldSpec = MapPrinterServlet.parseJson(requestData, null);
                            jsonSpec = OldAPIRequestConverter.convert(oldSpec,
                                    this.mapPrinter.getConfiguration());
                        } else {
                            jsonSpec = MapPrinter.parseSpec(requestData);
                        }

                        testsRan++;
                        String outputFormat = jsonSpec.getInternalObj().getString("outputFormat");
                        if (!ArrayUtils.contains(new String[]{"png", "jpg", "tiff"}, outputFormat)) {
                            jsonSpec.getInternalObj().put("outputFormat", "png");
                            outputFormat = "png";
                        }
                        ByteArrayOutputStream out = new ByteArrayOutputStream();

                        JSONObject headers = new JSONObject();
                        headers.append("Cookie", "examplesTestCookie=value");
                        headers.append("Referer", "http://localhost:8080/print");
                        headers.append("Host","localhost");
                        headers.append("User-Agent",
                                "Mozilla/5.0 (Windows NT 6.3; WOW64; rv:31.0) Gecko/20100101 Firefox/31.0");
                        headers.append("Accept",
                                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                        headers.append("Accept-Language", "en-US,en;q=0.5");
                        headers.append("Accept-Encoding", "gzip, deflate");
                        headers.append("Connection", "keep-alive");
                        headers.append("Cache-Control", "max-age=0");
                        JSONObject headersAttribute = new JSONObject();
                        headersAttribute.put(JSON_REQUEST_HEADERS, headers);

                        jsonSpec.getJSONObject(JSON_ATTRIBUTES).getInternalObj().put(
                                JSON_REQUEST_HEADERS, headersAttribute);
                        this.mapPrinter.print("main", jsonSpec, out);

                        BufferedImage image = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));

                        File expectedOutputDir = new File(example, "expected_output");
                        File expectedOutput = getExpectedOutput(outputFormat, requestFile, expectedOutputDir);
                        if (!expectedOutput.exists()) {
                            errors.put(
                                    example.getName() + " (" + requestFile.getName() + ")",
                                    new Exception("File not found: " + expectedOutput.toString()));
                            continue;
                        }

                        int similarity = 50;
                        File file = new File(expectedOutputDir, "image-similarity.txt");
                        if (file.isFile()) {
                            String similarityString = Files.toString(file, Constants.DEFAULT_CHARSET);
                            similarity = Integer.parseInt(similarityString.trim());
                        }
                        new ImageSimilarity(expectedOutput).assertSimilarity(image, similarity);
                    }
                } catch (Throwable e) {
                    errors.put(String.format("%s (%s)", example.getName(), requestFile.getName()), e);
                }
            }
        } catch (Throwable e) {
            errors.put(example.getName(), e);
        }

        return testsRan;
    }

    private File getExpectedOutput(String outputFormat, File requestFile, File expectedOutputDir) {
        File platformSpecificDir;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            platformSpecificDir = new File(expectedOutputDir, "win");
        } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            platformSpecificDir = new File(expectedOutputDir, "mac");
        } else {
            platformSpecificDir = new File(expectedOutputDir, "linux");
        }

        final String imageName = requestFile.getName().replace(".json",
                "." + outputFormat);
        if (new File(platformSpecificDir, imageName).exists()) {
            return new File(platformSpecificDir, imageName);
        }
        return new File(expectedOutputDir, imageName);
    }

    private boolean hasRequestFile(File example) {
        for (File file : Files.fileTreeTraverser().children(example)) {
            if (isRequestDataFile(file)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRequestDataFile(File requestFile) {
        return requestFile.getName().matches(REQUEST_DATA_FILE) ||
                requestFile.getName().matches(OLD_API_REQUEST_DATA_FILE);
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
            System.err.println("This main is expected to have at least one parameter, it is a regular " +
                    "expression for selecting the examples to run");
            System.exit(1);
        }
        if (args.length > 2) {
            System.err.println("A maximum of 2 parameters are allowed.  param 1=example regex, param2 = " +
                    "configRegexp");
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
}
