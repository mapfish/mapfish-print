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
import com.vividsolutions.jts.geom.Envelope;
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

import static java.lang.Double.parseDouble;
import static org.junit.Assert.assertEquals;

/**
 * Basic test of the Map processor.
 * <p/>
 * Created by Jesse on 3/26/14.
 */
public class CreateMapProcessorFlexibleScaleCenterTiledWmsTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "center_tiledwms_flexiblescale/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private TestHttpClientFactory requestFactory;
    @Autowired
    private MapfishParser parser;

    @Test
    @DirtiesContext
    public void testExecute() throws Exception {
        final String host = "center_tiledwms_flexiblescale";
        requestFactory.registerHandler(
                new Predicate<URI>() {
                    @Override
                    public boolean apply(URI input) {
                        return (("" + input.getHost()).contains(host + ".wms")) || input.getAuthority().contains(host + ".wms");
                    }
                }, new TestHttpClientFactory.Handler() {
                    @Override
                    public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws Exception {
                        final Multimap<String, String> parameters = URIUtils.getParameters(uri);

                        final String rawBBox = parameters.get("BBOX").iterator().next();
                        String[] bbox = rawBBox.split(",");
                        Envelope envelope = new Envelope(parseDouble(bbox[0]), parseDouble(bbox[2]), parseDouble(bbox[1]),
                                parseDouble(bbox[3]));

                        String imageName;
                        if (equalEnv(envelope,-137.6509441921722,19.240978728567754,-115.4743396477791,41.417583272960854)) {
                            imageName = "-137_6509,19_2409,-115_4743,41_4175.png";
                        } else if (equalEnv(envelope, -115.4743396477791,19.240978728567754,-93.297735103386,41.417583272960854)) {
                            imageName = "-115_4743,19_2409,-93_2977,41_4175.png";
                        } else if (equalEnv(envelope, -93.297735103386,19.240978728567754,-71.1211305589929,41.417583272960854)) {
                            imageName = "-93.2977_19.2409_-71.1211_41.4175.png";
                        } else if (equalEnv(envelope, -71.1211305589929,19.240978728567754,-48.9445260145998,41.417583272960854)) {
                            imageName = "-71.1211_19.2409_-48.9445_41.4175.png";
                        } else if (equalEnv(envelope, -137.6509441921722,41.417583272960854,-115.4743396477791,63.594187817353955)) {
                            imageName = "-137.6509_41.4175_-115.4743_63.5941.png";
                        } else if (equalEnv(envelope, -115.4743396477791,41.417583272960854,-93.297735103386,63.594187817353955)) {
                            imageName = "-115.4743_41.4175_-93.2977_63.5941.png";
                        } else if (equalEnv(envelope, -93.297735103386,41.417583272960854,-71.1211305589929,63.594187817353955)) {
                            imageName = "-93.2977_41.4175_-71.1211_63.5941.png";
                        } else if (equalEnv(envelope, -71.1211305589929,41.417583272960854,-48.9445260145998,63.594187817353955)) {
                            imageName = "-71.1211_41.4175_-48.9445_63.5941.png";
                        } else {
                            return error404(uri, httpMethod);
                        }
                        try {
                            byte[] bytes = Files.toByteArray(getFile("/map-data/tiled-wms-tiles/"+imageName));
                            return ok(uri, bytes, httpMethod);
                        } catch (AssertionError e) {
                            return error404(uri, httpMethod);
                        }
                    }

                    private boolean equalEnv(Envelope envelope, double minx, double miny, double maxx, double maxy) {
                        double difference = 0.00001;
                        return Math.abs(envelope.getMinX() - minx) < difference &&
                            Math.abs(envelope.getMinY() - miny) < difference &&
                            Math.abs(envelope.getMaxX() - maxx) < difference &&
                            Math.abs(envelope.getMaxY() - maxy) < difference;
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
        final Template template = config.getTemplate("A4 landscape");
        PJsonObject requestData = loadJsonRequestData();
        Values values = new Values(requestData, template, this.parser, getTaskDirectory(), this.requestFactory, new File("."));
        template.getProcessorGraph().createTask(values).invoke();

        @SuppressWarnings("unchecked")
        List<URI> layerGraphics = (List<URI>) values.getObject("layerGraphics", List.class);
        assertEquals(2, layerGraphics.size());
        final BufferedImage referenceImage = ImageSimilarity.mergeImages(layerGraphics, 780, 330);

//      ImageIO.write(referenceImage, "png", new File("e:/tmp/expectedSimpleImage.png"));
//      Files.copy(new File(layerGraphics.get(0)), new File("e:/tmp/0_"+getClass().getSimpleName()+".tiff"));
//      Files.copy(new File(layerGraphics.get(1)), new File("e:/tmp/1_"+getClass().getSimpleName()+".tiff"));

        new ImageSimilarity(referenceImage, 2).assertSimilarity(getFile(BASE_DIR + "expectedSimpleImage.png"), 30);

    }

    private static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(CreateMapProcessorFlexibleScaleCenterTiledWmsTest.class, BASE_DIR + "requestData.json");
    }
}
