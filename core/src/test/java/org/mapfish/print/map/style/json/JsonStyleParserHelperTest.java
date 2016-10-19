package org.mapfish.print.map.style.json;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mapfish.print.Constants;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.wrapper.json.PJsonArray;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.style.GraphicalSymbol;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mapfish.print.map.style.json.MapfishJsonStyleParserPlugin.Versions;
import static org.mapfish.print.map.style.json.MapfishJsonStyleParserPluginTest.REQUEST_DATA_STYLE_JSON_V1_STYLE_JSON;

public class JsonStyleParserHelperTest {

    private static final float FLOAT_DELTA = 0.00001f;

    final SLDTransformer transformer = new SLDTransformer();
    JsonStyleParserHelper helper;

    @Before
    public void setUp() throws Exception {
        Configuration configuration = new Configuration();
        final File file = getFile(MapfishJsonStyleParserPluginTest.class, REQUEST_DATA_STYLE_JSON_V1_STYLE_JSON);
        configuration.setConfigurationFile(file);
        helper = new JsonStyleParserHelper(configuration, new StyleBuilder(), true, Versions.ONE);
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
        transformer.transform(symbolizer); // assert it can be converted to SLD

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
        final String fontFamily = "Liberation Sans, sans-serif";
        final String fontWeight = "bold";
        final String labelXOffset = "-25.0";
        final String labelYOffset = "-35.0";
        final String haloColor = "#123456";
        final String haloRadius = "3.0";


        JSONObject style = new JSONObject();
        style.put("label", "name");
        style.put("fontColor", fontColor);
        style.put("fontStyle", fontStyle);
        style.put("fontFamily", fontFamily);
        style.put("fontWeight", fontWeight);
        style.put("fontSize", "12");
        style.put("labelXOffset", labelXOffset);
        style.put("labelYOffset", labelYOffset);
        style.put("labelRotation", "45");
        style.put("labelAlign", "cm");
        style.put("haloColor", haloColor);
        style.put("haloOpacity", "0.7");
        style.put("haloRadius", haloRadius);
        style.put("conflictResolution", "false");
        style.put("goodnessOfFit", "0.6");
        style.put("spaceAround", "10");

        final PJsonObject pStyle = new PJsonObject(style, null);
        TextSymbolizer symbolizer = helper.createTextSymbolizer(pStyle);
        assertNotNull(symbolizer);

        transformer.transform(symbolizer);  // test that it can be written to xml correctly

        assertFill(1.0, fontColor, symbolizer.getFill());
        assertEquals("name", valueOf(symbolizer.getLabel()));

        final Font font = symbolizer.getFont();
        final List<Expression> family = font.getFamily();
        assertEquals(2, family.size());
        assertEquals("Liberation Sans", valueOf(family.get(0)));
        assertEquals("SansSerif", valueOf(family.get(1)));
        assertEquals(12.0, valueOf(font.getSize()));
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
        assertEquals(15.0, valueOf(symbolizer.getFont().getSize()));
        assertEquals("false", symbolizer.getOptions().get("conflictResolution"));
        assertEquals("0.6", symbolizer.getOptions().get("goodnessOfFit"));
        assertEquals("10", symbolizer.getOptions().get("spaceAround"));
    }

    @Test
    public void testCreateTextSymbolizerInPX() throws Exception {
        final String fontColor = "#333333";
        final String fontStyle = "normal";
        final String fontFamily = "Liberation Sans, sans-serif";
        final String fontWeight = "bold";
        final String labelXOffset = "-25.0";
        final String labelYOffset = "-35.0";
        final String haloColor = "#123456";
        final String haloRadius = "3.0";


        JSONObject style = new JSONObject();
        style.put("label", "name");
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
        style.put("conflictResolution", "false");
        style.put("goodnessOfFit", "0.6");
        style.put("spaceAround", "10");

        final PJsonObject pStyle = new PJsonObject(style, null);
        TextSymbolizer symbolizer = helper.createTextSymbolizer(pStyle);
        assertNotNull(symbolizer);

        transformer.transform(symbolizer);  // test that it can be written to xml correctly

        assertFill(1.0, fontColor, symbolizer.getFill());
        assertEquals("name", valueOf(symbolizer.getLabel()));

        final Font font = symbolizer.getFont();
        final List<Expression> family = font.getFamily();
        assertEquals(2, family.size());
        assertEquals("Liberation Sans", valueOf(family.get(0)));
        assertEquals("SansSerif", valueOf(family.get(1)));
        assertEquals(12.0, valueOf(font.getSize()));
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
        style.put("fontSize", "15px");
        symbolizer = helper.createTextSymbolizer(pStyle);
        assertEquals("label", valueOf(symbolizer.getLabel()));
        assertEquals(15.0, valueOf(symbolizer.getFont().getSize()));
        assertEquals("false", symbolizer.getOptions().get("conflictResolution"));
        assertEquals("0.6", symbolizer.getOptions().get("goodnessOfFit"));
        assertEquals("10", symbolizer.getOptions().get("spaceAround"));
    }

