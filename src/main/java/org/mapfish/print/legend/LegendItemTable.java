/*
 * Copyright Landcare Research, New Zealand
 * Author: Tim-Hinnerk Heuer.
 */

package org.mapfish.print.legend;

import com.lowagie.text.pdf.PdfPTable;

/**
 *
 * @author Tim-Hinnerk Heuer, Landcare Research - New Zealand
 */
public class LegendItemTable extends PdfPTable {
    private boolean iconBeforeName = false;
    private float spaceBefore = 0f;

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
    
}
