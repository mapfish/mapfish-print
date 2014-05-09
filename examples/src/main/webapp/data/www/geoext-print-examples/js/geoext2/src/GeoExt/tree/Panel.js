/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include GeoExt/tree/Column.js
 * @include GeoExt/tree/View.js
 */

/**
 * An Ext.tree.Panel pre-configured with a GeoExt.tree.Column.
 * 
 * @class GeoExt.tree.Panel
 */
Ext.define('GeoExt.tree.Panel', {
    extend: 'Ext.tree.Panel',
    alias: 'widget.gx_treepanel',
    requires: [
        'GeoExt.tree.Column',
        'GeoExt.tree.View'
    ],
    viewType: 'gx_treeview',
    
    initComponent: function() {
        var me = this;

        if (!me.columns) {
            if (me.initialConfig.hideHeaders === undefined) {
                me.hideHeaders = true;
            }
            me.addCls(Ext.baseCSSPrefix + 'autowidth-table');
            me.columns = [{
                xtype    : 'gx_treecolumn',
                text     : 'Name',
                width    : Ext.isIE6 ? null : 10000,
                dataIndex: me.displayField         
            }];
        }

        me.callParent();
    }
});
