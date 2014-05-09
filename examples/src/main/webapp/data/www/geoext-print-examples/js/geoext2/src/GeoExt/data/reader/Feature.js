/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include OpenLayers/Feature/Vector.js
 */

/**
 * Used to read the attributes of a feature.
 *
 * @class GeoExt.data.reader.Feature
 */
Ext.define('GeoExt.data.reader.Feature', {
    extend: 'Ext.data.reader.Json',
    alias : 'reader.feature',
    requires: [
        'GeoExt.Version'
    ],
    /**
     * Force to have our convertRecordData.
     *
     * @private
     */
    buildExtractors: function() {
        this.callParent(arguments);
        this.convertRecordData = this.convertFeatureRecordData;
    },

    /**
     * Copy feature attribute to record.
     *
     * @param {Array} convertedValues
     * @param {Object} feature
     * @param {Object} record
     * @private
     */
    convertFeatureRecordData: function(convertedValues, feature, record) {
        var records = [];

        if (feature) {
            var fields = record.fields;
            var values = {};
            if (feature.attributes) {
                for (var j = 0, jj = fields.length; j < jj; j++){
                    var field = fields.items[j];
                    var v;
                    if (/[\[\.]/.test(field.mapping)) {
                        try {
                            v = new Function("obj", "return obj." + field.mapping)(feature.attributes);
                        } catch(e){
                            v = field.defaultValue;
                        }
                    }
                    else {
                        v = feature.attributes[field.mapping || field.name] || field.defaultValue;
                    }
                    if (field.convert) {
                        v = field.convert(v, record);
                    }
                    convertedValues[field.name] = v;
                }
            }
            record.state = feature.state;
            if (feature.state == OpenLayers.State.INSERT ||
                    feature.state == OpenLayers.State.UPDATE) {
                record.setDirty();
            }

            // newly inserted features need to be made into phantom records
            var id = (feature.state === OpenLayers.State.INSERT) ? undefined : feature.id;
            convertedValues['id'] = id;
        }

        return records;
    }
});
