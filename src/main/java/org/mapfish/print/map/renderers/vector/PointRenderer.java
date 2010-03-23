/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Server
 *
 * MapFish Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MapFish Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.map.renderers.vector;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Coordinate;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.BadElementException;
import com.lowagie.text.Image;
import com.lowagie.text.DocumentException;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.PDFUtils;
import org.mapfish.print.InvalidValueException;
import org.mapfish.print.config.ColorWrapper;
import org.mapfish.print.utils.PJsonObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.HashMap;

/**
 * Render point geometries. Support for the 3 OL stylings:
 * <ol>
 * <li>externalGraphic
 * <li>graphicName
 * <li>circle
 * </ol>
 */
public class PointRenderer extends GeometriesRenderer<Point> {

    private static final Map<String, float[]> SYMBOLS = new HashMap<String, float[]>();

    static {
        SYMBOLS.put("star", normalizeSymbol(new float[]{350, 75, 379, 161, 469, 161, 397, 215, 423, 301, 350, 250, 277, 301, 303, 215, 231, 161, 321, 161, 350, 75}));
        SYMBOLS.put("cross", normalizeSymbol(new float[]{4, 0, 6, 0, 6, 4, 10, 4, 10, 6, 6, 6, 6, 10, 4, 10, 4, 6, 0, 6, 0, 4, 4, 4, 4, 0}));
        SYMBOLS.put("x", normalizeSymbol(new float[]{0, 0, 25, 0, 50, 35, 75, 0, 100, 0, 65, 50, 100, 100, 75, 100, 50, 65, 25, 100, 0, 100, 35, 50, 0, 0}));
        SYMBOLS.put("square", normalizeSymbol(new float[]{0, 0, 0, 1, 1, 1, 1, 0, 0, 0}));
        SYMBOLS.put("triangle", normalizeSymbol(new float[]{0, 10, 10, 10, 5, 0, 0, 10}));
    }

    private static float[] normalizeSymbol(float[] coords) {
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int i = 0; i < coords.length; i += 2) {
            float x = coords[i];
            float y = coords[i + 1];
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        float width = maxX - minX;
        float height = maxY - minY;
        for (int i = 0; i < coords.length; i += 2) {
            coords[i] = (coords[i] - minX) / width;
            coords[i + 1] = (coords[i + 1] - minY) / height;
        }
        return coords;
    }

    protected void renderImpl(RenderingContext context, PdfContentByte dc, PJsonObject style, Point geometry) {
        PdfGState state = new PdfGState();
        final Coordinate coordinate = geometry.getCoordinate();
        float pointRadius = style.optFloat("pointRadius", 4.0f);
        final float f = context.getStyleFactor();

        String graphicName = style.optString("graphicName");
        float width = style.optFloat("graphicWidth", pointRadius * 2.0f);
        float height = style.optFloat("graphicHeight", pointRadius * 2.0f);
        float offsetX = style.optFloat("graphicXOffset", -width / 2.0f);
        float offsetY = style.optFloat("graphicYOffset", -height / 2.0f);
		// See Feature/Vector.js for more information about labels
		String label = style.optString("label");
		String labelAlign = style.optString("labelAlign", "lb");
		/*
		 * Valid values for horizontal alignment: "l"=left, "c"=center, "r"=right. 
		 * Valid values for vertical alignment: "t"=top, "m"=middle, "b"=bottom.
		 */
		float labelXOffset = style.optFloat("labelXOffset", (float) 0.0);
		float labelYOffset = style.optFloat("labelYOffset", (float) 0.0);
		String fontColor = style.optString("fontColor", "#000000");
		/* Supported itext fonts: COURIER, HELVETICA, TIMES_ROMAN */
		String fontFamily = style.optString("fontFamily", "HELVETICA");
		String fontSize = style.optString("fontSize", "12");
		String fontWeight = style.optString("fontWeight", "normal");

        if (style.optString("externalGraphic") != null) {
            float opacity = style.optFloat("graphicOpacity", style.optFloat("fillOpacity", 1.0f));
            state.setFillOpacity(opacity);
            state.setStrokeOpacity(opacity);
            dc.setGState(state);
            try {
                Image image = PDFUtils.createImage(context, width * f, height * f, new URI(style.getString("externalGraphic")), 0.0f);
                image.setAbsolutePosition((float) coordinate.x + offsetX * f, (float) coordinate.y + offsetY * f);
                dc.addImage(image);
            } catch (BadElementException e) {
                context.addError(e);
            } catch (URISyntaxException e) {
                context.addError(e);
            } catch (DocumentException e) {
                context.addError(e);
            }

        } else
        if (graphicName != null && !graphicName.equalsIgnoreCase("circle")) {
            PolygonRenderer.applyStyle(context, dc, style, state);
            float[] symbol = SYMBOLS.get(graphicName);
            if (symbol == null) {
                throw new InvalidValueException("graphicName", graphicName);
            }
            dc.setGState(state);
            dc.moveTo((float) coordinate.x + symbol[0] * width * f + offsetX * f, (float) coordinate.y + symbol[1] * height * f + offsetY * f);
            for (int i = 2; i < symbol.length - 2; i += 2) {
                dc.lineTo((float) coordinate.x + symbol[i] * width * f + offsetX * f, (float) coordinate.y + symbol[i + 1] * height * f + offsetY * f);

            }
            dc.closePath();
            dc.fillStroke();

        } else if (label != null && label.length() > 0) {
			BaseFont bf = PDFUtils.getBaseFont(fontFamily, fontSize, fontWeight);
			float fontHeight = (float) Double.parseDouble(fontSize.toLowerCase().replaceAll("px", "")) * f;
			dc.setFontAndSize(bf, fontHeight);
			dc.setColorFill(ColorWrapper.convertColor(fontColor));
			state.setFillOpacity((float) 1.0);
			dc.setGState(state);
			dc.beginText();
			dc.setTextMatrix((float) coordinate.x + labelXOffset * f, (float) coordinate.y + labelYOffset * f);
			dc.setGState(state);
			dc.showTextAligned(PDFUtils.getHorizontalAlignment(labelAlign), label, (float) coordinate.x + labelXOffset * f, (float) coordinate.y + labelYOffset * f - PDFUtils.getVerticalOffset(labelAlign, fontHeight), 0);
			dc.endText();
		} else {
            PolygonRenderer.applyStyle(context, dc, style, state);
            dc.setGState(state);

            dc.circle((float) coordinate.x, (float) coordinate.y, pointRadius * f);
            dc.fillStroke();
        }
    }
}
