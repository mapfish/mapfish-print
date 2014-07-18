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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Converts style definitions of the old API, which uses the OpenLayers 2 format,
 * to SLD styles.
 *
 * The code is based on the OpenLayers 2 SLD writer:
 * https://github.com/openlayers/openlayers/blob/master/lib/OpenLayers/Format/SLD/v1.js
 */
public final class OldAPIStyleConverter {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(OldAPIStyleConverter.class);
    
    private static final String OLD_STYLES_KEY = "styles";
    private static final String OLD_STYLEPROPERTY_KEY = "styleProperty";
    
    private static final Namespace XSI = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    private static final Namespace OGC = Namespace.getNamespace("ogc", "http://www.opengis.net/ogc");
    private static final Namespace XLINK = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");

    private OldAPIStyleConverter() { }
    
    /**
     * Converts the style definition of a vector layer of the old API
     * to a SLD style string.
     *
     * @param oldVectorLayer A layer definition of the old API.
     * @return A SLD string.
     */
    public static String convert(final PJsonObject oldVectorLayer) {
        PJsonObject oldStyles = oldVectorLayer.optJSONObject(OLD_STYLES_KEY);
        if (oldStyles == null || oldStyles.size() == 0) {
            return "";
        }

        String styleProperty = oldVectorLayer.optString(OLD_STYLEPROPERTY_KEY, "_style");
        List<Element> styleRules = getStyleRules(oldStyles, styleProperty);
        
        return generateSld(styleRules);
    }

    /**
     * Creates SLD rules for each old style.
     * 
     * @param oldStyles     The old styles.
     * @param styleProperty The style property (e.g. "_gx_style").
     */
    @VisibleForTesting
    protected static List<Element> getStyleRules(final PJsonObject oldStyles, final String styleProperty) {
        final List<Element> styleRules = new LinkedList<Element>();
        
        for (Iterator<String> iterator = oldStyles.keys(); iterator.hasNext();) {
            String styleKey = iterator.next();
            PJsonObject oldStyle = oldStyles.getJSONObject(styleKey);
            styleRules.add(getStyleRule(styleKey, oldStyle, styleProperty));
        }
        
        return styleRules;
    }

    private static Element getStyleRule(final String styleKey, final PJsonObject oldStyle, final String styleProperty) {
        Element rule = new Element("Rule");
        newElement("name", rule).setText(styleKey);

        if (!Strings.isNullOrEmpty(styleProperty)) {
            addFilter(rule, styleKey, styleProperty);
        }
        
        addPointSymbolizer(rule, oldStyle);
        addLineSymbolizer(rule, oldStyle);
        addPolygonSymbolizer(rule, oldStyle);
        addTextSymbolizer(rule, oldStyle);
        
        return rule;
    }

    private static void addFilter(final Element rule, final String styleKey, final String styleProperty) {
        Element filter = newElement("Filter", rule);
        Element propertyIsEqualTo = newElement("PropertyIsEqualTo", filter);
        newElement("PropertyName", propertyIsEqualTo).setText(styleProperty);
        newElement("Literal", propertyIsEqualTo).setText(styleKey);
    }

    /**
     * Add a point symbolizer definition to the rule.
     * @param rule      The SLD rule.
     * @param oldStyle  The old style.
     */
    @VisibleForTesting
    protected static void addPointSymbolizer(final Element rule, final PJsonObject oldStyle) {
        Element pointSymbolizer = newElement("PointSymbolizer", rule);

        Element graphic = newElement("Graphic", pointSymbolizer);
        if (!Strings.isNullOrEmpty(oldStyle.optString("externalGraphic"))) {
            String externalGraphicFile = oldStyle.getString("externalGraphic");
            Element externalGraphic = newElement("ExternalGraphic", graphic);
            
            Element onlineResource = newElement("OnlineResource", externalGraphic);
            onlineResource.setAttribute("type", "simple", XLINK);
            onlineResource.setAttribute("href", externalGraphicFile, XLINK);

            newElement("Format", externalGraphic).setText(getGraphicFormat(externalGraphicFile, oldStyle));
        } else {
            Element mark = newElement("Mark", graphic);
            String graphicName = oldStyle.optString("graphicName", "circle");
            newElement("WellKnownName", mark).setText(graphicName);
            
            if (oldStyle.optBool("fill", true)) {
                addFill(mark, oldStyle);
            }
            if (oldStyle.optBool("stroke", true)) {
                addStroke(mark, oldStyle);
            }
        }

        if (!Strings.isNullOrEmpty(oldStyle.optString("graphicOpacity"))) {
            newElement("Opacity", graphic).setText(oldStyle.getString("graphicOpacity"));
        }

        if (!Strings.isNullOrEmpty(oldStyle.optString("pointRadius"))) {
            double size = oldStyle.getDouble("pointRadius") * 2;
            newElement("Size", graphic).setText(Double.toString(size));
        } else if (!Strings.isNullOrEmpty(oldStyle.optString("graphicWidth"))) {
            newElement("Size", graphic).setText(oldStyle.getString("graphicWidth"));
        }

        if (!Strings.isNullOrEmpty(oldStyle.optString("rotation"))) {
            newElement("Rotation", graphic).setText(oldStyle.getString("rotation"));
        }
    }

