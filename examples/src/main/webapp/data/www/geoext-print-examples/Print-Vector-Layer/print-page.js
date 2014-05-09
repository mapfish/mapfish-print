/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 * 
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/** api: example[print-page]
 *  Print Your Map
 *  --------------
 *  Print the visible extent of a MapPanel with PrintPage and MapfishPrintProvider.
 */

Ext.require([
    'Ext.layout.container.Border',
    'Ext.form.field.Checkbox',
    'GeoExt.data.MapfishPrintProvider',
    'GeoExt.data.PrintPage',
    'GeoExt.panel.Map',
    'GeoExt.panel.Legend',
    'GeoExt.container.WmsLegend',
    'GeoExt.container.UrlLegend',
    'GeoExt.container.VectorLegend'
]);

var mapPanel, printPage, printProvider;
var matrixIds = new Array(31);
for (var i = 0; i < matrixIds.length; ++i) {
    matrixIds[i] = "EPSG:900913:" + i;
}

Ext.application({
    name: 'PrintPage and PrintProvider - GeoExt2',
    launch: function () {
        // The MapfishPrintProvider that connects us to the print service
        printProvider = Ext.create('GeoExt.data.MapfishPrintProvider', {
            method: "POST", // "POST" recommended for production use
            capabilities: printCapabilities, // from the info.json script in the html
            customParams: {
                mapTitle: "Printing Demo",
                comment: "This is a simple map printed from GeoExt."
            }
        });
        // Our print page. Tells the printProvider about the scale and center of
        // our page.
        printPage = Ext.create('GeoExt.data.PrintPage', {
            printProvider: printProvider
        });

        // allow testing of specific renderers via "?renderer=Canvas", etc
        var renderer = OpenLayers.Util.getParameters(window.location.href).renderer;
        renderer = (renderer) ? [renderer] : OpenLayers.Layer.Vector.prototype.renderers;
        /*
         * Layer style
         */
        // we want opaque external graphics and non-opaque internal graphics
        var layer_style = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style['default']);
        layer_style.fillOpacity = 0.2;
        layer_style.graphicOpacity = 1;

        /*
         * Blue style
         */
        var style_blue = OpenLayers.Util.extend({}, layer_style);
        style_blue.strokeColor = "blue";
        style_blue.fillColor = "blue";
        style_blue.graphicName = "star";
        style_blue.pointRadius = 10;
        style_blue.strokeWidth = 3;
        style_blue.rotation = 45;
        style_blue.strokeLinecap = "butt";

        /*
         * Green style
         */
        var style_green = {
            strokeColor: "#00FF00",
            strokeWidth: 3,
            strokeDashstyle: "dashdot",
            pointRadius: 6,
            pointerEvents: "visiblePainted",
            title: "this is a green line"
        };

        var vectorLayer = new OpenLayers.Layer.Vector("Simple Geometry", {
            style: layer_style,
            renderers: renderer
        });

        // create a point feature
        var point = new OpenLayers.Geometry.Point(-8234566.427097, 4979131.070529);
        var pointFeature = new OpenLayers.Feature.Vector(point,null,style_blue);

        // create a line feature from a list of points
        var pointList = [
            point,
            new OpenLayers.Geometry.Point(point.x + 500, point.y),
            new OpenLayers.Geometry.Point(point.x, point.y+500),
            new OpenLayers.Geometry.Point(point.x - 500, point.y + 1000)
        ];
        var lineFeature = new OpenLayers.Feature.Vector(
            new OpenLayers.Geometry.LineString(pointList),null,style_green);

        // create a polygon feature from a linear ring of points
        var pointList = [];
        for(var p=0; p<6; ++p) {
            var a = p * (2 * Math.PI) / 7;
            var r = Math.random(1) + 1000;
            var newPoint = new OpenLayers.Geometry.Point(point.x + (r * Math.cos(a)),
                point.y + (r * Math.sin(a)));
            pointList.push(newPoint);
        }
        pointList.push(pointList[0]);

        var linearRing = new OpenLayers.Geometry.LinearRing(pointList);
        var polygonFeature = new OpenLayers.Feature.Vector(
            new OpenLayers.Geometry.Polygon([linearRing]));


        vectorLayer.addFeatures([pointFeature, lineFeature, polygonFeature]);

        // The map we want to print
        mapPanel = Ext.create('GeoExt.panel.Map', {
            region: "center",
            layers: [
                new OpenLayers.Layer.WMS(
                    Env.layers.newYork.name,
                    Env.wmsUrl,
                    { layers: [Env.layers.newYork.id], format: "image/png"},
                    { singleTile: true, isBaseLayer: true}
                ),
                vectorLayer
            ],
            map: {
                projection: "EPSG:900913"
            },
            center: [-8234566.427097, 4979131.070529],
            zoom: 14
        });
        // The legend to optionally include on the printout
        var legendPanel = Ext.create('GeoExt.panel.Legend', {
            region: "west",
            width: 150,
            bodyStyle: "padding:5px",
            layerStore: mapPanel.layers
        });

        var includeLegend; // controlled by the "Include legend?" checkbox

        // The main panel
        Ext.create('Ext.Panel', {
            renderTo: "content",
            layout: "border",
            width: 780,
            height: 330,
            items: [mapPanel, legendPanel],
            bbar: ["->", {
                text: "Print",
                handler: function () {
                    // convenient way to fit the print page to the visible map area
                    printPage.fit(mapPanel, true);
                    // print the page, optionally including the legend
                    printProvider.print(mapPanel, printPage, includeLegend && {legend: legendPanel});
                }
            }, {
                xtype: "checkbox",
                boxLabel: "Include legend?",
                handler: function () {
                    includeLegend = this.checked;
                }
            }]
        });
    }
});