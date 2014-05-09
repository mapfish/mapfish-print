/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/**
 * A subclass of {@link Ext.tree.Column}, which provides indentation and
 * folder structure markup for a Tree, taking into account depth and
 * position within the tree hierarchy.
 *
 * @class GeoExt.tree.Column
 */
Ext.define('GeoExt.tree.Column', {
    extend: 'Ext.tree.Column',
    requires: [
        'GeoExt.Version'
    ],
    alias: 'widget.gx_treecolumn',

    initComponent: function() {
        var me = this;

        me.callParent();

        var parentRenderer = me.renderer;

        this.renderer = function(value, metaData, record, rowIdx, colIdx, store, view) {

            var buf   = [parentRenderer.call(this, value, metaData, record, rowIdx, colIdx, store, view)];

            // Replace all base layers from checkbox to radio
            if(record.get('checkedGroup')) {
                buf[0] = buf[0].replace(/class="([^-]+)-tree-checkbox([^"]+)?"/, 'class="$1-tree-checkbox$2 gx-tree-radio"'); //"
            }

            // Add a hook to add other components in the tree like legend icons
            buf.push('<div class="gx-tree-component gx-tree-component-off" id="tree-record-'+record.id+'"></div>');

            return buf.join('');
        };

    },

    /**
     * A basic default renderer return only it's passed value.
     */
    defaultRenderer: function(value) {
        return value;
    }
});
