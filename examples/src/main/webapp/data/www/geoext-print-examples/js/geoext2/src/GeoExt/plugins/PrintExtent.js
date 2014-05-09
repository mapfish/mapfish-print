/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include OpenLayers/Layer/Vector.js
 * @include OpenLayers/Control/TransformFeature.js
 * @include GeoExt/data/PrintPage.js
 */

/**
 * Provides a way to show and modify the extents of print pages on the map. It
 * uses a layer to render the page extent and handle features of print pages,
 * and provides a control to modify them. Must be set as a plugin to a
 * {@link GeoExt.MapPanel}.
 *
 * Sample code to create a MapPanel with a PrintExtent, and print it
 * immediately:
 *
 *     var printExtent = Ext.create('GeoExt.plugins.PrintExtent', {
 *         printProvider: Ext.create('GeoExt.data.MapfishPrintProvider', {
 *             capabilities: printCapabilities
 *         })
 *     });
 *     var mapPanel = Ext.create('GeoExt.panel.Map', {
 *         border: false,
 *         renderTo: "div-id",
 *         layers: [new OpenLayers.Layer.WMS("Tasmania", "http://demo.opengeo.org/geoserver/wms",
 *             {layers: "topp:tasmania_state_boundaries"}, {singleTile: true})],
 *         center: [146.56, -41.56],
 *         zoom: 6,
 *         plugins: printExtent
 *     });
 *     printExtent.addPage();
 *     // print the map
 *     printExtent.print();
 *
 * @class GeoExt.plugins.PrintExtent
 */
