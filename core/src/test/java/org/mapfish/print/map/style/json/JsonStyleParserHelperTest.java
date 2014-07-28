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

import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.Fill;
import org.geotools.styling.Font;
import org.geotools.styling.Graphic;
import org.geotools.styling.Halo;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointPlacement;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.SLDTransformer;
import org.geotools.styling.Stroke;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.TextSymbolizer;
import org.jdom.Namespace;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.style.GraphicalSymbol;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mapfish.print.map.style.json.MapfishJsonStyleParserPluginTest.REQUEST_DATA_STYLE_JSON_V1_STYLE_JSON;

public class JsonStyleParserHelperTest {

    private static final Namespace OGC = Namespace.getNamespace("ogc", "http://www.opengis.net/ogc");
    private static final Namespace XLINK = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");

    final SLDTransformer transformer = new SLDTransformer();
    JsonStyleParserHelper helper;

    @Before
    public void setUp() throws Exception {
        Configuration configuration = new Configuration();
        final File file = getFile(MapfishJsonStyleParserPluginTest.class, REQUEST_DATA_STYLE_JSON_V1_STYLE_JSON);
        configuration.setConfigurationFile(file);
        helper = new JsonStyleParserHelper(configuration, new StyleBuilder(), true);
    }

    private File getFile(Class<?> base, String fileName) throws URISyntaxException {
        final URI uri = base.getResource(fileName).toURI();
        return new File(uri);
    }
   private File getFile(String fileName) throws URISyntaxException {
        return getFile(JsonStyleParserHelperTest.class, fileName);
    }

    @Test
    public void testCreatePointSymbolizerExternalGraphic() throws Exception {

        JSONObject style = new JSONObject();
        style.put("externalGraphic", "mark.png");

        PointSymbolizer symbolizer = helper.createPointSymbolizer(new PJsonObject(style, null));
        assertNotNull(symbolizer);
        transformer.transform( symbolizer ); // assert it can be converted to SLD

        final Graphic graphic = symbolizer.getGraphic();
        assertNotNull(graphic);

        final List<GraphicalSymbol> graphicalSymbols = graphic.graphicalSymbols();

        assertEquals(1, graphicalSymbols.size());

        ExternalGraphic externalGraphic = (ExternalGraphic) graphicalSymbols.get(0);
        assertEquals("image/png", externalGraphic.getFormat());
        assertEquals(getFile("mark.png"), new File(externalGraphic.getLocation().toURI()));
    }

    @Test
    public void testCreatePointSymbolizerMark() throws Exception {
        JSONObject style = new JSONObject();
        style.put("fillColor", "#eee");
        final double fillOpacity = 0.3;
        style.put("fillOpacity", fillOpacity);
        style.put("graphicName", "circle");
        style.put("strokeColor", "#fff");
        final double stokeOpacity = 0.2;
        style.put("strokeOpacity", stokeOpacity);
        style.put("strokeWidth", 2);
        style.put("strokeDashstyle", "5 2");
        final String lineCap = "round";
        style.put("strokeLinecap", lineCap);
        style.put("graphicOpacity", 0.4);
        style.put("pointRadius", 5);
        style.put("rotation", 90);

        final PointSymbolizer symbolizer = helper.createPointSymbolizer(new PJsonObject(style, null));
        assertNotNull(symbolizer);

        transformer.transform(symbolizer); // verify it can be encoded without exceptions

        final Graphic graphic = symbolizer.getGraphic();
        assertEquals(1, graphic.graphicalSymbols().size());

        Mark externalGraphic = (Mark) graphic.graphicalSymbols().get(0);

        assertEquals("circle", valueOf(externalGraphic.getWellKnownName()));

        assertFill(fillOpacity, "#EEEEEE", externalGraphic.getFill());

        assertStroke(stokeOpacity, lineCap, externalGraphic.getStroke(), "#FFFFFF", new float[]{5f, 2f}, 2.0);

        assertEquals(0.4, valueOf(graphic.getOpacity()));
        assertEquals(10.0, valueOf(graphic.getSize()));
        assertEquals(90.0, valueOf(graphic.getRotation()));
    }

    @Test
    public void testCreatePolygonSymbolizer() throws Exception {
        JSONObject style = new JSONObject();
        style.put("fillColor", "#eee");
        style.put("fillOpacity", 0.3);
        style.put("strokeColor", "#fff");
        style.put("strokeOpacity", 0.2);
        style.put("strokeWidth", 2);
        style.put("strokeDashstyle", "5 2");
        style.put("strokeLinecap", "round");

        final PolygonSymbolizer symbolizer = helper.createPolygonSymbolizer(new PJsonObject(style, null));
        assertNotNull(symbolizer);

        transformer.transform(symbolizer); // verify it converts to xml correctly

        assertStroke(0.2, "round", symbolizer.getStroke(), "#FFFFFF", new float[]{5f, 2f}, 2.0);
        assertFill(0.3, "#EEEEEE", symbolizer.getFill());
    }

    @Test
    public void testCreateLineSymbolizer() throws Exception {
        JSONObject style = new JSONObject();
        style.put("strokeColor", "#fff");
        style.put("strokeOpacity", 0.2);
        style.put("strokeWidth", 2);
        style.put("strokeDashstyle", "5 2");
        style.put("strokeLinecap", "round");

        LineSymbolizer symbolizer = helper.createLineSymbolizer(new PJsonObject(style, null));
        assertNotNull(symbolizer);

        transformer.transform(symbolizer); // verify it converts to xml correctly

        assertStroke(0.2, "round", symbolizer.getStroke(), "#FFFFFF", new float[]{5f, 2f}, 2.0);
    }

