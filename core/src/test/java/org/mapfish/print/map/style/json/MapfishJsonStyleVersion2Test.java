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

package org.mapfish.print.map.style.json;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.referencing.CRS;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.TextSymbolizer;
import org.junit.Test;
import org.mapfish.print.Constants;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.attribute.map.CenterScaleMapBounds;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.map.Scale;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsLessThan;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mapfish.print.map.style.json.JsonStyleParserHelperTest.valueOf;

public class MapfishJsonStyleVersion2Test {
    private static final double DELTA = 0.000001;
    final MapfishJsonStyleParserPlugin mapfishJsonStyleParserPlugin = new MapfishJsonStyleParserPlugin();
    final TestHttpClientFactory httpClient = new TestHttpClientFactory();
    @Test
    public void testResolveAllValues() throws Exception {
        Map<String, String> values = Maps.newHashMap();
        values.put("val1", "value");
        values.put("val2", "${val1}2");
        values.put("val3", "${val2}--3${val1}");
        values.put("val4", "pre- ${val3} -- ${val1} -- ${val2} -post");
        values.put("val5", "${doesNotExist}");

        Map<String, String> updated = MapfishJsonStyleVersion2.resolveAllValues(values);
        assertEquals(5, updated.size());
        assertEquals("value", updated.get("val1"));
        assertEquals("value2", updated.get("val2"));
        assertEquals("value2--3value", updated.get("val3"));
        assertEquals("pre- value2--3value -- value -- value2 -post", updated.get("val4"));
        assertEquals("${doesNotExist}", updated.get("val5"));
    }

    @Test
    public void testParseSymbolizersWithDefaultsAndValues() throws Throwable {
        final Style style = parseStyle("v2-style-symbolizers-default-values.json");

        final List<FeatureTypeStyle> featureTypeStyles = style.featureTypeStyles();
        assertEquals(1, featureTypeStyles.size());
        final List<Rule> rules = featureTypeStyles.get(0).rules();
        assertEquals(1, rules.size());
        final Rule rule = rules.get(0);

        assertEquals(1000000, rule.getMaxScaleDenominator(), DELTA);
        assertEquals(100, rule.getMinScaleDenominator(), DELTA);
        final Filter filter = rule.getFilter();

        assertTrue(filter instanceof PropertyIsLessThan);
        assertEquals("att < 3", ECQL.toCQL(filter));

        assertEquals(1, rule.symbolizers().size());

        PointSymbolizer symbolizer = (PointSymbolizer) rule.symbolizers().get(0);

        assertEquals(1, symbolizer.getGraphic().graphicalSymbols().size());

        Mark mark = (Mark) symbolizer.getGraphic().graphicalSymbols().get(0);

        assertEquals("circle", valueOf(mark.getWellKnownName()));
        assertEquals(30, (Double) valueOf(symbolizer.getGraphic().getRotation()), DELTA);
        assertEquals(0.4, (Double) valueOf(symbolizer.getGraphic().getOpacity()), DELTA);
        assertEquals("#00FF00", valueOf(mark.getStroke().getColor()));

    }

    @Test
    public void testParseDefaultSymbolizers() throws Throwable {
        final Style style = parseStyle("v2-style-default-symbolizers.json");

        final List<FeatureTypeStyle> featureTypeStyles = style.featureTypeStyles();
        assertEquals(1, featureTypeStyles.size());
        final List<Rule> rules = featureTypeStyles.get(0).rules();
        assertEquals(1, rules.size());
        final Rule rule = rules.get(0);

        assertEquals(Filter.INCLUDE, rule.getFilter());

        assertEquals(4, rule.symbolizers().size());

        PointSymbolizer pointSymbolizer = (PointSymbolizer) rule.symbolizers().get(0);
        LineSymbolizer lineSymbolizer = (LineSymbolizer) rule.symbolizers().get(1);
        PolygonSymbolizer polygonSymbolizer = (PolygonSymbolizer) rule.symbolizers().get(2);
        TextSymbolizer textSymbolizer = (TextSymbolizer) rule.symbolizers().get(3);

        assertNotNull(pointSymbolizer);
        assertNotNull(lineSymbolizer);
        assertNotNull(polygonSymbolizer);
        assertNotNull(textSymbolizer);
    }
    private Style parseStyle(String styleJsonFileName) throws Throwable {
        Configuration config = new Configuration();
        config.setConfigurationFile(getFile(styleJsonFileName));
        final String styleJson = getSpec(styleJsonFileName);

        final CenterScaleMapBounds bounds = new CenterScaleMapBounds(CRS.decode("CRS:84"), 0, 0, new Scale(300000));
        MapfishMapContext context = new MapfishMapContext(bounds, new Dimension(500,500), 0, 72);
        final Optional<Style> styleOptional = mapfishJsonStyleParserPlugin.parseStyle(config, httpClient, styleJson, context);

        assertTrue(styleOptional.isPresent());

        return styleOptional.get();
    }

    private String getSpec(String name) throws IOException, URISyntaxException {
        return Files.toString(getFile(name), Constants.DEFAULT_CHARSET);
    }

    private File getFile(String name) throws URISyntaxException {
        return new File(MapfishJsonStyleVersion2Test.class.getResource(name).toURI());
    }
}