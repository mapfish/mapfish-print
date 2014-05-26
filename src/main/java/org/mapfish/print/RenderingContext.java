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

package org.mapfish.print;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.mapfish.print.config.Config;
import org.mapfish.print.config.layout.Layout;
import org.mapfish.print.utils.PJsonObject;

import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

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
    private final Map<String, String> headers;
    
    /**
     * Store the set of mergeable parameters, indexed by context
     * and then by parameter name.
     * The special context '*' is used for universal parameters, and
     * is used when the context property is not specified.
     * Each parameter can have 3 properties:
     *  - defaultValue: the value to be used when the parameter is empty for a merged layer (defaults to empty string)
     *  - separator: the separator to use to join merged values (defaults to comma)
     *  - context: the context the parameter should be merged in (defaults to *, that is any context)
     */
    private final Map<String,Map<String,PJsonObject>> mergeableParams;

    /**
     * Current page being rendered
     */
    private PJsonObject currentPageParams = null;

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
                            PJsonObject globalParams, String configDir, Layout layout, Map<String, String> headers) {
        this.document = document;
        this.writer = writer;
        this.config = config;
        this.globalParams = globalParams;
        if (globalParams.has("mergeableParams")) {
            mergeableParams = buildMergeableParams();
        } else {
            mergeableParams = null;
        }
        
        this.configDir = configDir;
        this.layout = layout;
        this.headers = headers;
        customBlocks = new PDFCustomBlocks(writer, this);
    }

    private Map<String, Map<String, PJsonObject>> buildMergeableParams() {
        Map<String, Map<String, PJsonObject>> result = new HashMap<String, Map<String, PJsonObject>>();
        PJsonObject mergeableParamsObj = globalParams
                .getJSONObject("mergeableParams");
        Iterator<String> mergeableParamsIt = mergeableParamsObj.keys();
        while (mergeableParamsIt.hasNext()) {
            String key = mergeableParamsIt.next();
            PJsonObject mergeableParam = mergeableParamsObj.getJSONObject(key);
            String context = mergeableParam.optString("context", "*");
            Map<String, PJsonObject> container = result.get(context.toUpperCase());
            if(container == null) {
                container = new HashMap<String, PJsonObject>();
            }
            container.put(key.toUpperCase(), mergeableParam);
            result.put(context.toUpperCase(), container);
        }
        return result;
    }
    
    /**
     * Gets the set of mergeable parameters for the given context.
     * The default (*) context parameters are added to the set.
     * 
     * @param context
     * @return
     */
    public Map<String, PJsonObject> getMergeableParams(String context) {
        Map<String, PJsonObject> result = new HashMap<String, PJsonObject>();
        if(mergeableParams != null) {
            if(mergeableParams.containsKey(context.toUpperCase())) {
                result.putAll(mergeableParams.get(context.toUpperCase()));
            } 
            if(mergeableParams.containsKey("*")) {
                result.putAll(mergeableParams.get("*"));
            }
        }
        return result;
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

    public void setCurrentPageParams(PJsonObject pageParams) {
        currentPageParams = pageParams;
    }

    public PJsonObject getCurrentPageParams() {
        return currentPageParams;
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

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Object getPdfLock() {
        return pdfLock;
    }
}
