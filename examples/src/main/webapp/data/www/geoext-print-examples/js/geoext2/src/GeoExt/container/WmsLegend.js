/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include OpenLayers/Layer/WMS.js
 * @include OpenLayers/Util.js
 * @requires GeoExt/container/LayerLegend.js
 * @include GeoExt/LegendImage.js
 */

/**
 * Show a legend image for a WMS layer. The image can be read from the styles
 * field of a layer record (if the record comes e.g. from a
 * GeoExt.data.WMSCapabilitiesReader). If not provided, a
 * GetLegendGraphic request will be issued to retrieve the image.
 *
 * @class GeoExt.container.WmsLegend
 */
Ext.define('GeoExt.container.WmsLegend', {
    extend: 'GeoExt.container.LayerLegend',
    alias: 'widget.gx_wmslegend',
    requires: ['GeoExt.LegendImage'],
    alternateClassName: 'GeoExt.WMSLegend',

    statics: {
        /**
         * Checks whether the given layer record supports an URL legend.
         *
         * @param {GeoExt.data.LayerRecord} layerRecord Record containing a
         *     WMS layer.
         * @return {Number} Either `1` when WMS legends are supported or `0`.
         */
        supports: function(layerRecord) {
            return layerRecord.getLayer() instanceof OpenLayers.Layer.WMS ? 1 : 0;
        }
    },

    /**
     * The WMS spec does not say if the first style advertised for a layer in
     * a Capabilities document is the default style that the layer is
     * rendered with. We make this assumption by default. To be strictly WMS
     * compliant, set this to false, but make sure to configure a STYLES
     * param with your WMS layers, otherwise LegendURLs advertised in the
     * GetCapabilities document cannot be used.
     *
     * @cfg {Boolean}
     */
    defaultStyleIsFirst: true,

    /**
     * Should we use the optional SCALE parameter in the SLD WMS
     * GetLegendGraphic request?
     *
     * @cfg {Boolean}
     */
    useScaleParameter: true,

    /**
     * Optional parameters to add to the legend url, this can e.g. be used to
     * support vendor-specific parameters in a SLD WMS GetLegendGraphic
     * request. To override the default MIME type of `image/gif` use the
     * `FORMAT` parameter in baseParams.
     *
     * Example:
     *
     *     var legendPanel = Ext.create('GeoExt.panel.Legend', {
     *         map: map,
     *         title: 'Legend Panel',
     *         defaults: {
     *             style: 'padding:5px',
     *             baseParams: {
     *                 FORMAT: 'image/png',
     *                 LEGEND_OPTIONS: 'forceLabels:on'
     *             }
     *         }
     *     });
     *
     * @cfg {Object}
     */
    baseParams: null,

    initComponent: function(){
        var me = this;
        me.callParent();
        var layer = me.layerRecord.getLayer();
        me._noMap = !layer.map;
        layer.events.register("moveend", me, me.onLayerMoveend);
        me.update();
    },

    /**
     * Called when `moveend` fires on the associated layer. Might call #update
     * to be in sync with layer style.
     *
     * @private
     * @param {Object} e
     */
    onLayerMoveend: function(e) {
        if ((e.zoomChanged === true && this.useScaleParameter === true) ||
            this._noMap) {
            delete this._noMap;
            this.update();
        }
    },

    /**
     * Get the legend URL of a sublayer.
     *
     * @param {String} layerName A sublayer.
     * @param {Array} layerNames The array of sublayers, read from #layerRecord
     *     if not provided.
     * @return {String} The legend URL.
     * @private
     */
    getLegendUrl: function(layerName, layerNames) {
        var rec = this.layerRecord;
        var url;
        var styles = rec && rec.get("styles");
        var layer = rec.getLayer();
        layerNames = layerNames || [layer.params.LAYERS].join(",").split(",");

        var styleNames = layer.params.STYLES &&
        [layer.params.STYLES].join(",").split(",");
        var idx = Ext.Array.indexOf(layerNames, layerName);
        var styleName = styleNames && styleNames[idx];
        // check if we have a legend URL in the record's
        // "styles" data field
        if(styles && styles.length > 0) {
            if(styleName) {
                Ext.each(styles, function(s) {
                    url = (s.name == styleName && s.legend) && s.legend.href;
                    return !url;
                });
            } else if(this.defaultStyleIsFirst === true && !styleNames &&
                !layer.params.SLD && !layer.params.SLD_BODY) {
                url = styles[0].legend && styles[0].legend.href;
            }
        }
        if(!url) {
            url = layer.getFullRequestString({
                REQUEST: "GetLegendGraphic",
                WIDTH: null,
                HEIGHT: null,
                EXCEPTIONS: "application/vnd.ogc.se_xml",
                LAYER: layerName,
                LAYERS: null,
                STYLE: (styleName !== '') ? styleName: null,
                STYLES: null,
                SRS: null,
                FORMAT: null,
                TIME: null
            });
        }
        var params = Ext.apply({}, this.baseParams);
        if (layer.params._OLSALT) {
            // update legend after a forced layer redraw
            params._OLSALT = layer.params._OLSALT;
        }
        url = Ext.urlAppend(url, Ext.urlEncode(params));
        if (url.toLowerCase().indexOf("request=getlegendgraphic") != -1) {
            if (url.toLowerCase().indexOf("format=") == -1) {
                url = Ext.urlAppend(url, "FORMAT=image%2Fgif");
            }
            // add scale parameter - also if we have the url from the record's
            // styles data field and it is actually a GetLegendGraphic request.
            if (this.useScaleParameter === true) {
                var scale = layer.map.getScale();
                url = Ext.urlAppend(url, "SCALE=" + scale);
            }
        }
        return url;
    },

    /**
     * Update the legend, adding, removing or updating
     * the per-sublayer box component.
     *
     * @private
     */
    update: function() {
        var layer = this.layerRecord.getLayer();
        // In some cases, this update function is called on a layer
        // that has just been removed, see ticket #238.
        // The following check bypass the update if map is not set.
        if(!(layer && layer.map)) {
            return;
        }
        this.callParent();

        var layerNames, layerName, i, len;

        layerNames = [layer.params.LAYERS].join(",").split(",");

        var destroyList = [];
        var textCmp = this.items.get(0);
        this.items.each(function(cmp) {
            i = Ext.Array.indexOf(layerNames, cmp.itemId);
            if(i < 0 && cmp != textCmp) {
                destroyList.push(cmp);
            } else if(cmp !== textCmp){
                layerName = layerNames[i];
                var newUrl = this.getLegendUrl(layerName, layerNames);
                if(!OpenLayers.Util.isEquivalentUrl(newUrl, cmp.url)) {
                    cmp.setUrl(newUrl);
                }
            }
        }, this);
        for(i = 0, len = destroyList.length; i<len; i++) {
            var cmp = destroyList[i];
            // cmp.destroy() does not remove the cmp from
            // its parent container!
            this.remove(cmp);
            cmp.destroy();
        }

        for(i = 0, len = layerNames.length; i<len; i++) {
            layerName = layerNames[i];
            if(!this.items || !this.getComponent(layerName)) {
                this.add({
                    xtype: "gx_legendimage",
                    url: this.getLegendUrl(layerName, layerNames),
                    itemId: layerName
                });
            }
        }
        this.doLayout();
    },

    /**
     * Unregisters the moveend-listener prior to destroying.
     */
    beforeDestroy: function() {
        if (this.useScaleParameter === true) {
            var layer = this.layerRecord.getLayer();
            layer && layer.events &&
            layer.events.unregister("moveend", this, this.onLayerMoveend);
        }
        this.callParent();
    }
}, function() {
    GeoExt.container.LayerLegend.types["gx_wmslegend"] =
        GeoExt.container.WmsLegend;
});