Ext.define('GeoExt.plugins.PrintExtent', {
    mixins: {
        observable: 'Ext.util.Observable'
    },
    requires: ['GeoExt.data.PrintPage'],
    alias : 'widget.gx_printextent',
    alternateClassName : 'GeoExt.PrintExtent',

    /**
     * Holds the initial config object passed to the constructor.
     *
     * @private
     * @property {Object}
     */
    initialConfig: null,

    /**
     * The print provider this form is connected to. Optional if pages are
     * provided.
     *
     * @cfg {GeoExt.data.MapfishPrintProvider} printProvider
     */
    /**
     * The print provider this form is connected to. Read-only.
     *
     * @property {GeoExt.data.MapfishPrintProvider} printProvider
     */
    printProvider: null,

    /**
     * The map the layer and control are added to.
     *
     * @private
     * @property {OpenLayers.Map} map
     */
    map: null,

    /**
     * The layer used to render extent and handle features to. Optional, will
     * be created if not provided.
     *
     * @cfg {OpenLayers.Layer.Vector} layer
     */
    /**
     * The layer used to render extent and handle features to.
     *
     * @property {OpenLayers.Layer.Vector} layer
     * @private
     */
    layer: null,

    /**
     * Optional options for the `OpenLayers.Control.TransformFeature` control.
     *
     * @cfg {Object} transformFeatureOptions
     */
    transformFeatureOptions: null,

    /**
     * The control used to change extent, center, rotation and scale.
     *
     * @property {OpenLayers.Control.TransformFeature} control
     * @private
     */
    control: null,

    /**
     * The pages that this plugin controls. Optional.
     *
     * If not provided, it will be created with one page that is completely
     * contained within the visible map extent. All pages must use the same
     * PrintProvider.
     *
     * @cfg {GeoExt.data.PrintPage[]} pages
     */
    /**
     * The pages that this component controls. Read-only.
     *
     * @property {GeoExt.data.PrintPage[]} pages
     */
    pages: null,

    /**
     * The page currently set for transformation.
     *
     * @property {GeoExt.data.PrintPage} page
     */
    page: null,

    /**
     * Private constructor override.
     *
     * @private
     */
    constructor: function(config) {
        config = config || {};

        Ext.apply(this, config);
        this.initialConfig = config;

        if(!this.printProvider) {
            this.printProvider = this.pages[0].printProvider;
        }

        if(!this.pages) {
            this.pages = [];
        }

        /**
         * Triggered when a page has been selected using the control.
         *
         * Listener arguments:
         *
         * * printPage - {@link GeoExt.data.PrintPage} this printPage
         *
         * @event selectpage
         */


        this.callParent(arguments);
    },

    /**
     * Prints all pages as shown on the map.
     *
     * @param {Object} options Options to send to the PrintProvider's
     *     print method. See the GeoExt.data.MapfishPrintProvider
     *     {@link GeoExt.data.MapfishPrintProvider#method-print print method}.
     */
    print: function(options) {
        this.printProvider.print(this.map, this.pages, options);
    },

    /**
     * Initializes the plugin.
     *
     * @param {GeoExt.panel.Map} mapPanel
     * @private
     */
    init: function(mapPanel) {
        this.map = mapPanel.map;
        mapPanel.on("destroy", this.onMapPanelDestroy, this);

        if (!this.layer) {
            this.layer = new OpenLayers.Layer.Vector(null, {
                displayInLayerSwitcher: false
            });
        }
        this.createControl();

        for(var i=0, len=this.pages.length; i<len; ++i) {
            this.addPage(this.pages[i]);
        }
        this.show();
    },

    /**
     * Adds a page to the list of pages that this plugin controls.
     *
     * @param {GeoExt.data.PrintPage} page The page to add to this plugin.
     *     If not provided, a page that fits the current extent is created.
     * @return {GeoExt.data.PrintPage} page
     */
    addPage: function(page) {
        page = page || Ext.create('GeoExt.data.PrintPage', {
            printProvider: this.printProvider
        });
        if(Ext.Array.indexOf(this.pages, page) === -1) {
            this.pages.push(page);
        }
        this.layer.addFeatures([page.feature]);
        page.on("change", this.onPageChange, this);

        this.page = page;
        var map = this.map;
        if(map.getCenter()) {
            this.fitPage();
        } else {
            map.events.register("moveend", this, function() {
                map.events.unregister("moveend", this, arguments.callee);
                this.fitPage();
            });
        }
        return page;
    },

    /**
     * Removes a page from the list of pages that this plugin controls.
     *
     * @param {GeoExt.data.PrintPage} page The page to remove from this plugin.
     */
    removePage: function(page) {
        Ext.Array.remove(this.pages, page);
        if (page.feature.layer) {
            this.layer.removeFeatures([page.feature]);
        }
        page.un("change", this.onPageChange, this);
    },

    /**
     * Selects the given page (i.e. calls the setFeature on the modify feature
     * control)
     *
     * @param {GeoExt.data.PrintPage} page The page to select.
     */
    selectPage: function(page) {
        this.control.active && this.control.setFeature(page.feature);
        // FIXME raise the feature up so that it is on top
    },

    /**
     * Sets up the plugin, initializing the `OpenLayers.Layer.Vector`
     * layer and `OpenLayers.Control.TransformFeature`, and centering
     * the first page if no pages were specified in the configuration.
     */
    show: function() {
        this.map.addLayer(this.layer);
        this.map.addControl(this.control);
        this.control.activate();

        // if we have a page and if the map has a center then update the
        // transform box for that page, in case the transform control
        // was deactivated when fitPage (and therefore onPageChange)
        // was called.
        if (this.page && this.map.getCenter()) {
            this.updateBox();
        }
    },

    /**
     * Tears down the plugin, removes the `OpenLayers.Control.TransformFeature`
     * control and the `OpenLayers.Layer.Vector` layer.
     */
    hide: function() {
        // note: we need to be extra cautious when destroying OpenLayers
        // objects here (the tests will fail if we're not cautious anyway).
        // We use obj.events to test whether an OpenLayers object is
        // destroyed or not.

        var map = this.map, layer = this.layer, control = this.control;

        if(control && control.events) {
            control.deactivate();
            if(map && map.events && control.map) {
                map.removeControl(control);
            }
        }

        if(map && map.events && layer && layer.map) {
            map.removeLayer(layer);
        }
    },

    /**
     * When the mappanel is destroyed, we need to remove our pages etc.
     *
     * @private
     */
    onMapPanelDestroy: function() {

        var map = this.map;

        for(var len = this.pages.length - 1, i = len; i>=0; i--) {
            this.removePage(this.pages[i]);
        }

        this.hide();

        var control = this.control;
        if(map && map.events &&
           control && control.events) {
            control.destroy();
        }

        var layer = this.layer;
        if(!this.initialConfig.layer &&
           map && map.events &&
           layer && layer.events) {
            layer.destroy();
        }

        delete this.layer;
        delete this.control;
        delete this.page;
        this.map = null;
    },

    /**
     * Creates the OpenLayers.Control.TransformFeature control to interact with
     * the print extent. Registers the appropriate listeners to keep us in sync.
     *
     * @private
     */
    createControl: function() {
        this.control = new OpenLayers.Control.TransformFeature(this.layer, Ext.applyIf({
            preserveAspectRatio: true,
            eventListeners: {
                "beforesetfeature": function(e) {
                    for(var i=0, len=this.pages.length; i<len; ++i) {
                        if(this.pages[i].feature === e.feature) {
                            this.page = this.pages[i];
                            e.object.rotation = -this.pages[i].rotation;
                            break;
                        }
                    }
                },
                "setfeature": function(e) {
                    for(var i=0, len=this.pages.length; i<len; ++i) {
                        if(this.pages[i].feature === e.feature) {
                            this.fireEvent("selectpage", this.pages[i]);
                            break;
                        }
                    }
                },
                "beforetransform": function(e) {
                    this._updating = true;
                    var page = this.page;
                    if(e.rotation) {
                        if(this.printProvider.layout.get("rotation")) {
                            page.setRotation(-e.object.rotation);
                        } else {
                            e.object.setFeature(page.feature);
                        }
                    } else if(e.center) {
                        page.setCenter(OpenLayers.LonLat.fromString(
                            e.center.toShortString()
                        ));
                    } else {
                        page.fit(e.object.box, {mode: "closest"});
                        var minScale = this.printProvider.scales.getAt(0);
                        var maxScale = this.printProvider.scales.getAt(
                            this.printProvider.scales.getCount() - 1);
                        var boxBounds = e.object.box.geometry.getBounds();
                        var pageBounds = page.feature.geometry.getBounds();
                        var tooLarge = page.scale === minScale &&
                            boxBounds.containsBounds(pageBounds);
                        var tooSmall = page.scale === maxScale &&
                            pageBounds.containsBounds(boxBounds);
                        if(tooLarge === true || tooSmall === true) {
                            this.updateBox();
                        }
                    }
                    delete this._updating;
                    return false;
                },
                "transformcomplete": this.updateBox,
                scope: this
            }
        }, this.transformFeatureOptions));
    },

    /**
     * Fits the current print page to the map.
     *
     * @private
     */
    fitPage: function() {
        if(this.page) {
            this.page.fit(this.map, {mode: "screen"});
        }
    },

    /**
     * Updates the transformation box after setting a new scale or layout, or to
     * fit the box to the extent feature after a transform.
     *
     * @private
     */
    updateBox: function() {
        var page = this.page;
        this.control.active &&
            this.control.setFeature(page.feature, {rotation: -page.rotation});
    },

    /**
     * Handler for a page's change event.
     *
     * @private
     */
    onPageChange: function(page, mods) {
        if(!this._updating) {
            this.control.active &&
                this.control.setFeature(page.feature, {rotation: -page.rotation});
        }
    }
});
