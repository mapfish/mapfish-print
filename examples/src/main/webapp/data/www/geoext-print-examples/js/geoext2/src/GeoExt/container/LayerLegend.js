/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/**
 * Base class for components of GeoExt.panel.Legend.
 *
 * @class GeoExt.container.LayerLegend
 */
Ext.define('GeoExt.container.LayerLegend', {
    extend : 'Ext.container.Container',
    requires: [
        'GeoExt.Version',
        'Ext.form.Label'
    ],
    alias : 'widget.gx_layerlegend',
    alternateClassName : 'GeoExt.LayerLegend',

    statics : {
        /**
         * Gets an array of legend xtypes that support the provided layer
         * record, with optionally provided preferred types listed first.
         *
         * @param {GeoExt.data.LayerRecord} layerRecord A layer record to get
         *     legend types for. If not provided, all registered types will be
         *     returned.
         * @param {Array} preferredTypes Types that should be considered.
         *     first. If not provided, all registered legend types will be
         *     returned in the order of their score for support of the provided
         *     layerRecord.
         * @return {Array} xtypes of legend types that can be used with
         *     the provided layerRecord.
         */
        getTypes: function(layerRecord, preferredTypes) {
            var types = (preferredTypes || []).concat(),
                scoredTypes = [], score, type;
            for (type in this.types) {
                score = this.types[type].supports(layerRecord);
                if(score > 0) {
                    // add to scoredTypes if not preferred
                    if (Ext.Array.indexOf(types, type) == -1) {
                        scoredTypes.push({
                            type: type,
                            score: score
                        });
                    }
                } else {
                    // preferred, but not supported
                    Ext.Array.remove(types, type);
                }
            }
            scoredTypes.sort(function(a, b) {
                return a.score < b.score ? 1 : (a.score == b.score ? 0 : -1);
            });
            var len = scoredTypes.length, goodTypes = new Array(len);
            for (var i=0; i<len; ++i) {
                goodTypes[i] = scoredTypes[i].type;
            }
            // take the remaining preferred types, and add other good types
            return types.concat(goodTypes);
        },
        /**
         * Checks whether this legend type supports the provided layerRecord.
         *
         * @param {GeoExt.data.LayerRecord} layerRecord The layer record
         *     to check support for.
         * @return {Integer} score indicating how good the legend supports the
         *     provided record. 0 means not supported.
         */
        supports: function(layerRecord) {
            // to be implemented by subclasses
        },
        /**
         * An object containing a name-class mapping of LayerLegend subclasses.
         * To register as LayerLegend, a subclass should add itself to this
         * object:
         *
         *     Ext.define('GeoExt.container.WmsLegend', {
         *         extend: 'GeoExt.container.LayerLegend'
         *         // ...
         *     }, function() {
         *         GeoExt.container.LayerLegend.types["gx_wmslegend"] =
         *             GeoExt.container.WmsLegend;
         *     });
         *
         * @cfg {Object}
         */
        types: {}
    },

    /**
     * The layer record for the legend
     *
     * @cfg {GeoExt.data.LayerRecord}
     */
    layerRecord: null,

    /**
     * Whether or not to show the title of a layer. This can be overridden
     * on the #layerStore record using the hideTitle property.
     *
     * @cfg {Boolean}
     */
    showTitle: true,

    /**
     * Optional title to be displayed instead of the layer title.  If this is
     * set, the value of `#showTitle` will be ignored (assumed to be true).
     *
     * @cfg {String}
     */
    legendTitle: null,

    /**
     * Optional CSS class to use for the layer title labels.
     *
     * @cfg {String}
     */
    labelCls: null,

    /**
     * @property layerStore {GeoExt.data.LayerStore}
     * @private
     */
    layerStore: null,

    /**
     * Initializes the LayerLegend component.
     */
    initComponent: function(){
        var me = this;
        me.callParent(arguments);
        me.autoEl = {};
        me.add({
            xtype: "label",
            html: this.getLayerTitle(this.layerRecord),
            cls: 'x-form-item x-form-item-label' +
            (this.labelCls ? ' ' + this.labelCls : '')
        });
        if (me.layerRecord && me.layerRecord.store) {
            me.layerStore = me.layerRecord.store;
            me.layerStore.on("update", me.onStoreUpdate, me);
            me.layerStore.on("add", me.onStoreAdd, me);
            me.layerStore.on("remove", me.onStoreRemove, me);
        }
    },

    /**
     * Get the label text of the legend.
     *
     * @private
     * @return {String}
     */
    getLabel: function() {
        var label = this.items.get(0);
        return label.rendered ? label.el.dom.innerHTML : label.html;
    },

    /**
     * Handler for remove event of the layerStore.
     *
     * @param {Ext.data.Store} store The store from which the record was
     *     removed.
     * @param {Ext.data.Record} record The record object corresponding
     *     to the removed layer.
     * @param {Integer} index The index in the store at which the record
     *     was remvoed.
     * @private
     */
    onStoreRemove: function(store, record, index) {
        // to be implemented by subclasses if needed
    },

    /**
     * Handler for add event of the layerStore.
     *
     * @param {Ext.data.Store} store The store to which the record was
     *     added.
     * @param {Ext.data.Record} record The record object corresponding
     *     to the added layer.
     * @param {Integer} index The index in the store at which the record
     *     was added.
     * @private
     */
    onStoreAdd: function(store, record, index) {
        // to be implemented by subclasses if needed
    },

    /**
     * Updates the legend. Gets called when the store fires the update event.
     * This usually means the visibility of the layer, its style or title
     * has changed.
     *
     * @param {Ext.data.Store} store The store in which the record was
     *     changed.
     * @param {Ext.data.Record} record The record object corresponding
     *     to the updated layer.
     * @param {String} operation The type of operation.
     * @private
     */
    onStoreUpdate: function(store, record, operation) {
        // if we don't have items, we are already awaiting garbage
        // collection after being removed by LegendPanel::removeLegend, and
        // updating will cause errors
        if (record === this.layerRecord && this.items.getCount() > 0) {
            var layer = record.getLayer();
            this.setVisible(layer.getVisibility() &&
                layer.calculateInRange() && layer.displayInLayerSwitcher &&
                !record.get('hideInLegend'));
            this.update();
        }
    },

    /**
     * Updates the legend.
     *
     * @private
     */
    update: function() {
        var title = this.getLayerTitle(this.layerRecord);
        var item = this.items.get(0);
        if (item instanceof Ext.form.Label && this.getLabel() !== title) {
            // we need to update the title
            item.setText(title, false);
        }
    },

    /**
     * Get a title for the layer. If the record doesn't have a title, the
     * name will be returned.
     *
     * @param {GeoExt.data.LayerRecord} record
     * @return {String} The title of the layer.
     * @private
     */
    getLayerTitle: function(record) {
        var title = this.legendTitle || "";
        if (this.showTitle && !title) {
            if (record && !record.get("hideTitle")) {
                title = record.get("title") ||
                record.get("name") ||
                record.getLayer().name || "";
            }
        }
        return title;
    },

    /**
     * Unbinds event listeners prior to destroying.
     *
     * @private
     */
    beforeDestroy: function() {
        if (this.layerStore) {
            this.layerStore.un("update", this.onStoreUpdate, this);
            this.layerStore.un("remove", this.onStoreRemove, this);
            this.layerStore.un("add", this.onStoreAdd, this);
        }
        this.callParent();
    },

    /**
     * Nullifies members #layerRecord and #layerStore when the legend is being
     * destroyed.
     *
     * @private
     */
    onDestroy: function() {
        this.layerRecord = null;
        this.layerStore = null;
        this.callParent(arguments);
    }

});
