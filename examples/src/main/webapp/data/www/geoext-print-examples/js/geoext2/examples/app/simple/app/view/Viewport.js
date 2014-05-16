/**
 * The main application viewport, which displays the whole application
 * @extends Ext.Viewport
 */
Ext.define('CF.view.Viewport', {
    extend: 'Ext.Viewport',
    layout: 'fit',

    requires: [
        'Ext.layout.container.Border',
        'Ext.resizer.Splitter',
        'CF.view.Header',
        'CF.view.Map',
        'CF.view.summit.Chart',
        'CF.view.summit.Grid'
    ],

    initComponent: function() {
        var me = this;

        Ext.apply(me, {
            items: [{
                xtype: 'panel',
                border: false,
                layout: 'border',
                dockedItems: [
                    Ext.create('CF.view.Header')
                ],
                items: [{
                    xtype: 'cf_mappanel'
                }, {
                    xtype: 'panel',
                    region: 'center',
                    border: false,
                    id    : 'viewport',
                    layout: {
                        type: 'vbox',
                        align: 'stretch'
                    },
                    items: [
                        Ext.create('CF.view.summit.Grid'),
                        {xtype: 'splitter'},
                        Ext.create('CF.view.summit.Chart')
                    ]
                }]
            }]
        });

        me.callParent(arguments);
    }
});
