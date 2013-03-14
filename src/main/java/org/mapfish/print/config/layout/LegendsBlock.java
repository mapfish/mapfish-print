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

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import org.apache.log4j.Logger;
import org.mapfish.print.InvalidValueException;
import org.mapfish.print.PDFUtils;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.legend.LegendItemTable;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;

/**
 * Bean to configure a !legends block.
 * <p/>
 * See http://trac.mapfish.org/trac/mapfish/wiki/PrintModuleServer#Legendsblock
 */
public class LegendsBlock extends Block {
    public static final Logger LOGGER = Logger.getLogger(LegendsBlock.class);
    private static String tempDir = System.getProperty("java.io.tmpdir");
    private static String fileSeparator = System.getProperty("file.separator");

    private double maxHeight = 0; // 0 mean multi column disable
    private double maxWidth = 0;

    private double maxIconWidth = 0; // 0 mean disable
    private double maxIconHeight = 8; // 0 mean disable
    private float scale = 0; // 0 mean disable
    private boolean inline = true;
    private double classIndentation = 20;
    private float layerSpace = 5;
    private float classSpace = 2;

    private float iconPadding[] = {0f,0f,0f,0f};
    private float textPadding[] = {0f,0f,0f,0f};

    private String layerFont = "Helvetica";
    protected double layerFontSize = 10;
    private String classFont = "Helvetica";
    protected double classFontSize = 8;
    private String fontEncoding = BaseFont.WINANSI;
    
    private double columnMargin = 3;
    private int legendItemHorizontalAlignment = Element.ALIGN_CENTER;
    
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
        /* 
         * need these to calculate widths/heights of output
         * 
         * For example a cell could contain the text "Hello World"
         * while it is the equivalent of 7 characters wide "World" would 
         * be wrapped onto the next line which would make the height 
         * calculation complicated if not actually rendered onto a page.
         * This is important for long legend texts which wrap.
         */
        private String tempFilename;
        private Document tempDocument = new Document();
        private PdfWriter writer;

        private PJsonObject params;
        private RenderingContext context;
        
        // all the pdf columns
        private ArrayList<PdfPTable> columns = new ArrayList<PdfPTable>();
        // all the columns width
        private ArrayList<Float> columnsWidth = new ArrayList<Float>();
        // the current cell width
        private float cellWidth = 0;
        // the current column
        private PdfPTable column;
        // the curent title
        private PdfPCell title;
        // the current cell height
        private double currentCellHeight = 0;
        // the current column height 
        private double currentColumnHeight = 0;
        private int currentColumnIndex = 0;
        private float maxActualImageWidth = 0;
        private float maxActualTextWidth = 0;
        private ArrayList<LegendItemTable> legendItems = new ArrayList<LegendItemTable>();

        /**
         * Construct
         * @param params the params
         * @param context the context
         */
        public Renderer(PJsonObject params, RenderingContext context) throws DocumentException {
            column = getDefaultOuterTable(1);
            columns.add(column);
            currentCellHeight = 0;
            this.params = params;
            this.context = context;
            makeTempDocument(); // need this to calculate widths and heights of elements!
        }
        
