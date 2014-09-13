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

package org.mapfish.print.processor.map.scalebar;

import com.google.common.base.Predicate;
import com.google.common.io.Files;
import jsr166y.ForkJoinPool;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.parser.MapfishParser;
import org.mapfish.print.test.util.ImageSimilarity;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.annotation.DirtiesContext;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Basic test of the Scalebar processor.
 */
public class CreateScaleBarProcessorFixedScaleCenterOsmTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "center_osm_fixedscale/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private ForkJoinPool forkJoinPool;
    @Autowired
    private TestHttpClientFactory requestFactory;
    @Autowired
    private MapfishParser parser;

    @Test
    @DirtiesContext
    public void testExecute() throws Exception {
        final String host = "center_osm_fixedscale";
        requestFactory.registerHandler(
                new Predicate<URI>() {
                    @Override
                    public boolean apply(URI input) {
                        return (("" + input.getHost()).contains(host + ".osm")) || input.getAuthority().contains(host + ".osm");
                    }
                }, new TestHttpClientFactory.Handler() {
                    @Override
                    public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws Exception {
                        try {
                            byte[] bytes = Files.toByteArray(getFile("/map-data/osm" + uri.getPath()));
                            return ok(uri, bytes, httpMethod);
                        } catch (AssertionError e) {
                            return error404(uri, httpMethod);
                        }
                    }
                }
        );
        requestFactory.registerHandler(
                new Predicate<URI>() {
                    @Override
                    public boolean apply(URI input) {
                        return (("" + input.getHost()).contains(host + ".json")) || input.getAuthority().contains(host + ".json");
                    }
                }, new TestHttpClientFactory.Handler() {
                    @Override
                    public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws Exception {
                        try {
                            byte[] bytes = Files.toByteArray(getFile("/map-data" + uri.getPath()));
                            return ok(uri, bytes, httpMethod);
                        } catch (AssertionError e) {
                            return error404(uri, httpMethod);
                        }
                    }
                }
        );
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
        final Template template = config.getTemplate("main");
        PJsonObject requestData = loadJsonRequestData();
        Values values = new Values(requestData, template, this.parser, getTaskDirectory(), this.requestFactory, new File("."));
        this.forkJoinPool.invoke(template.getProcessorGraph().createTask(values));

        @SuppressWarnings("unchecked")
        List<URI> layerGraphics = (List<URI>) values.getObject("layerGraphics", List.class);
        assertEquals(2, layerGraphics.size());

//        Files.copy(new File(layerGraphics.get(0)), new File("/tmp/0_" + getClass().getSimpleName() + ".tiff"));
//        Files.copy(new File(layerGraphics.get(1)), new File("/tmp/1_" + getClass().getSimpleName() + ".tiff"));

        final BufferedImage referenceImage = ImageSimilarity.mergeImages(layerGraphics, 780, 330);

        new ImageSimilarity(referenceImage, 2)
                .assertSimilarity(getFile(BASE_DIR + "expectedSimpleImage.tiff"), 30);

        URI scalebarGraphic = values.getObject("graphic", URI.class);
//        Files.copy(new File(scalebarGraphic), new File("e:/tmp/expectedScalebar_" + getClass().getSimpleName() + ".tiff"));

        new ImageSimilarity(new File(scalebarGraphic), 4).assertSimilarity(getFile(BASE_DIR + ScalebarDrawerTest.expectedDir + "expectedScalebar.tiff"), 5);

    }

    private static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(CreateScaleBarProcessorFixedScaleCenterOsmTest.class, BASE_DIR + "requestData.json");
    }

}
