/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include GeoExt/tree/LayerNode.js
 */

/**
 * A loader that will load layers from a GeoExt.data.LayerStore.
 * By default, only layers that have `displayInLayerSwitcher` set to `true`
 * will be included. The childrens' iconCls defaults to "gx-tree-layer-icon".
 *
 * Example:
 *
 *     var loader = Ext.create('GeoExt.tree.LayerLoader', {
 *         baseAttrs: {
 *             iconCls: 'baselayer-icon',
 *             checkedGroup: 'baselayer'
 *         },
 *         filter: function(record) {
 *             var layer = record.getLayer();
 *             return layer.displayInLayerSwitcher === true &&
 *                 layer.isBaseLayer === true;
 *         }
 *     });
 *
 * The above creates a loader which only loads base layers, and configures
 * its nodes with the 'baselayer-icon' icon class and the 'baselayer' group.
 * This is basically the same loader that the GeoExt.tree.BaseLayerContainer
 * uses.
 *
 * @class GeoExt.tree.LayerLoader
 */
Ext.define('GeoExt.tree.LayerLoader', {
    extend: 'Ext.util.Observable',
    requires: [
        'GeoExt.tree.LayerNode'
    ],

    /**
     * Triggered before loading children. Return false to avoid
     * loading children.
     *
     * @event beforeload
     * @param {GeoExt.tree.LayerLoader} this This loader.
     * @param {Ext.data.NodeInterface} node The node that this loader is
     *     configured with.
     */

    /**
     * Triggered after children were loaded.
     *
     * @event load
     * @param {GeoExt.tree.LayerLoader} loader This loader.
     * @param {Ext.data.NodeInterface} node The node that this loader is
     *     configured with.
     */

    /**
     * The layer store containing layers to be added by this loader.
     *
     * @cfg {GeoExt.data.LayerStore} store
     */
    /**
     * The layer store containing layers to be added by this loader.
     *
     * @property {GeoExt.data.LayerStore} store
     */
     store: null,

    /**
     * A function, called in the scope of this loader, with a
     * GeoExt.data.LayerRecord as argument. Is expected to return `true` for
     * layers to be loaded, `false` otherwise. By default, the filter checks
     * for `displayInLayerSwitcher`:
     *
     *     filter: function(record) {
     *         return record.getLayer().displayInLayerSwitcher === true
     *     }
     *
     * @property {Function} filter
     * @param {GeoExt.data.LayerRecord} record
     */
    filter: function(record) {
        return record.getLayer().displayInLayerSwitcher === true;
    },

    /**
     * An object containing attributes to be added to all nodes created by
     * this loader.
     *
     * @cfg
     */
    baseAttrs: null,

    /**
     * @param {GeoExt.data.LayerTreeModel} node The node to add children to.
     * @private
     */
    load: function(node) {
        if (this.fireEvent("beforeload", this, node)) {
            this.removeStoreHandlers();
            while (node.firstChild) {
                node.removeChild(node.firstChild);
            }

            if (!this.store) {
                this.store = GeoExt.MapPanel.guess().layers;
            }
            this.store.each(function(record) {
                this.addLayerNode(node, record);
            }, this);
            this.addStoreHandlers(node);

            this.fireEvent("load", this, node);
        }
    },

    /**
     * Listener for the store's add event.
     *
     * @param {Ext.data.Store} store
     * @param {Ext.data.Record[]} records
     * @param {Number} index
     * @param {GeoExt.data.LayerTreeModel} node
     * @private
     */
    onStoreAdd: function(store, records, index, node) {
        if (!this._reordering) {
            var nodeIndex = node.get('container')
                .recordIndexToNodeIndex(index+records.length-1, node);
            for (var i=0, ii=records.length; i<ii; ++i) {
                this.addLayerNode(node, records[i], nodeIndex);
            }
        }
    },

    /**
     * Listener for the store's remove event.
     *
     * @param {Ext.data.Store} store
     * @param {Ext.data.Record} record
     * @param {Integer} index
     * @param {GeoExt.data.LayerTreeModel} node
     * @private
     */
    onStoreRemove: function(layerRecord, node) {
        if (!this._reordering) {
            this.removeLayerNode(node, layerRecord);
        }
    },

    /**
     * Adds a child node representing a layer of the map
     *
     * @param {GeoExt.data.LayerTreeModel} node The node that the layer node
     *     will be added to as child.
     * @param {GeoExt.data.LayerModel} layerRecord The layer record containing
     *     the layer to be added.
     * @param {Integer} index Optional index for the new layer.  Default is 0.
     * @private
     */
    addLayerNode: function(node, layerRecord, index) {
        index = index || 0;
        if (this.filter(layerRecord) === true) {
            var layer = layerRecord.getLayer();
            var child = this.createNode({
                plugins: [{
                    ptype: 'gx_layer'
                }],
                layer: layer,
                text: layer.name,
                listeners: {
                    move: this.onChildMove,
                    scope: this
                }
            });
            if (index !== undefined) {
                node.insertChild(index, child);
            } else {
                node.appendChild(child);
            }
            node.getChildAt(index).on("move", this.onChildMove, this);
        }
    },

    /**
     * Removes a child node representing a layer of the map
     *
     * @param {GeoExt.data.LayerTreeModel} node The node that the layer node
     *     will be removed from as child.
     * @param {GeoExt.data.LayerModel} layerRecord The layer record containing
     *     the layer to be removed.
     * @private
     */
    removeLayerNode: function(node, layerRecord) {
        if (this.filter(layerRecord) === true) {
            var child = node.findChildBy(function(node) {
                return node.get('layer') == layerRecord.getLayer();
            });
            if (child) {
                child.un("move", this.onChildMove, this);
                child.remove();
            }
        }
    },

    /**
     * Listener for child node "move" events.  This updates the order of
     * records in the store based on new node order if the node has not
     * changed parents.
     *
     * @param {GeoExt.data.LayerTreeModel} node
     * @param {GeoExt.data.LayerTreeModel} oldParent
     * @param {GeoExt.data.LayerTreeModel} newParent
     * @param {Integer} index
     * @private
     */
    onChildMove: function(node, oldParent, newParent, index) {
        var me = this,
            record = me.store.getByLayer(node.get('layer')),
            container = newParent.get('container'),
            parentLoader = container.loader;

        // remove the record and re-insert it at the correct index
        me._reordering = true;
        if (parentLoader instanceof me.self && me.store === parentLoader.store) {
            parentLoader._reordering = true;
            me.store.remove(record);
            var newRecordIndex;
            if (newParent.childNodes.length > 1) {
                // find index by neighboring node in the same container
                var searchIndex = (index === 0) ? index + 1 : index - 1;
                newRecordIndex = me.store.findBy(function(r) {
                    return newParent.childNodes[searchIndex]
                        .get('layer') === r.getLayer();
                });
                if (index === 0) {
                    newRecordIndex++;
                }
            } else if (oldParent.parentNode === newParent.parentNode) {
                // find index by last node of a container above
                var prev = newParent;
                do {
                    prev = prev.previousSibling;
                } while (prev &&
                    !(prev.get('container') instanceof container.self &&
                    prev.lastChild));
                if (prev) {
                    newRecordIndex = me.store.findBy(function(r) {
                        return prev.lastChild.get('layer') === r.getLayer();
                    });
                } else {
                    // find indext by first node of a container below
                    var next = newParent;
                    do {
                        next = next.nextSibling;
                    } while (next &&
                        !(next.get('container') instanceof container.self &&
                        next.firstChild));
                    if (next) {
                        newRecordIndex = me.store.findBy(function(r) {
                            return next.firstChild.get('layer') === r.getLayer();
                        });
                    }
                    newRecordIndex++;
                }
            }
            if (newRecordIndex !== undefined) {
                me.store.insert(newRecordIndex, [record]);
            } else {
                me.store.insert(oldRecordIndex, [record]);
            }
            delete parentLoader._reordering;
        }
        delete me._reordering;
    },

    /**
     * Adds appropriate listeners on the store.
     *
     * @param {GeoExt.data.LayerTreeModel} node
     * @private
     */
    addStoreHandlers: function(node) {
        if (!this._storeHandlers) {
            this._storeHandlers = {
                "add": function(store, layerRecords, index) {
                    this.onStoreAdd(store, layerRecords, index, node);
                },
                "remove": function(parent, removedRecord) {
                    this.onStoreRemove(removedRecord, node);
                }
            };
            for (var evt in this._storeHandlers) {
                this.store.on(evt, this._storeHandlers[evt], this);
            }
        }
    },

    /**
     * Removes the bound listeners on the store.
     *
     * @private
     */
    removeStoreHandlers: function() {
        if (this._storeHandlers) {
            for (var evt in this._storeHandlers) {
                this.store.un(evt, this._storeHandlers[evt], this);
            }
            delete this._storeHandlers;
        }
    },

    /**
     * Extend this function to modify the node attributes at creation time.
     *
     * @param {Object} attr attributes for the new node
     */
    createNode: function(attr) {
        if (this.baseAttrs){
            Ext.apply(attr, this.baseAttrs);
        }

        return attr;
    },

    /**
     * Unregisters bound listeners via #removeStoreHandlers
     *
     * @private
     */
    destroy: function() {
        this.removeStoreHandlers();
    }
});
