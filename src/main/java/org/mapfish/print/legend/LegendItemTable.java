/*
 * Copyright Landcare Research, New Zealand
 * Author: Tim-Hinnerk Heuer.
 */
package org.mapfish.print.legend;

import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import org.mapfish.print.utils.PJsonObject;

/**
 *
 * @author Tim-Hinnerk Heuer, Landcare Research - New Zealand
 */
public class LegendItemTable extends PdfPTable {
    private boolean iconBeforeName = false;
    private float spaceBefore = 0f;
    private boolean isFirst = false; // whether it is the first
    private boolean isHeading = false; // whether it is the heading
    private boolean newColumn = false;
    private PdfPCell imageCell = null;
    private PdfPCell nameCell = null;

    /**
     * @return the params
     */
    public Params getParams() {
        return params;
    }

    /**
     * @param params the params to set
     */
    public void setParams(Params params) {
        this.params = params;
    }

    /**
     * PARAMS
     */
    public class Params {

        public float indent;
        public PJsonObject node;
        public Font pdfFont;
        public float lineSpace;
        public boolean defaultIconBeforeName;
        public float spaceBefore;
        public boolean heading;
    }
    private Params params = new Params();

    public LegendItemTable() {
        super(1);
    }
    public LegendItemTable(int numberOfColumns) {
        super(numberOfColumns);
    }
    /**
     * @return the iconBeforeName
     */
    public boolean isIconBeforeName() {
        return iconBeforeName;
    }

    /**
     * @param iconBeforeName the iconBeforeName to set
     */
    public void setIconBeforeName(boolean iconBeforeName) {
        this.iconBeforeName = iconBeforeName;
    }

    /**
     * @return the spaceBefore
     */
    public float getSpaceBefore() {
        return spaceBefore;
    }

    /**
     * @param spaceBefore the spaceBefore to set
     */
    public void setSpaceBefore(float spaceBefore) {
        this.spaceBefore = spaceBefore;
    }

    public void setHeading(boolean heading) {
        this.isHeading = heading;
    }

    /**
     * /PARAMS
     */
    public boolean isHeading() {
        return isHeading;
    }

    /**
     * @param isFirst the isFirst to set
     */
    public void setIsFirst(boolean isFirst) {
        this.isFirst = isFirst;
    }

    /**
     * @return the newColumn
     */
    public boolean isNewColumn() {
        return newColumn;
    }

    /**
     * @param newColumn the newColumn to set
     */
    public void setNewColumn(boolean newColumn) {
        this.newColumn = newColumn;
    }

    /**
     * @return the iconCell
     */
    public PdfPCell getImageCell() {
        return imageCell;
    }

    /**
     * @param imageCell the iconCell to set
     */
    public void setImageCell(PdfPCell imageCell) {
        this.imageCell = imageCell;
    }

    /**
     * @return the nameCell
     */
    public PdfPCell getNameCell() {
        return nameCell;
    }

    /**
     * @param nameCell the nameCell to set
     */
    public void setNameCell(PdfPCell nameCell) {
        this.nameCell = nameCell;
    }

    /**
     * @return the isFirst
     */
    public boolean isFirst() {
        return isFirst;
    }

    /**
     * Setting the params for later use
     *
     * @param indent
     * @param node
     * @param pdfFont
     * @param lineSpace
     * @param defaultIconBeforeName
     * @param spaceBefore
     * @param heading
     */
    public void setParams(float indent, PJsonObject node, Font pdfFont, float lineSpace, boolean defaultIconBeforeName, float spaceBefore, boolean heading) {
        params.indent = indent;
        params.node = node;
        params.pdfFont = pdfFont;
        params.lineSpace = lineSpace;
        params.defaultIconBeforeName = defaultIconBeforeName;
        params.spaceBefore = spaceBefore;
        params.heading = heading;
    }

}
