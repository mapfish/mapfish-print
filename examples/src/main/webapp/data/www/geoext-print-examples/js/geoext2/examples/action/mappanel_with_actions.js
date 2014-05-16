/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 * 
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

Ext.require([
    'Ext.container.Viewport',
    'Ext.window.MessageBox',
    'GeoExt.panel.Map',
    'GeoExt.Action'
]);

Ext.application({
    name: 'ActionExample',
    launch: function(){

        var map = new OpenLayers.Map({});
        map.addControl(new OpenLayers.Control.LayerSwitcher());
        var wms = new OpenLayers.Layer.WMS(
            "OpenLayers WMS",
            "http://vmap0.tiles.osgeo.org/wms/vmap0?",
            {layers: 'basic'}
        );

        var vector = new OpenLayers.Layer.Vector("vector");
        map.addLayers([wms, vector]);
        
        var ctrl, toolbarItems = [], action, actions = {};
        
        // ZoomToMaxExtent control, a "button" control
        action = Ext.create('GeoExt.Action', {
            control: new OpenLayers.Control.ZoomToMaxExtent(),
            map: map,
            text: "max extent",
            tooltip: "zoom to max extent"
        });
        actions["max_extent"] = action;
        toolbarItems.push(Ext.create('Ext.button.Button', action));
        toolbarItems.push("-");
        
        // Navigation control and DrawFeature controls
        // in the same toggle group
        action = Ext.create('GeoExt.Action', {
            text: "nav",
            control: new OpenLayers.Control.Navigation(),
            map: map,
            // button options
            toggleGroup: "draw",
            allowDepress: false,
            pressed: true,
            tooltip: "navigate",
            // check item options
            group: "draw",
            checked: true
        });
        actions["nav"] = action;
        toolbarItems.push(Ext.create('Ext.button.Button', action));
        
        action = Ext.create('GeoExt.Action', {
            text: "draw poly",
            control: new OpenLayers.Control.DrawFeature(vector, OpenLayers.Handler.Polygon),
            map: map,
            // button options
            toggleGroup: "draw",
            allowDepress: false,
            tooltip: "draw polygon",
            // check item options
            group: "draw"
        });
        actions["draw_poly"] = action;
        toolbarItems.push(Ext.create('Ext.button.Button', action));
        
        action = Ext.create('GeoExt.Action', {
            text: "draw line",
            control: new OpenLayers.Control.DrawFeature(vector, OpenLayers.Handler.Path),
            map: map,
            // button options
            toggleGroup: "draw",
            allowDepress: false,
            tooltip: "draw line",
            // check item options
            group: "draw"
        });
        actions["draw_line"] = action;
        toolbarItems.push(Ext.create('Ext.button.Button', action));
        toolbarItems.push("-");
        
        // SelectFeature control, a "toggle" control
        action = Ext.create('GeoExt.Action', {
            text: "select",
            control: new OpenLayers.Control.SelectFeature(vector, {
                type: OpenLayers.Control.TYPE_TOGGLE,
                hover: true
            }),
            map: map,
            // button options
            enableToggle: true,
            tooltip: "select feature"
        });
        actions["select"] = action;
        toolbarItems.push(Ext.create('Ext.button.Button', action));
        toolbarItems.push("-");
        
        // Navigation history - two "button" controls
        ctrl = new OpenLayers.Control.NavigationHistory();
        map.addControl(ctrl);
        
        action = Ext.create('GeoExt.Action', {
            text: "previous",
            control: ctrl.previous,
            disabled: true,
            tooltip: "previous in history"
        });
        actions["previous"] = action;
        toolbarItems.push(Ext.create('Ext.button.Button', action));
        
        action = Ext.create('GeoExt.Action', {
            text: "next",
            control: ctrl.next,
            disabled: true,
            tooltip: "next in history"
        });
        actions["next"] = action;
        toolbarItems.push(Ext.create('Ext.button.Button', action));
        toolbarItems.push("->");
        
        // Reuse the GeoExt.Action objects created above
        // as menu items
        toolbarItems.push({
            text: "menu",
            menu: Ext.create('Ext.menu.Menu', {
                items: [
                    // ZoomToMaxExtent
                    Ext.create('Ext.button.Button', actions["max_extent"]),
                    // Nav
                    Ext.create('Ext.menu.CheckItem', actions["nav"]),
                    // Draw poly
                    Ext.create('Ext.menu.CheckItem', actions["draw_poly"]),
                    // Draw line
                    Ext.create('Ext.menu.CheckItem', actions["draw_line"]),
                    // Select control
                    Ext.create('Ext.menu.CheckItem', actions["select"]),
                    // Navigation history control
                    Ext.create('Ext.button.Button', actions["previous"]), 
                    Ext.create('Ext.button.Button', actions["next"])
                ]
            })
        });
        
        var mappanel = Ext.create('GeoExt.panel.Map', {
            title: 'Using GeoExt.Action instances in various places',
            map: map,
            extent: '5.19,46.85,15.47,55.63',
            dockedItems: [{
                xtype: 'toolbar',
                dock: 'top',
                items: toolbarItems
            }]
        });
        
        
        Ext.create('Ext.container.Viewport', {
            layout: 'fit',
            items: [mappanel]
        });
        
    }
});
