/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 * 
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/** api: example[wms-capabilities]
 *  WMS Capabilities Store
 *  ----------------------
 *  Create layer records from WMS capabilities documents.
 */

var store;

Ext.require([
    'Ext.data.writer.Json',
    'Ext.grid.Panel',
    'GeoExt.data.reader.WmsCapabilities',
    'GeoExt.data.WmsCapabilitiesLayerStore',
    'GeoExt.panel.Map'
]);

Ext.application({
    name: 'WMSGetCapabilities',
    launch: function() {
        
        // create a new WMS capabilities store
        store = Ext.create('GeoExt.data.WmsCapabilitiesStore', {
            storeId: 'wmscapsStore',
            url: "../data/wmscap.xml",
            autoLoad: true
        });
    
        // create a grid to display records from the store
        var grid = Ext.create('Ext.grid.Panel', {
            title: "WMS Capabilities",
            store: Ext.data.StoreManager.lookup('wmscapsStore'),
            columns: [
                {header: "Title", dataIndex: "title", sortable: true},
                {header: "Name", dataIndex: "name", sortable: true},
                {header: "Queryable", dataIndex: "queryable", sortable: true, width: 70},
                {id: "description", header: "Description", dataIndex: "abstract", flex: 1}
            ],
            renderTo: "capgrid",
            height: 300,
            width: 650,
            listeners: {
                itemdblclick: mapPreview
            }
        });
        
        function mapPreview(grid, record) {
            var layer = record.getLayer().clone();
            var win = Ext.create('Ext.Window', {
                title: "Preview: " + record.get("title"),
                width: 512,
                height: 256,
                layout: "fit",
                items: [{
                    xtype: "gx_mappanel",
                    layers: [layer],
                    extent: record.get("llbbox")
                }]
            });
            win.show();
        }
    }
});