    /**
     * Add a line symbolizer definition to the rule.
     * @param rule      The SLD rule.
     * @param oldStyle  The old style.
     */
    @VisibleForTesting
    protected static void addLineSymbolizer(final Element rule, final PJsonObject oldStyle) {
        Element lineSymbolizer = newElement("LineSymbolizer", rule);
        addStroke(lineSymbolizer, oldStyle);
    }

    /**
     * Add a polygon symbolizer definition to the rule.
     * @param rule      The SLD rule.
     * @param oldStyle  The old style.
     */
    @VisibleForTesting
    protected static void addPolygonSymbolizer(final Element rule, final PJsonObject oldStyle) {
        Element polygonSymbolizer = newElement("PolygonSymbolizer", rule);

        if (oldStyle.optBool("fill", true)) {
            addFill(polygonSymbolizer, oldStyle);
        }
        if (oldStyle.optBool("stroke", true)) {
            addStroke(polygonSymbolizer, oldStyle);
        }
    }

    /**
     * Add a text symbolizer definition to the rule.
     * @param rule      The SLD rule.
     * @param oldStyle  The old style.
     */
    @VisibleForTesting
    protected static void addTextSymbolizer(final Element rule, final PJsonObject oldStyle) {
        Element textSymbolizer = newElement("TextSymbolizer", rule);

        if (!Strings.isNullOrEmpty(oldStyle.optString("label"))) {
            // note: only simple labels are supported (e.g. "Name: ${name}" does not work)
            Element label = newElement("Label", textSymbolizer);
            String labelValue = oldStyle.getString("label").replace("${", "").replace("}", "");
            newElement("PropertyName", label).setNamespace(OGC).setText(labelValue);
        }

        if (!Strings.isNullOrEmpty(oldStyle.optString("fontFamily")) ||
            !Strings.isNullOrEmpty(oldStyle.optString("fontSize")) ||
            !Strings.isNullOrEmpty(oldStyle.optString("fontWeight")) ||
            !Strings.isNullOrEmpty(oldStyle.optString("fontStyle"))) {
            addFont(textSymbolizer, oldStyle);
        }

        if (!Strings.isNullOrEmpty(oldStyle.optString("labelAnchorPointX")) ||
            !Strings.isNullOrEmpty(oldStyle.optString("labelAnchorPointY")) ||
            !Strings.isNullOrEmpty(oldStyle.optString("labelAlign")) ||
            !Strings.isNullOrEmpty(oldStyle.optString("labelXOffset")) ||
            !Strings.isNullOrEmpty(oldStyle.optString("labelYOffset")) ||
            !Strings.isNullOrEmpty(oldStyle.optString("labelRotation")) ||
            !Strings.isNullOrEmpty(oldStyle.optString("labelPerpendicularOffset"))) {
            addLabelPlacement(textSymbolizer, oldStyle);
        }

        if (!Strings.isNullOrEmpty(oldStyle.optString("haloRadius")) ||
            !Strings.isNullOrEmpty(oldStyle.optString("haloColor")) ||
            !Strings.isNullOrEmpty(oldStyle.optString("haloOpacity"))) {
            addHalo(textSymbolizer, oldStyle);
        }

        if (!Strings.isNullOrEmpty(oldStyle.optString("fontColor")) ||
            !Strings.isNullOrEmpty(oldStyle.optString("fontOpacity"))) {
            addFill(textSymbolizer, oldStyle.optString("fontColor"), oldStyle.optString("fontOpacity"));
        }
        
    }

    private static void addFont(final Element parent, final PJsonObject oldStyle) {
        Element font = newElement("Font", parent);
        
        if (!Strings.isNullOrEmpty(oldStyle.optString("fontFamily"))) {
            newElement("CssParameter", font)
                .setAttribute("name", "font-family")
                .setText(oldStyle.getString("fontFamily"));
        }
        
        if (!Strings.isNullOrEmpty(oldStyle.optString("fontSize"))) {
            newElement("CssParameter", font)
                .setAttribute("name", "font-size")
                .setText(oldStyle.getString("fontSize"));
        }
        
        if (!Strings.isNullOrEmpty(oldStyle.optString("fontWeight"))) {
            newElement("CssParameter", font)
                .setAttribute("name", "font-weight")
                .setText(oldStyle.getString("fontWeight"));
        }
        
        if (!Strings.isNullOrEmpty(oldStyle.optString("fontStyle"))) {
            newElement("CssParameter", font)
                .setAttribute("name", "font-style")
                .setText(oldStyle.getString("fontStyle"));
        }
    }

