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

package org.mapfish.print;

import com.google.common.collect.Maps;
import com.google.common.io.Files;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mapfish.print.servlet.MapPrinterServlet;
import org.mapfish.print.servlet.oldapi.OldAPIRequestConverter;
import org.mapfish.print.test.util.ImageSimilarity;
import org.mapfish.print.wrapper.json.PJsonObject;
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
import javax.imageio.ImageIO;

import static org.junit.Assert.fail;

/**
 * To run this test make sure that the test GeoServer is running:
 * <p/>
 * ./gradlew examples:jettyRun
 * <p/>
 * Or run the tests with the following task (which automatically starts the server):
 * <p/>
 * ./gradlew examples:test
 *
 * @author Jesse on 3/31/14.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        ExamplesTest.DEFAULT_SPRING_XML,
        ExamplesTest.TEST_SPRING_XML
})
public class ExamplesTest {
    public static final String DEFAULT_SPRING_XML = "classpath:mapfish-spring-application-context.xml";
    public static final String TEST_SPRING_XML = "classpath:test-http-request-factory-application-context.xml";

    private static final String REQUEST_DATA_FILE = "requestData(-.*)?.json";
    private static final String OLD_API_REQUEST_DATA_FILE = "oldApi-requestData(-.*)?.json";
    private static final String CONFIG_FILE = "config.yaml";
    @Autowired
    MapPrinter mapPrinter;

    @Test
    public void testAllExamples() throws Exception {
        Map<String, Throwable> errors = Maps.newHashMap();

        int testsRan = 0;
        final File examplesDir = getFile(ExamplesTest.class, "/examples");

        for (File example : Files.fileTreeTraverser().children(examplesDir)) {
            if (example.isDirectory()) {
                testsRan += runExample(example, errors);
            }
        }

        if (!errors.isEmpty()) {
            for (Map.Entry<String, Throwable> error : errors.entrySet()) {
                System.err.println("\nExample: '" + error.getKey() + "' failed with the error:");
                error.getValue().printStackTrace();
            }
            StringBuilder errorReport = new StringBuilder();
            errorReport.append("\n").append(errors.size()).append(" errors encountered while running ");
            errorReport.append(testsRan).append(" examples.\n");
            errorReport.append("See Standard Error for the stacktraces.  A summary is as follows:\n\n");
            for (Map.Entry<String, Throwable> entry : errors.entrySet()) {
                errorReport.append("    * ").append(entry.getKey()).append(" -> ").append(entry.getValue().getMessage());
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

            for (File requestFile : Files.fileTreeTraverser().children(example)) {
                if (!requestFile.isFile()) {
                    continue;
                }
                try {
                    if (requestFile.getName().matches(REQUEST_DATA_FILE) || requestFile.getName().matches(OLD_API_REQUEST_DATA_FILE)) {
                        String requestData = Files.asCharSource(requestFile, Charset.forName(Constants.DEFAULT_ENCODING)).read();
                        final PJsonObject jsonSpec;
                        if (requestFile.getName().matches(OLD_API_REQUEST_DATA_FILE)) {
                            PJsonObject oldSpec = MapPrinterServlet.parseJson(requestData, null);
                            jsonSpec = OldAPIRequestConverter.convert(oldSpec, this.mapPrinter.getConfiguration());
//                            continue;
                        } else {
                            jsonSpec = MapPrinter.parseSpec(requestData);
//                            continue;
                        }
                        testsRan++;
                        jsonSpec.getInternalObj().put("outputFormat", "png");
                        ByteArrayOutputStream out = new ByteArrayOutputStream();

                        Map<String, String> headers = Maps.newHashMap();
                        this.mapPrinter.print(jsonSpec, out);

                        BufferedImage image = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));

//                        File outDir = new File("e:/temp", example.getName()+"/expected_output");
//                        outDir.mkdirs();
//                        ImageIO.write(image, "png", new File(outDir, requestFile.getName().replace(".json", ".png")));

                        File expectedOutputDir = new File(example, "expected_output");
                        File expectedOutput = new File(expectedOutputDir, requestFile.getName().replace(".json", ".png"));
                        new ImageSimilarity(image, 50).assertSimilarity(expectedOutput, 50);
                    }
                } catch (Throwable e) {
                    errors.put(example.getName() + " (" + requestFile.getName() + ")", e);
                }
            }
        } catch (Throwable e) {
            errors.put(example.getName(), e);
        }

        return testsRan;
    }


    private static File getFile(Class<?> testClass, String fileName) {
        final URL resource = testClass.getResource(fileName);
        if (resource == null) {
            throw new AssertionError("Unable to find test resource: " + fileName);
        }

        return new File(resource.getFile());
    }
}
