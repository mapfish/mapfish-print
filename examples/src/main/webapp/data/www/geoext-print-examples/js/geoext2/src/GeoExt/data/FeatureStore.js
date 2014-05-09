/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include OpenLayers/Feature/Vector.js
 * @include GeoExt/data/reader/Feature.js
 */

/**
 * A store that synchronizes a features array of an `OpenLayers.Layer.Vector`.
 *
 * @class GeoExt.data.FeatureStore
 */
Ext.define('GeoExt.data.FeatureStore', {
    extend: 'Ext.data.Store',
    requires: [
        'GeoExt.data.reader.Feature'
    ],

    statics: {
        /**
         * @static
         * @property {Number} LAYER_TO_STORE
         * Bitfield specifying the layer to store sync direction.
         */
        LAYER_TO_STORE: 1,
        /**
         * @static
         * @property {Number} STORE_TO_LAYER
         * Bitfield specifying the store to layer sync direction.
         */
        STORE_TO_LAYER: 2
    },

    /**
     * Fires when the store is bound to a layer.
     *
     * @event bind
     * @param {GeoExt.data.FeatureStore} store
     * @param {OpenLayers.Layer.Vector} layer
     */

    /**
     * True when the vector layer is binded.
     *
     * @private
     * @property {OpenLayers.Layer.Vector}
     */
    isLayerBinded: false,

    /**
     * Layer that this store will be in sync with. If not provided, the
     * store will not be bound to a layer.
     *
     * @cfg {OpenLayers.Layer.Vector} layer
     */

    /**
     * Vector layer that the store is synchronized with, if any.
     *
     * @property {OpenLayers.Layer.Vector} layer
     */
    layer: null,

    /**
     * @cfg {OpenLayers.Layer/Array} features
     * Features that will be added to the store (and the layer, depending on the
     * value of the `initDir` option.
     */

    /**
     * @cfg {Number} initDir
     * Bitfields specifying the direction to use for the initial sync between
     * the layer and the store, if set to 0 then no initial sync is done.
     * Defaults to `GeoExt.data.FeatureStore.LAYER_TO_STORE|GeoExt.data.FeatureStore.STORE_TO_LAYER`
     */

    /**
     * @cfg {OpenLayers.Filter} featureFilter
     * This filter is evaluated before a feature record is added to the store.
     */
    featureFilter: null,

    /**
     * @param {Object} config Creation parameters
     * @private
     */
    constructor: function(config) {
        config = Ext.apply({
            proxy: {
                type: 'memory',
                reader: {
                    type: 'feature',
                    idProperty: 'id'
                }
            }
        }, config);

        if (config.layer) {
            this.layer = config.layer;
            delete config.layer;
        }

        // features option. Alias to data option
        if (config.features) {
            config.data = config.features;
        }
        delete config.features;

        this.callParent([config]);

        var options = {initDir: config.initDir};
        delete config.initDir;

        if (this.layer) {
            this.bind(this.layer, options);
        }
    },

    /**
     * Unbinds own listeners by calling #unbind when being destroyed.
     *
     * @private
     */
    destroy: function() {
        this.unbind();
        this.callParent();
    },

    /**
     * Bind this store to a layer instance. Once bound the store
     * is synchronized with the layer and vice-versa.
     *
     * @param {OpenLayers.Layer.Vector} layer The layer instance.
     * @param {Object} options
     */
    bind: function(layer, options) {
        options = options || {};

        if (this.isLayerBinded) {
            // already bound
            return;
        }
        this.layer = layer;
        this.isLayerBinded = true;

        var initDir = options.initDir;
        if (options.initDir == undefined) {
            initDir = GeoExt.data.FeatureStore.LAYER_TO_STORE |
                GeoExt.data.FeatureStore.STORE_TO_LAYER;
        }

        var features = layer.features.slice(0);

        if (initDir & GeoExt.data.FeatureStore.STORE_TO_LAYER) {
            this.each(function(record) {
                layer.addFeatures([record.raw]);
            }, this);
        }

        if (initDir & GeoExt.data.FeatureStore.LAYER_TO_STORE &&
                layer.features.length > 0) {
            // append a snapshot of the layer's features
            this.loadRawData(features, true);
        }

        this.layer.events.on({
            'featuresadded': this.onFeaturesAdded,
            'featuresremoved': this.onFeaturesRemoved,
            'featuremodified': this.onFeatureModified,
            scope: this
        });
        this.on({
            'load': this.onLoad,
            'clear': this.onClear,
            'add': this.onAdd,
            'remove': this.onRemove,
            'update': this.onStoreUpdate,
            scope: this
        });

        this.fireEvent("bind", this, this.layer);
    },

    /**
     * Unbind this store from his layer instance.
     */
    unbind: function() {
        if (this.isLayerBinded) {
            this.layer.events.un({
                'featuresadded': this.onFeaturesAdded,
                'featuresremoved': this.onFeaturesRemoved,
                'featuremodified': this.onFeatureModified,
                scope: this
            });
            this.un({
                'load': this.onLoad,
                'clear': this.onClear,
                'add': this.onAdd,
                'remove': this.onRemove,
                'update': this.onStoreUpdate,
                scope: this
            });
            this.layer = null;
            this.isLayerBinded = false;
        }
    },

    /**
     * Convenience method to add features.
     *
     * @param {OpenLayers.Feature.Vector[]} features The features to add.
     */
    addFeatures: function(features) {
        return this.loadRawData(features, true);
    },

    /**
     * Convenience method to remove features.
     *
     * @param {OpenLayers.Feature.Vector[]} features The features to remove.
     */
    removeFeatures: function(features) {
        //accept both a single-argument array of records, or any number of record arguments
        if (!Ext.isArray(features)) {
            features = Array.prototype.slice.apply(arguments);
        } else {
            // Create an array copy
            features = features.slice(0);
        }
        Ext.Array.each(features, function(feature) {
            this.remove(this.getByFeature(feature));
        }, this);
    },

    /**
     * Returns the record corresponding to a feature.
     *
     * @param {OpenLayers.Feature} feature An OpenLayers.Feature.Vector object.
     * @return {String} The model instance corresponding to a feature.
     */
    getByFeature: function(feature) {
        return this.getAt(this.findBy(function(record, id) {
            return record.raw == feature;
        }));
    },

    /**
     * Returns the record corresponding to a feature id.
     *
     * @param {String} id An OpenLayers.Feature.Vector id string.
     * @return {String} The model instance corresponding to the given id.
     */
    getById: function(id) {
        return (this.snapshot || this.data).findBy(function(record) {
            return record.raw.id === id;
        });
    },

    /**
     * Adds the given records to the associated layer.
     *
     * @param {Ext.data.Model[]} records
     * @private
     */
    addFeaturesToLayer: function(records) {
        var features = [];
        for (var i = 0, len = records.length; i < len; i++) {
            features.push(records[i].raw);
        }
        this._adding = true;
        this.layer.addFeatures(features);
        delete this._adding;
    },

    /**
     * Handler for layer featuresadded event.
     *
     * @param {Object} evt
     * @private
     */
    onFeaturesAdded: function(evt) {
        if (!this._adding) {
            var features = evt.features,
                toAdd = features;
            if (this.featureFilter) {
                toAdd = [];
                for (var i = 0, len = features.length; i < len; i++) {
                    var feature = features[i];
                    if (this.featureFilter.evaluate(feature) !== false) {
                        toAdd.push(feature);
                    }
                }
            }
            toAdd = this.proxy.reader.read(toAdd).records;
            this._adding = true;
            this.add(toAdd);
            delete this._adding;
        }
    },

    /**
     * Handler for layer featuresremoved event.
     *
     * @param {Object} evt
     * @private
     */
    onFeaturesRemoved: function(evt) {
        if (!this._removing) {
            var features = evt.features;
            for (var i = features.length - 1; i >= 0; i--) {
                var record = this.getByFeature(features[i]);
                if (record) {
                    this._removing = true;
                    this.remove(record);
                    delete this._removing;
                }
            }
        }
    },

    /**
     * Handler for layer featuremodified event.
     *
     * @param {Object} evt
     * @private
     */
    onFeatureModified: function(evt) {
        var record_old = this.getByFeature(evt.feature);
        if (record_old) {
            var record_new = this.proxy.reader.read(evt.feature).records[0];
            Ext.Object.each(record_new.getData(), function(key, value) {
                record_old.set(key, value);
            }, this);
        }
    },

    /**
     * Handler for a store's load event.
     *
     * @param {Ext.data.Store} store
     * @param {Ext.data.Model[]} records
     * @param {Boolean} successful
     * @private
     */
    onLoad: function(store, records, successful) {
        if (successful) {
            this._removing = true;
            this.layer.removeAllFeatures();
            delete this._removing;

            this.addFeaturesToLayer(records);
        }
    },

    /**
     * Handler for a store's clear event.
     *
     * @param {Ext.data.Store} store
     * @private
     */
    onClear: function(store) {
        this._removing = true;
        this.layer.removeFeatures(this.layer.features);
        delete this._removing;
    },

    /**
     * Handler for a store's add event.
     *
     * @param {Ext.data.Store} store
     * @param {Ext.data.Model[]} records
     * @param {Number} index
     * @private
     */
    onAdd: function(store, records, index) {
        if (!this._adding) {
            // addFeaturesToLayer takes care of setting
            // this._adding to true and deleting it
            this.addFeaturesToLayer(records);
        }
    },

    /**
     * Handler for a store's remove event.
     *
     * @param {Ext.data.Store} store
     * @param {Ext.data.Model} record
     * @param {Number} index
     * @private
     */
    onRemove: function(store, record, index) {
        if (!this._removing) {
            var feature = record.raw;
            if (this.layer.getFeatureById(feature.id) != null) {
                this._removing = true;
                this.layer.removeFeatures([feature]);
                delete this._removing;
            }
        }
    },

    /**
     * Handler for a store's update event.
     *
     * @param {Ext.data.Store} store
     * @param {Ext.data.Model} record
     * @param {Number} operation
     * @param {Array} modifiedFieldNames
     *
     * @private
     */
    onStoreUpdate: function(store, record, operation, modifiedFieldNames) {
        if (!this._updating) {
            var feature = record.raw;
            if (feature.state !== OpenLayers.State.INSERT) {
                feature.state = OpenLayers.State.UPDATE;
            }
            var cont = this.layer.events.triggerEvent('beforefeaturemodified', {
                feature: feature
            });
            if (cont !== false) {
                Ext.Array.each(modifiedFieldNames, function(field) {
                    feature.attributes[field] = record.get(field);
                });
                this._updating = true;
                this.layer.events.triggerEvent('featuremodified', {
                    feature: feature
                });
                delete this._updating;
            }
        }
    },

    /**
     * @inheritdoc
     *
     * The event firing behaviour of Ext.4.1 is reestablished here. See also:
     * [This discussion on the Sencha forum](http://www.sencha.com/forum/
     * showthread.php?253596-beforeload-is-not-fired-by-loadRawData)
     *
     * In version 4.2.1 this method reads
     *
     *     //...
     *     loadRawData : function(data, append) {
     *         var me      = this,
     *             result  = me.proxy.reader.read(data),
     *             records = result.records;
     *
     *         if (result.success) {
     *             me.totalCount = result.total;
     *             me.loadRecords(records, append ? me.addRecordsOptions : undefined);
     *         }
     *     },
     *     // ...
     *
     * While the previous version 4.1.3 has also
     * the line `me.fireEvent('load', me, records, true);`:
     *
     *     // ...
     *     if (result.success) {
     *         me.totalCount = result.total;
     *         me.loadRecords(records, append ? me.addRecordsOptions : undefined);
     *         me.fireEvent('load', me, records, true);
     *     }
     *     // ...
     *
     * Our overwritten method has the code from 4.1.3, so that the #load-event
     * is being fired.
     *
     * See also the source code of [version 4.1.3](http://docs-origin.sencha.
     * com/extjs/4.1.3/source/Store.html#Ext-data-Store-method-loadRawData) and
     * of [version 4.2.1](http://docs-origin.sencha.com/extjs/4.2.1/source/
     * Store.html#Ext-data-Store-method-loadRawData).
     */
    loadRawData : function(data, append) {
        var me      = this,
            result  = me.proxy.reader.read(data),
            records = result.records;

        if (result.success) {
            me.totalCount = result.total;
            me.loadRecords(records, append ? me.addRecordsOptions : undefined);
            me.fireEvent('load', me, records, true);
        }
    }
});
