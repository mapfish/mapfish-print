/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include OpenLayers/Util.js
 */

/**
 * The legend image.
 *
 * @class GeoExt.LegendImage
 */
Ext.define('GeoExt.LegendImage', {
    extend : 'Ext.Component',
    alias : 'widget.gx_legendimage',
    requires: [
        'GeoExt.Version'
    ],
    /**
     * The url of the image to load.
     *
     * @cfg {String} url
     */
    url: null,

    /**
     * Path to image that will be used if the legend image fails
     * to load.  Default is Ext.BLANK_IMAGE_URL.
     *
     * @cfg {String} defaultImgSrc
     */
    defaultImgSrc: null,

    /**
     * Optional CSS class to apply to img tag.
     *
     * @cfg {String} imgCls
     */
    imgCls: null,

    /**
     * CSS class applied to img tag when no image is available or the default
     * image was loaded.
     * 
     * @cfg {String}
     */
    noImgCls: "gx-legend-noimage",

    initComponent: function(){
        var me = this;
        me.callParent(arguments);
        if(this.defaultImgSrc === null) {
            this.defaultImgSrc = Ext.BLANK_IMAGE_URL;
        }
        this.autoEl = {
            tag: "img",
            "class": (this.imgCls ? this.imgCls + " " + this.noImgCls : this.noImgCls),
            src: this.defaultImgSrc
        };
    },

    /**
     * Sets the url of the legend image.
     *
     * @param {String} url The new URL.
     */
    setUrl: function(url) {
        this.url = url;
        var el = this.getEl();
        if (el) {
            el.un("load", this.onImageLoad, this);
            el.on("load", this.onImageLoad, this, {single: true});
            el.un("error", this.onImageLoadError, this);
            el.on("error", this.onImageLoadError, this, {
                single: true
            });
            el.dom.src = url;
        }
    },

    /**
     * Private method called when the legend image component is being rendered.
     *
     * @private
     */
    onRender: function(ct, position) {
        this.callParent(arguments);
        if(this.url) {
            this.setUrl(this.url);
        }
    },

    /**
     * Private method called during the destroy sequence.
     *
     * @private
     */
    onDestroy: function() {
        var el = this.getEl();
        if(el) {
            el.un("load", this.onImageLoad, this);
            el.un("error", this.onImageLoadError, this);
        }
        this.callParent();
    },

    /**
     * Private method called if the legend image fails loading.
     *
     * @private
     */
    onImageLoadError: function() {
        var el = this.getEl();
        el.addCls(this.noImgCls);
        el.dom.src = this.defaultImgSrc;
    },

    /**
     * Private method called after the legend image finished loading.
     *
     * @private
     */
    onImageLoad: function() {
        var el = this.getEl();
        if (!OpenLayers.Util.isEquivalentUrl(el.dom.src, this.defaultImgSrc)) {
            el.removeCls(this.noImgCls);
        }
    }

});
