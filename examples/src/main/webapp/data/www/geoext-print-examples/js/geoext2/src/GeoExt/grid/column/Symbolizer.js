/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include GeoExt/FeatureRenderer.js
 */

/**
 * An Ext.grid.column.Column pre-configured with a GeoExt.FeatureRenderer
 * 
 * @class GeoExt.grid.column.Symbolizer
 */
Ext.define('GeoExt.grid.column.Symbolizer', {
    extend: 'Ext.grid.column.Column',
    alternateClassName: 'GeoExt.grid.SymbolizerColumn',
    alias: ['widget.gx_symbolizercolumn'],
    requires: ['GeoExt.FeatureRenderer'],

    /**
     * The default renderer Method for Features.
     */
    defaultRenderer: function(value, meta, record) {
        if (value) {
            var id = Ext.id();
            var symbolType = "Polygon";
            if (record) {
                var symbolType = "Line";
                var className = record.raw.geometry ? record.raw.geometry.CLASS_NAME : null;
                if (className == "OpenLayers.Geometry.Point" ||
                        className == "OpenLayers.Geometry.MultiPoint") {
                    symbolType = "Point";
                }
                else if (className == "OpenLayers.Geometry.Polygon" ||
                        className == "OpenLayers.Geometry.MultiPolygon") {
                    symbolType = "Polygon";
                }
            }
            window.setTimeout(function() {
                var ct = Ext.get(id);
                // ct for old field may not exist any more during a grid update
                if (ct) {
                    var renderer = Ext.create('GeoExt.FeatureRenderer', {
                        renderTo: ct,
                        symbolizers: value instanceof Array ? value : [value],
                        symbolType: symbolType
                    });
                }
            }, 0);
            meta.css = "gx-grid-symbolizercol";
            return Ext.String.format('<div id="{0}"></div>', id);
        }
    }
});
