/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/**
 * Model for trees that use GeoExt tree components. It can also hold plain
 * Ext JS layer nodes.
 *
 * This model adds several fields that are specific to tree extensions
 * provided by GeoExt:
 *
 * * **plugins** Object[]: The plugins for this node.
 * * **layer** OpenLayers.Layer: The layer this node is connected to.
 * * **container** Ext.AbstractPlugin: The instance of a container plugin.
 *   Read only.
 * * **checkedGroup** String: An identifier for a group of mutually exclusive
 *   layers. If set, the node will render with a radio button instead of a
 *   checkbox.
 * * **fixedText** Boolean: Used to determine if a node's name should change.
 *   dynamically if the name of the connected layer changes, if any. Read only.
 * * **component** Ext.Component: The component to be rendered with this node,
 *   if any.
 *
 * A typical configuration that makes use of some of these extended sttings
 * could look like this:
 *
 *     {
 *         plugins: [{ptype: 'gx_layer'}],
 *         layer: myLayerRecord.getLayer(),
 *         checkedGroup: 'natural',
 *         component: {
 *             xtype: "gx_wmslegend",
 *             layerRecord: myLayerRecord,
 *             showTitle: false
 *         }
 *     }
 *
 * The above creates a node with a GeoExt.tree.LayerNode plugin, and connects
 * it to a layer record that was previously assigned to the myLayerRecord
 * variable. The node will be rendered with a GeoExt.container.WmsLegend,
 * configured with the same layer.
 *
 * @class GeoExt.data.LayerTreeModel
 */
Ext.define('GeoExt.data.LayerTreeModel',{
    alternateClassName: 'GeoExt.data.LayerTreeRecord',
    extend: 'Ext.data.Model',
    requires: [
        'Ext.data.proxy.Memory',
        'Ext.data.reader.Json',
        'GeoExt.Version'
    ],
    alias: 'model.gx_layertree',
    fields: [
        {name: 'text', type: 'string'},
        {name: 'plugins'},
        {name: 'layer'},
        {name: 'container'},
        {name: 'checkedGroup', type: 'string'},
        {name: 'fixedText', type: 'bool'},
        {name: 'component'}
    ],
    proxy: {
        type: "memory"
    },

    /**
     * Fires after the node's fields were modified.
     *
     * @event afteredit
     * @param {GeoExt.data.LayerTreeModel} this This model instance.
     * @param {String[]} modifiedFieldNames The names of the fields that were
     * edited.
     */

    /**
     * @private
     */
    constructor: function(data, id, raw, convertedData) {
        var me = this;

        me.callParent(arguments);

        window.setTimeout(function() {
            var plugins = me.get('plugins');

            if (plugins) {
                var plugin, instance;
                for (var i=0, ii=plugins.length; i<ii; ++i) {
                    plugin = plugins[i];
                    instance = Ext.PluginMgr.create(Ext.isString(plugin) ? {
                        ptype: plugin
                    } : plugin);
                    instance.init(me);
                }
            }
        });
    },

    /**
     * Fires the #afteredit event after the node's fields were modified.
     *
     * @private
     */
    afterEdit: function(modifiedFieldNames) {
        var me = this;
        me.callParent(arguments);
        me.fireEvent('afteredit', this, modifiedFieldNames);
    }
});