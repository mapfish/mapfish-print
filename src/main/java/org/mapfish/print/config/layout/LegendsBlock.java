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

package org.mapfish.print.config.layout;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import org.apache.log4j.Logger;
import org.mapfish.print.PDFUtils;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.InvalidValueException;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;

import java.io.IOException;
import java.net.URI;

/**
 * Bean to configure a !legends block.
 * <p/>
 * See http://trac.mapfish.org/trac/mapfish/wiki/PrintModuleServer#Legendsblock
 */
public class LegendsBlock extends Block {
    public static final Logger LOGGER = Logger.getLogger(LegendsBlock.class);

    private double maxIconWidth = 0; // 0 mean disable
    private double maxIconHeight = 8; // 0 mean disable
    private float scale = 0; // 0 mean disable
    private boolean inline = true;
    private double classIndentation = 20;
    private float layerSpace = 5;
    private float classSpace = 2;

    private String layerFont = "Helvetica";
    protected double layerFontSize = 10;
    private String classFont = "Helvetica";
    protected double classFontSize = 8;
    private String fontEncoding = BaseFont.WINANSI;

    public void render(PJsonObject params, PdfElement target, RenderingContext context) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100f);

        Font layerPdfFont = getLayerPdfFont();
        Font classPdfFont = getClassPdfFont();

        PJsonArray legends = context.getGlobalParams().optJSONArray("legends");
        if (legends != null && legends.size() > 0) {
            for (int i = 0; i < legends.size(); ++i) {
                PJsonObject layer = legends.getJSONObject(i);
                createLine(context, 0.0, layer, layerPdfFont, params, table, i == 0 ? layerSpace : 0);

                PJsonArray classes = layer.getJSONArray("classes");
                for (int j = 0; j < classes.size(); ++j) {
                    PJsonObject clazz = classes.getJSONObject(j);
                    createLine(context, classIndentation, clazz, classPdfFont, params, table, classSpace);
                }
            }
        }
        table.setSpacingAfter((float) spacingAfter);
        target.add(table);
    }

    private void createLine(RenderingContext context, double indent, PJsonObject node, Font pdfFont, PJsonObject params,
            PdfPTable table, float lineSpace) throws DocumentException {
        final String name = node.getString("name");
        final String icon = node.optString("icon");
        final PJsonArray icons = node.optJSONArray("icons");

        Paragraph result = new Paragraph();
        if (icon != null) {
            try {
                if (icon.indexOf("image%2Fsvg%2Bxml") != -1) { // TODO: make this cleaner
                    result.add(PDFUtils.createImageChunkFromSVG(context, icon, maxIconWidth, maxIconHeight));
                } else {
                    result.add(PDFUtils.createImageChunk(context, maxIconWidth, maxIconHeight, URI.create(icon), 0f));
                }
                if (!inline) {
                    addCell(context, indent, params, table, lineSpace, result);
                    result = new Paragraph();
                    result.setFont(pdfFont);
                }
                else {
                    result.add(" ");
                }
            } catch (IOException ioe) {
                LOGGER.warn("Failed to load " + icon + " with " + ioe.getMessage());
            } catch (InvalidValueException e) {
                LOGGER.warn("Failed to create image chunk: " + e.getMessage());
            }
        }
        if (icons != null) {
            for (int i = 0; i < icons.size(); ++i) {
                String iconItem = icons.getString(i);
                try {
                    if (iconItem.indexOf("image%2Fsvg%2Bxml") != -1) { // TODO: make this cleaner
                        result.add(PDFUtils.createImageChunkFromSVG(context, iconItem, maxIconWidth, maxIconHeight));
                    } else {
                        result.add(PDFUtils.createImageChunk(context, maxIconWidth, maxIconHeight, scale, URI.create(iconItem), 0f));
                    }
                    if (!inline) {
                        addCell(context, indent, params, table, 0, result);
                        result = new Paragraph();
                        result.setFont(pdfFont);
                    }
                    else {
                        result.add(" ");
                    }
                } catch (IOException ioe) {
                    LOGGER.warn("Failed to load " + iconItem + " with " + ioe.getMessage());
                } catch (InvalidValueException e) {
                    LOGGER.warn("Failed to create image chunk: " + e.getMessage());
                }
            }
        }
        result.add(name);

        final PdfPCell cell = new PdfPCell(result);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPadding(0f);
        cell.setPaddingLeft((float) indent);

        result.setFont(pdfFont);
        result.add(name);

        if (getBackgroundColorVal(context, params) != null) {
            cell.setBackgroundColor(getBackgroundColorVal(context, params));
        }
        table.addCell(cell);
    }

    private void addCell(RenderingContext context, double indent, PJsonObject params, PdfPTable table,
            float lineSpace, final Paragraph result) {
        final PdfPCell cell = new PdfPCell(result);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPadding(0f);
        cell.setPaddingLeft((float) indent);

        if (getBackgroundColorVal(context, params) != null) {
            cell.setBackgroundColor(getBackgroundColorVal(context, params));
        }

        cell.setPaddingTop(lineSpace);
        table.addCell(cell);
    }

    public void setDefaultScale(double scale) {
        this.scale = (float)scale;
        if (scale < 0.0) throw new InvalidValueException("scale", scale);
    }

    public void setInline(String inline) {
        this.inline = "true".equalsIgnoreCase(inline);
        if (!inline.equalsIgnoreCase("true") || !inline.equalsIgnoreCase("false")) throw new InvalidValueException("inline", inline);
    }
    
    public void setMaxIconWidth(double maxIconWidth) {
        this.maxIconWidth = maxIconWidth;
        if (maxIconWidth < 0.0) throw new InvalidValueException("maxIconWidth", maxIconWidth);
    }

    public void setMaxIconHeight(double maxIconHeight) {
        this.maxIconHeight = maxIconHeight;
        if (maxIconHeight < 0.0) throw new InvalidValueException("maxIconHeight", maxIconHeight);
    }

    public void setClassIndentation(double classIndentation) {
        this.classIndentation = classIndentation;
        if (classIndentation < 0.0) throw new InvalidValueException("classIndentation", classIndentation);
    }

    public void setClassFont(String classFont) {
        this.classFont = classFont;
    }

    public void setClassFontSize(double classFontSize) {
        this.classFontSize = classFontSize;
        if (classFontSize < 0.0) throw new InvalidValueException("classFontSize", classFontSize);
    }

    public String getClassFont() {
        return classFont;
    }

    protected Font getLayerPdfFont() {
        return FontFactory.getFont(layerFont, fontEncoding, (float) layerFontSize);
    }

    protected Font getClassPdfFont() {
        return FontFactory.getFont(classFont, fontEncoding, (float) classFontSize);
    }

    public void setLayerSpace(double layerSpace) {
        this.layerSpace = (float)layerSpace;
        if (layerSpace < 0.0) throw new InvalidValueException("layerSpace", layerSpace);
    }

    public void setClassSpace(double classSpace) {
        this.classSpace = (float)classSpace;
        if (classSpace < 0.0) throw new InvalidValueException("classSpace", classSpace);
    }

    public void setLayerFont(String layerFont) {
        this.layerFont = layerFont;
    }

    public void setLayerFontSize(double layerFontSize) {
        this.layerFontSize = layerFontSize;
        if (layerFontSize < 0.0) throw new InvalidValueException("layerFontSize", layerFontSize);
    }

    public void setFontEncoding(String fontEncoding) {
        this.fontEncoding = fontEncoding;
    }
}
