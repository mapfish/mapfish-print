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

package org.mapfish.print.servlet.oldapi;


import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.processor.Processor;
import org.mapfish.print.processor.jasper.TableProcessor;
import org.mapfish.print.processor.map.CreateMapProcessor;
import org.mapfish.print.servlet.MapPrinterServlet;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.PObject;
import org.mapfish.print.wrapper.json.PJsonObject;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.mapfish.print.Constants.JSON_LAYOUT_KEY;
import static org.mapfish.print.Constants.OUTPUT_FILENAME_KEY;
import static org.mapfish.print.servlet.MapPrinterServlet.JSON_OUTPUT_FORMAT;

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
     * @param oldRequest          the request in the format of the old API
     * @param configuration the configuration
     */
    public static PJsonObject convert(final PJsonObject oldRequest, final Configuration configuration) throws JSONException {
        final String layout = oldRequest.getString(JSON_LAYOUT_KEY);
        
        if (configuration.getTemplate(layout) == null) {
            throw new IllegalArgumentException("Layout '" + layout + "' is not configured");
        }
        
        final JSONObject request = new JSONObject();
        request.put(JSON_LAYOUT_KEY, oldRequest.getString(JSON_LAYOUT_KEY));
        if (oldRequest.has(OUTPUT_FILENAME_KEY)) {
            request.put(OUTPUT_FILENAME_KEY, oldRequest.getString(OUTPUT_FILENAME_KEY));
        }
        if (oldRequest.has(JSON_OUTPUT_FORMAT)) {
            request.put(JSON_OUTPUT_FORMAT, oldRequest.getString(JSON_OUTPUT_FORMAT));
        }
        request.put(MapPrinterServlet.JSON_ATTRIBUTES, getAttributes(oldRequest, configuration.getTemplate(layout)));
        
        return new PJsonObject(request, "spec");
    }

    private static JSONObject getAttributes(final PJsonObject oldRequest, final Template template) throws JSONException {
        final JSONObject attributes = new JSONObject();
        
        setMapAttribute(attributes, oldRequest, template);
        setTableAttribute(attributes, oldRequest, template);
        // TODO legends, scales, ...
        
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

    /**
     * Converts a table definition from something like:
     *          
     *   "table":{
     *       "data":[
     *          {
     *             "col0":"a",
     *             "col1":"b",
     *             "col2":"c"
     *          },
     *          {
     *             "col0":"d",
     *             "col1":"e",
     *             "col2":"f"
     *          },
     *          {},
     *          {}
     *       ],
     *       "columns":[
     *          "col0",
     *          "col1",
     *          "col2"
     *       ]
     *    },
     *    "col0":"Column 1",
     *    "col1":"Column 2",
     *    "col2":"Column 3"
     *    
     *    ... to ...:
     *    
     *   "table": {
     *       "columns": ["Column 1", "Column 2", "Column 3"],
     *       "data": [
     *           ["a", "b", "c"],
     *           ["d", "e", "f"]
     *       ]
     *   },
     */
    private static void setTableAttribute(final JSONObject attributes,
            final PJsonObject oldRequest, final Template template) throws JSONException {
        final TableProcessor tableProcessor = getTableProcessor(template);
        final PJsonObject oldTablePage = (PJsonObject) getOldTablePage(oldRequest);
        
        if (tableProcessor == null || oldTablePage == null) {
            // no table, no work
            return;
        } else if (tableProcessor != null && oldTablePage == null) {
            throw new IllegalArgumentException("Configuration expects a table, but no "
                    + "table is defined in the request.");
        }
        
        String tableAttributeName = "table";
        if (tableProcessor.getInputMapperBiMap().containsValue("table")) {
            tableAttributeName = tableProcessor.getInputMapperBiMap().inverse().get("table");
        }
        
        final JSONObject table = new JSONObject();
        attributes.put(tableAttributeName, table);
        
        final List<String> columnKeys = getTableColumnKeys(oldTablePage);
        final List<String> columnLabels = getTableColumnLabels(columnKeys, oldTablePage);
        final List<JSONArray> tableData = getTableData(columnKeys, oldTablePage);
        
        table.put("columns", columnLabels);
        table.put("data", tableData);
    }

    private static TableProcessor getTableProcessor(final Template template) {
        TableProcessor tableProcessor = null;
        
        for (Processor<?, ?> processor : template.getProcessors()) {
            if (processor instanceof TableProcessor) {
                if (tableProcessor == null) {
                    tableProcessor = (TableProcessor) processor;
                } else {
                    throw new UnsupportedOperationException("Template contains "
                            + "more than one table configuration. The legacy API "
                            + "supports only one table per template.");
                }
            }
        }
        return tableProcessor;
    }

    private static PObject getOldTablePage(final PJsonObject oldRequest) {
        final PArray pages = oldRequest.getArray("pages");
        
        PObject tablePage = null;
        for (int i = 0; i < pages.size(); i++) {
            final PObject page = pages.getObject(i);
            
            if (isTablePage(page)) {
                if (tablePage == null) {
                    tablePage = page;
                } else {
                    throw new UnsupportedOperationException("Request contains "
                            + "more than one page with a table. The legacy API "
                            + "supports only one table per report.");
                }
            }
        }
        return tablePage;
    }

    private static boolean isTablePage(final PObject page) {
        if (page.has("table")) {
            // page has a table, but let's check if it's not a "dummy" table created by GeoExt,
            // like this one:
            //
            // "table":{
            //    "data":[
            //       {
            //          "col0":""
            //       }
            //    ],
            //    "columns":[
            //       "col0"
            //    ]
            // }
            PObject table = page.getObject("table");
            if (table.getArray("columns").size() == 1 && table.getArray("data").size() == 1) {
                String columnName = table.getArray("columns").getString(0);
                String value = table.getArray("data").getObject(0).getString(columnName);
                return !Strings.isNullOrEmpty(value);
            }
            return true;
        }
        return false;
    }

    private static List<String> getTableColumnKeys(final PJsonObject oldTablePage) {
        final PObject table = oldTablePage.getObject("table");
        final PArray columns = table.getArray("columns");
        
        final List<String> columnKeys = new LinkedList<String>();
        for (int i = 0; i < columns.size(); i++) {
            columnKeys.add(columns.getString(i));
        }
        
        return columnKeys;
    }

    private static List<String> getTableColumnLabels(final List<String> columnKeys,
            final PJsonObject oldTablePage) {
        final List<String> columnLabels = new LinkedList<String>();
        
        for (String key : columnKeys) {
            if (oldTablePage.has(key)) {
                columnLabels.add(oldTablePage.getString(key));
            } else {
                columnLabels.add("");
            }
        }
        
        return columnLabels;
    }

    private static List<JSONArray> getTableData(final List<String> columnKeys,
            final PJsonObject oldTablePage) {
        final PObject table = oldTablePage.getObject("table");
        final PArray oldTableRows = table.getArray("data");
        
        final List<JSONArray> tableData = new LinkedList<JSONArray>();
        for (int i = 0; i < oldTableRows.size(); i++) {
            final PObject oldRow = oldTableRows.getObject(i);
            if (!oldRow.keys().hasNext()) {
                // row is empty, skip
                continue;
            }
            
            // copy the values for each column
            final JSONArray row = new JSONArray();
            for (String key : columnKeys) {
                row.put(oldRow.getString(key));
            }
            tableData.add(row);
        }
        
        return tableData;
    }
}
