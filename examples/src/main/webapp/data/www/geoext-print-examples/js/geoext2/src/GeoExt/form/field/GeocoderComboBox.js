/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/**
 * Creates a combo box that handles results from a geocoding service. By
 * default it uses OSM Nominatim, but it can be configured with a custom store
 * to use other services. If the user enters a valid address in the search
 * box, the combo's store will be populated with records that match the
 * address.  By default, records have the following fields:
 *
 * * name - `String` The formatted address.
 * * lonlat - `Array` Location matching address, for use with
 *     OpenLayers.LonLat.fromArray.
 * * bounds - `Array` Recommended viewing bounds, for use with
 *     OpenLayers.Bounds.fromArray.
 *
 * @class GeoExt.form.field.GeocoderComboBox
 */
Ext.define('GeoExt.form.field.GeocoderComboBox', {
    extend : 'Ext.form.field.ComboBox',
    requires: [
        "GeoExt.panel.Map",
        "Ext.data.JsonStore",
        "Ext.data.proxy.JsonP"
    ],
    alias : 'widget.gx_geocodercombo',
    alternateClassName : 'GeoExt.form.GeocoderComboBox',

    /**
     * Text to display for an empty field (i18n).
     *
     * @cfg {String}
     */
    emptyText: "Search",

    /**
     * The map that will be controlled by
     * this GeoCoderComboBox. Only used if this component is not added as item
     * or toolbar item to a GeoExt.panel.Map.
     *
     * @cfg {GeoExt.panel.Map/OpenLayers.Map} map
     */
    /**
     * @property {OpenLayers.Map} map
     * @private
     */

    /**
     * The srs used by the geocoder service.
     *
     * @cfg {String/OpenLayers.Projection}
     */
    srs: "EPSG:4326",

    /**
     * The minimum zoom level to use when zooming to a location.
     * Not used when zooming to a bounding box.
     *
     * @cfg {Integer}
     */
    zoom: 10,

    /**
     * Delay before the search occurs in ms.
     *
     * @cfg {Number}
     */
    queryDelay: 100,

    /**
     * Field from selected record to use when the combo's
     * getValue method is called.  Default is "bounds". This field is
     * supposed to contain an array of [left, bottom, right, top] coordinates
     * for a bounding box or [x, y] for a location.
     *
     * @cfg {String}
     */
    valueField: "bounds",

    /**
     * The field to display in the combo box. Default is
     * "name" for instant use with the default store for this component.
     *
     * @cfg {String}
     */
    displayField: "name",

    /**
     * The field to get the location from. This field is supposed
     * to contain an array of [x, y] for a location. Default is "lonlat" for
     * instant use with the default store for this component.
     *
     * @cfg {String}
     */
    locationField: "lonlat",

    /**
     * URL template for querying the geocoding service. If a store is
     * configured, this will be ignored. Note that the queryParam will be used
     * to append the user's combo box input to the url.
     *
     * Default is "http://nominatim.openstreetmap.org/search?format=json", for
     * instant use with the OSM Nominatim geolocator. However, if you intend to
     * use that, note the [Nominatim Usage
     * Policy](http://wiki.openstreetmap.org/wiki/Nominatim_usage_policy).
     *
     * @cfg {String}
     */
    url: "http://nominatim.openstreetmap.org/search?format=json",

    /**
     * The query parameter for the user entered search text.
     * Default is "q" for instant use with OSM Nominatim.
     *
     * @cfg {String}
     */
    queryParam: "q",

    /**
     * Minimum number of entered characters to trigger a search.
     *
     * @cfg {Number}
     */
    minChars: 3,

    /**
     * The store used for this combo box. Default is a
     * store with a ScriptTagProxy and the url configured as :obj:`url`
     * property.
     *
     * @cfg {Ext.data.Store}
     */
    store: null,

    /**
     * Last center that was zoomed to after selecting a location in the combo
     * box.
     *
     * @property {OpenLayers.LonLat}
     * @private
     */
    center: null,

    /**
     * Last location provided by the geolocator.
     * Only set if layer is configured.
     *
     * @property {OpenLayers.Feature.Vector}
     * @private
     */
    locationFeature: null,

    initComponent: function() {
        if (this.map) {
            this.setMap(this.map);
        }
        if (Ext.isString(this.srs)) {
            this.srs = new OpenLayers.Projection(this.srs);
        }
        if (!this.store) {
            this.store = Ext.create("Ext.data.JsonStore", {
                root: null,
                fields: [
                    {name: "name", mapping: "display_name"},
                    {name: "bounds", convert: function(v, rec) {
                        var bbox = rec.raw.boundingbox;
                        return [bbox[2], bbox[0], bbox[3], bbox[1]];
                    }},
                    {name: "lonlat", convert: function(v, rec) {
                        return [rec.raw.lon, rec.raw.lat];
                    }}
                ],
                proxy: Ext.create("Ext.data.proxy.JsonP", {
                    url: this.url,
                    callbackKey: "json_callback"
                })
            });
        }

        this.on({
            added: this.findMapPanel,
            select: this.handleSelect,
            focus: function() {
                this.clearValue();
                this.removeLocationFeature();
            },
            scope: this
        });
        return this.callParent(arguments);
    },

    /**
     * Find the MapPanel somewhere up in the hierarchy and set the map.
     *
     * @private
     */
    findMapPanel: function() {
        var mapPanel = this.up('gx_mappanel');
        if (mapPanel) {
            this.setMap(mapPanel);
        }
    },

    /**
     * Zoom to the selected location, and also set a location marker if this
     * component was configured with a layer.
     *
     * @private
     */
    handleSelect: function(combo, rec) {
        if (!this.map) {
            this.findMapPanel();
        }
        var value = this.getValue();
        if (Ext.isArray(value)) {
            var mapProj = this.map.getProjectionObject();
            delete this.center;
            delete this.locationFeature;
            if (value.length === 4) {
                this.map.zoomToExtent(
                    OpenLayers.Bounds.fromArray(value)
                        .transform(this.srs, mapProj)
                );
            } else {
                this.map.setCenter(
                    OpenLayers.LonLat.fromArray(value)
                        .transform(this.srs, mapProj),
                    Math.max(this.map.getZoom(), this.zoom)
                );
            }
            rec = rec[0];
            this.center = this.map.getCenter();
            var lonlat = rec.get(this.locationField);
            if (this.layer && lonlat) {
                var geom = new OpenLayers.Geometry.Point(
                    lonlat[0], lonlat[1]).transform(this.srs, mapProj);
                this.locationFeature = new OpenLayers.Feature.Vector(geom, rec.data);
                this.layer.addFeatures([this.locationFeature]);
            }
        }
    },

    /**
     * Remove the location marker from the `layer` and destroy the
     * `#locationFeature`.
     *
     * @private
     */
    removeLocationFeature: function() {
        if (this.locationFeature) {
            this.layer.destroyFeatures([this.locationFeature]);
        }
    },

    /**
     * Handler for the map's moveend event. Clears the selected location
     * when the map center has changed.
     *
     * @private
     */
    clearResult: function() {
        if (this.center && !this.map.getCenter().equals(this.center)) {
            this.clearValue();
        }
    },

    /**
     * Set the `#map` for this instance.
     *
     * @param {GeoExt.panel.Map/OpenLayers.Map} map
     * @private
     */
    setMap: function(map) {
        if (map instanceof GeoExt.panel.Map) {
            map = map.map;
        }
        this.map = map;
        map.events.on({
            "moveend": this.clearResult,
            "click": this.removeLocationFeature,
            scope: this
        });
    },

    /**
     * Called by a MapPanel if this component is one of the items in the panel.
     * @param {GeoExt.panel.Map} panel
     *
     * @private
     */
    addToMapPanel: Ext.emptyFn,

    /**
     * Unbind various event listeners and deletes #map, #layer and #center
     * properties.
     *
     * @private
     */
    beforeDestroy: function() {
        if (this.map && this.map.events) {
            this.map.events.un({
                "moveend": this.clearResult,
                "click": this.removeLocationFeature,
                scope: this
            });
        }
        this.removeLocationFeature();
        delete this.map;
        delete this.layer;
        delete this.center;
        this.callParent(arguments);
    }
});
