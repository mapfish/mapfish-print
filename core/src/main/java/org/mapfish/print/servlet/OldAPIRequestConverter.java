/*
 * Copyright (C) 2014  Camptocamp
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

package org.mapfish.print.servlet;


import com.google.common.collect.Sets;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapfish.print.Constants;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.processor.Processor;
import org.mapfish.print.processor.map.CreateMapProcessor;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.PObject;
import org.mapfish.print.wrapper.json.PJsonObject;

import java.util.Iterator;
import java.util.Set;

/**
 * Converter for print requests of the old API.
 */
public final class OldAPIRequestConverter {
    
    private OldAPIRequestConverter() { }
    
    private static final Set<String> NON_CUSTOM_PARAMS = Sets.newHashSet(
            "units", "srs", "layout", "dpi", "layers", "pages", "legends",
            "geodetic", "outputFilename", "outputFormat");
    
    /**
     * Converts a print request of the old API into the new request format.
     * 
     * Note that the converter does not support all features of the old API, for example
     * only requests containing a single map are supported.
     * 
     * @param spec          the request in the format of the old API
     * @param configuration the configuration
     */
    public static PJsonObject convert(final String spec, final Configuration configuration) throws JSONException {
        final PJsonObject oldRequest = MapPrinter.parseSpec(spec);
        final String layout = oldRequest.getString("layout");
        
        if (configuration.getTemplate(layout) == null) {
            throw new IllegalArgumentException("Layout '" + layout + "' is not configured");
        }
        
        final JSONObject request = new JSONObject();
        request.put(Constants.JSON_LAYOUT_KEY, oldRequest.getString("layout"));
        if (oldRequest.has("outputFilename")) {
            request.put(Constants.OUTPUT_FILENAME_KEY, oldRequest.getString("outputFilename"));
        }
        if (oldRequest.has("outputFormat")) {
            request.put("outputFormat", oldRequest.getString("outputFormat"));
        }
        request.put("attributes", getAttributes(oldRequest, configuration.getTemplate(layout)));
        
        return new PJsonObject(request, "spec");
    }

    private static JSONObject getAttributes(final PJsonObject oldRequest, final Template template) throws JSONException {
        final JSONObject attributes = new JSONObject();
        
        setMapAttribute(attributes, oldRequest, template);
        // TODO table, legends
        
        // copy custom parameters
        Iterator<String> keys = oldRequest.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!NON_CUSTOM_PARAMS.contains(key)) {
                attributes.put(key, oldRequest.getInternalObj().get(key));
            }
        }
        
        return attributes;
    }

    private static void setMapAttribute(final JSONObject attributes,
            final PJsonObject oldRequest, final Template template) throws JSONException {
        final CreateMapProcessor mapProcessor = getMapProcessor(template);
        final PJsonObject oldMapPage = (PJsonObject) getOldMapPage(oldRequest);
        
        if (mapProcessor == null && oldMapPage == null) {
            // no map, no work
            return;
        } else if (mapProcessor != null && oldMapPage == null) {
            throw new IllegalArgumentException("Configuration expects a map, but no "
                    + "map is defined in the request.");
        }
        
        String mapAttributeName = "map";
        if (mapProcessor.getInputMapperBiMap().containsValue("map")) {
            mapAttributeName = mapProcessor.getInputMapperBiMap().inverse().get("map");
        }
        
        final JSONObject map = new JSONObject();
        attributes.put(mapAttributeName, map);
        
        if (oldRequest.has("srs")) {
            map.put("projection", oldRequest.getString("srs"));
        }
        if (oldRequest.has("dpi")) {
            map.put("dpi", oldRequest.getInt("dpi"));
        }
        if (oldMapPage.has("rotation")) {
            map.put("rotation", oldMapPage.getDouble("rotation"));
        }
        if (oldMapPage.has("bbox")) {
            map.put("bbox", oldMapPage.getInternalObj().getJSONArray("bbox"));
        } else if (oldMapPage.has("center") && oldMapPage.has("scale")) {
            map.put("center", oldMapPage.getInternalObj().getJSONArray("center"));
            map.put("scale", oldMapPage.getDouble("scale"));
        }
        setMapLayers(map, oldRequest);
    }

    private static CreateMapProcessor getMapProcessor(final Template template) {
        CreateMapProcessor mapProcessor = null;
        
        for (Processor<?, ?> processor : template.getProcessors()) {
            if (processor instanceof CreateMapProcessor) {
                if (mapProcessor == null) {
                    mapProcessor = (CreateMapProcessor) processor;
                } else {
                    throw new UnsupportedOperationException("Template contains "
                            + "more than one map configuration. The legacy API "
                            + "supports only one map per template.");
                }
            }
        }
        return mapProcessor;
    }

    private static PObject getOldMapPage(final PJsonObject oldRequest) {
        final PArray pages = oldRequest.getArray("pages");
        
        PObject mapPage = null;
        for (int i = 0; i < pages.size(); i++) {
            final PObject page = pages.getObject(i);
            
            if (isMapPage(page)) {
                if (mapPage == null) {
                    mapPage = page;
                } else {
                    throw new UnsupportedOperationException("Request contains "
                            + "more than one page with a map. The legacy API "
                            + "supports only one map per report.");
                }
            }
        }
        return mapPage;
    }

    private static boolean isMapPage(final PObject page) {
        if (page.has("bbox") || (page.has("center") && page.has("scale"))) {
            return page.optBool("showMap", true);
        }
        return false;
    }

    private static void setMapLayers(final JSONObject map, final PJsonObject oldRequest) throws JSONException {
        final JSONArray layers = new JSONArray();
        map.put("layers", layers);
        
        if (!oldRequest.has("layers")) {
            return;
        }
        
        PArray oldLayers = oldRequest.getArray("layers");
        for (int i = 0; i < oldLayers.size(); i++) {
            PJsonObject oldLayer = (PJsonObject) oldLayers.getObject(i);
            layers.put(OldAPILayerConverter.convert(oldLayer));
        }
    }
}
