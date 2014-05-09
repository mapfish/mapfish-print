/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/**
 * Small Base class to make creating stores for remote OWS information sources
 * easier.
 *
 * NOTE: This is a BASE CLASS and is not designed for direct use in an
 * application. Instead, one should extend from this class in any situation in
 * which a you need a {@link Ext.data.proxy.Server} (ex: 'ajax', 'jsonp', etc)
 * and a reader which requires an {@link OpenLayers.Format} to parse the data.
 *
 * @class GeoExt.data.OwsStore
 */
Ext.define('GeoExt.data.OwsStore', {
    extend: 'Ext.data.Store',
    requires: [
        'GeoExt.Version'
    ],
    alternateClassName: ['GeoExt.data.OWSStore'],

    config: {
        /**
         * The URL from which to retrieve the WMS DescribeLayer document.
         *
         * @cfg {String}
         */
        url : null,

        /**
         * A parser for transforming the XHR response into an array of objects
         * representing attributes. Defaults to an {OpenLayers.Format.WMSDescribeLayer}
         * parser.
         *
         * @cfg {OpenLayers.Format}
         */
        format : null
    },

    /**
     * @private
     */
    constructor: function(config) {
        // At this point, we have to copy the complex objects from the config
        // into the prototype. This is because Ext.data.Store's constructor
        // creates deep copies of these objects.
        if (config.format) {
            this.format = config.format;
            delete config.format;
        }

        this.callParent([config]);

        if(config.url) {
            this.setUrl(config.url);
        }
        if(this.format) {
            this.setFormat(this.format);
        }
    },

    /**
     * @private
     */
    applyUrl: function(newValue) {
        if(newValue && Ext.isString(newValue)) {
            var proxy = this.getProxy();
            if(proxy) {
                proxy.url = newValue;
            }
        }
    },

    /**
     * @private
     */
    applyFormat: function(newFormat) {
        var proxy = this.getProxy();
        var reader = (proxy) ? proxy.getReader() : null;
        if(reader) {
            reader.format = newFormat;
        }
    }
});