    @Test
    public void testCreateTextSymbolizerInPT() throws Exception {
        final double delta = 0.00000000000001;

        final String fontColor = "#333333";
        final String fontStyle = "normal";
        final String fontFamily = "Liberation Sans, sans-serif";
        final String fontWeight = "bold";
        final String labelXOffset = "-25.0";
        final String labelYOffset = "-35.0";
        final String haloColor = "#123456";
        final String haloRadius = "3.0";


        JSONObject style = new JSONObject();
        style.put("label", "name");
        style.put("fontColor", fontColor);
        style.put("fontStyle", fontStyle);
        style.put("fontFamily", fontFamily);
        style.put("fontWeight", fontWeight);
        style.put("fontSize", "12pt");
        style.put("labelXOffset", labelXOffset);
        style.put("labelYOffset", labelYOffset);
        style.put("labelRotation", "45");
        style.put("labelAlign", "cm");
        style.put("haloColor", haloColor);
        style.put("haloOpacity", "0.7");
        style.put("haloRadius", haloRadius);
        style.put("conflictResolution", "false");
        style.put("goodnessOfFit", "0.6");
        style.put("spaceAround", "10");

        final PJsonObject pStyle = new PJsonObject(style, null);
        TextSymbolizer symbolizer = helper.createTextSymbolizer(pStyle);
        assertNotNull(symbolizer);

        transformer.transform(symbolizer);  // test that it can be written to xml correctly

        assertFill(1.0, fontColor, symbolizer.getFill());
        assertEquals("name", valueOf(symbolizer.getLabel()));

        final Font font = symbolizer.getFont();
        final List<Expression> family = font.getFamily();
        assertEquals(2, family.size());
        assertEquals("Liberation Sans", valueOf(family.get(0)));
        assertEquals("SansSerif", valueOf(family.get(1)));
        assertEquals(12.0, (Double)valueOf(font.getSize()), delta);
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
        style.put("fontSize", "15pt");
        symbolizer = helper.createTextSymbolizer(pStyle);
        assertEquals("label", valueOf(symbolizer.getLabel()));
        assertEquals(15.0, (Double)valueOf(symbolizer.getFont().getSize()), delta);
        assertEquals("false", symbolizer.getOptions().get("conflictResolution"));
        assertEquals("0.6", symbolizer.getOptions().get("goodnessOfFit"));
        assertEquals("10", symbolizer.getOptions().get("spaceAround"));
    }

