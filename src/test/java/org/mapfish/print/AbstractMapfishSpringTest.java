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

import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import org.junit.runner.RunWith;
import org.mapfish.print.config.ConfigurationFactoryTest;
import org.mapfish.print.json.PJsonElement;
import org.mapfish.print.json.PJsonObject;
import org.mapfish.print.processor.map.MapProcessorTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Class that loads the normal spring application context from the spring config file.
 * Subclasses can use Autowired to get dependencies from the application context.
 *
 * @author jesseeichar on 3/25/14.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        AbstractMapfishSpringTest.DEFAULT_SPRING_XML
})
public abstract class AbstractMapfishSpringTest {
    public static final String DEFAULT_SPRING_XML = "classpath:mapfish-spring-application-context.xml";

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
    public PJsonObject parseJSONObjectFromString(String jsonString) {
        return MapPrinter.parseSpec(jsonString);
    }

    public PJsonObject parseJSONObjectFromFile(Class<?> testClass, String fileName) throws IOException {
        String jsonString = Files.asCharSource(getFile(testClass, fileName), Charset.forName(Constants.ENCODING)).read();
        return parseJSONObjectFromString(jsonString);
    }
}
