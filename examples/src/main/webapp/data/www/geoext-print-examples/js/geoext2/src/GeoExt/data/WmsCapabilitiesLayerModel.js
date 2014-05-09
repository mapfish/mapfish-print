/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include GeoExt/data/reader/WmsCapabilities.js
 */

/**
 * The model for WMS layers coming from a WMS GetCapabilities document.
 *
 * @class GeoExt.data.WmsCapabilitiesLayerModel
 */
Ext.define('GeoExt.data.WmsCapabilitiesLayerModel',{
    extend: 'GeoExt.data.LayerModel',
    alternateClassName: [
        'GeoExt.data.WMSCapabilitiesModel',
        'GeoExt.data.WmsCapabilitiesModel'
    ],
    requires: ['GeoExt.data.reader.WmsCapabilities'],
    alias: 'model.gx_wmscapabilities',
    fields: [
        {name: "name", type: "string", mapping: "metadata.name"},
        {name: "abstract", type: "string", mapping: "metadata.abstract"},
        {name: "queryable", type: "boolean", mapping: "metadata.queryable"},
        {name: "opaque", type: "boolean", mapping: "metadata.opaque"},
        {name: "noSubsets", type: "boolean", mapping: "metadata.noSubsets"},
        {name: "cascaded", type: "int", mapping: "metadata.cascaded"},
        {name: "fixedWidth", type: "int", mapping: "metadata.fixedWidth"},
        {name: "fixedHeight", type: "int", mapping: "metadata.fixedHeight"},
        {name: "minScale", type: "float", mapping: "metadata.minScale"},
        {name: "maxScale", type: "float", mapping: "metadata.maxScale"},
        {name: "prefix", type: "string", mapping: "metadata.prefix"},
        {name: "attribution", type: "string"},
        {name: "formats", mapping: "metadata.formats"}, // array
        {name: "infoFormats", mapping: "metadata.infoFormats"}, //array
        {name: "styles", mapping: "metadata.styles"}, // array
        {name: "srs", mapping: "metadata.srs"}, // object
        {name: "dimensions", mapping: "metadata.dimensions"}, // object
        {name: "bbox", mapping: "metadata.bbox"}, // object
        {name: "llbbox", mapping: "metadata.llbbox"}, // array
        {name: "keywords", mapping: "metadata.keywords"}, // array
        {name: "identifiers", mapping: "metadata.identifiers"}, // object
        {name: "authorityURLs", mapping: "metadata.authorityURLs"}, // object
        {name: "metadataURLs", mapping: "metadata.metadataURLs"} // array
    ],
    proxy: {
        type: 'ajax',
        reader: {
            type: 'gx_wmscapabilities'
        }
    }
});
