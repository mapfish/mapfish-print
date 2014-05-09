/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include OpenLayers/Format/WMC.js
 */

/**
 * Data reader class to create an array of records from a WMC document.
 *
 * @class GeoExt.data.reader.Wmc
 */
Ext.define('GeoExt.data.reader.Wmc', {
    alternateClassName: ['GeoExt.data.WMCReader'],
    extend: 'Ext.data.reader.Json',
    alias: 'reader.gx_wmc',
    requires: [
        'GeoExt.Version'
    ],

    /**
     * Creates new Reader.
     *
     * @param {Object} [config] Config object.
     */
    constructor: function(config) {
        if (!this.model) {
            this.model = 'GeoExt.data.WmcLayerModel';
        }
        this.callParent([config]);
        if (!this.format) {
            this.format = new OpenLayers.Format.WMC();
        }
    },

    /**
     * Gets the records.
     *
     * @param {Object} request The XHR object which contains the parsed XML
     *     document.
     * @return {Object} A data block which is used by an Ext.data.Store
     *     as a cache of Ext.data.Model objects.
     */
    getResponseData: function(request) {
        var data = request.responseXML;
        if(!data || !data.documentElement) {
            data = request.responseText;
        }
        return this.readRecords(data);
    },

    /**
     * Create a data block containing Ext.data.Records from an XML document.
     *
     * @param {DOMElement/String/Object} data A document element or XHR
     *     response string.  As an alternative to fetching capabilities data
     *     from a remote source, an object representing the capabilities can
     *     be provided given that the structure mirrors that returned from the
     *     capabilities parser.
     * @return  {Object} A data block which is used by an Ext.data.Store
     *     as a cache of Ext.data.Model objects.
     * @private
     */
    readRecords: function(data) {
        if(typeof data === "string" || data.nodeType) {
            data = this.format.read(data);
        }
        var layersContext = data ? data.layersContext : undefined;
        var records = [];

        if(layersContext) {
            var fields = this.getFields();
            var i, lenI, j, lenJ, layerContext, metadata, field, layer;
            for (i = 0, lenI = layersContext.length; i < lenI; i++) {
                layerContext = layersContext[i];
                layerContext.metadata = layerContext.metadata || {};
                for(j = 0, lenJ = fields.length; j < lenJ; j++){
                    field = fields[j];
                    layerContext.metadata[field.name] = layerContext[field.name];
                }
                layerContext.metadata.name = layerContext.name;
                layer = this.format.getLayerFromContext(layerContext);
                records.push(layer);
            }
        }

        return this.callParent([records]);
    }

});