    @Test
    public void testCreateTextSymbolizer() throws Exception {
        final String fontColor = "#333333";
        final String fontStyle = "normal";
        final String fontFamily = "sans-serif";
        final String fontWeight = "bold";
        final String labelXOffset = "-25.0";
        final String labelYOffset = "-35.0";
        final String haloColor = "#123456";
        final String haloRadius = "3.0";


        JSONObject style = new JSONObject();
        style.put("label", "${name}");
        style.put("fontColor", fontColor);
        style.put("fontStyle", fontStyle);
        style.put("fontFamily", fontFamily);
        style.put("fontWeight", fontWeight);
        style.put("fontSize", "12px");
        style.put("labelXOffset", labelXOffset);
        style.put("labelYOffset", labelYOffset);
        style.put("labelRotation", "45");
        style.put("labelAlign", "cm");
        style.put("haloColor", haloColor);
        style.put("haloOpacity", "0.7");
        style.put("haloRadius", haloRadius);

        final PJsonObject pStyle = new PJsonObject(style, null);
        TextSymbolizer symbolizer = helper.createTextSymbolizer(pStyle);
        assertNotNull(symbolizer);

        transformer.transform(symbolizer);  // test that it can be written to xml correctly

        assertFill(1.0, fontColor, symbolizer.getFill());
        assertEquals("name", ((PropertyName) symbolizer.getLabel()).getPropertyName());

        final Font font = symbolizer.getFont();
        final List<Expression> family = font.getFamily();
        assertEquals(1, family.size());
        assertEquals(fontFamily, valueOf(family.get(0)));
        assertEquals(12, valueOf(font.getSize()));
        assertEquals(fontStyle, valueOf(font.getStyle()));
        assertEquals(fontWeight, valueOf(font.getWeight()));

        PointPlacement placement = (PointPlacement) symbolizer.getLabelPlacement();
        assertEquals(45.0, valueOf(placement.getRotation()));
        assertEquals(0.5, valueOf(placement.getAnchorPoint().getAnchorPointX()));
        assertEquals(0.5, valueOf(placement.getAnchorPoint().getAnchorPointY()));
        assertEquals(labelXOffset, valueOf(placement.getDisplacement().getDisplacementX()).toString());
        assertEquals(labelYOffset, valueOf(placement.getDisplacement().getDisplacementY()).toString());

        Halo halo = symbolizer.getHalo();
        assertFill(0.7, haloColor, halo.getFill());
        assertEquals(haloRadius, valueOf(halo.getRadius()).toString());

        style.put("label", "label");
        style.put("fontSize", "15");
        symbolizer = helper.createTextSymbolizer(pStyle);
        assertEquals("label", valueOf(symbolizer.getLabel()));
        assertEquals(15, valueOf(symbolizer.getFont().getSize()));
    }

    @Test
    public void testCreateStroke_DashStyle() throws Exception {

        assertDashStyle("5 4 3 4", new float[]{5f, 4f, 3f, 4f});
        assertDashStyle(JsonStyleParserHelper.STROKE_DASHSTYLE_SOLID, new float[]{1f});
        assertDashStyle(JsonStyleParserHelper.STROKE_DASHSTYLE_DASH, new float[]{2f, 2f});
        assertDashStyle(JsonStyleParserHelper.STROKE_DASHSTYLE_DASHDOT, new float[]{3f, 2f, 0.1f, 2f});
        assertDashStyle(JsonStyleParserHelper.STROKE_DASHSTYLE_DOT, new float[]{0.1f, 2f});
        assertDashStyle(JsonStyleParserHelper.STROKE_DASHSTYLE_LONGDASH, new float[]{4f, 2f});
        assertDashStyle(JsonStyleParserHelper.STROKE_DASHSTYLE_LONGDASHDOT, new float[]{5f, 2f, 0.1f, 2f});
    }

    private void assertDashStyle(String dashStyle, float[] expectedDashArray) throws JSONException {
        JSONObject strokeJson = new JSONObject();
        strokeJson.put(JsonStyleParserHelper.JSON_STROKE_DASHSTYLE, dashStyle);

        PJsonObject pStyle = new PJsonObject(strokeJson, "style");
        final Stroke stroke = helper.createStroke(pStyle, false);
        assertNotNull(stroke);
        assertArrayEquals(Arrays.toString(stroke.getDashArray()), expectedDashArray, stroke.getDashArray(), 0.00001f);
    }

    static void assertStroke(double stokeOpacity, String lineCap, Stroke stroke, String strokeColor, float[] dashArray,
                              double strokeWidth) {
        assertEquals(stokeOpacity, valueOf(stroke.getOpacity()));
        assertEquals(strokeColor, valueOf(stroke.getColor()));
        assertArrayEquals(dashArray, stroke.getDashArray(), 0.001f);
        assertEquals(strokeWidth, valueOf(stroke.getWidth()));
        assertEquals(lineCap, valueOf(stroke.getLineCap()));
    }

    static Object valueOf(Expression expr) {
        return ((Literal) expr).getValue();
    }

    static void assertFill(double fillOpacity, String fillColor, Fill fill) {
        assertEquals(fillColor, valueOf(fill.getColor()));
        assertEquals(fillOpacity, valueOf(fill.getOpacity()));
    }

}