    private static void addLabelPlacement(final Element parent, final PJsonObject oldStyle) {
        Element labelPlacement = newElement("LabelPlacement", parent);
        
        // PointPlacement and LinePlacement are choices, so don't output both
        if ((!Strings.isNullOrEmpty(oldStyle.optString("labelAnchorPointX")) ||
                !Strings.isNullOrEmpty(oldStyle.optString("labelAnchorPointY")) ||
                !Strings.isNullOrEmpty(oldStyle.optString("labelAlign")) ||
                !Strings.isNullOrEmpty(oldStyle.optString("labelXOffset")) ||
                !Strings.isNullOrEmpty(oldStyle.optString("labelYOffset")) ||
                !Strings.isNullOrEmpty(oldStyle.optString("labelRotation"))) 
                && Strings.isNullOrEmpty(oldStyle.optString("labelPerpendicularOffset"))) {
            addPointPlacement(labelPlacement, oldStyle);
        }
        
        if (!Strings.isNullOrEmpty(oldStyle.optString("labelPerpendicularOffset"))) {
            addLinePlacement(labelPlacement, oldStyle);
        }
    }

    private static void addLinePlacement(final Element labelPlacement, final PJsonObject oldStyle) {
        Element linePlacement = newElement("LinePlacement", labelPlacement);
        newElement("PerpendicularOffset", linePlacement).setText(oldStyle.getString("labelPerpendicularOffset"));
    }

    private static void addPointPlacement(final Element labelPlacement, final PJsonObject oldStyle) {
        Element pointPlacement = newElement("PointPlacement", labelPlacement);
        
        if (!Strings.isNullOrEmpty(oldStyle.optString("labelAnchorPointX"))
                || !Strings.isNullOrEmpty(oldStyle.optString("labelAnchorPointY"))
                || !Strings.isNullOrEmpty(oldStyle.optString("labelAlign"))) {
            addAnchorPoint(pointPlacement, oldStyle);
        }
        
        if (!Strings.isNullOrEmpty(oldStyle.optString("labelXOffset"))
                || !Strings.isNullOrEmpty(oldStyle.optString("labelYOffset"))) {
            Element displacement = newElement("Displacement", pointPlacement);

            if (!Strings.isNullOrEmpty(oldStyle.optString("labelXOffset"))) {
                newElement("DisplacementX", displacement).setText(oldStyle.getString("labelXOffset"));
            }
            if (!Strings.isNullOrEmpty(oldStyle.optString("labelYOffset"))) {
                newElement("DisplacementY", displacement).setText(oldStyle.getString("labelYOffset"));
            }
        }
        
        if (!Strings.isNullOrEmpty(oldStyle.optString("labelRotation"))) {
            newElement("Rotation", pointPlacement).setText(oldStyle.getString("labelRotation"));
        }
        
    }

    private static void addAnchorPoint(final Element pointPlacement, final PJsonObject oldStyle) {
        Element anchorPoint = newElement("AnchorPoint", pointPlacement);
        
        String x = oldStyle.optString("labelAnchorPointX");
        String y = oldStyle.optString("labelAnchorPointY");
        
        if (!Strings.isNullOrEmpty(x)) {
            newElement("AnchorPointX", anchorPoint).setText(x);
        }
        if (!Strings.isNullOrEmpty(y)) {
            newElement("AnchorPointY", anchorPoint).setText(y);
        }
        if (Strings.isNullOrEmpty(x) && Strings.isNullOrEmpty(y)) {
            String labelAlign = oldStyle.getString("labelAlign");
            String xAlign = labelAlign.substring(0, 1);
            String yAlign = labelAlign.substring(1, 2);
            
            if ("l".equals(xAlign)) {
                x = "0";
            } else if ("c".equals(xAlign)) {
                x = "0.5";
            } else if ("r".equals(xAlign)) {
                x = "1";
            }
            if ("b".equals(yAlign)) {
                y = "0";
            } else if ("m".equals(yAlign)) {
                y = "0.5";
            } else if ("t".equals(yAlign)) {
                y = "1";
            }
            newElement("AnchorPointX", anchorPoint).setText(x);
            newElement("AnchorPointY", anchorPoint).setText(y);
        }
    }

