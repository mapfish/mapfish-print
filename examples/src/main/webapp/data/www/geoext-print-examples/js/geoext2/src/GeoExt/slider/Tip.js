/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/**
 * Create a slider tip displaying `Ext.slider.SingleSlider` values over slider
 * thumbs.
 *
 * Example:
 *
 *     var slider = Ext.create('GeoExt.slider.Zoom', {
 *         map: panel.map,
 *         aggressive: true,
 *         width: 200,
 *         plugins: Ext.create('GeoExt.slider.Tip', {
 *             getText: function(thumb) {
 *                 return Ext.String.format(
 *                     '<div>Scale: 1:{0}</div>',
 *                     thumb.slider.getScale()
 *                 );
 *             }
 *         }),
 *         renderTo: Ext.getBody()
 *     });
 *
 * @class GeoExt.slider.Tip
 */
Ext.define('GeoExt.slider.Tip', {
    extend : 'Ext.slider.Tip',
    alternateClassName : 'GeoExt.SliderTip',
    requires: [
        'GeoExt.Version'
    ],
    /**
     * Display the tip when hovering over the thumb.  If `false`, tip will
     * only be displayed while dragging.  Default is `true`.
     *
     * @cfg {Boolean} hover
     */
    hover: true,

    /**
     * Minimum width of the tip.  Default is 10.
     *
     * @cfg {Number} minWidth
     */
    minWidth: 10,

    /**
     * A two item list that provides x, y offsets for the tip.
     *
     * @cfg {Number[]} offsets
     */
    offsets : [0, -10],

    /**
     * The thumb is currently being dragged.
     *
     * @property {Boolean} dragging
     */
    dragging: false,

    /**
     * Called when the plugin is initialized.
     *
     * @param {Ext.slider.SingleSlider} slider
     * @private
     */
    init: function(slider) {
        this.callParent(arguments);
        if (this.hover) {
            slider.on("render", this.registerThumbListeners, this);
        }

        this.slider = slider;
    },

    /**
     * Set as a listener for 'render' if hover is true.
     *
     * @private
     */
    registerThumbListeners: function() {
        var thumb, el;
        for (var i=0, ii=this.slider.thumbs.length; i<ii; ++i) {
            thumb = this.slider.thumbs[i];
            el = thumb.tracker.el;
            (function(thumb, el) {
                el.on({
                    mouseover: function(e) {
                        this.onSlide(this.slider, e, thumb);
                        this.dragging = false;
                    },
                    mouseout: function() {
                        if (!this.dragging) {
                            this.hide.apply(this, arguments);
                        }
                    },
                    scope: this
                });
            }).apply(this, [thumb, el]);
        }
    },

    /**
     * Listener for dragstart and drag.
     *
     * @param {Ext.slider.SingleSlider} slider
     * @param {Object} e
     * @param {Object} thumb
     * @private
     */
    onSlide: function(slider, e, thumb) {
        this.dragging = true;
        return this.callParent(arguments);
    }

});
