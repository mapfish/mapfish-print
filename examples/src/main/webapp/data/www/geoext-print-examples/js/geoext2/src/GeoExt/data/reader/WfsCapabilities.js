/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include OpenLayers/Format/WFSCapabilities.js
 * @include OpenLayers/Protocol/WFS.js
 * @include OpenLayers/Strategy/Fixed.js
 * @include OpenLayers/Layer/Vector.js
 */

/**
 * Data reader class to create {GeoExt.data.WfsCapabilitiesLayerModel[]}
 * from a WFS GetCapabilities response.
 *
 * @class GeoExt.data.reader.WfsCapabilities
 */
Ext.define('GeoExt.data.reader.WfsCapabilities', {
    alternateClassName: [
        'GeoExt.data.reader.WFSCapabilities',
        'GeoExt.data.WFSCapabilitiesReader'
    ],
    extend: 'Ext.data.reader.Json',
    alias: 'reader.gx_wfscapabilities',
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
            this.model = 'GeoExt.data.WfsCapabilitiesLayerModel';
        }
        this.callParent([config]);
        if (!this.format) {
            this.format = new OpenLayers.Format.WFSCapabilities();
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

        var featureTypes = data.featureTypeList.featureTypes;
        var fields = this.getFields();

        var featureType, metadata, field, v, parts, layer;
        var layerOptions, protocolOptions;

        var protocolDefaults = {
            url: data.capability.request.getfeature.href.post
        };

        var records = [];

        for(var i=0, lenI=featureTypes.length; i<lenI; i++) {
            featureType = featureTypes[i];
            if(featureType.name) {
                metadata = {};

                for(var j=0, lenJ=fields.length; j<lenJ; j++) {
                    field = fields[j];
                    v = featureType[field.name];
                    metadata[field.name] = v;
                }

                metadata['name'] = featureType.name;
                metadata['featureNS'] = featureType.featureNS;

                protocolOptions = {
                    featureType: featureType.name,
                    featureNS: featureType.featureNS
                };
                if(this.protocolOptions) {
                    Ext.apply(protocolOptions, this.protocolOptions,
                        protocolDefaults);
                } else {
                    Ext.apply(protocolOptions, {}, protocolDefaults);
                }

                layerOptions = {
                    metadata: metadata,
                    protocol: new OpenLayers.Protocol.WFS(protocolOptions),
                    strategies: [new OpenLayers.Strategy.Fixed()]
                };
                var metaLayerOptions = this.layerOptions;
                if (metaLayerOptions) {
                    Ext.apply(layerOptions, Ext.isFunction(metaLayerOptions) ?
                        metaLayerOptions() : metaLayerOptions);
                }

                layer = new OpenLayers.Layer.Vector(
                    featureType.title || featureType.name,
                    layerOptions
                );

                records.push(layer);
            }
        }
        return this.callParent([records]);
    }
});
