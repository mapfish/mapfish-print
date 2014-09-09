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

package org.mapfish.print.config;

import com.google.common.collect.Maps;
import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointPlacement;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.junit.Test;
import org.mapfish.print.Constants;
import org.mapfish.print.attribute.map.BBoxMapBounds;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mockito.Mockito;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for {@link org.mapfish.print.config.Configuration} class.
 *
 * @author Jesse on 3/27/14.
 */
public class ConfigurationTest {

    @Test
    public void testGetTemplate() throws Exception {

        final Configuration configuration = new Configuration();
        Map<String, Template> templates = Maps.newHashMap();
        final Template t1Template = new Template();
        templates.put("t1", t1Template);
        configuration.setTemplates(templates);
        assertEquals(t1Template, configuration.getTemplate("t1"));
        assertEquals(1, configuration.getTemplates().size());
        assertEquals(t1Template, configuration.getTemplates().values().iterator().next());

        try {
            configuration.getTemplate("Doesn't exist");
            fail("Exception should have been thrown");
        } catch (Exception e) {
            // good
        }
    }

    @Test
    public void testGetDefaultStyle_IsPresentInMap() throws Exception {
        MapfishMapContext mapContext = new MapfishMapContext(new BBoxMapBounds(null, 0,0,10,10), new Dimension(20,20), 0, 72, null);

        final Configuration configuration = new Configuration();
        Map<String, Style> styles = new HashMap<String, Style>();
        final Style pointStyle = Mockito.mock(Style.class);
        final Style lineStyle = Mockito.mock(Style.class);
        final Style polygonStyle = Mockito.mock(Style.class);
        final Style geomStyle = Mockito.mock(Style.class);
        styles.put("point", pointStyle);
        styles.put("line", lineStyle);
        styles.put("polygon", polygonStyle);
        styles.put("geometry", geomStyle);
        styles.put("grid", lineStyle);
        configuration.setDefaultStyle(styles);

        assertSame(pointStyle, configuration.getDefaultStyle("POINT"));
        assertSame(pointStyle, configuration.getDefaultStyle("MultiPOINT"));

        assertSame(lineStyle, configuration.getDefaultStyle("lIne"));
        assertSame(lineStyle, configuration.getDefaultStyle("lInestring"));
        assertSame(lineStyle, configuration.getDefaultStyle("linearRing"));
        assertSame(lineStyle, configuration.getDefaultStyle("multilInestring"));
        assertSame(lineStyle, configuration.getDefaultStyle("multiline"));
        assertSame(lineStyle, configuration.getDefaultStyle("grid"));

        assertSame(polygonStyle, configuration.getDefaultStyle("poly"));
        assertSame(polygonStyle, configuration.getDefaultStyle("polygon"));
        assertSame(polygonStyle, configuration.getDefaultStyle("multiPolygon"));

        assertSame(geomStyle, configuration.getDefaultStyle("geom"));
        assertSame(geomStyle, configuration.getDefaultStyle("geometry"));
        assertSame(geomStyle, configuration.getDefaultStyle("geometryCollection"));
        assertSame(geomStyle, configuration.getDefaultStyle("MultiGeometry"));

        assertSame(geomStyle, configuration.getDefaultStyle("other"));
    }

