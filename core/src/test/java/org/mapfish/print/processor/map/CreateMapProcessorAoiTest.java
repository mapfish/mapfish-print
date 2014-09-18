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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.io.Files;
import org.apache.batik.transcoder.TranscoderException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.attribute.map.AreaOfInterest;
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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;
import static org.mapfish.print.attribute.map.AreaOfInterest.AoiDisplay.RENDER;

/**
 * Basic test of the Map processor.
 * <p/>
 * Created by Jesse on 3/26/14.
 */
public class CreateMapProcessorAoiTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "center_wms1_0_0_flexiblescale_area_of_interest/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private TestHttpClientFactory requestFactory;
    @Autowired
    private MapfishParser parser;

    @Test
    @DirtiesContext
    public void testExecute() throws Exception {
        final String host = "center_wms1_0_0_flexiblescale";
        requestFactory.registerHandler(
                new Predicate<URI>() {
                    @Override
                    public boolean apply(URI input) {
                        return (("" + input.getHost()).contains(host + ".wms")) || input.getAuthority().contains(host + ".wms");
                    }
                }, new TestHttpClientFactory.Handler() {
                    @Override
                    public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws Exception {
                        try {
                            byte[] bytes = Files.toByteArray(getFile("/map-data/zoomed-in-ny-tiger.tif"));
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

        createMap(template, "expectedSimpleImage-default.png", RENDER, null, false, null);
        createMap(template, "expectedSimpleImage-render-thinline.png", RENDER, "file://thinline.sld", false, null);
        createMap(template, "expectedSimpleImage-render-jsonStyle.png", RENDER, createJsonStyle().toString(), false, null);
        createMap(template, "expectedSimpleImage-render-polygon.png", RENDER, "polygon",
                false, null);
        createMap(template, "expectedSimpleImage-none.png", AreaOfInterest.AoiDisplay.NONE, null, false, null);
        createMap(template, "expectedSimpleImage-clip.png", AreaOfInterest.AoiDisplay.CLIP, null, false, null);

        // Test when SVG is used for vector layers
        createMap(template, "expectedSimpleImage-render-polygon-svg.png", RENDER, createJsonStyle().toString(), true, null);
        createMap(template, "expectedSimpleImage-clip-svg.png", AreaOfInterest.AoiDisplay.CLIP, null, true, null);
        Function<PJsonObject, Void> setRotationUpdater = new Function<PJsonObject, Void>() {

            @Nullable
            @Override
            public Void apply(@Nonnull PJsonObject input) {
                try {
                    getMapAttributes(input).getInternalObj().put("rotation", 90);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        };
        createMap(template, "expectedSimpleImage-rotate-clip-svg.png", AreaOfInterest.AoiDisplay.CLIP, null, true, setRotationUpdater);
        createMap(template, "expectedSimpleImage-rotate-render-svg.png", AreaOfInterest.AoiDisplay.RENDER, null, true, setRotationUpdater);
    }

    private JSONObject createJsonStyle() throws JSONException {
        JSONObject jsonStyle = new JSONObject();
        jsonStyle.put("version", "2");
        final JSONObject polygonSymb = new JSONObject();
        polygonSymb.put("type", "polygon");
        polygonSymb.put("fillColor", "green");
        polygonSymb.put("fillOpacity", ".8");
        polygonSymb.put("strokeColor", "black");
        JSONArray symbs = new JSONArray();
        symbs.put(polygonSymb);

        JSONObject rule = new JSONObject();
        rule.put("symbolizers", symbs);
        jsonStyle.put("*", rule);
        return jsonStyle;
    }

    private void createMap(Template template, String expectedImageName, AreaOfInterest.AoiDisplay aoiDisplay, String styleRef,
                           boolean useSVG, Function<PJsonObject, Void> requestUpdater) throws IOException, JSONException, TranscoderException {
        PJsonObject requestData = loadJsonRequestData();
        final PJsonObject mapAttribute = getMapAttributes(requestData);
        mapAttribute.getJSONArray("layers").getJSONObject(0).getInternalObj().put("renderAsSvg", useSVG);

        final PJsonObject areaOfInterest = mapAttribute.getJSONObject("areaOfInterest");
        areaOfInterest.getInternalObj().put("display", aoiDisplay.name().toLowerCase()); // doesn't have to be lowercase,
        // this is to make things more interesting
        areaOfInterest.getInternalObj().put("style", styleRef);

        if (requestUpdater != null) {
            requestUpdater.apply(requestData);
        }

        Values values = new Values(requestData, template, this.parser, getTaskDirectory(), this.requestFactory, new File("."));
        template.getProcessorGraph().createTask(values).invoke();

        @SuppressWarnings("unchecked")
        List<URI> layerGraphics = (List<URI>) values.getObject("layerGraphics", List.class);
        assertEquals(aoiDisplay == RENDER ? 3 : 2, layerGraphics.size());

        final BufferedImage actualImage = ImageSimilarity.mergeImages(layerGraphics, 630, 294);
//        ImageIO.write(actualImage, "png", new File("e:/tmp/" + expectedImageName));
        File expectedImage = getFile(BASE_DIR + "/output/" + expectedImageName);
        new ImageSimilarity(actualImage, 2).assertSimilarity(expectedImage, 50);
    }

    private PJsonObject getMapAttributes(PJsonObject requestData) {
        return requestData.getJSONObject("attributes").getJSONObject("mapDef");
    }

    private static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(CreateMapProcessorAoiTest.class, BASE_DIR + "requestData.json");
    }
}
