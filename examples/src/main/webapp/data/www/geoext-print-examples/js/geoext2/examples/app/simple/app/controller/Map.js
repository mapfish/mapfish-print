/**
 * Map controller
 * Used to manage map layers and showing their related views
 */
Ext.define('CF.controller.Map', {
    extend: 'Ext.app.Controller',

    models: ['Summit'],
    stores: ['Summits'],

    refs: [
        {ref: 'summitChart', selector: 'summitchart'},
        {ref: 'summitGrid', selector: 'summitgrid'}
    ],

    init: function() {
        var me = this;

        me.getSummitsStore().on({
            scope: me,
            load : me.onSummitsStoreLoad
        });

        this.control({
            'cf_mappanel': {
                'beforerender': this.onMapPanelBeforeRender
            }
        }, this);
    },

    onMapPanelBeforeRender: function(mapPanel, options) {
        var me = this;

        var layers = [];

        // OpenLayers object creating
        var wms = new OpenLayers.Layer.WMS(
            "OpenLayers WMS",
            "http://vmap0.tiles.osgeo.org/wms/vmap0?",
            {layers: 'basic'}
        );
        layers.push(wms);

        // create vector layer
        var context = {
            getColor: function(feature) {
                if (feature.attributes.elevation < 2000) {
                    return 'green';
                }
                if (feature.attributes.elevation < 2300) {
                    return 'orange';
                }
                return 'red';
            }
        };
        var template = {
            cursor: "pointer",
            fillOpacity: 0.5,
            fillColor: "${getColor}",
            pointRadius: 5,
            strokeWidth: 1,
            strokeOpacity: 1,
            strokeColor: "${getColor}",
            graphicName: "triangle"
        };
        var style = new OpenLayers.Style(template, {context: context});
        var vecLayer = new OpenLayers.Layer.Vector("vector", {
            styleMap: new OpenLayers.StyleMap({
                'default': style
            }),
            protocol: new OpenLayers.Protocol.HTTP({
                url: "../../data/summits.json",
                format: new OpenLayers.Format.GeoJSON()
            }),
            strategies: [new OpenLayers.Strategy.Fixed()]
        });
        layers.push(vecLayer);

        // manually bind store to layer
        me.getSummitsStore().bind(vecLayer);

        mapPanel.map.addLayers(layers);

        // some more controls
        mapPanel.map.addControls([new OpenLayers.Control.DragFeature(vecLayer, {
            autoActivate: true,
            onComplete: function(feature, px) {
                var store = me.getSummitsStore();
                store.fireEvent('update', store, store.getByFeature(feature));
            }
        })]);

        // for dev purpose
        map = mapPanel.map;
        mapPanel = mapPanel;
    },

    onLaunch: function() {
        var me = this;

        // for dev purpose
        ctrl = this;
    },

    onSummitsStoreLoad: function(store, records) {
        // do custom stuff on summits load if you want, for example here we
        // zoom to summits extent
        var dataExtent = store.layer.getDataExtent();
        if (dataExtent) {
            store.layer.map.zoomToExtent(dataExtent);
        }
    }
});
