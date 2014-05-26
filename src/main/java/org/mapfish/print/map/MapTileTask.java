/*
 * Copyright (C) 2013  Camptocamp
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

package org.mapfish.print.map;

import java.io.IOException;

import org.mapfish.print.RenderingContext;
import org.pvalsecc.concurrent.OrderedResultsExecutor;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfContentByte;

/**
 * Task for loading and rendering a tile.
 */
public abstract class MapTileTask implements OrderedResultsExecutor.Task<MapTileTask> {
    /**
     * When not null, we had an exception in the reading.
     */
    private Exception readException;

    public MapTileTask process() {
        try {
            readTile();
        } catch (Exception e) {
            readException = e;
        }
        return this;
    }

    public boolean handleException(RenderingContext context) {
        if (readException != null) {
            context.addError(readException);
            return true;
        }
        return false;
    }

    /**
     * Do the reading.
     * <p/>
     * Everything called from here must be thread safe!
     */
    protected abstract void readTile() throws IOException, DocumentException;

    /**
     * Do the rendering.
     */
    protected abstract void renderOnPdf(PdfContentByte dc) throws DocumentException;

    /**
     * Task for rending something (no loading needed)
     */
    public static abstract class RenderOnly extends MapTileTask {
        protected final void readTile() {
            //nothing to do
        }
    }
}
