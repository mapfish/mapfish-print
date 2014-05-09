/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @requires OpenLayers/Feature/Vector.js
 * @include OpenLayers/Geometry/Point.js
 * @include OpenLayers/Geometry/LinearRing.js
 * @include OpenLayers/Geometry/Polygon.js
 * @include OpenLayers/Geometry/LineString.js
 * @include OpenLayers/Renderer/SVG.js
 */

/**
 * The feature renderer
 *
 * @class GeoExt.FeatureRenderer
 */
Ext.define('GeoExt.FeatureRenderer', {
    extend: 'Ext.Component',
    alias: 'widget.gx_renderer',
    requires: [
        'GeoExt.Version'
    ],

    /**
     * Optional vector to be drawn.  If not provided, and if `symbolizers`
     * is configured with an array of plain symbolizer objects, `symbolType`
     * should be configured.
     *
     * @cfg {OpenLayers.Feature.Vector}
     */
    feature: undefined,

    /**
     * An array of `OpenLayers.Symbolizer` instances or plain symbolizer
     * objects (in painters order) for rendering a  feature.  If no
     * symbolizers are provided, the OpenLayers default will be used. If a
     * symbolizer is an instance of `OpenLayers.Symbolizer`, its type will
     * override the symbolType for rendering.
     *
     * @cfg {Object[]}
     */
    symbolizers: [OpenLayers.Feature.Vector.style["default"]],

    /**
     * One of `"Point"`, `"Line"`, `"Polygon"` or `"Text"`.  Only
     * pertinent if OpenLayers.Symbolizer objects are not used.  If `feature`
     * is provided, it will be preferred.
     *
     * @cfg {String}
     */
    symbolType: "Polygon",

    /**
     * The resolution for the renderer.
     *
     * @property {Number}
     * @private
     */
    resolution: 1,

    /**
     * @property {Number}
     * @private
     */
    minWidth: 20,

    /**
     * @property {Number}
     * @private
     */
    minHeight: 20,

    /**
     * List of supported Renderer classes. Add to this list to add support for
     * additional renderers. The first renderer in the list that returns
     * `true` for the `supported` method will be used, if not defined in
     * the `renderer` config property.
     *
     * @property {String[]}
     * @private
     */
    renderers: ["SVG", "VML", "Canvas"],

    /**
     * Options for the renderer. See `OpenLayers.Renderer` for supported
     * options.
     *
     * @property {Object}
     * @private
     */
    rendererOptions: null,

    /**
     * Feature with point geometry.
     *
     * @property {OpenLayers.Feature.Vector}
     * @private
     */
    pointFeature: undefined,

    /**
     * Feature with LineString geometry.  Default zig-zag is provided.
     *
     * @property {OpenLayers.Feature.Vector}
     * @private
     */
    lineFeature: undefined,

    /**
     * Feature with Polygon geometry.  Default is a soft cornered rectangle.
     *
     * @property {OpenLayers.Feature.Vector}
     * @private
     */
    polygonFeature: undefined,

    /**
     * Feature with invisible Point geometry and text label.
     *
     * @property {OpenLayers.Feature.Vector}
     * @private
     */
    textFeature: undefined,

    /**
     * @private
     * @property {OpenLayers.Renderer}
     */
    renderer: null,

    initComponent: function(){
        var me = this;

        this.autoEl = {
            tag: "div",
            "class": (this.imgCls ? this.imgCls : ""),
            id: this.getId()
        };
        me.callParent(arguments);

        Ext.applyIf(this, {
            pointFeature: new OpenLayers.Feature.Vector(
                new OpenLayers.Geometry.Point(0, 0)
                ),
            lineFeature: new OpenLayers.Feature.Vector(
                new OpenLayers.Geometry.LineString([
                    new OpenLayers.Geometry.Point(-8, -3),
                    new OpenLayers.Geometry.Point(-3, 3),
                    new OpenLayers.Geometry.Point(3, -3),
                    new OpenLayers.Geometry.Point(8, 3)
                    ])
                ),
            polygonFeature: new OpenLayers.Feature.Vector(
                new OpenLayers.Geometry.Polygon([
                    new OpenLayers.Geometry.LinearRing([
                        new OpenLayers.Geometry.Point(-8, -4),
                        new OpenLayers.Geometry.Point(-6, -6),
                        new OpenLayers.Geometry.Point(6, -6),
                        new OpenLayers.Geometry.Point(8, -4),
                        new OpenLayers.Geometry.Point(8, 4),
                        new OpenLayers.Geometry.Point(6, 6),
                        new OpenLayers.Geometry.Point(-6, 6),
                        new OpenLayers.Geometry.Point(-8, 4)
                        ])
                    ])
                ),
            textFeature: new OpenLayers.Feature.Vector(
                new OpenLayers.Geometry.Point(0, 0)
            )
        });
        if(!this.feature) {
            this.setFeature(null, {
                draw: false
            });
        }
        this.addEvents(
            /**
             * Fires when the feature is clicked on.
             *
             * Listener arguments:
             *
             *  * renderer - GeoExt.FeatureRenderer This feature renderer.
             *
             * @event
             */
            "click"
        );
    },

    /**
     * (Re-)Initializes our custom event listeners, mainly #onClick.
     *
     * @private
     */
    initCustomEvents: function() {
        this.clearCustomEvents();
        this.el.on("click", this.onClick, this);

    },

    /**
     * Unbinds previously bound listeners on #el.
     *
     * @private
     */
    clearCustomEvents: function() {
        if (this.el && this.el.removeAllListeners) {
            this.el.removeAllListeners();
        }
    },

    /**
     * Bound to the click event on the #el, this fires the click event.
     *
     * @private
     */
    onClick: function() {
        this.fireEvent("click", this);
    },

    /**
     * When we are rendered, we setup our own DOM structure and eventually
     * call #drawFeature.
     *
     * @private
     */
    onRender: function(ct, position) {
        if(!this.el) {
            this.el = document.createElement("div");
            this.el.id = this.getId();
//            document.body.appendChild(this.el);

        }
        if(!this.renderer || !this.renderer.supported()) {
            this.assignRenderer();
        }
        // monkey-patch renderer so we always get a resolution
        this.renderer.map = {
            //            getResolution: (function() {
            //                return this.resolution;
            //            }).createDelegate(this)
            getResolution: Ext.Function.bind(function() {
                return this.resolution;
            }, this)
        };
        this.callParent(arguments);
        this.drawFeature();
    },

    /**
     * After rendering we setup our own custom events using #initCustomEvents.
     *
     * @private
     */
    afterRender: function() {
        this.callParent(arguments);
        this.initCustomEvents();
    },

    /**
     * When resizing has happened, we might need to re-set the renderer's
     * dimensions via #setRendererDimensions.
     *
     * @private
     */
    onResize: function(w, h) {
        this.setRendererDimensions();
        this.callParent(arguments);
    },

    /**
     * Sets the dimensions of the renderer according to the #feature geometry
     * and our own dimensions.
     *
     * @private
     */
    setRendererDimensions: function() {
        var gb = this.feature.geometry.getBounds();
        var gw = gb.getWidth();
        var gh = gb.getHeight();
        /*
         * Determine resolution based on the following rules:
         * 1) always use value specified in config
         * 2) if not specified, use max res based on width or height of element
         * 3) if no width or height, assume a resolution of 1
         */
        var resolution = this.initialConfig.resolution;
        if(!resolution) {
            resolution = Math.max(gw / this.width || 0, gh / this.height || 0) || 1;
        }
        this.resolution = resolution;
        // determine height and width of element
        var width = Math.max(this.width || this.minWidth, gw / resolution);
        var height = Math.max(this.height || this.minHeight, gh / resolution);
        // determine bounds of renderer
        var center = gb.getCenterPixel();
        var bhalfw = width * resolution / 2;
        var bhalfh = height * resolution / 2;
        var bounds = new OpenLayers.Bounds(
            center.x - bhalfw, center.y - bhalfh,
            center.x + bhalfw, center.y + bhalfh
            );
        this.renderer.setSize(new OpenLayers.Size(Math.round(width), Math.round(height)));
        this.renderer.setExtent(bounds, true);
    },

    /**
     * Iterate through the available renderer implementations and selects
     * and assign the first one whose `supported` method returns `true`.
     *
     * @private
     */
    assignRenderer: function()  {
        for(var i=0, len=this.renderers.length; i<len; ++i) {
            var Renderer = OpenLayers.Renderer[this.renderers[i]];
            if(Renderer && Renderer.prototype.supported()) {
                this.renderer = new Renderer(
                    this.el, this.rendererOptions
                    );
                break;
            }
        }
    },

    /**
     * Update the symbolizers used to render the feature.
     *
     * @param symbolizers {Object[]} An array of symbolizers
     * @param options {Object}
     * @param options.draw {Boolean} Draw the feature after setting it.
     *     Default is `true`.
     */
    setSymbolizers: function(symbolizers, options) {
        this.symbolizers = symbolizers;
        if(!options || options.draw) {
            this.drawFeature();
        }
    },

    /**
     * Create a new feature based on the geometry type and render it.
     *
     * @param type {String} One of the `symbolType` strings.
     * @param options {Object}
     * @param options.draw {Boolean} Draw the feature after setting it.
     *     Default is `true`.
     *
     */
    setSymbolType: function(type, options) {
        this.symbolType = type;
        this.setFeature(null, options);
    },

    /**
     * Update the feature and redraw.
     *
     * @param feature {OpenLayers.Feature.Vector} The feature to be rendered.
     *     If none is provided, one will be created based on `symbolType`.
     * @param options {Object}
     * @param options.draw {Boolean} Draw the feature after setting it.
     *     Default is `true`.
     *
     */
    setFeature: function(feature, options) {
        this.feature = feature || this[this.symbolType.toLowerCase() + "Feature"];
        if(!options || options.draw) {
            this.drawFeature();
        }
    },

    /**
     * Render the feature with the symbolizers.
     *
     * @private
     */
    drawFeature: function() {
        this.renderer.clear();
        this.setRendererDimensions();
        var symbolizer, feature, geomType;
        for (var i=0, len=this.symbolizers.length; i<len; ++i) {
            symbolizer = this.symbolizers[i];
            feature = this.feature;
            if (symbolizer instanceof OpenLayers.Symbolizer) {
                symbolizer = symbolizer.clone();
                if (OpenLayers.Symbolizer.Text &&
                    symbolizer instanceof OpenLayers.Symbolizer.Text &&
                    symbolizer.graphic === false) {
                        // hide the point geometry
                        symbolizer.fill = symbolizer.stroke = false;
                }
                if (!this.initialConfig.feature) {
                    geomType = symbolizer.CLASS_NAME.split(".").pop().toLowerCase();
                    feature = this[geomType + "Feature"];
                }
            } else {
                // TODO: remove this when OpenLayers.Symbolizer is used everywhere
                symbolizer = Ext.apply({}, symbolizer);
            }
            this.renderer.drawFeature(
                feature.clone(),
                symbolizer
            );
        }
    },

    /**
     * Update the `symbolType` or `feature` and `symbolizer` and redraw
     * the feature.
     *
     * Valid options:
     *
     * @param options {Object} Object with properties to be updated.
     * @param options.feature {OpenLayers.Feature.Vector} The new or updated
     *     feature. If provided, the feature gets precedence over `symbolType`.
     * @param options.symbolType {String} One of the allowed `symbolType`
     *     values.
     * @param options.symbolizers {Object[]} An array of symbolizer objects.
     */
    update: function(options) {
        options = options || {};
        if(options.feature) {
            this.setFeature(options.feature, {
                draw: false
            });
        } else if(options.symbolType) {
            this.setSymbolType(options.symbolType, {
                draw: false
            });
        }
        if(options.symbolizers) {
            this.setSymbolizers(options.symbolizers, {
                draw: false
            });
        }
        this.drawFeature();
    },

    /**
     * Private method called during the destroy sequence.
     *
     * @private
     */
    beforeDestroy: function() {
        this.clearCustomEvents();
        if (this.renderer) {
            this.renderer.destroy();
        }
    }
});