    @Test
    public void testCreateTextSymbolizerInPC() throws Exception {
        final double delta = 0.00000000000001;

        final String fontColor = "#333333";
        final String fontStyle = "normal";
        final String fontFamily = "Liberation Sans, sans-serif";
        final String fontWeight = "bold";
        final String labelXOffset = "-25.0";
        final String labelYOffset = "-35.0";
        final String haloColor = "#123456";
        final String haloRadius = "3.0";


        JSONObject style = new JSONObject();
        style.put("label", "name");
        style.put("fontColor", fontColor);
        style.put("fontStyle", fontStyle);
        style.put("fontFamily", fontFamily);
        style.put("fontWeight", fontWeight);
        style.put("fontSize", "1pc");
        style.put("labelXOffset", labelXOffset);
        style.put("labelYOffset", labelYOffset);
        style.put("labelRotation", "45");
        style.put("labelAlign", "cm");
        style.put("haloColor", haloColor);
        style.put("haloOpacity", "0.7");
        style.put("haloRadius", haloRadius);
        style.put("conflictResolution", "false");
        style.put("goodnessOfFit", "0.6");
        style.put("spaceAround", "10");

        final PJsonObject pStyle = new PJsonObject(style, null);
        TextSymbolizer symbolizer = helper.createTextSymbolizer(pStyle);
        assertNotNull(symbolizer);

        transformer.transform(symbolizer);  // test that it can be written to xml correctly

        assertFill(1.0, fontColor, symbolizer.getFill());
        assertEquals("name", valueOf(symbolizer.getLabel()));

        final Font font = symbolizer.getFont();
        final List<Expression> family = font.getFamily();
        assertEquals(2, family.size());
        assertEquals("Liberation Sans", valueOf(family.get(0)));
        assertEquals("SansSerif", valueOf(family.get(1)));
        assertEquals(12.0, (Double)valueOf(font.getSize()), delta);
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
        style.put("fontSize", "1.2pc");
        symbolizer = helper.createTextSymbolizer(pStyle);
        assertEquals("label", valueOf(symbolizer.getLabel()));
        assertEquals(14.4, (Double)valueOf(symbolizer.getFont().getSize()), delta);
        assertEquals("false", symbolizer.getOptions().get("conflictResolution"));
        assertEquals("0.6", symbolizer.getOptions().get("goodnessOfFit"));
        assertEquals("10", symbolizer.getOptions().get("spaceAround"));
    }

    @Test
    public void testHaloAliasLabelOutline() throws Exception {
        final String haloColor = "#123456";
        final String haloRadius = "3.0";


        JSONObject style = new JSONObject();
        style.put("label", "name");
        style.put(JsonStyleParserHelper.JSON_LABEL_OUTLINE_COLOR, haloColor);
        style.put(JsonStyleParserHelper.JSON_LABEL_OUTLINE_WIDTH, haloRadius);

        final PJsonObject pStyle = new PJsonObject(style, null);
        TextSymbolizer symbolizer = helper.createTextSymbolizer(pStyle);
        assertNotNull(symbolizer);

        transformer.transform(symbolizer);  // test that it can be written to xml correctly

        Halo halo = symbolizer.getHalo();
        assertFill(1.0, haloColor, halo.getFill());
        assertEquals(haloRadius, valueOf(halo.getRadius()).toString());
    }

    @Test
    public void testCreateStroke_DashStyle() throws Exception {

        assertDashStyle("5 4 3 4", new float[]{5f, 4f, 3f, 4f});
        assertDashStyle(JsonStyleParserHelper.STROKE_DASHSTYLE_SOLID, null);
        assertDashStyle(JsonStyleParserHelper.STROKE_DASHSTYLE_DASH, new float[]{2f, 2f});
        assertDashStyle(JsonStyleParserHelper.STROKE_DASHSTYLE_DASHDOT, new float[]{3f, 2f, 0.1f, 2f});
        assertDashStyle(JsonStyleParserHelper.STROKE_DASHSTYLE_DOT, new float[]{0.1f, 2f});
        assertDashStyle(JsonStyleParserHelper.STROKE_DASHSTYLE_LONGDASH, new float[]{4f, 2f});
        assertDashStyle(JsonStyleParserHelper.STROKE_DASHSTYLE_LONGDASHDOT, new float[]{5f, 2f, 0.1f, 2f});
    }

    @Test
    public void testDefaultPointSymbolizer() throws Exception {
        helper.setAllowNullSymbolizer(false);
        JSONObject json = new JSONObject();
        PJsonObject pJson = new PJsonObject(json, "symbolizers");
        final PointSymbolizer pointSymbolizer = this.helper.createPointSymbolizer(pJson);
        assertNotNull(pointSymbolizer);

        final Graphic graphic = pointSymbolizer.getGraphic();
        assertEquals(1, graphic.graphicalSymbols().size());
        Mark mark = (Mark) graphic.graphicalSymbols().get(0);

        assertEquals("circle", valueOf(mark.getWellKnownName()));
        assertNotNull(mark.getFill());
        assertNotNull(mark.getStroke());
    }

