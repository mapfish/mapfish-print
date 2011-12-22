/*
 * Copyright (C) 2009  Camptocamp
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

package org.mapfish.print.map.renderers.vector;

import org.mapfish.print.PDFUtils;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.config.ColorWrapper;
import org.mapfish.print.utils.PJsonObject;

import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;

public class LabelRenderer {

	public static final Logger LOGGER = Logger.getLogger(LabelRenderer.class);

	static void applyStyle(RenderingContext context, PdfContentByte dc,
			PJsonObject style, Geometry geometry) {
		/*
		 * See Feature/Vector.js for more information about labels
		 */
		String label = style.optString("label");

		if (label != null && label.length() > 0) {
			/*
			 * Valid values for horizontal alignment: "l"=left, "c"=center,
			 * "r"=right. Valid values for vertical alignment: "t"=top,
			 * "m"=middle, "b"=bottom.
			 */
			String labelAlign = style.optString("labelAlign", "cm");
			float labelXOffset = style.optFloat("labelXOffset", (float) 0.0);
			float labelYOffset = style.optFloat("labelYOffset", (float) 0.0);
			String fontColor = style.optString("fontColor", "#000000");
			/* Supported itext fonts: COURIER, HELVETICA, TIMES_ROMAN */
			String fontFamily = style.optString("fontFamily", "HELVETICA");
			if (!"COURIER".equalsIgnoreCase(fontFamily)
					|| !"HELVETICA".equalsIgnoreCase(fontFamily)
					|| !"TIMES_ROMAN".equalsIgnoreCase(fontFamily)) {

				LOGGER.info("Font: '"+ fontFamily +
						"' not supported, supported fonts are 'HELVETICA', " +
						"'COURIER', 'TIMES_ROMAN', defaults to 'HELVETICA'");
				fontFamily = "HELVETICA";
			}
			String fontSize = style.optString("fontSize", "12");
			String fontWeight = style.optString("fontWeight", "normal");
			Point center = geometry.getCentroid();
			float f = context.getStyleFactor();
			BaseFont bf = PDFUtils
					.getBaseFont(fontFamily, fontSize, fontWeight);
			float fontHeight = (float) Double.parseDouble(fontSize
					.toLowerCase().replaceAll("px", "")) * f;
			dc.setFontAndSize(bf, fontHeight);
			dc.setColorFill(ColorWrapper.convertColor(fontColor));
			dc.beginText();
			dc.setTextMatrix((float) center.getX() + labelXOffset * f,
					(float) center.getY() + labelYOffset * f);
			dc.showTextAligned(
					PDFUtils.getHorizontalAlignment(labelAlign),
					label,
					(float) center.getX() + labelXOffset * f,
					(float) center.getY()
							+ labelYOffset
							* f
							- PDFUtils
									.getVerticalOffset(labelAlign, fontHeight),
					0);
			dc.endText();
		}
	}
}