        public void render(PdfElement target) throws DocumentException {
            float optimumIconCellWidth = 0f;
            float optimumTextWidth = 0f;
            float optimumTextWidthWithoutIcon = 0f;
            int numColumns = 1;

            // create the legend
            PJsonArray legends = context.getGlobalParams().optJSONArray("legends");

            //column.getDefaultCell().setBorder(PdfPCell.NO_BORDER);
            if (legends != null && legends.size() > 0) {
                for (int i = 0; i < legends.size(); ++i) {
                    /*
                    PJsonObject layer = legends.getJSONObject(i);
                    createLine(0.0, layer, layerPdfFont, i == 0 ? 0 : layerSpace, true, true);
    
                    PJsonArray classes = layer.getJSONArray("classes");
                    for (int j = 0; j < classes.size(); ++j) {
                        PJsonObject clazz = classes.getJSONObject(j);
                        createLine(classIndentation, clazz, classPdfFont, classSpace, false, false);
                    }
                    */
                    createLegend(legends.getJSONObject(i), i == 0);
                }
                optimumIconCellWidth = Math.min(maxActualImageWidth, (float) maxIconWidth);
                optimumTextWidth = Math.min(maxActualTextWidth, (float) maxWidth - optimumIconCellWidth);
                optimumTextWidthWithoutIcon = Math.min(maxActualTextWidth, (float) maxWidth);
                
                float totalHeight = 0f;
                for (LegendItemTable legendItem : legendItems) {
                    /**
                     * need the padding set before in createLegend
                     * and add it to the optimum absolute widths
                     */
                    PdfPCell cells[] =  legendItem.getRow(0).getCells();
                    int numCells = cells.length;
                    PdfPCell leftCell = cells[0];
                    PdfPCell rightCell = null;
                    if (numCells > 1) {
                        rightCell = cells[1];
                    }
                    float absoluteWidths[] = null;
                    if (numCells == 1) {
                        absoluteWidths = new float[1];
                    } else {
                        absoluteWidths = new float[2];
                    }
                    if (legendItem.isIconBeforeName()) {
                        if (numCells == 1) {
                            absoluteWidths[0] = optimumTextWidthWithoutIcon + 
                                    leftCell.getPaddingLeft() +
                                    leftCell.getPaddingRight();
                        } else {
                            absoluteWidths[0] = optimumIconCellWidth + 
                                    leftCell.getPaddingLeft() +
                                    leftCell.getPaddingRight();
                            absoluteWidths[1] = optimumTextWidth + 
                                    rightCell.getPaddingLeft() +
                                    rightCell.getPaddingRight();
                        }
                    } else {
                        if (numCells == 1) {
                            absoluteWidths[0] = optimumTextWidthWithoutIcon +
                                    leftCell.getPaddingLeft() +
                                    leftCell.getPaddingRight();
                                    
                        } else {
                            absoluteWidths[0] = optimumTextWidth + 
                                    rightCell.getPaddingLeft() +
                                    rightCell.getPaddingRight();
                            absoluteWidths[1] = optimumIconCellWidth + 
                                    leftCell.getPaddingLeft() +
                                    leftCell.getPaddingRight();
                        }
                    }
                    if (numCells == 1) {
                        //float optimumLegendWidth = absoluteWidths[0] + absoluteWidths[1];
                        //absoluteWidths = new float[1];
                        absoluteWidths[0] = optimumTextWidthWithoutIcon;
                    }
                    legendItem.setTotalWidth(absoluteWidths);
                    legendItem.setLockedWidth(true);
                    legendItem.setHorizontalAlignment(legendItemHorizontalAlignment);

                    totalHeight += getHeight(legendItem);
                    if (totalHeight <= maxHeight) {
                        column.addCell(legendItem);
                    } else {
                        column = getDefaultOuterTable(1);
                        columns.add(column);
                        totalHeight = 0f;
                    }
                }
                //column.setTotalWidth(optimumIconCellWidth+optimumTextWidth);
                //column.setLockedWidth(true);
                column.setHorizontalAlignment(legendItemHorizontalAlignment);
                //column.getDefaultCell().setBorder(El);
            }

            numColumns = columns.size();

            PdfPTable table = getDefaultOuterTable(numColumns);

            for (PdfPTable col : columns) {
                table.addCell(col);
            }
            target.add(table);
            
            /*
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

            target.add(column);
            */
            
            /*
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
            tempDocument.add(table);
            System.out.println("table width = "+ table.getTotalWidth());
            */
            cleanup(); // don't forget to cleanup afterwards
        }
        
