/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include OpenLayers/Format/WMSCapabilities.js
 * @include OpenLayers/Layer/WMS.js
 * @include OpenLayers/Util.js
 */

/**
 * Data reader class to create GeoExt.data.WmsCapabilitiesLayerModel[]
 * from a WMS GetCapabilities response.
 *
 * @class GeoExt.data.reader.WmsCapabilities
 */
Ext.define('GeoExt.data.reader.WmsCapabilities', {
    alternateClassName: [
        'GeoExt.data.reader.WMSCapabilities',
        'GeoExt.data.WMSCapabilitiesReader'
    ],
    extend: 'Ext.data.reader.Json',
    alias: 'reader.gx_wmscapabilities',
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
            this.model = 'GeoExt.data.WmsCapabilitiesLayerModel';
        }
        this.callParent([config]);
        if (!this.format) {
            this.format = new OpenLayers.Format.WMSCapabilities();
        }
    },

    /**
     * CSS class name for the attribution DOM elements.
     * Element class names append "-link", "-image", and "-title" as
     * appropriate.  Default is "gx-attribution".
     *
     * @cfg {String}
     */
    attributionCls: "gx-attribution",

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
     * @param {String[]} formats An array of service exception format strings.
     * @return {String} The (supposedly) best service exception format.
     * @private
     */
    serviceExceptionFormat: function(formats) {
        if (OpenLayers.Util.indexOf(formats,
            "application/vnd.ogc.se_inimage")>-1) {
            return "application/vnd.ogc.se_inimage";
        }
        if (OpenLayers.Util.indexOf(formats,
            "application/vnd.ogc.se_xml")>-1) {
            return "application/vnd.ogc.se_xml";
        }
        return formats[0];
    },

    /**
     * @param {Object} layer The layer's capabilities object.
     * @return {String} The (supposedly) best mime type for requesting
     *     tiles.
     * @private
     */
    imageFormat: function(layer) {
        var formats = layer.formats;
        if (layer.opaque &&
            OpenLayers.Util.indexOf(formats, "image/jpeg")>-1) {
            return "image/jpeg";
        }
        if (OpenLayers.Util.indexOf(formats, "image/png")>-1) {
            return "image/png";
        }
        if (OpenLayers.Util.indexOf(formats, "image/png; mode=24bit")>-1) {
            return "image/png; mode=24bit";
        }
        if (OpenLayers.Util.indexOf(formats, "image/gif")>-1) {
            return "image/gif";
        }
        return formats[0];
    },

    /**
     * @param {Object} layer The layer's capabilities object.
     * @return {Boolean} The TRANSPARENT param.
     * @private
     */
    imageTransparent: function(layer) {
        return layer.opaque == undefined || !layer.opaque;
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
        if (Ext.isArray(data)) {
            return this.callParent(data);
        }
        if(typeof data === "string" || data.nodeType) {
            data = this.format.read(data);
        }
        if (!!data.error) {
            Ext.Error.raise({msg: "Error parsing WMS GetCapabilities", arg: data.error});
        }
        var version = data.version;
        var capability = data.capability || {};
        var url = capability.request && capability.request.getmap &&
            capability.request.getmap.href;
        var layers = capability.layers;
        var formats = capability.exception ? capability.exception.formats : [];
        var exceptions = this.serviceExceptionFormat(formats);
        var records = [];

        if(url && layers) {
            var fields = this.getFields();
            var layer, metadata, options, params, field, v;

            for(var i=0, lenI=layers.length; i<lenI; i++){
                layer = layers[i];
                if(layer.name) {
                    metadata = {};
                    for(var j=0, lenJ=fields.length; j<lenJ; j++) {
                        field = fields[j];
                        metadata[field.name] = layer[field.name];
                    }
                    metadata['name'] = layer.name;
                    options = {
                        attribution: layer.attribution ?
                            this.attributionMarkup(layer.attribution) :
                            undefined,
                        minScale: layer.minScale,
                        maxScale: layer.maxScale,
                        metadata: metadata
                    };
                    if(this.layerOptions) {
                        Ext.apply(options, this.layerOptions);
                    }
                    params = {
                            layers: layer.name,
                            exceptions: exceptions,
                            format: this.imageFormat(layer),
                            transparent: this.imageTransparent(layer),
                            version: version
                    };
                    if (this.layerParams) {
                        Ext.apply(params, this.layerParams);
                    }
                    layer = new OpenLayers.Layer.WMS(
                        layer.title || layer.name, url, params, options
                    );
                    records.push(layer);
                }
            }
        }
        return this.callParent([records]);
    },

    /**
     * Generates attribution markup using the Attribution metadata
     * from WMS Capabilities.
     *
     * @param {Object} attribution The attribution property of the layer
     *     object as parsed from a WMS Capabilities document
     * @return {String} HTML markup to display attribution information.
     * @private
     */
    attributionMarkup : function(attribution){
        var markup = [];

        if (attribution.logo){
            markup.push("<img class='"+this.attributionCls+"-image' "
                        + "src='" + attribution.logo.href + "' />");
        }

        if (attribution.title) {
            markup.push("<span class='"+ this.attributionCls + "-title'>"
                        + attribution.title
                        + "</span>");
        }

        if(attribution.href){
            for(var i = 0; i < markup.length; i++){
                markup[i] = "<a class='" +
                    this.attributionCls +
                    "-link' " +
                    "href=" +
                    attribution.href +
                    ">" +
                    markup[i] +
                    "</a>";
            }
        }

        return markup.join(" ");
    }
});
