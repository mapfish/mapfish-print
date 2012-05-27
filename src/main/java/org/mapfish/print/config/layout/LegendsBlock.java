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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.mapfish.print.InvalidValueException;
import org.mapfish.print.PDFUtils;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;

import com.lowagie.text.Chunk;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

/**
 * Bean to configure a !legends block.
 * <p/>
 * See http://trac.mapfish.org/trac/mapfish/wiki/PrintModuleServer#Legendsblock
 */
public class LegendsBlock extends Block {
    public static final Logger LOGGER = Logger.getLogger(LegendsBlock.class);

    private double maxHeight = 0; // 0 mean multi column disable
    private double maxWidth = 0;

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
    
    private double columnMargin = 3;
    
    /**
     * Render the legends block
     * @see org.mapfish.print.config.layout.Block#render(org.mapfish.print.utils.PJsonObject, org.mapfish.print.config.layout.Block.PdfElement, org.mapfish.print.RenderingContext)
     */
    public void render(PJsonObject params, PdfElement target, RenderingContext context) throws DocumentException {
        Renderer renderer = new Renderer(params, context);
        renderer.render(target);
    }
    
    /**
     * A renderer to render the legend block
     * @author Stéphane Brunner
     */
    private class Renderer {
        private PJsonObject params;
        private RenderingContext context;
        
        // all the pdf columns
        private ArrayList<PdfPTable> columns = new ArrayList<PdfPTable>();
        // all the columns width
        private ArrayList<Float> columnsWidth = new ArrayList<Float>();
        // the current cell width
        private float cellWidth = 0;
        // the current column
        private PdfPTable column = new PdfPTable(1);
        // the curent title
        private PdfPCell title;
        // the current cell height
        private double currentCellHeight = 0;
        // the current column height 
        private double currentColumnHeight = 0;

        /**
         * Construct
         * @param params the params
         * @param context the context
         */
        public Renderer(PJsonObject params, RenderingContext context) {
            column.setWidthPercentage(100f);
            columns.add(column);
            currentCellHeight = 0;
            columnsWidth.add(0f);
            this.params = params;
            this.context = context;
        }
        
        /**
         * Render
         * @param target the target element
         * @throws DocumentException
         */
        public void render(PdfElement target) throws DocumentException {

            Font layerPdfFont = getLayerPdfFont();
            Font classPdfFont = getClassPdfFont();

            // create the legend
            PJsonArray legends = context.getGlobalParams().optJSONArray("legends");
            if (legends != null && legends.size() > 0) {
                for (int i = 0; i < legends.size(); ++i) {
                    PJsonObject layer = legends.getJSONObject(i);
                    createLine(0.0, layer, layerPdfFont, i == 0 ? 0 : layerSpace, true);
    
                    PJsonArray classes = layer.getJSONArray("classes");
                    for (int j = 0; j < classes.size(); ++j) {
                        PJsonObject clazz = classes.getJSONObject(j);
                        createLine(classIndentation, clazz, classPdfFont, classSpace, false);
                    }
                }
            }
            
            // complete the width of the last column
            { 
                int index = columnsWidth.size() - 1;
                columnsWidth.set(index, Math.max(columnsWidth.get(index), cellWidth));
            }
            if (title != null) {
                column.addCell(title);
            }
            
            // calculate the fullWidth (sum of width of visible column with margin)
            int len = columns.size();
            float fullWidth = 0;
            for (int i = 0 ; i < len ; i++) {
                float width = columnsWidth.get(i);
                if (fullWidth + width < maxWidth) {
                    fullWidth += width + columnMargin;
                }
                else {
                    len = i + 1;
                }
            }
            
            // create the column with array for the table
            float[] pdfWidths = new float[len + 1];
            for (int i = 0 ; i < len ; i++) {
                pdfWidths[i] = columnsWidth.get(i);
            }
            // for empty column
            pdfWidths[len] = Math.max(0f, (float)maxWidth - fullWidth);
            
            // table used for column
            PdfPTable table = new PdfPTable(pdfWidths);
            table.setWidthPercentage(100f);
            table.getDefaultCell().setBorder(PdfPCell.NO_BORDER);
            table.getDefaultCell().setPadding(0f);
            table.setSpacingAfter((float) spacingAfter);
    
            // add columns
            for (int i = 0 ; i < len ; i++) {
                table.addCell(columns.get(i));
            }
            // create empty column
            table.addCell("");
            
            // add to result 
            target.add(table);
        }
    