    @Test
    public void testSomeDefaultPointSymbolizer() throws Exception {
        helper.setAllowNullSymbolizer(false);
        helper.setVersion(Versions.TWO);
        JSONObject json = new JSONObject();
        json.put(JsonStyleParserHelper.JSON_STROKE_DASHSTYLE, "5 4");

        PJsonObject pJson = new PJsonObject(json, "symbolizers");
        final PointSymbolizer pointSymbolizer = this.helper.createPointSymbolizer(pJson);
        assertNotNull(pointSymbolizer);

        final Graphic graphic = pointSymbolizer.getGraphic();
        Mark mark = (Mark) graphic.graphicalSymbols().get(0);
        Stroke stroke = mark.getStroke();
        assertArrayEquals(Arrays.toString(stroke.getDashArray()), new float[]{5f, 4f}, stroke.getDashArray(), FLOAT_DELTA);
    }

    @Test
    public void testDefaultLineSymbolizer() throws Exception {
        helper.setAllowNullSymbolizer(false);
        JSONObject json = new JSONObject();
        PJsonObject pJson = new PJsonObject(json, "symbolizers");
        final LineSymbolizer lineSymbolizer = this.helper.createLineSymbolizer(pJson);
        assertNotNull(lineSymbolizer);

        final Stroke stroke = lineSymbolizer.getStroke();
        assertNotNull(stroke);

        assertNull(stroke.getDashArray());
    }

    @Test
    public void testSomeDefaultLineSymbolizer() throws Exception {
        helper.setAllowNullSymbolizer(false);
        helper.setVersion(Versions.TWO);
        JSONObject json = new JSONObject();
        json.put(JsonStyleParserHelper.JSON_STROKE_DASHSTYLE, "5 4");

        PJsonObject pJson = new PJsonObject(json, "symbolizers");
        final LineSymbolizer lineSymbolizer = this.helper.createLineSymbolizer(pJson);
        assertNotNull(lineSymbolizer);

        final Stroke stroke = lineSymbolizer.getStroke();
        assertArrayEquals(Arrays.toString(stroke.getDashArray()), new float[]{5f, 4f}, stroke.getDashArray(), FLOAT_DELTA);
    }

    @Test
    public void testDefaultPolygonSymbolizer() throws Exception {
        helper.setAllowNullSymbolizer(false);
        JSONObject json = new JSONObject();
        PJsonObject pJson = new PJsonObject(json, "symbolizers");
        final PolygonSymbolizer polygonSymbolizer = this.helper.createPolygonSymbolizer(pJson);
        assertNotNull(polygonSymbolizer);

        final Stroke stroke = polygonSymbolizer.getStroke();
        assertNotNull(stroke);
        assertNull(stroke.getDashArray());

        assertNotNull(polygonSymbolizer.getFill());
    }

    @Test
    public void testSomeDefaultPolygonSymbolizer() throws Exception {
        helper.setAllowNullSymbolizer(false);
        helper.setVersion(Versions.TWO);
        JSONObject json = new JSONObject();
        json.put(JsonStyleParserHelper.JSON_STROKE_DASHSTYLE, "5 4");

        PJsonObject pJson = new PJsonObject(json, "symbolizers");
        final PolygonSymbolizer polygonSymbolizer = this.helper.createPolygonSymbolizer(pJson);
        assertNotNull(polygonSymbolizer);

        final Stroke stroke = polygonSymbolizer.getStroke();
        assertArrayEquals(Arrays.toString(stroke.getDashArray()), new float[]{5f, 4f}, stroke.getDashArray(), FLOAT_DELTA);

        assertNotNull(polygonSymbolizer.getFill());
    }

    @Test
    public void testDefaultTextSymbolizer() throws Exception {
        final String label = "label";

        helper.setAllowNullSymbolizer(false);
        helper.setVersion(Versions.TWO);
        JSONObject json = new JSONObject();
        json.put(JsonStyleParserHelper.JSON_LABEL, label);
        PJsonObject pJson = new PJsonObject(json, "symbolizers");
        final TextSymbolizer textSymbolizer = this.helper.createTextSymbolizer(pJson);
        assertNotNull(textSymbolizer);

        assertNotNull(textSymbolizer.getFill());
    }

