/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include GeoExt/form/Basic.js
 */

/**
 * A specific {@link Ext.form.Panel} whose internal form is a
 * {@link GeoExt.form.Basic} instead of {@link Ext.form.Basic}.
 * One would use this form to do search requests through
 * an `OpenLayers.Protocol` object (`OpenLayers.Protocol.WFS`
 * for example).
 *
 * Look at {@link GeoExt.form.action.Search} to understand how
 * form fields must be named for appropriate filters to be
 * passed to the protocol.
 *
 * Sample code showing how to use a GeoExt form panel.
 *
 *     var formPanel = Ext.create('GeoExt.form.FormPanel', {
 *         renderTo: "formpanel",
 *         protocol: new OpenLayers.Protocol.WFS({
 *             url: "http://publicus.opengeo.org/geoserver/wfs",
 *             featureType: "tasmania_roads",
 *             featureNS: "http://www.openplans.org/topp"
 *         }),
 *         items: [{
 *             xtype: "textfield",
 *             name: "name__ilike",
 *             value: "mont"
 *         }, {
 *             xtype: "textfield",
 *             name: "elevation__ge",
 *             value: "2000"
 *         }],
 *         listeners: {
 *             actioncomplete: function(form, action) {
 *                 // this listener triggers when the search request
 *                 // is complete, the OpenLayers.Protocol.Response
 *                 // resulting from the request is available
 *                 // in "action.response"
 *             }
 *         }
 *     });
 *     formPanel.add({
 *         xtype: 'toolbar',
 *         items: [{
 *             text: "Search",
 *             handler: function() {
 *                 this.search();
 *             },
 *             scope: formPanel
 *         }]
 *     });
 *
 * @class GeoExt.form.Panel
 */
Ext.define('GeoExt.form.Panel', {
    extend: 'Ext.form.Panel',
    requires: ['GeoExt.form.Basic'],
    alias: 'widget.gx_form',

    /**
     * The protocol instance this form panel
     * is configured with, actions resulting from this form
     * will be performed through the protocol.
     *
     * @cfg {OpenLayers.Protocol} protocol
     */
    protocol: null,

    /**
     * Create the internal {@link GeoExt.form.Basic} instance.
     *
     * @return {GeoExt.form.Basic} The basic form.
     * @private
     */
    createForm: function() {
        return new GeoExt.form.Basic(this, Ext.applyIf({listeners: {}},
                                     this.initialConfig));
    },

    /**
     * Shortcut to the internal form's search method.
     *
     * @param {Object} options The options passed to the
     * GeoExt.form.action.Search constructor.
     *
     */
    search: function(options) {
        this.getForm().search(options);
    }
});
