/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

Ext.require([
    'Ext.Window',
    'GeoExt.panel.Map',
    'GeoExt.data.MapfishPrintProvider',
    'GeoExt.panel.PrintMap'
]);

var mapPanel, printDialog, printProvider;


Ext.application({
    name: 'PrintPreviewGeoExt2',

    launch: function() {

        // The PrintProvider that connects us to the print service
        printProvider = Ext.create('GeoExt.data.MapfishPrintProvider', {
            method: "GET", // "POST" recommended for production use
            capabilities: printCapabilities, // provide url instead for lazy loading
            customParams: {
                mapTitle: "GeoExt Printing Demo",
                comment: "This demo shows how to use GeoExt.PrintMapPanel"
            }
        });

    var style = {
            strokeColor: '#000000',
            strokeWidth: 2,
            strokeOpacity: 1,
            fillColor: '#ee0000',
            fillOpacity: 1,
            pointRadius: 6,
            graphicName: 'circle',
        },
        vecLayer = new OpenLayers.Layer.Vector("A Vector Layer", {
            styleMap: new OpenLayers.StyleMap({
                "default": style
            })
        }),
        // Both Burnie and Devonport (cities in Tasmania) are styled through the
        // above stylemap
        burnie = new OpenLayers.Feature.Vector(
            new OpenLayers.Geometry.Point(145.875278, -41.063611)
        ),
        devonport = new OpenLayers.Feature.Vector(
            new OpenLayers.Geometry.Point(146.333333, -41.166667)
        ),
        // Hobart (the capital) gets inline style, this will also be printable
        hobart = new OpenLayers.Feature.Vector(
            new OpenLayers.Geometry.Point(147.325, -42.880556),
            null,
            Ext.applyIf({graphicName: 'square'}, style)
        );
        // Add the cities to the layer
        vecLayer.addFeatures([burnie, devonport, hobart]);

        // A MapPanel with a "Print..." button
        mapPanel = Ext.create('GeoExt.panel.Map', {
            renderTo: "content",
            width: 500,
            height: 350,
            map: {
                maxExtent: new OpenLayers.Bounds(
                    143.835, -43.648,
                    148.479, -39.574
                ),
                maxResolution: 0.018140625,
                projection: "EPSG:4326",
                units: 'degrees'
            },
            layers: [
                new OpenLayers.Layer.WMS(
                    "Tasmania State Boundaries",
                    "http://demo.opengeo.org/geoserver/wms",
                    { layers: "topp:tasmania_state_boundaries" }
                ),
                vecLayer
            ],
            center: [146.56, -41.56],
            zoom: 0,
            bbar: [{
                text: "Print...",
                handler: function(){
                    // A window with the PrintMapPanel, which we can use to adjust
                    // the print extent before creating the pdf.
                    printDialog = Ext.create('Ext.Window', {
                        title: "Print Preview",
                        layout: "fit",
                        border: false,
                        width: 350,
                        autoHeight: true,
                        items: [{
                            xtype: "gx_printmappanel",
                            sourceMap: mapPanel,
                            printProvider: printProvider
                        }],
                        bbar: [{
                            text: "Create PDF",
                            handler: function(){ printDialog.items.get(0).print(); }
                        }]
                    });
                    printDialog.show();
                }
            }]
        });
    }
});
