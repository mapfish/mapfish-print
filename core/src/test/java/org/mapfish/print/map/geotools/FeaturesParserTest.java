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

package org.mapfish.print.map.geotools;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.Constants;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.http.ConfigFileResolvingHttpRequestFactory;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.http.MfClientHttpRequestFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;

public class FeaturesParserTest extends AbstractMapfishSpringTest {


    private static final String EXAMPLE_GEOJSONFILE = "geojson/geojson-inconsistent-attributes-2.json";
    @Autowired
    private MfClientHttpRequestFactoryImpl requestFactory;
    @Autowired
    private ConfigurationFactory configurationFactory;


    @Test
    public void testAutoTreat() throws Exception {

    }

    @Test
    public void testTreatStringAsURL() throws Exception {

    }

    @Test
    public void testTreatStringAsGeoJson() throws Exception {
        Configuration configuration = configurationFactory.getConfig(getFile("geojson/config.yaml"));
        MfClientHttpRequestFactory configRequestFactory = new ConfigFileResolvingHttpRequestFactory(requestFactory, configuration);
        FeaturesParser featuresParser = new FeaturesParser( configRequestFactory, false);
        for (File geojsonExample : getGeoJsonExamples()) {
            try {
                int numFeatures = getNumExpectedFeatures(geojsonExample);
                final String geojson = Files.toString(geojsonExample, Constants.DEFAULT_CHARSET);
                final SimpleFeatureCollection simpleFeatureCollection = featuresParser.treatStringAsGeoJson(geojson);
                assertEquals(geojsonExample.getName(), numFeatures, simpleFeatureCollection.size());
            } catch (AssertionError e) {
                throw e;
            } catch (Throwable t) {
                throw new AssertionError("Exception raised when processing: " + geojsonExample.getName() + "\n" + t.getMessage(), t);
            }
        }
    }

    private int getNumExpectedFeatures(File geojsonExample) {
        final Pattern numExpectedFilesPattern = Pattern.compile(".*-(\\d+)\\.json");

        final Matcher matcher = numExpectedFilesPattern.matcher(geojsonExample.getName());
        matcher.find();
        final String numFeatures = matcher.group(1);
        return Integer.parseInt(numFeatures);

    }

    private Iterable<File> getGeoJsonExamples() {
        final File file = getFile(EXAMPLE_GEOJSONFILE);
        File directory = file.getParentFile();
        return Iterables.filter(Files.fileTreeTraverser().children(directory), new Predicate<File>() {
            @Override
            public boolean apply(@Nullable File input) {
                return input.getName().endsWith(".json");
            }
        });
    }
}