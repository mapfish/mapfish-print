/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include GeoExt/data/reader/WfsCapabilities.js
 * @requires GeoExt/data/OwsStore.js
 */

/**
 * Small helper class to make creating stores for remote WFS layer data easier.
 * 
 * The store is pre-configured with a built-in Ext.data.proxy.Ajax and
 * GeoExt.data.reader.WfsCapabilities. The proxy is configured to allow caching
 * and issues requests via GET.
 * 
 * If you require some other proxy/reader combination then you'll have to
 * configure this with your own proxy or create a basic
 * GeoExt.data.LayerStore and configure as needed.
 *
 * @class GeoExt.data.WfsCapabilitiesLayerStore
 */
Ext.define('GeoExt.data.WfsCapabilitiesLayerStore',{
    extend: 'GeoExt.data.OwsStore',
    requires: ['GeoExt.data.reader.WfsCapabilities'],
    model: 'GeoExt.data.WfsCapabilitiesLayerModel',
    alternateClassName: [
        'GeoExt.data.WFSCapabilitiesStore',
        'GeoExt.data.WfsCapabilitiesStore'
    ]
});