        /**
         * Render
         * @param target the target element
         * @throws DocumentException
         */
        public void render_old(PdfElement target) throws DocumentException {

            Font layerPdfFont = getLayerPdfFont();
            Font classPdfFont = getClassPdfFont();

            // create the legend
            PJsonArray legends = context.getGlobalParams().optJSONArray("legends");
            if (legends != null && legends.size() > 0) {
                for (int i = 0; i < legends.size(); ++i) {
                    PJsonObject layer = legends.getJSONObject(i);
                    createLine(0.0, layer, layerPdfFont, i == 0 ? 0 : layerSpace, true, true);
    
                    PJsonArray classes = layer.getJSONArray("classes");
                    for (int j = 0; j < classes.size(); ++j) {
                        PJsonObject clazz = classes.getJSONObject(j);
                        createLine(classIndentation, clazz, classPdfFont, classSpace, false, false);
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
                float lineSpace, boolean escapeOrphanTitle, 
                boolean defaultIconBeforeName) throws DocumentException {
            final String name = node.getString("name");
            final String icon = node.optString("icon");
            final PJsonArray icons = node.optJSONArray("icons");
    
            /*
            Paragraph result = new Paragraph();
            boolean iconBeforeName = node.optBool("iconBeforeName", defaultIconBeforeName);
            if(iconBeforeName) {
                result = addIconToParagraph(indent, lineSpace, icon, icons, result);
                addName(pdfFont, escapeOrphanTitle, name, result);
                addCell(indent, lineSpace, result);
                result = new Paragraph();
                addTitleSeparator(indent, lineSpace, result);
            } else {
                addName(pdfFont, escapeOrphanTitle, name, result);
                addCell(indent, lineSpace, result);
                result = new Paragraph();
                result = addIconToParagraph(indent, lineSpace, icon, icons, result);
                addTitleSeparator(indent, lineSpace, result);
            }
    
            if (!escapeOrphanTitle) {
                column.addCell(title);
                title = null;
            }
            */
            
            /***** NEW CODE *****/
            //PdfPTable legendItemTable = new PdfPTable(2);
            /*
            if (columnsWidth.size() <= currentColumnIndex) {
                columnsWidth.add();
            }
            */
            Phrase imagePhrase = new Phrase();
            //imagePhrase.setFont(pdfFont);
            Chunk iconChunk = null;
            if (icon != null) {
                iconChunk = createImageChunk(context, icon, maxIconWidth, maxIconHeight);
            } else {
                iconChunk = new Chunk(""); 
            }
            imagePhrase.add(iconChunk);
            
            Phrase namePhrase = new Phrase();
            namePhrase.setFont(pdfFont);
            namePhrase.add(name);
            
            //columns.add(column);
            float height = imagePhrase.getFont().getSize();

            float textWidth = getTextWidth(name, pdfFont);
            float imageWidth = icon == null ? 0f : iconChunk.getImage().getPlainWidth();
            
            int columnsWidthSize = columnsWidth.size();
            float maxWidthF = textWidth + imageWidth; // total with of legend item
            if (columnsWidthSize <= currentColumnIndex) {// need to add
                columnsWidth.add(Math.min((float) maxWidth, maxWidthF));
            } else if (columnsWidthSize >= 1 && currentColumnIndex == 0) {
                // need to get the min of max
                maxWidthF = Math.max(columnsWidth.get(0), maxWidthF);
                columnsWidth.set(0, Math.min((float) maxWidth, maxWidthF));
            }
            currentCellHeight += Math.max(height, 1);
            currentColumnHeight += currentCellHeight;
            currentCellHeight = 0;
            

            //maxIconWidth = Math.min(imageWidth, maxIconWidth);
            
            float relativeWidths[] = {(float) maxIconWidth, (float) maxWidth};
            PdfPTable pdfPTable = new PdfPTable(relativeWidths);
            pdfPTable.setWidthPercentage(100f);
            //pdfPTable.getDefaultCell().setBorder(PdfPCell.NO_BORDER);
            pdfPTable.getDefaultCell().setPadding(0f);
            pdfPTable.setSpacingAfter((float) spacingAfter);
            pdfPTable.addCell(imagePhrase);
            pdfPTable.addCell(namePhrase);

            PdfPCell cell = new PdfPCell(pdfPTable);
            cell.setPaddingTop(lineSpace);
            column.addCell(pdfPTable);
            System.out.println("HEIGHT "+ pdfPTable.getRow(0).getCells()[0].getHeight());

                //tempDocument.add(legendItemTable);
            System.out.println("TOT HEIGHT "+ getHeight(pdfPTable));
            pdfPTable.getRow(0).getCells()[0].setPhrase(new Phrase("overwrite"));
                    //currentCellHeight += iconChunk.getImage().getPlainHeight() + lineSpace;
            /*
            if (escapeOrphanTitle) {
                currentCellHeight += pdfFont.getSize();
                cellWidth = Math.max(cellWidth, width);
            }
            else {
                currentColumnHeight += pdfFont.getSize();
                int index = columnsWidth.size() - 1;
                columnsWidth.set(index, Math.max(columnsWidth.get(index), width));
            }
            */
        }

        private float getTextWidth(String myString, Font pdfFont) {
            BaseFont baseFont = pdfFont.getBaseFont();
            float width = baseFont == null ? 
                    new Chunk(myString).getWidthPoint() : 
                    baseFont.getWidthPoint(myString, pdfFont.getSize());
            return width;
        }
        private void createLine_old(double indent, PJsonObject node, Font pdfFont,
                float lineSpace, boolean escapeOrphanTitle, boolean defaultIconBeforeName) throws DocumentException {
            final String name = node.getString("name");
            final String icon = node.optString("icon");
            final PJsonArray icons = node.optJSONArray("icons");
    
            Paragraph result = new Paragraph();
            boolean iconBeforeName = node.optBool("iconBeforeName", defaultIconBeforeName);
            if(iconBeforeName) {
                result = addIconToParagraph(indent, lineSpace, icon, icons, result);
                addName(pdfFont, escapeOrphanTitle, name, result);
                addCell(indent, lineSpace, result);
                result = new Paragraph();
                addTitleSeparator(indent, lineSpace, result);
            } else {
                addName(pdfFont, escapeOrphanTitle, name, result);
                addCell(indent, lineSpace, result);
                result = new Paragraph();
                result = addIconToParagraph(indent, lineSpace, icon, icons, result);
                addTitleSeparator(indent, lineSpace, result);
            }
    
    
            if (!escapeOrphanTitle) {
                column.addCell(title);
                title = null;
            }
            
        }

        private void addName(Font pdfFont, boolean escapeOrphanTitle, final String name, Paragraph result) {
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
    
            if (title != null && getBackgroundColorVal(context, params) != null) {
                title.setBackgroundColor(getBackgroundColorVal(context, params));
            }
        }

        private void addTitleSeparator(double indent, float lineSpace, Paragraph result) {
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
        }

        private Paragraph addIconToParagraph(double indent, float lineSpace, final String icon, final PJsonArray icons, Paragraph result)
                throws DocumentException {
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
            return result;
        }

        /**
         * Create a chunk from an image (svg, png, ...)
         * @param context PDF rendering context
         * @param iconItem URL of the image
         * @param maxIconWidth width of the chunk
         * @param maxIconHeight height of the chunk
         * @return Chunk with image in it
         * @throws DocumentException 
         */
        private Chunk createImageChunk(RenderingContext context, 
                String iconItem, 
                double maxIconWidth, 
                double maxIconHeight) throws DocumentException {
            Chunk iconChunk = null;
            try {
                if (iconItem.indexOf("image%2Fsvg%2Bxml") != -1) { // TODO: make this cleaner
                    iconChunk = PDFUtils.createImageChunkFromSVG(context, iconItem, maxIconWidth, maxIconHeight);
                } else {
                    iconChunk = PDFUtils.createImageChunk(context, maxIconWidth, maxIconHeight, scale, 
                            URI.create(iconItem), 0f);
                }
            } catch (IOException e) {
                throw new DocumentException(e);
            }
            return iconChunk;
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
                } else {
                    currentCellHeight = Math.max(iconChunk.getImage().getPlainHeight() + lineSpace, currentCellHeight);
                    cellWidth = Math.max(cellWidth, iconChunk.getImage().getPlainWidth());
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
            //if (!inline && maxHeight != 0 && (currentColumnHeight + currentCellHeight > maxHeight)) { //}
            if (maxHeight != 0 && (currentColumnHeight + currentCellHeight > maxHeight)) {
                column = new PdfPTable(1);
                column.setWidthPercentage(100f);
                columns.add(column);
                currentColumnHeight = 0;
                //currentCellHeight = 0;
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

        private void makeTempDocument() throws DocumentException {
            try {
                tempFilename = tempDir.indexOf('/') != -1 ? "" : "\\";
                long time = (new Date()).getTime();
                tempFilename = tempDir + fileSeparator +
                        "mapfish-print-tmp-"+ time +".pdf";
                // Unfortunately have to open an actual file on disk
                // for the calculations to work properly
                writer = PdfWriter.getInstance(tempDocument, 
                        new FileOutputStream(tempFilename));
                tempDocument.open();
            } catch (FileNotFoundException e) {
                throw new DocumentException(e);
            } catch (DocumentException e) {
                // don't forget to delete the useless file
                new File(tempFilename).delete();
                throw new DocumentException(e);
            }
        }

        private void cleanup() throws DocumentException {
            try {
                tempDocument.close();
                writer.close();
                // don't forget to delete the useless file
            } catch (Exception e) {
                throw new DocumentException(e);
            } finally {
                new File(tempFilename).delete();
            }
        }

        private float getHeight(Element element) throws DocumentException {
            tempDocument.add(element);
            if (element instanceof PdfPTable) {
                return ((PdfPTable) element).getTotalHeight();
            }
            if (element instanceof PdfPCell) {
                return ((PdfPCell) element).getHeight();
            }
            return -1;
        }

        private float createLegend(PJsonObject layer, boolean isFirst) throws DocumentException {
            Font layerPdfFont = getLayerPdfFont();
            Font classPdfFont = getClassPdfFont();
            //float space = isFirst ? 0 : layerSpace;
            //PJsonObject layer = jsonObject;
            float height = createTableLine(0.0, layer, layerPdfFont, layerSpace, true, true);
            PJsonArray classes = layer.getJSONArray("classes");
            for (int j = 0; j < classes.size(); ++j) {
                PJsonObject clazz = classes.getJSONObject(j);
                height += createTableLine(classIndentation, clazz, classPdfFont, classSpace, false, false);
            }
            return height;
        }

        private float createTableLine(double indent, PJsonObject node, Font pdfFont,
                float lineSpace, boolean isFirst, 
                boolean defaultIconBeforeName) throws DocumentException {
            final String name = node.getString("name");
            final String icon = node.optString("icon");
            final PJsonArray icons = node.optJSONArray("icons");
            boolean iconBeforeName = node.optBool("iconBeforeName", defaultIconBeforeName);
    
            /*
            Paragraph result = new Paragraph();
            boolean iconBeforeName = node.optBool("iconBeforeName", defaultIconBeforeName);
            if(iconBeforeName) {
                result = addIconToParagraph(indent, lineSpace, icon, icons, result);
                addName(pdfFont, escapeOrphanTitle, name, result);
                addCell(indent, lineSpace, result);
                result = new Paragraph();
                addTitleSeparator(indent, lineSpace, result);
            } else {
                addName(pdfFont, escapeOrphanTitle, name, result);
                addCell(indent, lineSpace, result);
                result = new Paragraph();
                result = addIconToParagraph(indent, lineSpace, icon, icons, result);
                addTitleSeparator(indent, lineSpace, result);
            }
    
            if (!escapeOrphanTitle) {
                column.addCell(title);
                title = null;
            }
            */
            
            /***** NEW CODE *****/
            //PdfPTable legendItemTable = new PdfPTable(2);
            /*
            if (columnsWidth.size() <= currentColumnIndex) {
                columnsWidth.add();
            }
            */
            Phrase imagePhrase = new Phrase();
            Chunk iconChunk = null;
            if (icon != null) {
                iconChunk = createImageChunk(context, icon, maxIconWidth, maxIconHeight);
            } else {
                iconChunk = new Chunk(""); 
            }
            imagePhrase.add(iconChunk);
            
            Phrase namePhrase = new Phrase();
            namePhrase.setFont(pdfFont);
            namePhrase.add(name);
            
            //columns.add(column);
            //float height = imagePhrase.getFont().getSize();

            float textWidth = getTextWidth(name, pdfFont);
            float imageWidth = icon == null ? 0f : iconChunk.getImage().getPlainWidth();
            
            int columnsWidthSize = columnsWidth.size();
            float maxWidthF = textWidth + imageWidth; // total with of legend item
            if (columnsWidthSize <= currentColumnIndex) {// need to add
                columnsWidth.add(Math.min((float) maxWidth, maxWidthF));
            } else if (columnsWidthSize >= 1 && currentColumnIndex == 0) {
                // need to get the min of max
                maxWidthF = Math.max(columnsWidth.get(0), maxWidthF);
                columnsWidth.set(0, Math.min((float) maxWidth, maxWidthF));
            }
            //currentCellHeight += Math.max(height, 1);
            currentColumnHeight += currentCellHeight;
            currentCellHeight = 0;
            

            //maxIconWidth = Math.min(imageWidth, maxIconWidth);
            
            float absoluteWidths[] = null;
            //PdfPTable legendItemTable = new PdfPTable(absoluteWidths);
            //PdfPTable legendItemTable = new PdfPTable(2);
            LegendItemTable legendItemTable = null;
            if (icon == null) {
                legendItemTable = new LegendItemTable(1);
                absoluteWidths = new float[1];
                absoluteWidths[0] = (float) maxWidth;
            } else {
                legendItemTable = new LegendItemTable(2);
                absoluteWidths = new float[2];
                absoluteWidths[0] = (float) maxIconWidth;
                absoluteWidths[1] = (float) (maxWidth - maxIconWidth);
            }
            //legendItemTable.setWidthPercentage(100f);
            legendItemTable.setIconBeforeName(iconBeforeName);
            legendItemTable.setTotalWidth(absoluteWidths);
            //legendItemTable.getDefaultCell().setBorder(PdfPCell.NO_BORDER);
            legendItemTable.getDefaultCell().setPadding(0f);
            //legendItemTable.setSpacingAfter((float) spacingAfter);
            //legendItemTable.setSpacingBefore((float) indent);
            PdfPCell imageCell = null;
            if (icon != null) {
                imageCell = new PdfPCell(imagePhrase);
                /**
                 * CSS like padding for icons:
                 * not to forget indent!
                 */
                imageCell.setPaddingTop(iconPadding[0]);
                imageCell.setPaddingRight(iconPadding[1]);
                imageCell.setPaddingBottom(lineSpace + iconPadding[2]);
                imageCell.setPaddingLeft((float) indent + iconPadding[3]);

                imageCell.setBorder(PdfPCell.NO_BORDER);

            }
            PdfPCell nameCell = new PdfPCell(namePhrase);

            /**
             * CSS like padding for text
             * not to forget spacing!
             */
            nameCell.setPaddingTop(0f);

            nameCell.setPaddingTop(textPadding[0]);
            nameCell.setPaddingRight((float) spacingAfter + textPadding[1]);
            nameCell.setPaddingBottom(lineSpace + textPadding[2]);
            nameCell.setPaddingLeft(textPadding[3]);
            
            nameCell.setBorder(PdfPCell.NO_BORDER);
            
            if (inline) {
                if (iconBeforeName) {
                    if (imageCell != null) {
                        legendItemTable.addCell(imageCell);
                    }
                    legendItemTable.addCell(nameCell);
                } else {
                    legendItemTable.addCell(nameCell);
                    if (imageCell != null) {
                        legendItemTable.addCell(imageCell);
                    }
                }
            } else {
                legendItemTable = new LegendItemTable(1);
                if (iconBeforeName) {
                    if (imageCell != null) {
                        legendItemTable.addCell(imageCell);
                    }
                    legendItemTable.addCell(nameCell);
                } else {
                    legendItemTable.addCell(nameCell);
                    if (imageCell != null) {
                        legendItemTable.addCell(imageCell);
                    }
                }
            }

            //PdfPCell cell = new PdfPCell(legendItemTable);
            //cell.setPaddingTop(lineSpace);
            //column.addCell(legendItemTable);
            //System.out.println("HEIGHT "+ legendItemTable.getRow(0).getCells()[0].getHeight());

                //tempDocument.add(legendItemTable);
            //System.out.println("TOT HEIGHT "+ getHeight(legendItemTable));
            //float mywidths[] = {imageWidth,textWidth};
            //System.out.println("widths: "+ imageWidth +" "+ textWidth);
            maxActualImageWidth = Math.max(imageWidth, maxActualImageWidth);
            maxActualTextWidth = Math.max(textWidth, maxActualTextWidth);
            //pdfPTable.getRow(0).getCells()[0].setPhrase(new Phrase("overwrite"));
                    //currentCellHeight += iconChunk.getImage().getPlainHeight() + lineSpace;
            /*
            if (escapeOrphanTitle) {
                currentCellHeight += pdfFont.getSize();
                cellWidth = Math.max(cellWidth, width);
            }
            else {
                currentColumnHeight += pdfFont.getSize();
                int index = columnsWidth.size() - 1;
                columnsWidth.set(index, Math.max(columnsWidth.get(index), width));
            }
            */
            legendItems.add(legendItemTable);
            return getHeight(legendItemTable);
        }

        private PdfPTable getDefaultOuterTable(int numColumns) {
            ///column = new PdfPTable(1);
            PdfPTable pdfPTable = new PdfPTable(numColumns);
            pdfPTable.getDefaultCell().setBorder(PdfPCell.NO_BORDER);
            pdfPTable.setWidthPercentage(100f);
            pdfPTable.getDefaultCell().setPadding(0f);
            return pdfPTable;
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

    /**
     * set the horizontal alignment of legend items inside the table
     * and the table itself
     * @param value left|center|right
     */
    public void setHorizontalAlignment(String value) {
        if (value.equalsIgnoreCase("left")) {
            this.legendItemHorizontalAlignment = Element.ALIGN_LEFT;
        } else if (value.equalsIgnoreCase("right")) {
            this.legendItemHorizontalAlignment = Element.ALIGN_RIGHT;
        }
    }

    public void setIconPadding(String values) {
        this.iconPadding = getFloatCssValues(values);
    }

    public void setTextPadding(String values) {
        this.textPadding = getFloatCssValues(values);
    }

    /**
     * get CSS like values for padding
     * @param values space separated floating point values
     * @return css padding like array of floats
     */
    private float[] getFloatCssValues(String values) {
        float result[] = {0f,0f,0f,0f};
        String topRightBottomLeft[] = values.split(" ");
        int len = topRightBottomLeft.length > 4 ? 4 : topRightBottomLeft.length;
        switch (len) {
            default:
            case 1:
                for (int i = 0; i < 4; ++i) {
                    result[i] = Float.parseFloat(topRightBottomLeft[0]);
                }
                break;
            case 2:
                result[0] = result[2] = Float.parseFloat(topRightBottomLeft[0]);
                result[1] = result[3] = Float.parseFloat(topRightBottomLeft[1]);
                break;
            case 3:
                result[0] = Float.parseFloat(topRightBottomLeft[0]);
                result[1] = result[3] = Float.parseFloat(topRightBottomLeft[1]);
                result[2] = Float.parseFloat(topRightBottomLeft[3]);
                break;
            case 4:
                for (int i = 0; i < len; ++i) {
                    float val = Float.parseFloat(topRightBottomLeft[i]);
                    result[i] = val;
                }
                break;
        }
        return result;
    }
}
