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

import com.google.common.base.Optional;
import org.geotools.data.Query;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.json.PJsonObject;
import org.mapfish.print.processor.map.CreateMapProcessorTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test GeoJson layeer
 * @author Jesse on 3/27/14.
 */
public class GeoJsonLayerTest extends AbstractMapfishSpringTest {

    @Autowired
    private GeoJsonLayer.Plugin geojsonLayerParser;

    @Test
    public void testGeoJsonEmbedded() throws Exception {
        final PJsonObject requestData = CreateMapProcessorTest.loadJsonRequestData()
                .getJSONObject("attributes")
                .getJSONObject("map")
                .getJSONArray("layers").getJSONObject(0);

        final Configuration configuration = new Configuration();
        configuration.setConfigurationFile(new File("."));

        Template template = new Template();
        template.setConfiguration(configuration);
        template.setStyle("polygon", template.getConfiguration().getDefaultStyle("polygon"));

        final Optional<GeoJsonLayer> layerOptional = geojsonLayerParser.parse(template, requestData);

        assertTrue(layerOptional.isPresent());

        final List<? extends Layer> layers = layerOptional.get().getLayers();

        assertEquals(1, layers.size());

        FeatureLayer layer = (FeatureLayer) layers.get(0);
        final int count = layer.getFeatureSource().getCount(Query.ALL);
        assertEquals(3, count);
    }

    @Test
    public void testGeoJsonMissingType() throws Exception {
        final File file = getFile(CreateMapProcessorTest.class, "basicMapExample/geojson.json");
        final PJsonObject requestData = parseJSONObjectFromString("{type:\"wfs\";style:\"polygon\";geoJson:\""
                                                                  + file.toURI().toURL() + "\"}");

        final Configuration configuration = new Configuration();
        configuration.setConfigurationFile(file);

        Template template = new Template();
        template.setConfiguration(configuration);
        template.setStyle("polygon", template.getConfiguration().getDefaultStyle("polygon"));

        final Optional<GeoJsonLayer> layerOptional = geojsonLayerParser.parse(template, requestData);

        assertFalse(layerOptional.isPresent());
    }
    @Test
    public void testGeoJsonWrongType() throws Exception {
        final File file = getFile(CreateMapProcessorTest.class, "basicMapExample/geojson.json");
        final PJsonObject requestData = parseJSONObjectFromString("{type:\"wfs\";style:\"polygon\";geoJson:\""
                                                                  + file.toURI().toURL() + "\"}");

        final Configuration configuration = new Configuration();
        configuration.setConfigurationFile(file);

        Template template = new Template();
        template.setConfiguration(configuration);
        template.setStyle("polygon", template.getConfiguration().getDefaultStyle("polygon"));

        final Optional<GeoJsonLayer> layerOptional = geojsonLayerParser.parse(template, requestData);

        assertFalse(layerOptional.isPresent());
    }
    @Test(expected = IllegalArgumentException.class)
    public void testGeoIllegalFileUrl() throws Exception {
        final File file = getFile(CreateMapProcessorTest.class, "basicMapExample/geojson.json");
        final PJsonObject requestData = parseJSONObjectFromString("{type:\"geojson\";style:\"polygon\";geoJson:\""
                                                                  + file.toURI().toURL() + "\"}");

        final Configuration configuration = new Configuration();
        configuration.setConfigurationFile(File.createTempFile("xyz", ".yaml"));

        Template template = new Template();
        template.setConfiguration(configuration);
        template.setStyle("polygon", template.getConfiguration().getDefaultStyle("polygon"));

        geojsonLayerParser.parse(template, requestData);

    }
    @Test(expected = Exception.class)
    public void testGeoNotUrlNotGeoJson() throws Exception {
        final File file = getFile(CreateMapProcessorTest.class, "basicMapExample/geojson.json");
        final PJsonObject requestData = parseJSONObjectFromString("{type:\"geojson\";style:\"polygon\";geoJson:\"Random\"}");

        final Configuration configuration = new Configuration();
        configuration.setConfigurationFile(file);

        Template template = new Template();
        template.setConfiguration(configuration);
        template.setStyle("polygon", template.getConfiguration().getDefaultStyle("polygon"));

        geojsonLayerParser.parse(template, requestData);
    }

    @Test
    public void testGeoJsonUrl() throws Exception {
        final File file = getFile(CreateMapProcessorTest.class, "basicMapExample/geojson.json");
        final PJsonObject requestData = parseJSONObjectFromString("{type:\"geojson\";style:\"polygon\";geoJson:\""
                                                                  + file.toURI().toURL() + "\"}");

        final Configuration configuration = new Configuration();
        configuration.setConfigurationFile(file);

        Template template = new Template();
        template.setConfiguration(configuration);
        template.setStyle("polygon", template.getConfiguration().getDefaultStyle("polygon"));

        final Optional<GeoJsonLayer> layerOptional = geojsonLayerParser.parse(template, requestData);

        assertTrue(layerOptional.isPresent());

        final List<? extends Layer> layers = layerOptional.get().getLayers();

        assertEquals(1, layers.size());

        FeatureLayer layer = (FeatureLayer) layers.get(0);
        final int count = layer.getFeatureSource().getCount(Query.ALL);
        assertEquals(3, count);
    }
    @Test
    public void testRelativeUrl() throws Exception {
        final File file = getFile(CreateMapProcessorTest.class, "basicMapExample/geojson.json");
        final PJsonObject requestData = parseJSONObjectFromString("{type:\"geojson\";style:\"polygon\";geoJson:\"file://"
                                                                  + file.getName() + "\"}");

        final Configuration configuration = new Configuration();
        configuration.setConfigurationFile(file);

        Template template = new Template();
        template.setConfiguration(configuration);
        template.setStyle("polygon", template.getConfiguration().getDefaultStyle("polygon"));

        final Optional<GeoJsonLayer> layerOptional = geojsonLayerParser.parse(template, requestData);

        assertTrue(layerOptional.isPresent());

        final List<? extends Layer> layers = layerOptional.get().getLayers();

        assertEquals(1, layers.size());

        FeatureLayer layer = (FeatureLayer) layers.get(0);
        final int count = layer.getFeatureSource().getCount(Query.ALL);
        assertEquals(3, count);
    }
}
