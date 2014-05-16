/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include OpenLayers/Util.js
 * @include GeoExt/data/ScaleModel.js
 * @include GeoExt/panel/Map.js
 */

/**
 * A store that contains a cache of available zoom levels.  The store can
 * optionally be kept synchronized with an {OpenLayers.Map} or
 * GeoExt.panel.Map object.
 *
 * @class GeoExt.data.ScaleStore
 */
Ext.define('GeoExt.data.ScaleStore', {
    requires: [
        'GeoExt.data.ScaleModel',
        'GeoExt.panel.Map'
    ],
    extend: 'Ext.data.Store',
    model: 'GeoExt.data.ScaleModel',

    /**
     * Optional map or map panel from which to derive scale values.
     *
     * @cfg {OpenLayers.Map/GeoExt.panel.Map}
     */
    map: null,

    /**
     * Construct a ScaleStore from a configuration.  The ScaleStore accepts
     * some custom parameters addition to the fields accepted by Ext.Store.
     *
     * @private
     */
    constructor: function(config) {
        config = config || {};
        var map = (config.map instanceof GeoExt.panel.Map ? config.map.map : config.map);
        delete config.map;
        this.callParent([config]);
        if (map) {
            this.bind(map);
        }
    },

    /**
     * Bind this store to a map; that is, maintain the zoom list in sync with
     * the map's current configuration.  If the map does not currently have a
     * set scale list, then the store will remain empty until the map is
     * configured with one.
     *
     * @param {GeoExt.panel.Map/OpenLayers.Map} map Map to which we should bind.
     */
    bind: function(map, options) {
        this.map = (map instanceof GeoExt.panel.Map ? map.map : map);
        this.map.events.register('changebaselayer', this, this.populateFromMap);
        if (this.map.baseLayer) {
            this.populateFromMap();
        } else {
            this.map.events.register('addlayer', this, this.populateOnAdd);
        }
    },

    /**
     * Un-bind this store from the map to which it is currently bound.  The
     * currently stored zoom levels will remain, but no further changes from
     * the map will affect it.
     */
    unbind: function() {
        if (this.map) {
            if (this.map.events) {
                this.map.events.unregister('addlayer', this, this.populateOnAdd);
                this.map.events.unregister('changebaselayer', this, this.populateFromMap);
            }
            delete this.map;
        }
    },

    /**
     * This method handles the case where we have `#bind` called on a
     * not-fully-configured map so that the zoom levels can be detected when a
     * baselayer is finally added.
     *
     * @param {Object} evt
     * @private
     */
    populateOnAdd: function(evt) {
        if (evt.layer.isBaseLayer) {
            this.populateFromMap();
            this.map.events.unregister('addlayer', this, this.populateOnAdd);
        }
    },

    /**
     * This method actually loads the zoom level information from the
     * OpenLayers.Map and converts it to Ext Records.
     *
     * @private
     */
    populateFromMap: function() {
        var zooms = [];
        var resolutions = this.map.baseLayer.resolutions;
        var units = this.map.baseLayer.units;

        for (var i=resolutions.length-1; i >= 0; i--) {
            var res = resolutions[i];
            zooms.push({
                level: i,
                resolution: res,
                scale: OpenLayers.Util.getScaleFromResolution(res, units)
            });
        }

        this.loadData(zooms);
    },

    /**
     * Unregisters listeners by calling #unbind prior to destroying.
     *
     * @private
     */
    destroy: function() {
        this.unbind();
        this.callParent(arguments);
    }
});
