/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include GeoExt/tree/LayerLoader.js
 * @include GeoExt/data/Loader.js
 */

/**
 * A layer node plugin that will collect all layers of an OpenLayers map. Only
 * layers that have `displayInLayerSwitcher` set to `true` will be included.
 * The childrens' iconCls defaults to "gx-tree-layer-icon" and this node'
 * text defaults to "Layers".
 *
 * To create a tree node that holds the layers of a tree, it needs to be
 * configured with the gx_layercontainer plugin that this class provides - like
 * the root node in the example below:
 *
 *     var mapPanel = Ext.create('GeoExt.panel.Map', {
 *         layers: [new OpenLayers.Layer('foo')]
 *     });
 *
 *     var treeStore = Ext.create('Ext.data.TreeStore', {
 *         model: 'GeoExt.data.LayerTreeModel',
 *         root: {
 *             plugins: [{
 *                 ptype: 'gx_layercontainer',
 *                 loader: {store: mapPanel.layers}
 *             }],
 *             expanded: true
 *         }
 *     });
 *
 * @class GeoExt.tree.LayerContainer
 */
Ext.define('GeoExt.tree.LayerContainer', {
    extend: 'Ext.AbstractPlugin',
    requires: [
        'GeoExt.tree.LayerLoader'
    ],
    alias: 'plugin.gx_layercontainer',

    /**
     * The loader to use with this container. If an Object is provided, a
     * GeoExt.tree.LayerLoader, configured with the the properties from
     * the provided object, will be created. By default, a LayerLoader for
     * all layers of the first MapPanel found by the ComponentManager will be
     * created.
     *
     * @cfg {GeoExt.tree.LayerLoader/Object} loader
     */

    /**
     * The default text for the target node.
     *
     * @private
     */
    defaultText: 'Layers',

    /**
     * @private
     */
    init: function(target) {
        var me = this;

        var loader = me.loader;

        me.loader = (loader && loader instanceof GeoExt.tree.LayerLoader) ?
            loader : new GeoExt.tree.LayerLoader(loader);

        target.set('container', me);
        if (!target.get('text')) {
            target.set('text', me.defaultText);
            target.commit();
        }
        me.loader.load(target);

    },

    /**
     * @param {Number} index  The record index in the layer store.
     * @returns {Number} The appropriate child node index for the record.
     * @private
     */
    recordIndexToNodeIndex: function(index, node) {
        var me = this;
        var store = me.loader.store;
        var count = store.getCount();
        var nodeCount = node.childNodes.length;
        var nodeIndex = -1;
        for(var i=count-1; i>=0; --i) {
            if(me.loader.filter(store.getAt(i)) === true) {
                ++nodeIndex;
                if(index === i || nodeIndex > nodeCount-1) {
                    break;
                }
            }
        }
        return nodeIndex;
    }
});
