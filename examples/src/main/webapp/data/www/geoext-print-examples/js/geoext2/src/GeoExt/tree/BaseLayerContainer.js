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
 * map. Only layers that have `displayInLayerSwitcher` set to `true` will
 * be included. The childrens' iconCls defaults to "gx-tree-baselayer-icon"
 * and the node' text defaults to "Base Layer".
 *
 * Children will be rendered with a radio button instead of a checkbox,
 * showing the user that only one base layer can be active at a time.
 *
 * To use this node plugin in a tree node config, configure a node like this:
 *
 *     {
 *         plugins: "gx_baselayercontainer",
 *         text: "My base layers"
 *     }
 *
 * @class GeoExt.tree.BaseLayerContainer
 */
Ext.define('GeoExt.tree.BaseLayerContainer', {
    extend: 'GeoExt.tree.LayerContainer',
    alias: 'plugin.gx_baselayercontainer',

    /**
     * The default text for the target node.
     *
     * @private
     */
    defaultText: 'Base Layers',

    /**
     * @private
     */
    init: function(target) {
        var me = this,
            loader = me.loader;

        me.loader = Ext.applyIf(loader || {}, {
            baseAttrs: Ext.applyIf((loader && loader.baseAttrs) || {}, {
                iconCls: 'gx-tree-baselayer-icon',
                checkedGroup: 'baselayer'
            }),
            filter: function(record) {
                var layer = record.getLayer();
                return layer.displayInLayerSwitcher === true &&
                    layer.isBaseLayer === true;
            }
        });
        me.callParent(arguments);
    }
});
