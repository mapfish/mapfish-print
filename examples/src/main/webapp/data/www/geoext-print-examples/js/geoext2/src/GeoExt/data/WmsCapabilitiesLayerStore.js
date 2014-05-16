/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include GeoExt/data/reader/WmsCapabilities.js
 * @requires GeoExt/data/OwsStore.js
 */

/**
 * Small helper class to make creating stores for remote WMS layer data
 * easier. The store is pre-configured with a built-in
 * {Ext.data.proxy.Ajax} and GeoExt.data.reader.WmsCapabilities.
 * The proxy is configured to allow caching and issues requests via GET.
 * If you require some other proxy/reader combination then you'll have to
 * configure this with your own proxy.
 *
 * @class GeoExt.data.WmsCapabilitiesLayerStore
 */
Ext.define('GeoExt.data.WmsCapabilitiesLayerStore',{
    extend: 'GeoExt.data.OwsStore',
    requires: ['GeoExt.data.reader.WmsCapabilities'],
    model: 'GeoExt.data.WmsCapabilitiesLayerModel',
    alternateClassName: [
        'GeoExt.data.WMSCapabilitiesStore',
        'GeoExt.data.WmsCapabilitiesStore'
    ]
});
