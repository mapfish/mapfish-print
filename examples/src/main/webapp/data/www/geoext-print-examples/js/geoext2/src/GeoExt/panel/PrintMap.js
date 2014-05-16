/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include OpenLayers/Layer/Vector.js
 * @requires GeoExt/panel/Map.js
 * @include GeoExt/data/PrintProvider.js
 * @include GeoExt/data/PrintPage.js
 */

/**
 * A GeoExt.panel.Map that controls scale and center of a print page. Based on
 * the current view (i.e. layers and extent) of a source map, this panel will be
 * sized according to the aspect ratio of the print page. As the user zooms
 * and pans in the `GeoExt.PrintMapPanel`, the print page will update
 * its scale and center accordingly. If the scale on the print page changes
 * (e.g. by setting it using a combo box with a
 * {@link GeoExt.plugins.PrintPageField}), the extent of the
 * {@link GeoExt.PrintMapPanel} will be updated to match the page bounds.
 *
 * The #zoom, #center and #extent config options will have no effect, as
 * they will be determined by the #sourceMap.
 *
 * A map with a "Print..." button. If clicked, a dialog containing a
 * PrintMapPanel will open, with a "Create PDF" button.
 *
 * Example:
 *
 *     var mapPanel = Ext.create("GeoExt.panel.Map", {
 *         renderTo: "map",
 *         layers: [
 *             new OpenLayers.Layer.WMS(
 *                 "Tasmania State Boundaries",
 *                 "http://demo.opengeo.org/geoserver/wms",
 *                 { layers: "topp:tasmania_state_boundaries" },
 *                 { singleTile: true }
 *             )
 *         ],
 *         center: [146.56, -41.56],
 *         zoom: 6,
 *         bbar: [{
 *             text: "Print...",
 *             handler: function() {
 *                 var printDialog = Ext.create("Ext.Window", {
 *                     autoHeight: true,
 *                     width: 350,
 *                     items: [
 *                         Ext.create("GeoExt.panel.PrintMap", {
 *                             sourceMap: mapPanel,
 *                             printProvider: {
 *                                 capabilities: printCapabilities
 *                             }
 *                         })
 *                     ],
 *                     bbar: [{
 *                         text: "Create PDF",
 *                         handler: function() {
 *                             printDialog.items.get(0).print();
 *                         }
 *                     }]
 *                 });
 *                 printDialog.show();
 *             }
 *         }]
 *     });
 *
 * @class GeoExt.panel.PrintMap
 */
