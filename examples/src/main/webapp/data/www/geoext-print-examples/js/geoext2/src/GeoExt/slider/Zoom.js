/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include OpenLayers/Util.js
 * @include GeoExt/panel/Map.js
 */

/**
 * Create a slider to control the zoom of a layer.
 * Important: Due to changes in OpenLayers 2.13, you need to set 
 * 'zoomMethod: null' on the map to avoid inconsistent zoom-Levels.
 * This is only needed when using 'aggressive: true' on the slider.
 * The issue has already been fixed in the current trunk of OpenLayers.
 *
 * Sample code to render a slider outside the map viewport:
 *
 * Example:
 *
 *     var slider = Ext.create('GeoExt.slider.Zoom', {
 *         renderTo: document.body,
 *         width: 200,
 *         map: map
 *     });
 *
 *  Sample code to add a slider to a map panel:
 *
 * Example:
 *
 *     var panel = Ext.create('GeoExt.panel.Map', {
 *         renderTo: document.body,
 *         height: 300,
 *         width: 400,
 *         map: {
 *             controls: [new OpenLayers.Control.Navigation()],
 *             maxResolution: 0.703125,
 *             zoomMethod: null
 *         },
 *         layers: [new OpenLayers.Layer.WMS(
 *             "Global Imagery",
 *             "http://maps.opengeo.org/geowebcache/service/wms",
 *             {layers: "bluemarble"}
 *         )],
 *         extent: [-5, 35, 15, 55],
 *         items: [{
 *             xtype: "gx_zoomslider",
 *             aggressive: true,
 *             vertical: true,
 *             height: 100,
 *             x: 10,
 *             y: 20
 *         }]
 *     });
 *
 * @class GeoExt.slider.Zoom
 */
Ext.define('GeoExt.slider.Zoom', {
    extend : 'GeoExt.slider.MapPanelItem',
    requires : [
        'GeoExt.panel.Map'
    ],
    alias : 'widget.gx_zoomslider',
    alternateClassName : 'GeoExt.ZoomSlider',

    /**
     * The map that the slider controls.
     *
     * @cfg {OpenLayers.Map/GeoExt.MapPanel} map
     */
    map: null,

    /**
     * The CSS class name for the slider elements.  Default is "gx-zoomslider".
     *
     * @cfg {String} baseCls
     */
    baseCls: "gx-zoomslider",

    /**
     * If set to true, the map is zoomed as soon as the thumb is moved. Otherwise
     * the map is zoomed when the thumb is released (default).
     *
     * @cfg {Boolean} aggressive
     */
    aggressive: false,

    /**
     * The slider position is being updated by itself (based on map zoomend).
     *
     * @property {Boolean} updating
     */
    updating: false,

    /**
     * The map is zoomed by the slider (based on map change/changecomplete).
     *
     * @property {Boolean} zooming
     */
    zooming: false,

    /**
     * Initialize the component.
     *
     * @private
     */
    initComponent: function(){
        this.callParent(arguments);

        if(this.map) {
            if(this.map instanceof GeoExt.MapPanel) {
                this.map = this.map.map;
            }
            this.bind(this.map);
        }

        if (this.aggressive === true) {
            this.on('change', this.changeHandler, this);
        } else {
            this.on('changecomplete', this.changeHandler, this);
        }
        this.on("beforedestroy", this.unbind, this);
    },

    /**
     * Override onRender to set base CSS class.
     *
     * @private
     */
    onRender: function() {
        this.callParent(arguments);
        this.el.addCls(this.baseCls);
    },

    /**
     * Override afterRender because the render event is fired too early to call
     * update.
     *
     * @private
     */
    afterRender : function(){
        this.callParent(arguments);
        this.update();
    },

    /**
     * The base class takes care of the rendered dimensions of the slider, and
     * we only additionally call #bind in the afterrender-event.
     *
     * @inheritdoc
     */
    addToMapPanel: function(panel) {
        var me = this;
        me.callParent(arguments);
        me.on({
            /**
             * Once we are rendered and we know that we are a child of a
             * mappanel, we bind our event handlers to the map.
             */
            afterrender: function(){
                // bind the map to the slider
                me.bind(panel.map);
            }
        });
    },

    /**
     * Registers the relevant listeners on the #map to be in sync with it.
     *
     * @param {OpenLayers.Map} map
     * @private
     */
    bind: function(map) {
        this.map = map;
        this.map.events.on({
            zoomend: this.update,
            changebaselayer: this.initZoomValues,
            scope: this
        });
        if(this.map.baseLayer) {
            this.initZoomValues();
            this.update();
        }
    },

    /**
     * Unregisters the bound listeners on the #map, e.g. when being destroyed.
     *
     * Will automatically be called from the inherited #removeFromMapPanel
     * method.
     *
     * @private
     */
    unbind: function() {
        if(this.map && this.map.events) {
            this.map.events.un({
                zoomend: this.update,
                changebaselayer: this.initZoomValues,
                scope: this
            });
        }
    },

    /**
     * Set the min/max values for the slider if not set in the config.
     *
     * @private
     */
    initZoomValues: function() {
        var layer = this.map.baseLayer;
        if(this.initialConfig.minValue === undefined) {
            this.minValue = layer.minZoomLevel || 0;
        }
        if(this.initialConfig.maxValue === undefined) {
            this.maxValue = layer.minZoomLevel == null ?
                layer.numZoomLevels - 1 : layer.maxZoomLevel;
        }
    },

    /**
     * Get the zoom level for the associated map based on the slider value.
     *
     * @return {Number} The map zoom level.
     */
    getZoom: function() {
        return this.getValue();
    },

    /**
     * Get the scale denominator for the associated map based on the slider
     * value.
     *
     * @return {Number} The map scale denominator.
     */
    getScale: function() {
        return OpenLayers.Util.getScaleFromResolution(
            this.map.getResolutionForZoom(this.getValue()),
            this.map.getUnits()
        );
    },

    /**
     * Get the resolution for the associated map based on the slider value.
     *
     * @return {Number} The map resolution.
     */
    getResolution: function() {
        return this.map.getResolutionForZoom(this.getValue());
    },

    /**
     * Registered as a listener for slider changecomplete. Zooms the map.
     *
     * @private
     */
    changeHandler: function() {
        if(this.map && !this.updating) {
            this.zooming = true;
            this.map.zoomTo(this.getValue());
        }
    },

    /**
     * Registered as a listener for map zoomend.Updates the value of the slider.
     *
     * @private
     */
    update: function() {
        if(this.rendered && this.map && !this.zooming) {
            this.updating = true;
            this.setValue(this.map.getZoom());
            this.updating = false;
        }
        this.zooming = false;
    }
});
