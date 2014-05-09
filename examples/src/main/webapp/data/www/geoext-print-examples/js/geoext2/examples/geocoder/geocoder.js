/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 * 
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/** api: example[geocoder]
 *  GeocoderComboBox
 *  ----------------
 *  MapPanel with a OSM Nominatim search field.
 */

var mapPanel;

Ext.require([
    'GeoExt.panel.Map',
    'GeoExt.form.field.GeocoderComboBox'
]);

Ext.application({
    name: 'GeocoderComboBox GeoExt2',
    launch: function() {
        var locationLayer = new OpenLayers.Layer.Vector("Location", {
            styleMap: new OpenLayers.Style({
                externalGraphic: "http://openlayers.org/api/img/marker.png",
                graphicYOffset: -25,
                graphicHeight: 25,
                graphicTitle: "${name}"
            })
        });
        mapPanel = Ext.create("GeoExt.panel.Map", {
            renderTo: "mappanel",
            height: 400,
            width: 500,
            layers: [new OpenLayers.Layer.OSM(), locationLayer],
            zoom: 1,
            tbar: [{
                xtype: "gx_geocodercombo",
                layer: locationLayer,
                // To restrict the search to a bounding box, uncomment the following
                // line and change the viewboxlbrt parameter to a left,bottom,right,top
                // bounds in EPSG:4326:
                //url: "http://nominatim.openstreetmap.org/search?format=json&viewboxlbrt=15,47,17,49",
                width: 200
            }]
        });
    }
});
