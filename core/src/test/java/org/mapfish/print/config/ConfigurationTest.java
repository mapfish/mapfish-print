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

import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link org.mapfish.print.config.Configuration} class.
 *
 * @author Jesse on 3/27/14.
 */
public class ConfigurationTest {
    @Test
    public void testGetDefaultStyle_IsPresentInMap() throws Exception {
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
        configuration.setDefaultStyle(styles);

        assertSame(pointStyle, configuration.getDefaultStyle("POINT"));
        assertSame(pointStyle, configuration.getDefaultStyle("MultiPOINT"));

        assertSame(lineStyle, configuration.getDefaultStyle("lIne"));
        assertSame(lineStyle, configuration.getDefaultStyle("lInestring"));
        assertSame(lineStyle, configuration.getDefaultStyle("linearRing"));
        assertSame(lineStyle, configuration.getDefaultStyle("multilInestring"));
        assertSame(lineStyle, configuration.getDefaultStyle("multiline"));

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

        assertSame(geomStyle, configuration.getDefaultStyle("geom"));
        assertSame(geomStyle, configuration.getDefaultStyle("geometry"));
        assertSame(geomStyle, configuration.getDefaultStyle("geometryCollection"));
        assertSame(geomStyle, configuration.getDefaultStyle("MultiGeometry"));
    }
    @Test
    public void testGetDefaultStyle_GeomNotInMap() throws Exception {
        final Configuration configuration = new Configuration();

        assertStyleType(Symbolizer.class, configuration.getDefaultStyle("geom"));
        assertStyleType(Symbolizer.class, configuration.getDefaultStyle("geometry"));
        assertStyleType(Symbolizer.class, configuration.getDefaultStyle("geometryCollection"));
        assertStyleType(Symbolizer.class, configuration.getDefaultStyle("MultiGeometry"));
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
