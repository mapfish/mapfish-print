/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include OpenLayers/Format/WMSDescribeLayer.js
 */

/**
 * Data reader class to create an array of layer description objects from a WMS
 * DescribeLayer response.
 *
 * @class GeoExt.data.reader.WmsDescribeLayer
 */
Ext.define('GeoExt.data.reader.WmsDescribeLayer', {
    alternateClassName: ['GeoExt.data.reader.WMSDescribeLayer', 'GeoExt.data.WMSCapabilitiesReader'],
    extend: 'Ext.data.reader.Json',
    alias: 'reader.gx_wmsdescribelayer',
    requires: [
        'GeoExt.Version'
    ],
    /**
     * Creates new Reader.
     *
     * @param {Object} config (optional) Config object.
     */
    constructor: function(config) {
        if (!this.model) {
            this.model = 'GeoExt.data.WmsDescribeLayerModel';
        }
        this.callParent([config]);
        if (!this.format) {
            this.format = new OpenLayers.Format.WMSDescribeLayer();
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
     * @return {Object} A data block which is used by an Ext.data.Store
     *     as a cache of Ext.data.Model objects.
     */
    readRecords: function(data) {
        if(typeof data === "string" || data.nodeType) {
            data = this.format.read(data);
        }
        if (!!data.error) {
            Ext.Error.raise({msg: "Error parsing WMS DescribeLayer", arg: data.error});
        }
        return this.callParent([data]);
    }
});