    private static void addHalo(final Element textSymbolizer, final PJsonObject oldStyle) {
        Element halo = newElement("Halo", textSymbolizer);
        
        if (!Strings.isNullOrEmpty(oldStyle.optString("haloRadius"))) {
            newElement("Radius", halo).setText(oldStyle.getString("haloRadius"));
        }

        if (!Strings.isNullOrEmpty(oldStyle.optString("haloColor")) ||
            !Strings.isNullOrEmpty(oldStyle.optString("haloOpacity"))) {
            addFill(halo, oldStyle.optString("haloColor"), oldStyle.optString("haloOpacity"));
        }
    }

    private static void addFill(final Element parent, final PJsonObject oldStyle) {
        addFill(parent, oldStyle.optString("fillColor"), oldStyle.optString("fillOpacity"));
    }
    
    private static void addFill(final Element parent, final String fillColor, final String fillOpacity) {
        Element fill = newElement("Fill", parent);
        
        if (!Strings.isNullOrEmpty(fillColor)) {
            newElement("CssParameter", fill)
                .setAttribute("name", "fill")
                .setText(fillColor);
        }
        
        if (!Strings.isNullOrEmpty(fillOpacity)) {
            newElement("CssParameter", fill)
                .setAttribute("name", "fill-opacity")
                .setText(fillOpacity);
        }
    }

    private static void addStroke(final Element parent, final PJsonObject oldStyle) {
        Element stroke = newElement("Stroke", parent);
        
        if (!Strings.isNullOrEmpty(oldStyle.optString("strokeColor"))) {
            newElement("CssParameter", stroke)
                .setAttribute("name", "stroke")
                .setText(oldStyle.getString("strokeColor"));
        }
        
        if (!Strings.isNullOrEmpty(oldStyle.optString("strokeOpacity"))) {
            newElement("CssParameter", stroke)
                .setAttribute("name", "stroke-opacity")
                .setText(oldStyle.getString("strokeOpacity"));
        }
        
        if (!Strings.isNullOrEmpty(oldStyle.optString("strokeWidth"))) {
            newElement("CssParameter", stroke)
                .setAttribute("name", "stroke-width")
                .setText(oldStyle.getString("strokeWidth"));
        }
        
        if (!Strings.isNullOrEmpty(oldStyle.optString("strokeDashstyle")) &&
                !"solid".equals(oldStyle.getString("strokeDashstyle"))) {
            newElement("CssParameter", stroke)
                .setAttribute("name", "stroke-dasharray")
                .setText(oldStyle.getString("strokeDashstyle"));
        }
        
        if (!Strings.isNullOrEmpty(oldStyle.optString("strokeLinecap"))) {
            newElement("CssParameter", stroke)
                .setAttribute("name", "stroke-linecap")
                .setText(oldStyle.getString("strokeLinecap"));
        }
    }

    private static String getGraphicFormat(final String externalGraphicFile, final PJsonObject oldStyle) {
        if (!Strings.isNullOrEmpty(oldStyle.optString("graphicFormat"))) {
            return oldStyle.getString("graphicFormat");
        } else {
            int seperatorPos = externalGraphicFile.lastIndexOf(".");
            
            if (seperatorPos >= 0) {
                return "image/" + externalGraphicFile.substring(seperatorPos + 1);
            } else {
                return "";
            }
        }
    }

    private static String generateSld(final List<Element> styleRules) {
        Namespace xmlns = Namespace.getNamespace("http://www.opengis.net/sld");
        Element styledLayerDescriptor = new Element("StyledLayerDescriptor", xmlns);
        Document doc = new Document(styledLayerDescriptor);
        doc.setRootElement(styledLayerDescriptor);
        
        styledLayerDescriptor.setAttribute("version", "1.0.0");
        styledLayerDescriptor.addNamespaceDeclaration(XSI);
        styledLayerDescriptor.setAttribute("schemaLocation", "http://www.opengis.net/sld StyledLayerDescriptor.xsd", XSI);
        styledLayerDescriptor.addNamespaceDeclaration(OGC);
        styledLayerDescriptor.addNamespaceDeclaration(XLINK);
        
        Element namedLayer = new Element("NamedLayer");
        styledLayerDescriptor.addContent(namedLayer);

        Element userStyle = new Element("UserStyle");
        namedLayer.addContent(userStyle);

        Element featureTypeStyle = new Element("FeatureTypeStyle");
        userStyle.addContent(featureTypeStyle);
        
        for (Element styleRule : styleRules) {
            featureTypeStyle.addContent(styleRule);
        }
        
        try {
            StringWriter writer = new StringWriter();
            new XMLOutputter().output(doc, writer);
            return writer.toString();
        } catch (IOException e) {
            LOGGER.warn("Error writing style sld", e);
        }
        
        return "";
    }

    private static Element newElement(final String name, final Element parent) {
        Element newElement = new Element(name);
        parent.addContent(newElement);
        return newElement;
    }
}
