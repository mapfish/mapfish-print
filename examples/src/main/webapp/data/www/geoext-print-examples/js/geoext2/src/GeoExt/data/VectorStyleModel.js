/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
* @include OpenLayers/Format/CQL.js
*/

/**
 * A specific model for CQL Style Rules.
 *
 * Preconfigured with an Ajax proxy and a JSON reader.
 *
 * @class GeoExt.data.VectorStyleModel
 */
Ext.define('GeoExt.data.VectorStyleModel', {
    extend : 'Ext.data.Model',
    requires : [
        'Ext.data.JsonReader',
        'GeoExt.Version'
    ],
    fields : [{
        name : "elseFilter",
        defaultValue : null
    }, {
        name : "label",
        mapping : "title",
        type : 'string'
    }, "name", "description", "minScaleDenominator", "maxScaleDenominator", {
        name : "symbolizers",
        convert : function(symbolizers, rec) {
            //symbolizers should be an array of OpenLayers.Symbolizer objects
            symbolizers = ( symbolizers instanceof Array) ? symbolizers : [symbolizers];
            for(var i = 0; i < symbolizers.length; i++) {
                var symbolizer = symbolizers[i];
                //due to the way that the initial data provided to a store is processed,
                //the instanceof test no longer works and we need to clone the symbolizer
                //for it to be recognized as a symbolizer class again
                if(!( symbolizer instanceof OpenLayers.Symbolizer) && symbolizer.CLASS_NAME && symbolizer.clone) {
                    symbolizers[i] = symbolizer.clone();
                }
            }
            return symbolizers;
        },
        defaultValue : null
    }, {
        name : "filter",
        convert : function(filter) {
            if( typeof filter === "string") {
                filter = filter ? OpenLayers.Format.CQL.prototype.read(filter) : null;
            }
            return filter;
        },
        defaultValue : null
    }],
    proxy : {
        type : 'memory',
        reader : {
            type : 'json',
            root : "rules"
        }
    }
});
