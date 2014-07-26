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
import com.google.common.collect.Lists;
import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.SLDTransformer;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.servlet.MapPrinterServletTest;
import org.mapfish.print.servlet.ServletMapPrinterFactory;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@ContextConfiguration(locations = {
        MapPrinterServletTest.PRINT_CONTEXT,
})
public class MapfishJsonStyleParserPluginTest extends AbstractMapfishSpringTest {
    static final String REQUEST_DATA_STYLE_JSON_V1_STYLE_JSON = "requestData-style-json-v1-style.json";
    @Autowired
    private ServletMapPrinterFactory printerFactory;

    final SLDTransformer transformer = new SLDTransformer();
    MapfishJsonStyleParserPlugin parser = new MapfishJsonStyleParserPlugin();
    @Test
    public void testVersion1() throws Throwable {
        PJsonObject layerJson = loadLayerDataAsJson();
        Optional<Style> style = parser.parseStyle(null, new TestHttpClientFactory(), layerJson.getString("style"),
                null);
        assertTrue(style.isPresent());

        transformer.transform( style.get() ); // assert it can be converted to SLD

        final List<Rule> rules = Lists.newArrayList();
        style.get().accept(new AbstractStyleVisitor() {
            @Override
            public void visit(Rule rule) {
                rules.add(rule);
            }
        });
        assertEquals(1, rules.size());
        Rule rule = rules.get(0);

        assertEquals("1", rule.getName());

        assertTrue(rule.getFilter() instanceof PropertyIsEqualTo);
        PropertyIsEqualTo filter = (PropertyIsEqualTo) rule.getFilter();

        PropertyName propertyName = (PropertyName) filter.getExpression1();
        assertEquals("_gx_style", propertyName.getPropertyName());

        Literal valueExpression = (Literal) filter.getExpression2();
        assertEquals("1", valueExpression.getValue());

        final List<Symbolizer> symbolizers = rule.symbolizers();

        assertEquals(4, symbolizers.size());

        PointSymbolizer point = null;
        LineSymbolizer line = null;
        PolygonSymbolizer polygon = null;
        TextSymbolizer text = null;

        for (Symbolizer symbolizer : symbolizers) {
            if (symbolizer instanceof PointSymbolizer) {
                assertNull(point);
                point = (PointSymbolizer) symbolizer;
            } else if (symbolizer instanceof LineSymbolizer) {
                assertNull(line);
                line = (LineSymbolizer) symbolizer;
            } else if (symbolizer instanceof PolygonSymbolizer) {
                assertNull(polygon);
                polygon = (PolygonSymbolizer) symbolizer;
            } else if (symbolizer instanceof TextSymbolizer) {
                assertNull(text);
                text = (TextSymbolizer) symbolizer;
            } else {
                fail(symbolizer + " was unexpected");
            }
        }

        assertNotNull(point);
        assertNotNull(line);
        assertNotNull(polygon);
        assertNotNull(text);
    }

    private PJsonObject loadLayerDataAsJson() throws IOException {
        return AbstractMapfishSpringTest.parseJSONObjectFromFile(MapfishJsonStyleParserPluginTest.class,
                REQUEST_DATA_STYLE_JSON_V1_STYLE_JSON);
    }
}
