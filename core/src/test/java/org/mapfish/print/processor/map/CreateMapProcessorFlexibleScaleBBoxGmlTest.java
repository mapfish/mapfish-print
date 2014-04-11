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
import com.google.common.io.Files;
import org.json.JSONObject;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.json.PJsonObject;
import org.mapfish.print.json.parser.MapfishJsonParser;
import org.mapfish.print.output.Values;
import org.mapfish.print.util.ImageSimilarity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;

/**
 * Basic test of the Map processor.
 * <p/>
 * Created by Jesse on 3/26/14.
 */
public class CreateMapProcessorFlexibleScaleBBoxGmlTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "center_gml_flexible_scale/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private TestHttpClientFactory requestFactory;
    @Autowired
    private MapfishJsonParser jsonParser;


    @Test
    public void testExecute() throws Exception {
        final String host = "center_gml_flexible_scale.com";
        requestFactory.registerHandler(
                new Predicate<URI>() {
                    @Override
                    public boolean apply(URI input) {
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
        final JSONObject jsonLayer = requestData.getJSONObject("attributes").getJSONObject("mapDef").getJSONArray("layers")
                .getJSONObject(0).getInternalObj();

        for (String gmlDataName : new String[]{"ny-roads-3857-v311.gml", "ny-roads-3857-v2.gml"}) {
            jsonLayer.remove("url");
            jsonLayer.accumulate("url", "http://" + host + ":23432" + "/gml/" + gmlDataName);

            Values values = new Values(requestData, template, jsonParser);
            template.getProcessorGraph().createTask(values).invoke();

            BufferedImage map = values.getObject("mapOut", BufferedImage.class);
            new ImageSimilarity(map, 2).assertSimilarity(getFile(BASE_DIR + gmlDataName + ".png"), 0);
        }

    }

    private static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(CreateMapProcessorFlexibleScaleBBoxGmlTest.class, BASE_DIR + "requestData.json");
    }

}
