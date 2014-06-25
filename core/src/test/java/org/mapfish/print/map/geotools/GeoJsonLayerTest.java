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
import com.google.common.io.Files;
import org.geotools.data.Query;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.parser.MapfishParserTest;
import org.mapfish.print.processor.map.CreateMapProcessorFixedScaleBBoxGeoJsonTest;
import org.mapfish.print.processor.map.CreateMapProcessorFlexibleScaleBBoxGeoJsonTest;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;

import java.io.File;
import java.net.URI;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mapfish.print.processor.map.CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.BASE_DIR;

/**
 * Test GeoJson layer
 *
 * @author Jesse on 3/27/14.
 */
public class GeoJsonLayerTest extends AbstractMapfishSpringTest {

    @Autowired
    private GeoJsonLayer.Plugin geojsonLayerParser;
    @Autowired
    private TestHttpClientFactory httpRequestFactory;


    @Test
    public void testGeoJsonEmbedded() throws Exception {
        final PJsonObject requestData = CreateMapProcessorFixedScaleBBoxGeoJsonTest.loadJsonRequestData()
                .getJSONObject("attributes")
                .getJSONObject("mapDef")
                .getJSONArray("layers").getJSONObject(0);

        final Configuration configuration = new Configuration();
        configuration.setConfigurationFile(new File("."));

        Template template = new Template();
        template.setConfiguration(configuration);
        template.setStyle("polygon", template.getConfiguration().getDefaultStyle("polygon"));

        GeoJsonLayer.GeoJsonParam param = new GeoJsonLayer.GeoJsonParam();
        MapfishParserTest.populateLayerParam(requestData, param, "type");
        final GeoJsonLayer layer = geojsonLayerParser.parse(template, param);

        assertNotNull(layer);

        final List<? extends Layer> layers = layer.getLayers(httpRequestFactory, null, null, 1.0, null, true);

        assertEquals(1, layers.size());

        FeatureLayer featureLayer = (FeatureLayer) layers.get(0);
        final int count = featureLayer.getFeatureSource().getCount(Query.ALL);
        assertEquals(3, count);
        assertEquals(true, layer.shouldRenderAsSvg());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGeoIllegalFileUrl() throws Exception {
        final File file = getFile(CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.class, BASE_DIR + "geojson.json");
        final PJsonObject requestData = parseJSONObjectFromString("{type:\"geojson\";style:\"polygon\";geoJson:\""
                                                                  + file.toURI().toURL() + "\"}");

        final Configuration configuration = new Configuration();
        configuration.setConfigurationFile(File.createTempFile("xyz", ".yaml"));

        Template template = new Template();
        template.setConfiguration(configuration);
        template.setStyle("polygon", template.getConfiguration().getDefaultStyle("polygon"));

        GeoJsonLayer.GeoJsonParam param = new GeoJsonLayer.GeoJsonParam();
        MapfishParserTest.populateLayerParam(requestData, param, "type");
        geojsonLayerParser.parse(template, param).getLayers(httpRequestFactory, null, null, 1.0, null, true);

    }


    @Test(expected = IllegalArgumentException.class)
    public void testGeoIllegalFileUrl2() throws Exception {
        final Configuration configuration = new Configuration();
        configuration.setConfigurationFile(File.createTempFile("xyz", ".yaml"));

        Template template = new Template();
        template.setConfiguration(configuration);
        template.setStyle("polygon", template.getConfiguration().getDefaultStyle("polygon"));


        GeoJsonLayer.GeoJsonParam param = new GeoJsonLayer.GeoJsonParam();
        param.geoJson = "file://../" + BASE_DIR + "/geojson.json";
        geojsonLayerParser.parse(template, param).getLayers(httpRequestFactory, null, null, 1.0, null, true);
    }

    @Test(expected = Exception.class)
    public void testGeoNotUrlNotGeoJson() throws Exception {
        final File file = getFile(CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.class, BASE_DIR + "geojson.json");
        final PJsonObject requestData = parseJSONObjectFromString("{type:\"geojson\";style:\"polygon\";geoJson:\"Random\"}");

        final Configuration configuration = new Configuration();
        configuration.setConfigurationFile(file);

        Template template = new Template();
        template.setConfiguration(configuration);
        template.setStyle("polygon", template.getConfiguration().getDefaultStyle("polygon"));

        GeoJsonLayer.GeoJsonParam param = new GeoJsonLayer.GeoJsonParam();
        MapfishParserTest.populateLayerParam(requestData, param, "type");
        geojsonLayerParser.parse(template, param).getLayers(httpRequestFactory, null, null, 1.0, null, true);
    }

    @Test
    public void testUrl() throws Exception {
        final String host = "GeoJsonLayerTest.com";
        this.httpRequestFactory.registerHandler(
                new Predicate<URI>() {
                    @Override
                    public boolean apply(URI input) {
                        return (("" + input.getHost()).contains(host)) || input.getAuthority().contains(host);
                    }
                }, new TestHttpClientFactory.Handler() {
                    @Override
                    public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws Exception {
                        try {
                            final File file = getFile(CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.class, uri.getPath().substring(1));
                            byte[] bytes = Files.toByteArray(file);
                            return ok(uri, bytes, httpMethod);
                        } catch (AssertionError e) {
                            return error404(uri, httpMethod);
                        }
                    }
                }
        );
        final PJsonObject requestData = parseJSONObjectFromString("{type:\"geojson\";style:\"polygon\";geoJson:\"http://"
                                                                  + host + "/" + BASE_DIR + "geojson.json" + "\"}");

        final File file = getFile(CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.class, BASE_DIR + "geojson.json");
        final Configuration configuration = new Configuration();
        configuration.setConfigurationFile(file);

        Template template = new Template();
        template.setConfiguration(configuration);
        template.setStyle("polygon", template.getConfiguration().getDefaultStyle("polygon"));


        GeoJsonLayer.GeoJsonParam param = new GeoJsonLayer.GeoJsonParam();
        MapfishParserTest.populateLayerParam(requestData, param, "type");
        final GeoJsonLayer mapLayer = geojsonLayerParser.parse(template, param);

        assertNotNull(mapLayer);

        final List<? extends Layer> layers = mapLayer.getLayers(httpRequestFactory, null, null, 1.0, null, true);

        assertEquals(1, layers.size());

        FeatureLayer layer = (FeatureLayer) layers.get(0);
        final int count = layer.getFeatureSource().getCount(Query.ALL);
        assertEquals(3, count);
    }

    @Test
    public void testRelativeFileUrl() throws Exception {
        final File file = getFile(CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.class, BASE_DIR + "geojson.json");
        final PJsonObject requestData = parseJSONObjectFromString("{type:\"geojson\";style:\"polygon\";geoJson:\"file://"
                                                                  + file.getName() + "\"}");

        final Configuration configuration = new Configuration();
        configuration.setConfigurationFile(file);

        Template template = new Template();
        template.setConfiguration(configuration);
        template.setStyle("polygon", template.getConfiguration().getDefaultStyle("polygon"));


        GeoJsonLayer.GeoJsonParam param = new GeoJsonLayer.GeoJsonParam();
        MapfishParserTest.populateLayerParam(requestData, param, "type");
        final GeoJsonLayer mapLayer = geojsonLayerParser.parse(template, param);

        assertNotNull(mapLayer);

        final List<? extends Layer> layers = mapLayer.getLayers(httpRequestFactory, null, null, 1.0, null, true);

        assertEquals(1, layers.size());

        FeatureLayer layer = (FeatureLayer) layers.get(0);
        final int count = layer.getFeatureSource().getCount(Query.ALL);
        assertEquals(3, count);
        assertEquals(false, mapLayer.shouldRenderAsSvg());
    }
}
