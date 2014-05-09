/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 * 
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/** api: example[wfscapabilities]
 *  WFS Capabilities Store
 *  ----------------------
 *  Create layer records from WFS capabilities documents.
 */

var store;

Ext.require([
    'Ext.data.writer.Json',
    'Ext.grid.Panel',
    'GeoExt.data.reader.WfsCapabilities',
    'GeoExt.data.WfsCapabilitiesLayerStore',
    'GeoExt.panel.Map'
]);

Ext.application({
    name: 'WFSGetCapabilities',
    launch: function() {
        // create a new WFS capabilities store
        store = Ext.create("GeoExt.data.WfsCapabilitiesStore", {
            url: "../data/wfscap_tiny_100.xml",
            autoLoad: true,
            // set as a function that returns a hash of layer options.  This allows
            // to have new objects created upon each new OpenLayers.Layer.Vector
            // object creations.
            layerOptions: function() {
                return {
                    visibility: false,
                    displayInLayerSwitcher: false,
                    strategies: [new OpenLayers.Strategy.BBOX({ratio: 1})]
                };
            }
        });
        // create a grid to display records from the store
        var grid = Ext.create("Ext.grid.GridPanel", {
            title: "WFS Capabilities",
            store: store,
            columns: [
                {header: "Title", dataIndex: "title", sortable: true, width: 250},
                {header: "Name", dataIndex: "name", sortable: true},
                {header: "Namespace", dataIndex: "namespace", sortable: true, width: 150},
                {id: "description", header: "Description", dataIndex: "abstract"}
            ],
            renderTo: "capgrid",
            height: 300,
            width: 650
        });

    }
});
