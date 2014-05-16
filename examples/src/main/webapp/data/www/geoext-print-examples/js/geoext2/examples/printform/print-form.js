/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 * 
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/** api: example[print-form]
 *  Print Configuration with a Form
 *  -------------------------------
 *  Use form field plugins to control print output.
 */

Ext.require([
    'Ext.layout.container.Border',
    'Ext.Panel',
    'Ext.form.FormPanel',
    'GeoExt.data.MapfishPrintProvider',
    'GeoExt.panel.Map',
    'GeoExt.data.PrintPage',
    'GeoExt.plugins.PrintPageField',
    'GeoExt.plugins.PrintProviderField'
]);

var mapPanel, printPage;

Ext.application({
    name: 'PrintPageFieldAndPrintProviderField',
    launch: function() {

        // The printProvider that connects us to the print service
        var printProvider = Ext.create('GeoExt.data.MapfishPrintProvider', {
            method: "GET", // "POST" recommended for production use
            capabilities: printCapabilities, // from the info.json script in the html
            customParams: {
                mapTitle: "Printing Demo"
            }
        });
        // Our print page. Stores scale, center and rotation and gives us a page
        // extent feature that we can add to a layer.
        printPage = Ext.create('GeoExt.data.PrintPage', {
            printProvider: printProvider
        });
        // A layer to display the print page extent
        var pageLayer = new OpenLayers.Layer.Vector();
        pageLayer.addFeatures(printPage.feature);
    
        // The map we want to print
        mapPanel = Ext.create('GeoExt.panel.Map', {
            region: "center",
            map: {
                eventListeners: {
                    // recenter/resize page extent after pan/zoom
                    "moveend": function(){ printPage.fit(this, {mode: "screen"}); }
                }
            },
            layers: [
                new OpenLayers.Layer.WMS("Tasmania", "http://demo.opengeo.org/geoserver/wms",
                    {layers: "topp:tasmania_state_boundaries"}, {singleTile: true}),
                pageLayer
            ],
            center: [146.56, -41.56],
            zoom: 6
        });
        // The form with fields controlling the print output
        var formPanel = Ext.create('Ext.form.FormPanel', {
            region: "west",
            width: 250,
            bodyStyle: "padding:5px",
            labelAlign: "top",
            defaults: {anchor: "100%"},
            items: [{
                xtype: "textarea",
                name: "comment",
                value: "",
                fieldLabel: "Comment",
                allowBlank: false,
                plugins: Ext.create('GeoExt.plugins.PrintPageField', {
                    printPage: printPage
                })
            }, {
                xtype: "combo",
                store: printProvider.layouts,
                displayField: "name",
                fieldLabel: "Layout",
                typeAhead: true,
                queryMode: "local",
                triggerAction: "all",
                plugins: Ext.create('GeoExt.plugins.PrintProviderField', {
                    printProvider: printProvider
                })
            }, {
                xtype: "combo",
                store: printProvider.dpis,
                displayField: "name",
                fieldLabel: "Resolution",
                displayTpl: Ext.create('Ext.XTemplate', '<tpl for=".">{name} dpi</tpl>'),
                tpl: '<tpl for="."><li role="option" class="x-boundlist-item">{name} dpi</li></tpl>',
                typeAhead: true,
                queryMode: "local",
                triggerAction: "all",
                plugins: Ext.create('GeoExt.plugins.PrintProviderField', {
                    printProvider: printProvider
                })
            }, {
                xtype: "combo",
                store: printProvider.scales,
                displayField: "name",
                fieldLabel: "Scale",
                typeAhead: true,
                queryMode: "local",
                triggerAction: "all",
                plugins: Ext.create('GeoExt.plugins.PrintPageField',{
                    printPage: printPage
                })
            }, {
                xtype: "textfield",
                name: "rotation",
                fieldLabel: "Rotation",
                plugins: Ext.create('GeoExt.plugins.PrintPageField',{
                    printPage: printPage
                })
            }],
            buttons: [{
                text: "Create PDF",
                handler: function() {
                    if (formPanel.getForm().isValid()) {
                        printProvider.print(mapPanel, printPage);
                    } else {
                        Ext.Msg.show({
                            title: "Invalid form",
                            msg: "The values in the form are invalid.",
                            icon: Ext.Msg.ERROR
                        });
                    }
                }
            }]
        });
        // The main panel
        Ext.create('Ext.Panel', {
            renderTo: "content",
            layout: "border",
            width: 700,
            height: 420,
            items: [mapPanel, formPanel]
        });
    }
});
