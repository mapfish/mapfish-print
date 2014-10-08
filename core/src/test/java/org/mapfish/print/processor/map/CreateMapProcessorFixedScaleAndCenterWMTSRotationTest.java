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

package org.mapfish.print.processor.map;

import com.google.common.base.Predicate;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.URIUtils;
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
 * Tests map rotation for WMTS and GeoJSON layer.
 */
public class CreateMapProcessorFixedScaleAndCenterWMTSRotationTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "center_wmts_rotation_fixedscale/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private TestHttpClientFactory requestFactory;
    @Autowired
    private MapfishParser parser;


    @Test
    @DirtiesContext
    public void testExecute() throws Exception {
        requestFactory.registerHandler(
                new Predicate<URI>() {
                    @Override
                    public boolean apply(URI input) {
                        final String host = "center_wmts_rotation_fixedscale.com";
                        return (("" + input.getHost()).contains(host)) || input.getAuthority().contains(host);
                    }
                }, new TestHttpClientFactory.Handler() {
                    @Override
                    public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws Exception {
                        final Multimap<String, String> parameters = URIUtils.getParameters(uri);
                        String column = parameters.get("TILECOL").iterator().next();
                        String row = parameters.get("TILEROW").iterator().next();
                        try {
                            byte[] bytes = Files.toByteArray(getFile("/map-data/ny-tiles/" + column + "x" + row + "" +

                                                                     ".tiff"));
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
                        final String host = "center_wmts_rotation_fixedscale.json";
                        return (("" + input.getHost()).contains(host)) || input.getAuthority().contains(host);
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
        Values values = new Values(requestData, template, parser, getTaskDirectory(), this.requestFactory, new File("."));
        template.getProcessorGraph().createTask(values).invoke();

        @SuppressWarnings("unchecked")
        List<URI> layerGraphics = (List<URI>) values.getObject("layerGraphics", List.class);
        assertEquals(2, layerGraphics.size());

//      Files.copy(new File(layerGraphics.get(0)), new File("/tmp/0_" + getClass().getSimpleName() + ".tiff"));
//      Files.copy(new File(layerGraphics.get(1)), new File("/tmp/1_" + getClass().getSimpleName() + ".tiff"));

        final BufferedImage referenceImage = ImageSimilarity.mergeImages(layerGraphics, 630, 294);
        new ImageSimilarity(referenceImage, 2)
                .assertSimilarity(getFile(BASE_DIR + "expectedSimpleImage.tiff"), 25);
    }

    public static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(CreateMapProcessorFixedScaleAndCenterWMTSRotationTest.class, BASE_DIR + "requestData.json");
    }

}
