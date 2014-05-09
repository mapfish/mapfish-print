/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 * 
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/** api: example[layeropacityslider]
 *  Layer Opacity Slider
 *  --------------------
 *  Use a slider to control layer opacity.
 */

var panel1, panel2, wms, slider;

Ext.require([
    'Ext.container.Viewport',
    'Ext.layout.container.Border',
    'GeoExt.panel.Map',
    'GeoExt.slider.LayerOpacity',
    'GeoExt.slider.Tip'
]);

Ext.application({
    name: 'LayerOpacitySlider GeoExt2',
    launch: function() {
        wms = new OpenLayers.Layer.WMS(
            "Global Imagery",
            "http://maps.opengeo.org/geowebcache/service/wms",
            {layers: "bluemarble"}
        );

        // create a map panel with an embedded slider
        panel1 = Ext.create('GeoExt.panel.Map', {
            title: "Map 1",
            renderTo: "map1-container",
            height: 300,
            width: 400,
            map: {
                controls: [new OpenLayers.Control.Navigation()]
            },
            layers: [wms],
            extent: [-5, 35, 15, 55],
            items: [{
                xtype: "gx_opacityslider",
                layer: wms,
                vertical: true,
                height: 120,
                x: 10,
                y: 10,
                plugins: Ext.create("GeoExt.slider.Tip", {
                    getText: function(thumb) {
                        return Ext.String.format('Opacity: {0}%', thumb.value);
                    }
                })
            }]
        });
        // create a separate slider bound to the map but displayed elsewhere
        slider = Ext.create('GeoExt.slider.LayerOpacity', {
            layer: wms,
            aggressive: true, 
            width: 200,
            isFormField: true,
            inverse: true,
            fieldLabel: "opacity",
            renderTo: "slider",
            plugins: Ext.create("GeoExt.slider.Tip", {
                getText: function(thumb) {
                    return Ext.String.format('Transparency: {0}%', thumb.value);
                }
            })
        });
        
        var clone = wms.clone();
        var wms2 = new OpenLayers.Layer.WMS(
            "OpenLayers WMS",
            "http://vmap0.tiles.osgeo.org/wms/vmap0",
            {layers: 'basic'}
        );
        panel2 = Ext.create('GeoExt.panel.Map', {
            title: "Map 2",
            renderTo: "map2-container",
            height: 300,
            width: 400,
            map: {
                controls: [new OpenLayers.Control.Navigation()]
            },
            layers: [wms2, clone],
            extent: [-5, 35, 15, 55],
            items: [{
                xtype: "gx_opacityslider",
                layer: clone,
                complementaryLayer: wms2,
                changeVisibility: true,
                aggressive: true,
                vertical: true,
                height: 120,
                x: 10,
                y: 10,
                plugins: Ext.create("GeoExt.slider.Tip", {
                    getText: function(thumb) {
                        return Ext.String.format('{0}%', thumb.value);
                    }
                })
            }]
        });
    }
});
