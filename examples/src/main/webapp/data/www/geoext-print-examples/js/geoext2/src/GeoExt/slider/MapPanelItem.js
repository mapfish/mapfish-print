/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/**
 * A base class for sliders that can be rendered as items of a GeoExt.panel.Map.
 *
 * Usually you will not create instances of this class but rather of the
 * subclasses GeoExt.slider.LayerOpacity and GeoExt.slider.Zoom.
 *
 * This class mostly provides subclasses with the method #addToMapPanel and
 * the method #removeFromMapPanel, which take care of the dimensions of the
 * sliders DOM-elements and register/unregister appropriate eventlisteners.
 *
 * Subclasses may implement #unbind, which will be called when a slider is
 * being removed from the map panel.
 *
 * @class GeoExt.slider.MapPanelItem
 */
Ext.define('GeoExt.slider.MapPanelItem', {
    extend: 'Ext.slider.Single',
    requires: [
        'GeoExt.Version'
    ],

    /**
     * The number of milliseconds to wait (after rendering the slider) before
     * resizing of the slider happens in case this slider is rendered as child
     * of a GeoExt.panel.Map.
     *
     * This defaults to 200 milliseconds, which is not really noticeable, and
     * also rather conservative big.
     *
     * @protected
     */
    resizingDelayMS: 200,

    /**
     * The height in pixels of the slider thumb. Will be used when we need to
     * manually resize ourself in case we are added to a mappanel. This will
     * be the height of the element containing the thumb when we are rendered
     * horizontally (see #vertical).
     *
     * This value shouldn't usually be adjusted, when the default stylesheet of
     * ExtJS is used.
     *
     * @cfg {Number}
     */
    thumbHeight: 14,

    /**
     * The width in pixels of the slider thumb. Will be used when we need to
     * manually resize ourself in case we are added to a mappanel. This will
     * be the width of the element containing the thumb when we are rendered
     * vertically (see #vertical).
     *
     * This value shouldn't usually be adjusted, when the default stylesheet of
     * ExtJS is used.
     *
     * @cfg {Number}
     */
    thumbWidth: 15,

    /**
     * Called by a MapPanel if this component is one of the items in the panel.
     *
     * @param {GeoExt.panel.Map} panel
     * @protected
     */
    addToMapPanel: function(panel) {
        this.on({
            // Once we are rendered and we know that we are a child of a
            // mappanel, we need to make some adjustments to our DOMs
            // box dimensions.
            afterrender: function(){
                var me = this,
                    el = me.getEl(),
                    dim = {
                        // depending on our vertical setting, we need to find
                        // sane values for both width and height.
                        width: me.vertical ? me.thumbWidth : el.getWidth(),
                        height: !me.vertical ? me.thumbHeight : el.getHeight(),
                        top: me.y || 0,
                        left: me.x || 0
                    },
                    resizeFunction,
                    resizeTask;
                // Bind handlers that stop the mouse from interacting with the
                // map below the slider.
                el.on({
                    mousedown: me.stopMouseEvents,
                    click: me.stopMouseEvents
                });

                // This method takes some of the gathered values from above and
                // ensures that we have an expected look.
                resizeFunction = function(){
                    el.setStyle({
                        top: dim.top,
                        left: dim.left,
                        width: dim.width,
                        position: "absolute",
                        height: dim.height,
                        zIndex: panel.map.Z_INDEX_BASE.Control
                    });
                    // This is tricky...
                    if (me.vertical) {
                        // ...for vertical sliders the height of the surrounding
                        // element is controlled by the height of the element
                        // with the 'x-slider-inner'-class
                        el.down('.x-slider-inner').el.setStyle({
                            height: dim.height - me.thumbWidth
                        });
                    } else {
                        // ...but for horizontal sliders, it's the form element
                        // with class 'x-form-item-body' that controls the
                        // height.
                        el.down('.x-form-item-body').el.setStyle({
                            height: me.thumbHeight
                        });
                    }
                };
                // We delay the execution for a small amount of milliseconds,
                // so that our changes do take effect.
                resizeTask = new Ext.util.DelayedTask(resizeFunction);
                resizeTask.delay(me.resizingDelayMS);
            },
            scope: this
        });
    },

    /**
     * Called by a MapPanel if this component is one of the items in the panel.
     *
     * @param {GeoExt.panel.Map} panel
     * @protected
     */
    removeFromMapPanel: function(panel) {
        var me = this,
            el = me.getEl();
        el.un({
            mousedown: me.stopMouseEvents,
            click: me.stopMouseEvents,
            scope: me
        });
        me.unbind();
    },

    /**
     * Will be called when the slider is being removed from the mappanel.
     * Subclasses may implement custom event unlistening logic in this method.
     *
     * @protected
     */
    unbind: Ext.emptyFn,

    /**
     * Stops the event from propagating.
     *
     * @private
     */
    stopMouseEvents: function(e) {
        e.stopEvent();
    }
});
