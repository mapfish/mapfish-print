/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 * 
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/** api: example[zoom-chooser]
 *  Scale Chooser
 *  -------------
 *  Use a ComboBox to display available map scales.
 */

var mapPanel;

Ext.require([
    'Ext.container.Viewport',
    'Ext.layout.container.Border',
    'Ext.form.ComboBox',
    'GeoExt.panel.Map',
    'GeoExt.data.ScaleStore'
]);

Ext.application({
    name: 'ScaleStore GeoExt2',
    launch: function() {
        var map = new OpenLayers.Map();
        var layer = new OpenLayers.Layer.WMS(
            "Global Imagery",
            "http://maps.opengeo.org/geowebcache/service/wms",
            {layers: "bluemarble"}
        );
        map.addLayer(layer);

        var scaleStore = Ext.create("GeoExt.data.ScaleStore", {map: map});
        var zoomSelector = Ext.create("Ext.form.ComboBox", {
            store: scaleStore,
            emptyText: "Zoom Level",
            listConfig: {
                getInnerTpl: function() {
                    return "1: {scale:round(0)}";
                }
            },
            editable: false,
            triggerAction: 'all', // needed so that the combo box doesn't filter by its current content
            queryMode: 'local' // keep the combo box from forcing a lot of unneeded data refreshes
        });

        zoomSelector.on('select', 
            function(combo, record, index) {
                map.zoomTo(record[0].get("level"));
            },
            this
        );     

        map.events.register('zoomend', this, function() {
            var scale = scaleStore.queryBy(function(record){
                return this.map.getZoom() == record.data.level;
            });

            if (scale.length > 0) {
                scale = scale.items[0];
                zoomSelector.setValue("1 : " + parseInt(scale.data.scale));
            } else {
                if (!zoomSelector.rendered) return;
                zoomSelector.clearValue();
            }
        });

        mapPanel = Ext.create("GeoExt.panel.Map", {
            title: "GeoExt MapPanel",
            renderTo: "mappanel",
            height: 400,
            width: 600,
            map: map,
            center: new OpenLayers.LonLat(5, 45),
            zoom: 4,
            bbar: [zoomSelector]
        });

    }
});

