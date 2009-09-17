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

package org.mapfish.print.map;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfContentByte;
import org.mapfish.print.RenderingContext;
import org.pvalsecc.concurrent.BlockingSimpleTarget;
import org.pvalsecc.concurrent.OrderedResultsExecutor;

/**
 * An instance of this class is in charge of loading in parallel the tiles of a
 * single !map block.
 * <p/>
 * Since a lot of stuff is done in //, a lot of care has to be put on protecting
 * the shared resources. One of the big ones is the DirectContent (dc) or the
 * PDFWriter. For those, a lock on context.getPdfLock() is used.
 * <p/>
 * This class uses a global {@link org.pvalsecc.concurrent.OrderedResultsExecutor} to
 * do the things in // and a {@link org.pvalsecc.concurrent.BlockingSimpleTarget} to
 * know when everything is finished.
 */
public class ParallelMapTileLoader implements OrderedResultsExecutor.ResultCollector<MapTileTask> {
    private final PdfContentByte dc;
    private RenderingContext context;

    /**
     * Reference on the global executor to use.
     */
    private final OrderedResultsExecutor<MapTileTask> executor;

    /**
     * Target used to know when all the tiles are read and rendered.
     */
    private final BlockingSimpleTarget target = new BlockingSimpleTarget("mapTiles");

    /**
     * Number of tiles scheduled
     */
    private int nbTiles = 0;

    public ParallelMapTileLoader(RenderingContext context, PdfContentByte dc) {
        executor = context.getConfig().getMapRenderingExecutor();
        this.dc = dc;
        this.context = context;
    }

    /**
     * Schedule a tile to be loaded and rendered using the given task.
     */
    public void addTileToLoad(MapTileTask task) {
        nbTiles++;
        if (executor != null) {
            executor.addTask(task, this);
        } else {
            //no parallel loading... do it right away
            task.process();
            handle(task);
        }
    }

    /**
     * Wait for all the tiles to be loaded and rendered.
     */
    public void waitForCompletion() {
        target.setTarget(nbTiles);
        target.waitForCompletion();
    }

    /**
     * Called each time a result is available, in the order the tiles were
     * scheduled to be loaded. For one PDF file, not called in //.
     */
    public void handle(MapTileTask mapTileTaskResult) {
        if (!mapTileTaskResult.handleException(context)) {
            synchronized (context.getPdfLock()) {  //tiles may be currently loading in another thread
                dc.saveState();
                try {
                    mapTileTaskResult.renderOnPdf(dc);
                } catch (DocumentException e) {
                    context.addError(e);
                } finally {
                    dc.restoreState();
                    target.addDone(1);
                }
            }
        } else {
            //we had an error while loading the tile
            target.addDone(1);
        }
    }
}
