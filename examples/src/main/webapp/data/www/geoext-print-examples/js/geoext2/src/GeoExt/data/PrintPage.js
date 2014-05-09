/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include OpenLayers/Geometry/Polygon.js
 * @include OpenLayers/Geometry/LinearRing.js
 * @include OpenLayers/Geometry/Point.js
 * @include OpenLayers/Feature/Vector.js
 * @include GeoExt/panel/Map.js
 */

/**
 * Provides a representation of a print page for
 * {@link GeoExt.data.MapfishPrintProvider}. The extent of the page is stored as
 * `OpenLayers.Feature.Vector`. Widgets can use this to display the print
 * extent on the map.
 *
 * @class GeoExt.data.PrintPage
 */
Ext.define('GeoExt.data.PrintPage', {
    extend: 'Ext.util.Observable',
    requires: [
        'GeoExt.panel.Map'
    ],

    /**
     * The print provider to use with this page.
     *
     * @cfg {GeoExt.data.MapfishPrintProvider} printProvider
     */

    /**
     * @property {GeoExt.data.MapfishPrintProvider} printProvider
     * @private
     */
    printProvider: null,

    /**
     * Feature representing the page extent. To get the extent of the print page
     * for a specific map, use #getPrintExtent. Read-only.
     *
     * @property {OpenLayers.Feature.Vector} feature
     */
    feature: null,

    /**
     * The current center of the page. Read-only.
     *
     * @property {OpenLayers.LonLat} center
     */
    center: null,

    /**
     * The current scale record of the page. Read-only.
     *
     * @property {Ext.data.Record} scale
     */
    scale: null,

    /**
     * The current rotation of the page. Read-only.
     *
     * @property {Float} rotation
     */
    rotation: 0,

    /**
     * Key-value pairs of additional parameters that the printProvider will send
     * to the print service for this page.
     *
     * @cfg {Object} customParams
     */

    /**
     * Key-value pairs of additional parameters that the printProvider will send
     * to the print service for this page.
     *
     * @property {Object} customParams
     */
    customParams: null,

    /**
     * Private constructor override.
     *
     * @private
     */
    constructor: function(config) {
        this.initialConfig = config;
        Ext.apply(this, config);

        if(!this.customParams) {
            this.customParams = {};
        }

        /**
         * Triggered when any of the page properties have changed
         *
         * Listener arguments:
         *
         *  * printPage - {@link GeoExt.data.PrintPage} this printPage.
         *  * modifications - {Object} Object with one or more of
         *      `scale`, `center` and `rotation`, notifying
         *      listeners of the changed properties.
         *
         * @event change
         */

        this.callParent(arguments);

        this.feature = new OpenLayers.Feature.Vector(
            new OpenLayers.Geometry.Polygon([
                new OpenLayers.Geometry.LinearRing([
                    new OpenLayers.Geometry.Point(-1, -1),
                    new OpenLayers.Geometry.Point(1, -1),
                    new OpenLayers.Geometry.Point(1, 1),
                    new OpenLayers.Geometry.Point(-1, 1)
                ])
            ])
        );

        if(this.printProvider.capabilities) {
            this.setScale(this.printProvider.scales.getAt(0));
        } else {
            this.printProvider.on({
                "loadcapabilities": function() {
                    this.setScale(this.printProvider.scales.getAt(0));
                },
                scope: this,
                single: true
            });
        }
        this.printProvider.on({
            "layoutchange": this.onLayoutChange,
            scope: this
        });
    },

    /**
     * Gets this page's print extent for the provided map.
     *
     * @param {OpenLayers.Map/GeoExt.MapPanel} map The map to get the print
     *     extent for.
     * @return {OpenLayers.Bounds}
     */
    getPrintExtent: function(map) {
        map = map instanceof GeoExt.MapPanel ? map.map : map;
        return this.calculatePageBounds(this.scale, map.getUnits());
    },

    /**
     *
     * Updates the page geometry to match a given scale. Since this takes the
     * current layout of the printProvider into account, this can be used to
     * update the page geometry feature when the layout has changed.
     *
     * @param {Ext.data.Record} scale The new scale record.
     * @param {String} units map units to use for the scale calculation.
     *     Optional if the `feature` is on a layer which is added to a map.
     *     If not found, "dd" will be assumed.
     */
    setScale: function(scale, units) {

        var bounds = this.calculatePageBounds(scale, units);
        var geom = bounds.toGeometry();
        var rotation = this.rotation;
        if(Ext.isDefined(rotation) && rotation != 0) {
            geom.rotate(-rotation, geom.getCentroid());
        }
        this.updateFeature(geom, {scale: scale});
    },

    /**
     * Moves the page extent to a new center.
     *
     * @param {OpenLayers.LonLat} center The new center.
     */
    setCenter: function(center) {
        var geom = this.feature.geometry;
        var oldCenter = geom.getBounds().getCenterLonLat();

        var dx = center.lon - oldCenter.lon;
        var dy = center.lat - oldCenter.lat;
        geom.move(dx, dy);
        this.updateFeature(geom, {center: center});
    },

    /**
     * Sets a new rotation for the page geometry.
     *
     * @param {Float} rotation The new rotation.
     * @param {Boolean} force If set to true, the rotation will also be
     *     set when the layout does not support it. Default is false.
     */
    setRotation: function(rotation, force) {
        if(force || this.printProvider.layout.get("rotation") === true) {
            var geom = this.feature.geometry;
            geom.rotate(this.rotation - rotation, geom.getCentroid());
            this.updateFeature(geom, {rotation: rotation});
        }
    },

    /**
     * Fits the page layout to a map or feature extent. If the map extent has
     * not been centered yet, this will do nothing.
     *
     * Available options:
     *
     * * mode - `String` [closest/printer/screen] How to calculate the print
     *     extent? If `closest` the closest matching print extent will be
     *     chosen. If `printer`, the chosen print extent will be the closest one
     *     that can show the entire `fitTo` extent on the printer. If `screen`,
     *     it will be the closest one that is entirely visible inside the
     *     `fitTo` extent. Default is `printer`.
     *
     *  @param {GeoExt.MapPanel/OpenLayers.Map/OpenLayers.Feature.Vector} fitTo
     *      The map or feature to fit the page to.
     *  @param {Object} options Additional options to determine how to fit
     *
     */
    fit: function(fitTo, options) {
        options = options || {};

        var map = fitTo, extent;
        if(fitTo instanceof GeoExt.panel.Map) {
            map = fitTo.map;
        } else if(fitTo instanceof OpenLayers.Feature.Vector) {
            map = fitTo.layer.map;
            extent = fitTo.geometry.getBounds();
        }
        if(!extent) {
            extent = map.getExtent();
            if(!extent) {
                return;
            }
        }
        this._updating = true;
        var center = extent.getCenterLonLat();
        this.setCenter(center);
        var units = map.getUnits();
        var scale = this.printProvider.scales.getAt(0);
        var closest = Number.POSITIVE_INFINITY;
        var mapWidth = extent.getWidth();
        var mapHeight = extent.getHeight();

        this.printProvider.scales.each(function(rec) {
            var bounds = this.calculatePageBounds(rec, units);

            if (options.mode == "closest") {

                var diff =
                    Math.abs(bounds.getWidth() - mapWidth) +
                    Math.abs(bounds.getHeight() - mapHeight);
                if (diff < closest) {
                    closest = diff;
                    scale = rec;
                }
            } else {
                var contains = options.mode == "screen" ?
                    !extent.containsBounds(bounds) :
                    bounds.containsBounds(extent);
                if (contains || (options.mode == "screen" && !contains)) {
                    scale = rec;
                }

                return contains;
            }
        }, this);

        this.setScale(scale, units);
        delete this._updating;
        this.updateFeature(this.feature.geometry, {
            center: center,
            scale: scale
        });
    },

    /**
     * Updates the page feature with a new geometry and notifies listeners
     * of changed page properties.
     *
     * @param {OpenLayers.Geometry} geometry New geometry for the #feature.
     *     If not provided, the existing geometry will be left unchanged.
     * @param {Object} mods An object with one or more of #scale,
     *     #center and #rotation, reflecting the page properties to update.
     * @private
     */
    updateFeature: function(geometry, mods) {
        var f = this.feature;
        var modified = f.geometry !== geometry;
        geometry.id = f.geometry.id;
        f.geometry = geometry;

        if(!this._updating) {
            for(var property in mods) {
                if(mods[property] === this[property]) {
                    delete mods[property];
                } else {
                    this[property] = mods[property];
                    modified = true;
                }
            }
            Ext.apply(this, mods);

            f.layer && f.layer.drawFeature(f);
            modified && this.fireEvent("change", this, mods);
        }
    },

    /**
     * Calculates the page bounds for a given scale.
     *
     * @param {Ext.data.Record} scale Scale record to calculate the page
     *     bounds for.
     * @param {String} units Map units to use for the scale calculation.
     *     Optional if #feature is added to a layer which is added to a
     *     map. If not provided, "dd" will be assumed.
     * @return `OpenLayers.Bounds`
     * @private
     */
    calculatePageBounds: function(scale, units) {
        var s = scale.get("value");
        var f = this.feature;
        var geom = this.feature.geometry;
        var center = geom.getBounds().getCenterLonLat();

        var size = this.printProvider.layout.get("size");

        var units = units ||
            (f.layer && f.layer.map && f.layer.map.getUnits()) ||
            "dd";
        var unitsRatio = OpenLayers.INCHES_PER_UNIT[units];
        var w = size.width / 72 / unitsRatio * s / 2;
        var h = size.height / 72 / unitsRatio * s / 2;

        return new OpenLayers.Bounds(center.lon - w, center.lat - h,
            center.lon + w, center.lat + h);
    },

    /**
     * Handler for the printProvider's layoutchange event.
     *
     * @private
     */
    onLayoutChange: function() {
        if(this.printProvider.layout.get("rotation") === false) {
            this.setRotation(0, true);
        }
        // at init time the print provider triggers layoutchange
        // before loadcapabilities, i.e. before we set this.scale
        // to the first scale in the scales store, we need to
        // guard against that
        if (this.scale) {
            this.setScale(this.scale);
        }
    },

    /**
     * Unbinds layoutchange listener of the #printProvider.
     *
     * @private
     */
    destroy: function() {
        this.printProvider.un("layoutchange", this.onLayoutChange, this);
    }

});
