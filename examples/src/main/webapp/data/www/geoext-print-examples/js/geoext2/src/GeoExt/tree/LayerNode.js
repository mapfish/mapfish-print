/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/**
 * The LayerNode plugin. This is used to create a node that is connected to
 * a layer, so the checkbox and the layer's visibility are in sync. A basic
 * layer node would be configured like this:
 *
 *     {
 *         plugins: ['gx_layernode'],
 *         layer: myLayer
 *     }
 *
 * See GeoExt.data.LayerTreeModel for more details on GeoExt extensions to the
 * node configuration.
 *
 * @class GeoExt.tree.LayerNode
 */
Ext.define('GeoExt.tree.LayerNode', {
    extend: 'Ext.AbstractPlugin',
    alias: 'plugin.gx_layer',
    requires: [
        'GeoExt.Version'
    ],
    /**
     * The init method is invoked after initComponent method has been run for
     * the client Component. It performs plugin initialization.
     * 
     * @param {Ext.Component} target The client Component which owns this
     *     plugin.
     * @private
     */
    init: function(target) {

        this.target = target;
        var layer = target.get('layer');

        target.set('checked', layer.getVisibility());
        if (!target.get('checkedGroup') && layer.isBaseLayer) {
            target.set('checkedGroup', 'gx_baselayer');
        }
        target.set('fixedText', !!target.text);

        target.set('leaf', true);

        if(!target.get('iconCls')) {
            target.set('iconCls', "gx-tree-layer-icon");
        }

        target.on('afteredit', this.onAfterEdit, this);
        layer.events.on({
            "visibilitychanged": this.onLayerVisibilityChanged,
            scope: this
        });
    },

    /**
     * Handler for the node's afteredit event.
     *
     * @param {GeoExt.data.LayerTreeModel} node
     * @param {String[]} modifiedFields
     * @private
     */
    onAfterEdit: function(node, modifiedFields) {
        var me = this;

        if(~Ext.Array.indexOf(modifiedFields, 'checked')) {
            me.onCheckChange();
        }
    },

    /**
     * Handler for visibilitychanged events on the layer.
     *
     * @private
     */
    onLayerVisibilityChanged: function() {
        if(!this._visibilityChanging) {
            this.target.set('checked', this.target.get('layer').getVisibility());
        }
    },

    /**
     * Updates the visibility of the layer that is connected to the target
     * node.
     *
     * @private
     */
    onCheckChange: function() {
        var node = this.target,
            checked = this.target.get('checked');

        if(checked != node.get('layer').getVisibility()) {
            node._visibilityChanging = true;
            var layer = node.get('layer');
            if(checked && layer.isBaseLayer && layer.map) {
                layer.map.setBaseLayer(layer);
            } else if(!checked && layer.isBaseLayer && layer.map &&
                      layer.map.baseLayer && layer.id == layer.map.baseLayer.id) {
                // Must prevent the unchecking of radio buttons
                node.set('checked', layer.getVisibility());
            } else {
                layer.setVisibility(checked);
            }
            delete node._visibilityChanging;
        }
    }

});