    @Test
    public void testGetDefaultStyle_NotInMap() throws Exception {
        MapfishMapContext mapContext = new MapfishMapContext(new BBoxMapBounds(null, 0,0,10,10), new Dimension(20,20), 0, 72, null);
        final Configuration configuration = new Configuration();
        Map<String, Style> styles = new HashMap<String, Style>();
        final Style geomStyle = Mockito.mock(Style.class);
        styles.put("geometry", geomStyle);
        configuration.setDefaultStyle(styles);


        assertStyleType(PointSymbolizer.class, configuration.getDefaultStyle("POINT"));
        assertStyleType(PointSymbolizer.class, configuration.getDefaultStyle("MultiPOINT"));

        assertStyleType(LineSymbolizer.class, configuration.getDefaultStyle("lIne"));
        assertStyleType(LineSymbolizer.class, configuration.getDefaultStyle("lInestring"));
        assertStyleType(LineSymbolizer.class, configuration.getDefaultStyle("linearRing"));
        assertStyleType(LineSymbolizer.class, configuration.getDefaultStyle("multilInestring"));
        assertStyleType(LineSymbolizer.class, configuration.getDefaultStyle("multiline"));

        assertStyleType(PolygonSymbolizer.class, configuration.getDefaultStyle("poly"));
        assertStyleType(PolygonSymbolizer.class, configuration.getDefaultStyle("polygon"));
        assertStyleType(PolygonSymbolizer.class, configuration.getDefaultStyle("multiPolygon"));

        assertStyleType(RasterSymbolizer.class, configuration.getDefaultStyle(Constants.Style.Raster.NAME));

        assertSame(geomStyle, configuration.getDefaultStyle("geom"));
        assertSame(geomStyle, configuration.getDefaultStyle("geometry"));
        assertSame(geomStyle, configuration.getDefaultStyle("geometryCollection"));
        assertSame(geomStyle, configuration.getDefaultStyle("MultiGeometry"));
    }
    @Test
    public void testGetDefaultStyle_GeomNotInMap() throws Exception {
        MapfishMapContext mapContext = new MapfishMapContext(new BBoxMapBounds(null, 0,0,10,10), new Dimension(20,20), 0, 72, null);
        final Configuration configuration = new Configuration();

        assertStyleType(Symbolizer.class, configuration.getDefaultStyle("geom"));
        assertStyleType(Symbolizer.class, configuration.getDefaultStyle("geometry"));
        assertStyleType(Symbolizer.class, configuration.getDefaultStyle("geometryCollection"));
        assertStyleType(Symbolizer.class, configuration.getDefaultStyle("MultiGeometry"));
        assertStyleType(Symbolizer.class, configuration.getDefaultStyle(Constants.Style.Grid.NAME));
        assertStyleType(Symbolizer.class, configuration.getDefaultStyle(Constants.Style.Raster.NAME));
    }

    @Test
    public void testGridStyle() throws Exception {
        MapfishMapContext mapContext = new MapfishMapContext(new BBoxMapBounds(null, 0,0,10,10), new Dimension(20,20), 0, 72, null);
        final Configuration configuration = new Configuration();
        final Style gridStyle = configuration.getDefaultStyle(Constants.Style.Grid.NAME);
        final AtomicInteger foundLineSymb = new AtomicInteger(0);
        final AtomicInteger foundTextSymb = new AtomicInteger(0);

        final AbstractStyleVisitor styleValidator = new AbstractStyleVisitor() {
            @Override
            public void visit(LineSymbolizer line) {
                foundLineSymb.incrementAndGet();
                super.visit(line);
            }

            @Override
            public void visit(TextSymbolizer text) {
                foundTextSymb.incrementAndGet();
                final PointPlacement labelPlacement = (PointPlacement) text.getLabelPlacement();
                assertNotNull(labelPlacement.getDisplacement());
                super.visit(text);
            }
        };

        styleValidator.visit(gridStyle);

        assertEquals(1, foundLineSymb.intValue());
        assertEquals(1, foundTextSymb.intValue());
    }

    private void assertStyleType(Class<?> expectedSymbolizerType, Style style) {
        assertNotNull(style);
        final FeatureTypeStyle featureTypeStyle = style.featureTypeStyles().get(0);
        final Rule rule = featureTypeStyle.rules().get(0);
        final Class<? extends Symbolizer> symbClass = rule.symbolizers().get(0).getClass();
        assertTrue("Expected: " + expectedSymbolizerType.getName() + " but was: " + symbClass,
                expectedSymbolizerType.isAssignableFrom(symbClass));
    }
}
