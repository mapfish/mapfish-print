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
import org.mapfish.print.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;

import static org.junit.Assert.fail;

/**
 * @author Jesse on 3/31/14.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        ExamplesIntegrationTest.DEFAULT_SPRING_XML
})
public class ExamplesIntegrationTest {
    public static final String DEFAULT_SPRING_XML = "classpath:mapfish-spring-application-context.xml";

    private static final String REQUEST_DATA_FILE = "requestData.json";
    private static final String CONFIG_FILE = "config.yaml";
    @Autowired
    MapPrinter mapPrinter;

    @Test
    public void testAllExamples() throws Exception {
        Map<String, Throwable> errors = Maps.newHashMap();

        final File examplesDir = getFile(ExamplesIntegrationTest.class, "/examples");

        for (File example : Files.fileTreeTraverser().children(examplesDir)) {
            if (example.isDirectory()) {
                runExample(example, errors);
            }
        }

        if (!errors.isEmpty()) {
            for (Map.Entry<String, Throwable> error : errors.entrySet()) {
                System.err.println("Example: '" + error.getKey() + "' failed with the error:\n");
                error.getValue().printStackTrace();
            }
            fail(errors.size() + " errors encountered while running examples.");
        }
    }

    private void runExample(File example, Map<String, Throwable> errors) {
        try {
            final File configFile = new File(example, CONFIG_FILE);
            this.mapPrinter.setConfiguration(configFile);
            String requestData = Files.asCharSource(new File(example, REQUEST_DATA_FILE), Charset.forName(Constants.ENCODING)).read();
            final PJsonObject jsonSpec = MapPrinter.parseSpec(requestData);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Map<String, String> headers = Maps.newHashMap();
            this.mapPrinter.print(jsonSpec, out, headers);
        } catch (Throwable e) {
            errors.put(example.getName(), e);
        }
    }


    private static File getFile(Class<?> testClass, String fileName) {
        final URL resource = testClass.getResource(fileName);
        if (resource == null) {
            throw new AssertionError("Unable to find test resource: " + fileName);
        }

        return new File(resource.getFile());
    }
}
