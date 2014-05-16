/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 * 
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txttxt for the full
 * text of the license.
 */

/**
 * A smart store that creates records for client-side rendered legends. If
 * its store is configured with an {OpenLayers.Style2} instance as `data`,
 * each record will represent a rule of the Style, and the store will be
 * configured with `symbolizers` (Array of {OpenLayers.Symbolizer}),
 * `filter` ({OpenLayers.Filter}), `label` (String, the rule's title),
 * `name` (String), `description` (String), `elseFilter` (Boolean),
 * `minScaleDenominator` (Number) and `maxScaleDenominator` (Number)
 * fields. If the store's `data` is an {OpenLayers.Symbolizer.Raster}
 * instance, records will represent its ColorMap entries, and the available
 * fields will only be `symbolizers` (object literal with `color` and
 * `opacity` properties from the ColorMapEntry, and stroke set to false),
 * `filter` (String, the ColorMapEntry's quantity) and `label` (String).
 *
 * NOTE: Calling `commitChanges` on the store that is populated with
 * this reader will fail with OpenLayers 2.11 - it requires at least revision
 * https://github.com/openlayers/openlayers/commit/1db5ac3cbe874317968f78832901d6ef887ecca6
 * from 2011-11-28 of OpenLayers.
 *
 * Sample code to create a store that reads from an OpenLayers.Style2
 * object:
 *
 *    var store = Ext.create('GeoExt.data.StyleStore',{
 *        data: myStyle // OpenLayers.Style2 or OpenLayers.Symbolizer.Raster
 *    });
 *
 * @class GeoExt.data.StyleStore
 */
Ext.define('GeoExt.data.StyleStore', {
    extend: 'Ext.data.Store',
    requires: [
        'GeoExt.data.VectorStyleModel',
        'GeoExt.data.RasterStyleModel'
    ],
    alias: 'store.gx_style',
    constructor: function(config){
        config = Ext.apply({}, config);
        if(config.data && !config.model){
            if (config.data instanceof OpenLayers.Symbolizer.Raster) {
                config.model = 'GeoExt.data.RasterStyleModel';
                config.sorters = [{
                    property: 'filter',
                    direction: 'ASC',
                    root: 'data'
                }];
            } else {
                config.model = 'GeoExt.data.VectorStyleModel';
            }            
        }
        this.callParent([config]);
    }
});