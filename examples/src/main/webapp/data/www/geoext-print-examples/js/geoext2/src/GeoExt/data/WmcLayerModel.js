/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include GeoExt/data/reader/Wmc.js
 */

/**
 * The model for WMS layers coming from a Web Map Context document.
 *
 * @class GeoExt.data.WmcLayerModel
 */
Ext.define('GeoExt.data.WmcLayerModel',{
    extend: 'GeoExt.data.LayerModel',
    alternateClassName: ['GeoExt.data.WMCLayerModel'],
    requires: ['GeoExt.data.reader.Wmc'],
    alias: 'model.gx_wmc',
    fields: [
        {name: "name", type: "string", mapping: "metadata.name"},
        {name: "abstract", type: "string", mapping: "metadata.abstract"},
        {name: "metadataURL", type: "string", mapping: "metadata.metadataURL"},
        {name: "queryable", type: "boolean", mapping: "metadata.queryable"},
        {name: "formats", mapping: "metadata.formats"}, // array
        {name: "styles", mapping: "metadata.styles"} // array
    ],
    proxy: {
        type: 'ajax',
        reader: {
            type: 'gx_wmc'
        }
    }
});