Ext.define('GeoExt.panel.PrintMap', {
    extend : 'GeoExt.panel.Map',
    requires: [
        'Ext.data.Store',
        'GeoExt.data.MapfishPrintProvider',
        'GeoExt.data.PrintPage'
    ],
    alias : 'widget.gx_printmappanel',
    alternateClassName : 'GeoExt.PrintMapPanel',

    /**
     * Optional configuration for the `OpenLayers.Map` object
     * that this PrintMapPanel creates. Useful e.g. to configure a map with a
     * custom set of controls, or to add a `preaddlayer` listener for
     * filtering out layer types that cannot be printed.
     *
     * @cfg {Object} map
     */

    /**
     * The map that is to be printed.
     *
     * @cfg {GeoExt.MapPanel/OpenLayers.Map} sourceMap
     */

    /**
     * @private
     * @property {OpenLayers.Map} sourceMap
     */
    sourceMap: null,

    /**
     * @cfg {GeoExt.data.MapfishPrintProvider/Object} printProvider
     * PrintProvider to use for printing. If an `Object` is provided, a new
     * PrintProvider will be created and configured with the object.
     *
     * The PrintMapPanel requires the printProvider's capabilities
     * to be available upon initialization. This means that a PrintMapPanel
     * configured with an `Object` as `printProvider` will only work
     * when `capabilities` is provided in the printProvider's
     * configuration object. If `printProvider` is provided as an instance
     * of {@link GeoExt.data.MapfishPrintProvider}, the capabilities must be
     * loaded before PrintMapPanel initialization.
     */

    /**
     * PrintProvider for this PrintMapPanel.
     *
     * @property {GeoExt.data.MapfishPrintProvider} printProvider
     */
    printProvider: null,

    /**
     * PrintPage for this PrintMapPanel. Read-only.
     *
     * @property {GeoExt.data.PrintPage} printPage
     */
    printPage: null,

    /**
     * If set to true, the printPage cannot be set to scales that
     * would generate a preview in this {@link GeoExt.PrintMapPanel} with a
     * completely different extent than the one that would appear on the
     * printed map. Default is false.
     *
     * @cfg {Boolean} limitScales
     */

    /**
     * A data store with a subset of the printProvider's scales. By default,
     * this contains all the scales of the printProvider.
     *
     * If `limitScales` is set to true, it will only contain print scales
     * that can properly be previewed with this :class:`GeoExt.PrintMapPanel`.
     *
     * @property {Ext.data.Store} previewScales
     */
    previewScales: null,

    /**
     * A location for the map center. **Do not set**, as this will be overridden
     * with the `sourceMap` center.
     *
     * @cfg {OpenLayers.LonLat/Number[]} center
     */
    center: null,

    /**
     * An initial zoom level for the map. **Do not set**, because the initial
     * extent will be determined by the `sourceMap`.
     *
     * @cfg {Number} zoom
     */
    zoom: null,

    /**
     * An initial extent for the map. **Do not set**, because the initial extent
     * will be determined by the `sourceMap`.
     *
     * @cfg {OpenLayers.Bounds/Number[]} extent
     */
    extent: null,

    /**
     * @private
     * @property {Number} currentZoom
     */
    currentZoom: null,

    /**
     * @private
     */
    initComponent: function() {
        if(this.sourceMap instanceof GeoExt.MapPanel) {
            this.sourceMap = this.sourceMap.map;
        }

        if (!this.map) {
            this.map = {};
        }
        Ext.applyIf(this.map, {
            projection: this.sourceMap.getProjection(),
            maxExtent: this.sourceMap.getMaxExtent(),
            maxResolution: this.sourceMap.getMaxResolution(),
            units: this.sourceMap.getUnits(),
            tileManager: null,
            allOverlays: true,
            fallThrough: true
        });
        if(!(this.printProvider instanceof GeoExt.data.MapfishPrintProvider)) {
            this.printProvider = Ext.create('GeoExt.data.MapfishPrintProvider',
                this.printProvider);
        }
        this.printPage = Ext.create('GeoExt.data.PrintPage', {
            printProvider: this.printProvider
        });

        this.previewScales = Ext.create('Ext.data.Store', {
            fields: [
                 {name: 'name', type: 'string'},
                 {name: 'value', type: 'int'}
            ],
            data: this.printProvider.scales.getRange()
        });

        this.layers = [];
        var layer;
        Ext.each(this.sourceMap.layers, function(layer) {
            if (layer.getVisibility() === true) {
                this.layers.push(layer.clone());
            }
        }, this);

        this.extent = this.sourceMap.getExtent();
        this.callParent(arguments);
    },

    /**
     * Calls the internal adjustSize-function and resizes
     * this {@link GeoExt.panel.PrintMap PrintMapPanel} due
     * to the needed size, defined by the current layout of the #printProvider.
     *
     * The Function was removed from Ext.Panel in ExtJS 4 and is
     * now implemented here.
     *
     * @private
     *
     */
    syncSize: function() {
        var s = this.adjustSize(this.map.size.w, this.map.size.h);
        this.setSize(s.width, s.height);
    },

    /**
     * Register various event listeners.
     *
     * @private
     */
    bind: function() {
        // we have to call syncSize here because of changed
        // rendering order in ExtJS4
        this.syncSize();

        this.printPage.on("change", this.fitZoom, this);
        this.printProvider.on("layoutchange", this.syncSize, this);
        this.map.events.register("moveend", this, this.updatePage);
        this.on("resize", function() {
            this.doComponentLayout();
            this.map.updateSize();
        }, this);

        this.printPage.fit(this.sourceMap);

        if (this.initialConfig.limitScales === true) {
            this.on("resize", this.calculatePreviewScales, this);
            this.calculatePreviewScales();
        }
    },

    /**
     * Private method called after the panel has been rendered.
     *
     * @private
     */
    afterRender: function() {
        var me = this,
            listenerSpec = {
                "afterlayout": {
                    fn: me.bind,
                    scope: me,
                    single: true
                }
            };

        me.callParent(arguments);
        me.doComponentLayout();

        // binding will happen when either we or our container are finished
        // doing the layout.
        if (!me.ownerCt) {
            me.on(listenerSpec);
        } else {
            me.ownerCt.on(listenerSpec);
        }
    },

    /**
     * Private override - sizing this component always takes the aspect ratio
     * of the print page into account.
     *
     * @param {Number} width If not provided or 0, initialConfig.width will
     *     be used.
     * @param {Number} height If not provided or 0, initialConfig.height
     *     will be used.
     * @private
     */
    adjustSize: function(width, height) {
        var printSize = this.printProvider.layout.get("size");
        var ratio = printSize.width / printSize.height;
        // respect width & height when sizing according to the print page's
        // aspect ratio - do not exceed either, but don't take values for
        // granted if container is configured with autoWidth or autoHeight.
        var ownerCt = this.ownerCt;
        var targetWidth = (ownerCt && ownerCt.autoWidth) ? 0 :
            (width || this.initialConfig.width);
        var targetHeight = (ownerCt && ownerCt.autoHeight) ? 0 :
            (height || this.initialConfig.height);
        if (targetWidth) {
            height = targetWidth / ratio;
            if (targetHeight && height > targetHeight) {
                height = targetHeight;
                width = height * ratio;
            } else {
                width = targetWidth;
            }
        } else if (targetHeight) {
            width = targetHeight * ratio;
            height = targetHeight;
        }

        return {width: width, height: height};
    },

    /**
     * Fits this PrintMapPanel's zoom to the print scale.
     *
     * @private
     */
    fitZoom: function() {
        if (!this._updating && this.printPage.scale) {
            this._updating = true;
            var printBounds = this.printPage.getPrintExtent(this.map);
            this.currentZoom = this.map.getZoomForExtent(printBounds);
            this.map.zoomToExtent(printBounds, false);

            delete this._updating;
        }
    },

    /**
     * Updates the print page to match this PrintMapPanel's center and scale.
     *
     * @private
     */
    updatePage: function() {
        if (!this._updating) {
            var zoom = this.map.getZoom();
            this._updating = true;
            if (zoom === this.currentZoom) {
                this.printPage.setCenter(this.map.getCenter());
            } else {
                this.printPage.fit(this.map);
            }
            delete this._updating;
            this.currentZoom = zoom;
        }
    },

    /**
     * Recalculates all preview scales. This is e.g. needed when the size
     * changes.
     *
     * @private
     */
    calculatePreviewScales: function() {
        this.previewScales.removeAll();

        this.printPage.suspendEvents();
        var scale = this.printPage.scale;

        // group print scales by the zoom level they would be previewed at
        var viewSize = this.map.getSize();
        var scalesByZoom = {};
        var zooms = [];
        this.printProvider.scales.each(function(rec) {
            this.printPage.setScale(rec);
            var extent = this.printPage.getPrintExtent(this.map);
            var zoom = this.map.getZoomForExtent(extent);

            var idealResolution = Math.max(
                extent.getWidth() / viewSize.w,
                extent.getHeight() / viewSize.h
            );
            var resolution = this.map.getResolutionForZoom(zoom);
            // the closer to the ideal resolution, the better the fit
            var diff = Math.abs(idealResolution - resolution);
            if (!(zoom in scalesByZoom) || scalesByZoom[zoom].diff > diff) {
                scalesByZoom[zoom] = {
                    rec: rec,
                    diff: diff
                };
                if (Ext.Array.indexOf(zooms, zoom) === -1) {
                    zooms.push(zoom);
                }
            }
        }, this);

        // add only the preview scales that closely fit print extents
        for (var i=0, ii=zooms.length; i<ii; ++i) {
            this.previewScales.add(scalesByZoom[zooms[i]].rec);
        }

        scale && this.printPage.setScale(scale);
        this.printPage.resumeEvents();

        if (scale && this.previewScales.getCount() > 0) {
            var maxScale = this.previewScales.getAt(0);
            var minScale = this.previewScales.getAt(this.previewScales.getCount()-1);
            if (scale.get("value") < minScale.get("value")) {
                this.printPage.setScale(minScale);
            } else if (scale.get("value") > maxScale.get("value")) {
                this.printPage.setScale(maxScale);
            }
        }

        this.fitZoom();
    },

    /**
     * Convenience method for printing the map, without the need to interact
     * with the printProvider and printPage.
     *
     * @param {Object} options options for
     *     the {@link GeoExt.data.MapfishPrintProvider#method-print print}
     *     method.
     *
     */
    print: function(options) {
        this.printProvider.print(this.map, [this.printPage], options);
    },

    /**
     * Private method called during the destroy sequence.
     *
     * @private
     */
    beforeDestroy: function() {
        this.map.events.unregister("moveend", this, this.updatePage);
        this.printPage.un("change", this.fitZoom, this);
        this.printProvider.un("layoutchange", this.syncSize, this);

        this.callParent(arguments);
    }
});
