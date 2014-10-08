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

import com.google.common.io.Files;
import org.geotools.referencing.CRS;
import org.junit.runner.RunWith;
import org.mapfish.print.attribute.map.CenterScaleMapBounds;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.WorkingDirectories;
import org.mapfish.print.map.Scale;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that loads the normal spring application context from the spring config file.
 * Subclasses can use Autowired to get dependencies from the application context.
 *
 * @author jesseeichar on 3/25/14.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        AbstractMapfishSpringTest.DEFAULT_SPRING_XML,
        AbstractMapfishSpringTest.TEST_SPRING_XML
})
public abstract class AbstractMapfishSpringTest {
    public static final String DEFAULT_SPRING_XML = "classpath:mapfish-spring-application-context.xml";
    public static final String TEST_SPRING_XML = "classpath:test-http-request-factory-application-context.xml";
    static final Pattern IMPORT_PATTERN = Pattern.compile("@@importFile\\((\\S+)\\)@@");

    @Autowired
    private WorkingDirectories workingDirectories;

    /**
     * Look on the classpath for the named file.  Will look at the root package and in the same package as testClass.
     *
     * @param testClass class to look relative to.
     * @param fileName name of file to find.  Can be a path.
     */
    public static File getFile(Class<?> testClass, String fileName) {
        final URL resource = testClass.getResource(fileName);
        if (resource == null) {
            throw new AssertionError("Unable to find test resource: "+fileName);
        }

        return new File(resource.getFile());
    }

    /**
     * Parse the json string.
     *
     * @param jsonString the json string to parse.
     */
    public static PJsonObject parseJSONObjectFromString(String jsonString) {
        return MapPrinter.parseSpec(jsonString);
    }

    public static PJsonObject parseJSONObjectFromFile(Class<?> testClass, String fileName) throws IOException {
        final File file = getFile(testClass, fileName);
        final Charset charset = Charset.forName(Constants.DEFAULT_ENCODING);
        String jsonString = Files.asCharSource(file, charset).read();
        Matcher matcher = IMPORT_PATTERN.matcher(jsonString);
        while (matcher.find()) {
            final String importFileName = matcher.group(1);
            File importFile = new File(file.getParentFile(), importFileName);
            final String tagToReplace = matcher.group();
            final String importJson = Files.asCharSource(importFile, charset).read();
            jsonString = jsonString.replace(tagToReplace, importJson);
            matcher = IMPORT_PATTERN.matcher(jsonString);
        }
        return parseJSONObjectFromString(jsonString);
    }

    /**
     * Get a file from the classpath relative to this test class.
     * @param fileName the name of the file to load.
     */
    protected File getFile(String fileName) {
        return getFile(getClass(), fileName);
    }

    protected File getTaskDirectory() {
        return workingDirectories.getTaskDirectory();
    }

    public static MapfishMapContext createTestMapContext() {
        try {
            final CenterScaleMapBounds bounds = new CenterScaleMapBounds(CRS.decode("CRS:84"), 0, 0, new Scale(30000));
            return new MapfishMapContext(bounds, new Dimension(500,500), 0, 72, Constants.PDF_DPI, null, true);
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

}
