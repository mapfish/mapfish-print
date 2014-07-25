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

package org.mapfish.print.servlet.oldapi;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.jdom.Element;
import org.jdom.Namespace;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.servlet.MapPrinterServletTest;
import org.mapfish.print.servlet.ServletMapPrinterFactory;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = {
        MapPrinterServletTest.PRINT_CONTEXT,
})
public class OldAPIStyleConverterTest extends AbstractMapfishSpringTest {
    @Autowired
    private ServletMapPrinterFactory printerFactory;
    
    private static final Namespace OGC = Namespace.getNamespace("ogc", "http://www.opengis.net/ogc");
    private static final Namespace XLINK = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
    
    @Test
    public void testConvertEmpty() throws IOException, JSONException {
        JSONObject layer = new JSONObject();
        layer.append("styles", new JSONObject());
        PJsonObject layerJson = new PJsonObject(layer, null);
        String style = OldAPIStyleConverter.convert(layerJson);
        assertEquals("", style);
    }

    @Test
    public void testConvert() throws IOException {
        PJsonObject layerJson = loadLayerDataAsJson();
        String style = OldAPIStyleConverter.convert(layerJson);
        assertTrue(style.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(style.contains("<StyledLayerDescriptor"));
    }

    @Test
    public void testGetStyleRules() throws Exception {
        PJsonObject layerJson = loadLayerDataAsJson();
        PJsonObject oldStyles = layerJson.getJSONObject("styles");
        List<Element> styles = OldAPIStyleConverter.getStyleRules(oldStyles, "_gx_style");
        
        assertEquals(1, styles.size());
        Element style = styles.get(0);
        
        assertEquals("1", style.getChild("name").getText());
        
        Element filter = style.getChild("Filter");
        Element propertyIsEqualTo = filter.getChild("PropertyIsEqualTo");
        assertEquals("_gx_style", propertyIsEqualTo.getChild("PropertyName").getText());
        assertEquals("1", propertyIsEqualTo.getChild("Literal").getText());
        
        assertNotNull(style.getChild("PointSymbolizer"));
        assertNotNull(style.getChild("LineSymbolizer"));
        assertNotNull(style.getChild("PolygonSymbolizer"));
        assertNotNull(style.getChild("TextSymbolizer"));
    }

    @Test
    public void testAddPointSymbolizerExternalGraphic() throws Exception {
        JSONObject style = new JSONObject();
        style.put("externalGraphic", "marker.png");
        
        Element rule = new Element("Rule");
        OldAPIStyleConverter.addPointSymbolizer(rule, new PJsonObject(style, null));
        
        Element pointSymbolizer = rule.getChild("PointSymbolizer");
        Element graphic = pointSymbolizer.getChild("Graphic");
        Element externalGraphic = graphic.getChild("ExternalGraphic");
        Element onlineResource = externalGraphic.getChild("OnlineResource");
        
        assertEquals("marker.png", onlineResource.getAttribute("href", XLINK).getValue());
        assertEquals("image/png", externalGraphic.getChild("Format").getText());
    }

    @Test
    public void testAddPointSymbolizerMark() throws Exception {
        JSONObject style = new JSONObject();
        style.put("fillColor", "#eee");
        style.put("fillOpacity", 0.3);
        style.put("strokeColor", "#fff");
        style.put("strokeOpacity", 0.2);
        style.put("strokeWidth", 2);
        style.put("strokeDashstyle", "5 2");
        style.put("strokeLinecap", "round");
        style.put("graphicOpacity", 0.4);
        style.put("pointRadius", 5);
        style.put("rotation", 90);
        
        Element rule = new Element("Rule");
        OldAPIStyleConverter.addPointSymbolizer(rule, new PJsonObject(style, null));
        
        Element pointSymbolizer = rule.getChild("PointSymbolizer");
        Element graphic = pointSymbolizer.getChild("Graphic");
        Element mark = graphic.getChild("Mark");
        assertEquals("circle", mark.getChild("WellKnownName").getText());
        
        Element fill = mark.getChild("Fill");
        List<Element> children = (List<Element>) fill.getChildren("CssParameter");
        assertEquals("#eee", children.get(0).getText());
        assertEquals("fill", children.get(0).getAttributeValue("name"));
        assertEquals("0.3", children.get(1).getText());
        assertEquals("fill-opacity", children.get(1).getAttributeValue("name"));
        
        Element stroke = mark.getChild("Stroke");
        children = (List<Element>) stroke.getChildren("CssParameter");
        assertEquals("#fff", children.get(0).getText());
        assertEquals("stroke", children.get(0).getAttributeValue("name"));
        assertEquals("0.2", children.get(1).getText());
        assertEquals("stroke-opacity", children.get(1).getAttributeValue("name"));
        assertEquals("2", children.get(2).getText());
        assertEquals("stroke-width", children.get(2).getAttributeValue("name"));
        assertEquals("5 2", children.get(3).getText());
        assertEquals("stroke-dasharray", children.get(3).getAttributeValue("name"));
        assertEquals("round", children.get(4).getText());
        assertEquals("stroke-linecap", children.get(4).getAttributeValue("name"));
        
        assertEquals("0.4", graphic.getChild("Opacity").getText());
        assertEquals("10.0", graphic.getChild("Size").getText());
        assertEquals("90", graphic.getChild("Rotation").getText());
    }

    @Test
    public void testAddLineSymbolizer() throws Exception {
        JSONObject style = new JSONObject();
        style.put("fillColor", "#eee");
        style.put("fillOpacity", 0.3);
        style.put("strokeColor", "#fff");
        style.put("strokeOpacity", 0.2);
        style.put("strokeWidth", 2);
        style.put("strokeDashstyle", "5 2");
        style.put("strokeLinecap", "round");
        
        Element rule = new Element("Rule");
        OldAPIStyleConverter.addPolygonSymbolizer(rule, new PJsonObject(style, null));
        
        Element polygonSymbolizer = rule.getChild("PolygonSymbolizer");
        Element fill = polygonSymbolizer.getChild("Fill");
        List<Element> children = (List<Element>) fill.getChildren("CssParameter");
        assertEquals("#eee", children.get(0).getText());
        assertEquals("fill", children.get(0).getAttributeValue("name"));
        assertEquals("0.3", children.get(1).getText());
        assertEquals("fill-opacity", children.get(1).getAttributeValue("name"));
        
        Element stroke = polygonSymbolizer.getChild("Stroke");
        children = (List<Element>) stroke.getChildren("CssParameter");
        assertEquals("#fff", children.get(0).getText());
        assertEquals("stroke", children.get(0).getAttributeValue("name"));
        assertEquals("0.2", children.get(1).getText());
        assertEquals("stroke-opacity", children.get(1).getAttributeValue("name"));
        assertEquals("2", children.get(2).getText());
        assertEquals("stroke-width", children.get(2).getAttributeValue("name"));
        assertEquals("5 2", children.get(3).getText());
        assertEquals("stroke-dasharray", children.get(3).getAttributeValue("name"));
        assertEquals("round", children.get(4).getText());
        assertEquals("stroke-linecap", children.get(4).getAttributeValue("name"));
    }

    @Test
    public void testAddPolygonSymbolizer() throws Exception {
        JSONObject style = new JSONObject();
        style.put("strokeColor", "#fff");
        style.put("strokeOpacity", 0.2);
        style.put("strokeWidth", 2);
        style.put("strokeDashstyle", "5 2");
        style.put("strokeLinecap", "round");
        
        Element rule = new Element("Rule");
        OldAPIStyleConverter.addLineSymbolizer(rule, new PJsonObject(style, null));
        
        Element lineSymbolizer = rule.getChild("LineSymbolizer");
        Element stroke = lineSymbolizer.getChild("Stroke");
        List<Element> children = (List<Element>) stroke.getChildren("CssParameter");
        assertEquals("#fff", children.get(0).getText());
        assertEquals("stroke", children.get(0).getAttributeValue("name"));
        assertEquals("0.2", children.get(1).getText());
        assertEquals("stroke-opacity", children.get(1).getAttributeValue("name"));
        assertEquals("2", children.get(2).getText());
        assertEquals("stroke-width", children.get(2).getAttributeValue("name"));
        assertEquals("5 2", children.get(3).getText());
        assertEquals("stroke-dasharray", children.get(3).getAttributeValue("name"));
        assertEquals("round", children.get(4).getText());
        assertEquals("stroke-linecap", children.get(4).getAttributeValue("name"));
    }

    @Test
    public void testAddTextSymbolizer() throws Exception {
        JSONObject style = new JSONObject();
        style.put("label", "${name}");
        style.put("fontColor", "#333333");
        style.put("fontStyle", "normal");
        style.put("fontFamily", "sans-serif");
        style.put("fontWeight", "bold");
        style.put("fontSize", "12px");
        style.put("labelXOffset", "-25");
        style.put("labelYOffset", "-25");
        style.put("labelAlign", "cm");
        style.put("haloColor", "#123456");
        style.put("haloOpacity", "0.7");
        style.put("haloRadius", "3");

        Element rule = new Element("Rule");
        OldAPIStyleConverter.addTextSymbolizer(rule, new PJsonObject(style, null));
        
        Element textSymbolizer = rule.getChild("TextSymbolizer");
        assertEquals("name", textSymbolizer.getChild("Label").getChild("PropertyName", OGC).getText());

        Element font = textSymbolizer.getChild("Font");
        List<Element> children = (List<Element>) font.getChildren("CssParameter");
        assertEquals("sans-serif", children.get(0).getText());
        assertEquals("font-family", children.get(0).getAttributeValue("name"));
        assertEquals("12px", children.get(1).getText());
        assertEquals("font-size", children.get(1).getAttributeValue("name"));
        assertEquals("bold", children.get(2).getText());
        assertEquals("font-weight", children.get(2).getAttributeValue("name"));
        assertEquals("normal", children.get(3).getText());
        assertEquals("font-style", children.get(3).getAttributeValue("name"));

        Element labelPlacement = textSymbolizer.getChild("LabelPlacement");
        
        Element pointPlacement = labelPlacement.getChild("PointPlacement");
        Element anchorPoint = pointPlacement.getChild("AnchorPoint");
        assertEquals("0.5", anchorPoint.getChild("AnchorPointX").getText());
        assertEquals("0.5", anchorPoint.getChild("AnchorPointY").getText());
        
        Element displacement = pointPlacement.getChild("Displacement");
        assertEquals("-25", displacement.getChild("DisplacementX").getText());
        assertEquals("-25", displacement.getChild("DisplacementY").getText());

        Element halo = textSymbolizer.getChild("Halo");
        assertEquals("3", halo.getChild("Radius").getText());
        Element haloFill = halo.getChild("Fill");
        children = (List<Element>) haloFill.getChildren("CssParameter");
        assertEquals("#123456", children.get(0).getText());
        assertEquals("fill", children.get(0).getAttributeValue("name"));
        assertEquals("0.7", children.get(1).getText());
        assertEquals("fill-opacity", children.get(1).getAttributeValue("name"));
        
        Element textFill = textSymbolizer.getChild("Fill");
        children = (List<Element>) textFill.getChildren("CssParameter");
        assertEquals("#333333", children.get(0).getText());
        assertEquals("fill", children.get(0).getAttributeValue("name"));
    }

    private PJsonObject loadLayerDataAsJson() throws IOException {
        return AbstractMapfishSpringTest.parseJSONObjectFromFile(OldAPIStyleConverterTest.class, "requestData-style-old-api.json");
    }
}
