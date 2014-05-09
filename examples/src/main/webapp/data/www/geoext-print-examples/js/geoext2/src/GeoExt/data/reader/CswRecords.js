/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include OpenLayers/Format/CSWGetRecords.js
 */

/**
 * Data reader class to create an array of records from a CSW GetRecords
 * response. The raw response from the OpenLayers parser is available through
 * the jsonData property.
 *
 * Example:
 *
 *     var store = Ext.create('Ext.data.Store', {
 *         proxy: Ext.create('GeoExt.data.proxy.Protocol', {
 *             protocol: new OpenLayers.Protocol.CSW({
 *                 url: "http://demo.geonode.org/geonetwork/srv/en/csw"
 *             })
 *         }),
 *         reader: Ext.create('GeoExt.data.reader.CswRecords')
 *     });
 *
 * @class GeoExt.data.reader.CswRecords
 */
Ext.define('GeoExt.data.reader.CswRecords', {
    alternateClassName: ['GeoExt.data.CSWRecordsReader'],
    extend: 'Ext.data.reader.Json',
    alias: 'reader.gx_cswrecords',
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
            this.model = 'GeoExt.data.CswRecordsModel';
        }
        this.callParent([config]);
        if (!this.format) {
            this.format = new OpenLayers.Format.CSWGetRecords();
        }
    },

    /**
     * @param {XMLHttpRequest/OpenLayers.Protocol.Response} data If a
     *     ProtocolProxy is configured with OpenLayers.Protocol.CSW data will be
     *     OpenLayers.Protocol.Response. Otherwise data will be the
     *     XMLHttpRequest object.
     * @return  {Object} A data block which is used by an Ext.data.Store as a
     *     cache of Ext.data.Model objects.
     * @private
     */
    read: function(data) {
        var o = data.data;
        if (!o) {
            o = data.responseXML;
            if(!o || !o.documentElement) {
                o = data.responseText;
            }
        }
        return this.readRecords(o);
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
        var result = this.callParent([data.records]);
        // post-process so we flatten simple objects with a value property
        Ext.each(result.records, function(record) {
            for (var key in record.data) {
                var value = record.data[key];
                if (value instanceof Array) {
                    for (var i=0, ii=value.length; i<ii; ++i) {
                        if (value[i] instanceof Object) {
                            value[i] = value[i].value;
                        }
                    }
                }
            }
        });
        if (data.SearchResults) {
            delete result.totalRecords;
            result.total = data.SearchResults.numberOfRecordsMatched;
        }
        return result;
    }
});