        /**
         * Display a legend or class block (name and icon(s) ).
         * In not inline mode it's a line. 
         * @param indent left indentation
         * @param node the json node
         * @param pdfFont the font used for the title
         * @param lineSpace the top indent for the icons
         * @param escapeOrphanTitle don't have orphan title
         * @throws DocumentException
         */
        private void createLine(double indent, PJsonObject node, Font pdfFont,
                float lineSpace, boolean escapeOrphanTitle) throws DocumentException {
            final String name = node.getString("name");
            final String icon = node.optString("icon");
            final PJsonArray icons = node.optJSONArray("icons");
    
            Paragraph result = new Paragraph();
            if (icon != null) {
                result = createIcon(indent, lineSpace, icon, result);
            }
            if (icons != null) {
                for (int i = 0; i < icons.size(); ++i) {
                    String iconItem = icons.getString(i);
                    result = createIcon(indent, i == 0 ? lineSpace : 0,
                            iconItem, result);
                }
            }
    
            if (title != null) {
                column.addCell(title);
            }
            title = new PdfPCell(result);
            title.setBorder(PdfPCell.NO_BORDER);
            title.setPadding(0f);
            title.setPaddingLeft((float)indent);
            if (inline) {
                title.setPaddingTop(lineSpace);
            }
    
            result.setFont(pdfFont);
            result.add(name);
            if (name.trim().length() > 0) {
                BaseFont baseFont = pdfFont.getBaseFont();
                float width = baseFont == null ? pdfFont.getSize() : baseFont.getWidthPoint(name, pdfFont.getSize());
                if (escapeOrphanTitle) {
                    currentCellHeight += pdfFont.getSize();
                    cellWidth = Math.max(cellWidth, width);
                }
                else {
                    currentColumnHeight += pdfFont.getSize();
                    int index = columnsWidth.size() - 1;
                    columnsWidth.set(index, Math.max(columnsWidth.get(index), width));
                }
            }
    
            if (getBackgroundColorVal(context, params) != null) {
                title.setBackgroundColor(getBackgroundColorVal(context, params));
            }
            if (!escapeOrphanTitle) {
                column.addCell(title);
                title = null;
            }
        }

        /**
         * Creates a legend icon
         * @param indent left indentation
         * @param lineSpace the top indent for the icons
         * @param icon the icon to display
         * @param result the element to add to
         * @return the result for the next elements
         * @throws DocumentException
         */
        private Paragraph createIcon(double indent, float lineSpace,
                final String icon, Paragraph result) throws DocumentException {
            try {
                Chunk iconChunk = null;
                if (icon.indexOf("image%2Fsvg%2Bxml") != -1) { // TODO: make this cleaner
                    iconChunk = PDFUtils.createImageChunkFromSVG(context, icon, maxIconWidth, maxIconHeight);
                } else {
                    iconChunk = PDFUtils.createImageChunk(context, maxIconWidth, maxIconHeight, scale, 
                            URI.create(icon), 0f);
                }
                result.add(iconChunk);
                if (!inline) {
                    currentCellHeight += iconChunk.getImage().getPlainHeight() + lineSpace;
                    cellWidth = Math.max(cellWidth, iconChunk.getImage().getPlainWidth());
                    addCell(indent, lineSpace, result);
                    result = new Paragraph();
                }
                else {
                    result.add(" ");
                }
            } catch (IOException ioe) {
                LOGGER.warn("Failed to load " + icon + " with " + ioe.getMessage());
            } catch (InvalidValueException e) {
                LOGGER.warn("Failed to create image chunk: " + e.getMessage());
            }
            return result;
        }

        /**
         * Add an icon in the column as a cell.
         * @param indent left indentation
         * @param lineSpace the top indent for the icons
         * @param icon the icon element to add
         */
        private void addCell(double indent, float lineSpace, final Paragraph icon) {
            final PdfPCell cell = new PdfPCell(icon);
            cell.setBorder(PdfPCell.NO_BORDER);
            cell.setPadding(0f);
            cell.setPaddingLeft((float) indent);
    
            if (getBackgroundColorVal(context, params) != null) {
                cell.setBackgroundColor(getBackgroundColorVal(context, params));
            }
    
            cell.setPaddingTop(lineSpace);
            currentCellHeight += lineSpace;
            if (!inline && maxHeight != 0 && (currentColumnHeight + currentCellHeight > maxHeight)) {
                column = new PdfPTable(1);
                column.setWidthPercentage(100f);
                columns.add(column);
                currentColumnHeight = 0;
                columnsWidth.add(cellWidth);
            }
            else {
                int index = columnsWidth.size() - 1;
                columnsWidth.set(index, Math.max(columnsWidth.get(index), cellWidth));
            }
            cellWidth = 0;
            currentColumnHeight += currentCellHeight;
            currentCellHeight = 0;
            if (title != null) {
                column.addCell(title);
                title = null;
            }
            column.addCell(cell);
        }
    }

    public void setMaxWidth(double maxWidth) {
        this.maxWidth = maxWidth;
        if (maxWidth < 0.0) throw new InvalidValueException("maxWidth", maxWidth);
    }

    public void setMaxHeight(double maxHeight) {
        this.maxHeight = maxHeight;
        if (maxHeight < 0.0) throw new InvalidValueException("maxHeight", maxHeight);
    }

    public void setDefaultScale(double scale) {
        this.scale = (float)scale;
        if (scale < 0.0) throw new InvalidValueException("scale", scale);
    }

    public void setInline(String inline) {
        this.inline = "true".equalsIgnoreCase(inline);
        if (!(inline.equalsIgnoreCase("true") || inline.equalsIgnoreCase("false"))) throw new InvalidValueException("inline", inline);
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
    
    public void setColumnMargin(double columnMargin) {
        this.columnMargin = columnMargin;
    }
}
