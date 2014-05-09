/**
 * The GeoExt.panel.Map used in the application.  Useful to define map options
 * and stuff.
 * @extends GeoExt.panel.Map
 */
Ext.define('CF.view.Map', {
    // Ext.panel.Panel-specific options:
    extend: 'GeoExt.panel.Map',
    alias : 'widget.cf_mappanel',
    requires: [
        'Ext.window.MessageBox',
        'GeoExt.Action',
        'CF.view.help.Action'
    ],
    border: 'false',
    layout: 'fit',
    region: 'west',
    width: 600,
    // GeoExt.panel.Map-specific options :
    center: '12.3046875,51.48193359375',
    zoom: 6,

    initComponent: function() {
        var me = this,
            items = [],
            ctrl;

        var map = new OpenLayers.Map();

        // ZoomToMaxExtent control, a "button" control
        items.push(Ext.create('Ext.button.Button', Ext.create('GeoExt.Action', {
            control: new OpenLayers.Control.ZoomToMaxExtent(),
            map: map,
            text: "max extent",
            tooltip: "zoom to max extent"
        })));

        items.push("-");

        // Navigation control
        items.push(Ext.create('Ext.button.Button',Ext.create('GeoExt.Action', {
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
        })));

        items.push("-");

        // Navigation history - two "button" controls
        ctrl = new OpenLayers.Control.NavigationHistory();
        map.addControl(ctrl);
        
        items.push(Ext.create('Ext.button.Button', Ext.create('GeoExt.Action', {
            text: "previous",
            control: ctrl.previous,
            disabled: true,
            tooltip: "previous in history"
        })));
        
        items.push(Ext.create('Ext.button.Button', Ext.create('GeoExt.Action', {
            text: "next",
            control: ctrl.next,
            disabled: true,
            tooltip: "next in history"
        })));
        items.push("->");

        // Help action
        items.push(
            Ext.create('Ext.button.Button', Ext.create('CF.view.help.Action', {
                windowContentEl: "help"
            }))
        );
        
        Ext.apply(me, {
            map: map,
            dockedItems: [{
                xtype: 'toolbar',
                dock: 'top',
                items: items,
                style: {
                    border: 0,
                    padding: 0
                }
            }]
        });
                
        me.callParent(arguments);
    }
});
