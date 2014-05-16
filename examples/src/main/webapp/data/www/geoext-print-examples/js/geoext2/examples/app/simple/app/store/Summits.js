/**
 * The store used for summits
 */
Ext.define('CF.store.Summits', {
    extend: 'GeoExt.data.FeatureStore',
    model: 'CF.model.Summit',
    autoLoad: false
});