    @Test
    public void testSomeDefaultTextSymbolizer() throws Exception {
        final String label = "label";

        helper.setAllowNullSymbolizer(false);
        helper.setVersion(Versions.TWO);
        JSONObject json = new JSONObject();
        json.put(JsonStyleParserHelper.JSON_LABEL, label);
        json.put(JsonStyleParserHelper.JSON_FONT_COLOR, "red");

        PJsonObject pJson = new PJsonObject(json, "symbolizers");
        final TextSymbolizer textSymbolizer = this.helper.createTextSymbolizer(pJson);
        assertNotNull(textSymbolizer);

        assertNotNull(textSymbolizer.getFill());
        assertEquals("#FF0000", valueOf(textSymbolizer.getFill().getColor()));
    }

    @Test
    public void testGetGraphicFormatDetect() throws Exception {
        // geotools only accepts formats that are supported by ImageIO. so we can have a problem if the format is image/jpg but
        // ImageIO supports image/jpeg.  The two don't match so the image won't be loaded (even if it could).
        final List<String> strings = Arrays.asList(ImageIO.getReaderMIMETypes());
        PJsonObject styleJson = new PJsonObject(new JSONObject(), "style");
        for (String supportedMimetype : strings) {
            Set<String> compatibleMimetypes = findCompatibleMimeTypes(supportedMimetype);
            for (String mimeType : compatibleMimetypes) {
                if (Strings.isNullOrEmpty(mimeType)) {
                    continue;
                }
                styleJson.getInternalObj().put(JsonStyleParserHelper.JSON_GRAPHIC_FORMAT, mimeType);
                final String graphicFormat = helper.getGraphicFormat("http://somefile.com/file.jpg", styleJson);
                assertTrue(graphicFormat + " is not supported", strings.contains(graphicFormat));
            }
        }
    }

    private Set<String> findCompatibleMimeTypes(String mimeType) {
        for (Set<String> compatibleMimetypes : helper.COMPATIBLE_MIMETYPES) {
            if (compatibleMimetypes.contains(mimeType)) {
                return compatibleMimetypes;
            }
        }

        return Sets.newHashSet(mimeType);
    }


    @Test
    public void testLabelAttributes() throws Exception {
        JSONObject json = new JSONObject();
        PJsonObject pJson = new PJsonObject(json, "symbolizers");

        json.put(JsonStyleParserHelper.JSON_LABEL, "att");
        TextSymbolizer textSymbolizer = this.helper.createTextSymbolizer(pJson);
        assertTrue(textSymbolizer.getLabel() instanceof Literal);

        json.put(JsonStyleParserHelper.JSON_LABEL, "[att]");
        textSymbolizer = this.helper.createTextSymbolizer(pJson);
        assertTrue(textSymbolizer.getLabel() instanceof PropertyName);

        json.put(JsonStyleParserHelper.JSON_LABEL, "['att']");
        textSymbolizer = this.helper.createTextSymbolizer(pJson);
        assertTrue(textSymbolizer.getLabel() instanceof Literal);

        json.put(JsonStyleParserHelper.JSON_LABEL, "[env('java.home')]");
        textSymbolizer = this.helper.createTextSymbolizer(pJson);
        assertTrue(textSymbolizer.getLabel() instanceof Function);

        json.put(JsonStyleParserHelper.JSON_LABEL, "[centroid(geomAtt)]");
        textSymbolizer = this.helper.createTextSymbolizer(pJson);
        assertTrue(textSymbolizer.getLabel() instanceof Function);
    }

