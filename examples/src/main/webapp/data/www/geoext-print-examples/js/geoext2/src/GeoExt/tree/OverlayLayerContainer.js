/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include GeoExt/tree/LayerContainer.js
 */

/**
 * A layer node plugin that will collect all base layers of an OpenLayers
 * map. Only layers that have `displayInLayerSwitcher` set to `true`
 * will be included. The node's text defaults to 'Overlays'.
 *
 * To use this node plugin in a tree node config, configure a node like this:
 *
 *     {
 *         plugins: "gx_overlaylayercontainer",
 *         text: "My overlays"
 *     }
 *
 * @class GeoExt.tree.OverlayLayerContainer
 */
Ext.define('GeoExt.tree.OverlayLayerContainer', {
    extend: 'GeoExt.tree.LayerContainer',
    alias: 'plugin.gx_overlaylayercontainer',

    /**
     * The default text for the target node.
     *
     * @private
     */
    defaultText: 'Overlays',

    /**
     * @private
     */
    init: function(target) {
        var me = this;

        var loader = me.loader;

        me.loader = Ext.applyIf(loader || {}, {
            filter: function(record) {
                var layer = record.getLayer();
                return (layer.displayInLayerSwitcher && !layer.isBaseLayer);
            }
        });
        me.callParent(arguments);
    }
});
