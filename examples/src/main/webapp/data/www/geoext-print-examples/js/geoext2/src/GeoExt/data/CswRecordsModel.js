/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include GeoExt/data/reader/CswRecords.js
 */

/**
 * The model for the structure returned by CS-W GetRecords.
 *
 * @class GeoExt.data.CswRecordsModel
 */
Ext.define('GeoExt.data.CswRecordsModel',{
    extend: 'Ext.data.Model',
    requires: [
        'Ext.data.proxy.Memory',
        'GeoExt.data.reader.CswRecords'
    ],
    alias: 'model.gx_cswrecords',
    fields: [
        {name: "title"},
        {name: "subject"},
        {name: "URI"},
        {name: "bounds"},
        {name: "projection", type: "string"}
    ],
    proxy: {
        type: 'memory',
        reader: {
            type: 'gx_cswrecords'
        }
    }
});