    @Test
    public void testExpressionProperties() throws Exception {
        String jsonString = Files.toString(getFile("v2-style-all-properies-as-expressions.json"), Constants.DEFAULT_CHARSET);
        PJsonObject json = MapPrinter.parseSpec(jsonString).getJSONObject("*");

        final PJsonArray symb = json.getJSONArray(MapfishJsonStyleVersion2.JSON_SYMB);
        final PointSymbolizer pointSymbolizer = this.helper.createPointSymbolizer(symb.getJSONObject(0));
        final LineSymbolizer lineSymbolizer = this.helper.createLineSymbolizer(symb.getJSONObject(1));
        final PolygonSymbolizer polygonSymbolizer = this.helper.createPolygonSymbolizer(symb.getJSONObject(2));
        final TextSymbolizer textSymbolizer = this.helper.createTextSymbolizer(symb.getJSONObject(3));

        final Graphic graphic = pointSymbolizer.getGraphic();
        assertEquals("rotation", propertyName(graphic.getRotation()));
        assertEquals("graphicOpacity", propertyName(graphic.getOpacity()));
        assertEquals("pointRadius", propertyName(graphic.getSize()));

        Mark mark = (Mark) graphic.graphicalSymbols().get(0);
        assertEquals("graphicName", propertyName(mark.getWellKnownName()));

        assertEquals("fillOpacity", propertyName(mark.getFill().getOpacity()));
        assertEquals("fillColor", propertyName(mark.getFill().getColor()));

        assertEquals("strokeColor", propertyName(mark.getStroke().getColor()));
        assertEquals("strokeOpacity", propertyName(mark.getStroke().getOpacity()));
        assertEquals("strokeWidth", propertyName(mark.getStroke().getWidth()));
        assertEquals("strokeLinecap", propertyName(mark.getStroke().getLineCap()));

        assertEquals("lineStrokeColor", propertyName(lineSymbolizer.getStroke().getColor()));
        assertEquals("lineStrokeOpacity", propertyName(lineSymbolizer.getStroke().getOpacity()));
        assertEquals("lineStrokeWidth", propertyName(lineSymbolizer.getStroke().getWidth()));
        assertEquals("lineStrokeLinecap", propertyName(lineSymbolizer.getStroke().getLineCap()));

        assertEquals("PolyStrokeColor", propertyName(polygonSymbolizer.getStroke().getColor()));
        assertEquals("PolyStrokeOpacity", propertyName(polygonSymbolizer.getStroke().getOpacity()));
        assertEquals("PolyStrokeWidth", propertyName(polygonSymbolizer.getStroke().getWidth()));
        assertEquals("PolyStrokeLinecap", propertyName(polygonSymbolizer.getStroke().getLineCap()));

        assertEquals("PolyFillOpacity", propertyName(polygonSymbolizer.getFill().getOpacity()));
        assertEquals("PolyFillColor", propertyName(polygonSymbolizer.getFill().getColor()));

        assertEquals("fontColor", propertyName(textSymbolizer.getFill().getColor()));
        assertEquals("fontFamily", propertyName(textSymbolizer.getFont().getFamily().get(0)));
        assertEquals("fontSize", propertyName(textSymbolizer.getFont().getSize()));
        assertEquals("fontStyle", propertyName(textSymbolizer.getFont().getStyle()));
        assertEquals("fontWeight", propertyName(textSymbolizer.getFont().getWeight()));
        final Halo halo = textSymbolizer.getHalo();
        assertEquals("haloColor", propertyName(halo.getFill().getColor()));
        assertEquals("haloOpacity", propertyName(halo.getFill().getOpacity()));
        assertEquals("haloRadius", propertyName(halo.getRadius()));
        assertEquals("label", propertyName(textSymbolizer.getLabel()));
        assertEquals("labelRotation", propertyName(((PointPlacement)textSymbolizer.getLabelPlacement()).getRotation()));


    }

    private String propertyName(Expression rotation) {
        return ((PropertyName)rotation).getPropertyName();
    }

    private void assertDashStyle(String dashStyle, float[] expectedDashArray) throws JSONException {
        JSONObject strokeJson = new JSONObject();
        strokeJson.put(JsonStyleParserHelper.JSON_STROKE_DASHSTYLE, dashStyle);

        PJsonObject pStyle = new PJsonObject(strokeJson, "style");
        final Stroke stroke = helper.createStroke(pStyle, false);
        assertNotNull(stroke);
        assertArrayEquals(Arrays.toString(stroke.getDashArray()), expectedDashArray, stroke.getDashArray(), FLOAT_DELTA);
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