/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include GeoExt/data/reader/Attribute.js
 */

/**
 * @class GeoExt.data.AttributeModel
 *
 * A specific model for WFS DescribeFeatureType records.
 *
 * Preconfigured with an Ajax proxy and a GeoExt.data.reader.Attribute.
 */
Ext.define('GeoExt.data.AttributeModel', {
    alternateClassName: 'GeoExt.data.AttributeRecord',
    extend: 'Ext.data.Model',
    requires: [
        'Ext.data.proxy.Ajax',
        'GeoExt.data.reader.Attribute'
    ],
    alias: 'model.gx_attribute',
    fields: [
        {name: 'name', type: 'string'},
        {name: 'type', defaultValue: null},
        {name: 'restriction', defaultValue: null},
        {name: 'nillable', type: 'bool'}
        // No 'value' field by default. The 'value' field gets added by the
        // GeoExt.data.reader.Attribute constructor if it is given a feature.
    ],
    proxy: {
        type: 'ajax',
        reader: {
            type: 'gx_attribute'
        }
    }
});
