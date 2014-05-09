/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include OpenLayers/Symbolizer/Raster.js
 */

/**
 * A specific model for Raster Symbolizer classifications.
 *
 * Preconfigured with an Ajax proxy and a JSON reader.
 *
 * @class GeoExt.data.RasterStyleModel
 */
Ext.define('GeoExt.data.RasterStyleModel',{
    extend: 'Ext.data.Model',
    requires : [
         'Ext.data.JsonReader',
         'GeoExt.Version'
    ],
    idProperty: "filter",
    fields: [
        {name: "symbolizers", mapping: function(v) {
            return {
                fillColor: v.color,
                fillOpacity: v.opacity,
                stroke: false
            };
        }, defaultValue: null},
        {name: "filter", mapping: "quantity", type: "float", sortType: 'asFloat', sortDir: 'ASC'},
        {name: "label", mapping: function(v) {
            // fill label with quantity if empty
            return v.label || v.quantity;
        }}
    ],
    proxy: {
        type: 'memory',
        reader: {
            type: 'json',
            root: 'colorMap'
        }
    },
    listeners:{
        idchanged:function(rec){
            for(var i=0;i<rec.stores.length;i++){
                var store = rec.stores[i];
                store.sort();
            }
        }
    }
});
