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

package org.mapfish.print;

import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import org.mapfish.print.config.Config;
import org.mapfish.print.config.layout.Layout;
import org.mapfish.print.utils.PJsonObject;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds some "per rendering request" information.
 */
public class RenderingContext {
    private final Document document;
    private final PdfWriter writer;
    private final Config config;
    private final PJsonObject globalParams;
    private final String configDir;
    private final PDFCustomBlocks customBlocks;
    private final Layout layout;
    private final String referer;

    /**
     * Factor applyed to styles (line width, ...). Used to make features thinner
     * in the overview map.
     */
    private float styleFactor = 1.0f;

    /**
     * Cache of PDF images.
     */
    private Map<URI, PdfTemplate> templateCache = Collections.synchronizedMap(new HashMap<URI, PdfTemplate>());

    /**
     * Simple object on which we can synchronize to protect the PDF against parallel writing.
     *
     * Before, we were using the DirectContent for the locking, but it seems to
     * be problematic (had infinite loops in iText).
     */
    private final Object pdfLock=new Object();

    public RenderingContext(Document document, PdfWriter writer, Config config,
                            PJsonObject globalParams, String configDir, Layout layout, String referer) {
        this.document = document;
        this.writer = writer;
        this.config = config;
        this.globalParams = globalParams;
        this.configDir = configDir;
        this.layout = layout;
        this.referer = referer;
        customBlocks = new PDFCustomBlocks(writer, this);
    }

    public PDFCustomBlocks getCustomBlocks() {
        return customBlocks;
    }

    public Document getDocument() {
        return document;
    }

    public Config getConfig() {
        return config;
    }

    public PdfWriter getWriter() {
        return writer;
    }

    public PdfContentByte getDirectContent() {
        return writer.getDirectContent();
    }

    public PJsonObject getGlobalParams() {
        return globalParams;
    }

    public String getConfigDir() {
        return configDir;
    }

    public Layout getLayout() {
        return layout;
    }

    public void addError(Exception e) {
        customBlocks.addError(e);
    }

    public float getStyleFactor() {
        return styleFactor;
    }

    public void setStyleFactor(float styleFactor) {
        this.styleFactor = styleFactor;
    }

    public Map<URI, PdfTemplate> getTemplateCache() {
        return templateCache;
    }

    public String getReferer() {
        return referer;
    }

    public Object getPdfLock() {
        return pdfLock;
    }
}
