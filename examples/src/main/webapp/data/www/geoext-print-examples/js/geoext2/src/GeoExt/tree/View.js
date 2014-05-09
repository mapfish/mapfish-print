/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/**
 * This plugin provides basic tree - map synchronisation functionality for a
 * TreeView.
 *
 * It creates a specialized instance of modify the nodes on the fly and adds
 * event listeners to the tree and the maps to get both in sync.
 *
 * Note that the plugin must be added to the tree view, not to the tree panel.
 * For example using viewConfig:
 *
 *     viewConfig: {
 *         plugins: {
 *             ptype: 'layertreeview'
 *         }
 *     }
 *
 * @class GeoExt.tree.View
 */
Ext.define('GeoExt.tree.View', {
    extend: 'Ext.tree.View',
    alias: 'widget.gx_treeview',
    requires: [
        'GeoExt.Version'
    ],
    initComponent : function() {
        var me = this;

        me.on('itemupdate', this.onItem, this);
        me.on('itemadd', this.onItem, this);
        me.on('createchild', this.createChild, this);

        return me.callParent(arguments);
    },

    /**
     * Called when an item updates or is added.
     *
     * @param {Ext.data.Model} record The model instance
     * @param {Number} index The index of the record/node
     * @param {HTMLElement} node The node that has just been updated
     * @param {Object} options Options.
     */
    onItem: function(records, index, node, options) {
        var me = this;

        if(!(records instanceof Array)) {
            records = [records]
        }

        for(var i=0; i<records.length; i++) {
            this.onNodeRendered(records[i]);
        }
    },

    /**
     * Called when a node is being rendered.
     * 
     * 
     */
    onNodeRendered: function(node) {
        var me = this;

        var el = Ext.get('tree-record-'+node.id);
        if(!el) {
            return;
        }

        if(node.get('layer'))
            me.fireEvent('createchild', el, node);

        if(node.hasChildNodes()) {
            node.eachChild(function(node) {
                me.onNodeRendered(node);
            }, me);
        }
    },

    /**
     * Called when an item was created.
     */
    createChild: function(el, node) {
        var component = node.get('component'),
            cmpObj;

        if(component) {

            cmpObj = Ext.ComponentManager.create(component);

            if(cmpObj.xtype &&
               node.gx_treecomponents &&
               node.gx_treecomponents[cmpObj.xtype]) {

                node.gx_treecomponents[cmpObj.xtype].destroy();
                delete node.gx_treecomponents[cmpObj.xtype];

            }

            if(!node.gx_treecomponents) {
                node.gx_treecomponents = {};
            }
            node.gx_treecomponents[cmpObj.xtype] = cmpObj;

            cmpObj.render(el);

            el.removeCls('gx-tree-component-off');
        }
    }

});
