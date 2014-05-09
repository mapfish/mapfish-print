/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 * 
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

Ext.require([
    'GeoExt.panel.Map',
    'GeoExt.slider.Zoom',
    'GeoExt.slider.Tip'
]);

var panel, slider;

Ext.onReady(function() {
    
    // create a map panel with an embedded slider
    panel = Ext.create('GeoExt.MapPanel', {
        title: "Map",
        renderTo: "map-container",
        height: 300,
        width: 400,
        map: {
            controls: [new OpenLayers.Control.Navigation()],
            maxResolution: 0.703125,
            zoomMethod: null
        },
        layers: [new OpenLayers.Layer.WMS(
            "Global Imagery",
            "http://maps.opengeo.org/geowebcache/service/wms",
            {layers: "bluemarble"}
        )],
        extent: [-5, 35, 15, 55],
        items: [{
            xtype: "gx_zoomslider",
            vertical: true,
            height: 100,
            x: 10,
            y: 20,
            plugins: Ext.create('GeoExt.SliderTip', {
            	getText: function(thumb) {
            		var slider = thumb.slider;
            		var out = '<div>Zoom Level: {0}</div>' +
			        '<div>Resolution: {1}</div>' +
			        '<div>Scale: 1 : {2}</div>';
            		return Ext.String.format(out, slider.getZoom(), slider.getResolution(), slider.getScale());
            	}
            })
        }]
    });
    
    // create a separate slider bound to the map but displayed elsewhere
    slider = Ext.create('GeoExt.ZoomSlider', {
        map: panel.map,
        aggressive: true,                                                                                                                                                   
        width: 200,
        plugins: Ext.create('GeoExt.SliderTip', {
        	getText: function(thumb) {
        		return Ext.String.format('<div>Zoom Level: {0}</div>', thumb.slider.getZoom());
        	}
        }),
        renderTo: document.body
    });

});
