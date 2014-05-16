/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include GeoExt/Form.js
 */

/**
 * A specific `Ext.form.action.Action` to be used when using a form to
 * trigger search requests using an `OpenLayers.Protocol`.
 *
 * When run this action builds an `OpenLayers.Filter` from the form
 * and passes this filter to its protocol's read method. The form fields
 * must be named after a specific convention, so that an appropriate
 * `OpenLayers.Filter.Comparison` filter is created for each
 * field.
 *
 * For example a field with the name `foo__like` would result in an
 * `OpenLayers.Filter.Comparison` of type
 * `OpenLayers.Filter.Comparison.LIKE` being created.
 *
 * Here is the convention:
 *
 * * `<name>__eq: OpenLayers.Filter.Comparison.EQUAL_TO`
 * * `<name>__ne: OpenLayers.Filter.Comparison.NOT_EQUAL_TO`
 * * `<name>__lt: OpenLayers.Filter.Comparison.LESS_THAN`
 * * `<name>__le: OpenLayers.Filter.Comparison.LESS_THAN_OR_EQUAL_TO`
 * * `<name>__gt: OpenLayers.Filter.Comparison.GREATER_THAN`
 * * `<name>__ge: OpenLayers.Filter.Comparison.GREATER_THAN_OR_EQUAL_TO`
 * * `<name>__like: OpenLayers.Filter.Comparison.LIKE`
 *
 * In most cases you would not directly create `GeoExt.form.action.Search`
 * objects, but use `GeoExt.form.FormPanel` instead.
 *
 * Sample code showing how to use a GeoExt Search Action with an Ext
 * form panel:
 *
 *     var formPanel = Ext.create('Ext.form.Panel', {
 *          renderTo: "formpanel",
 *          items: [{
 *              xtype: "textfield",
 *              name: "name__like",
 *              value: "mont"
 *          }, {
 *              xtype: "textfield",
 *              name: "elevation__ge",
 *              value: "2000"
 *          }]
 *      });
 *
 *      var searchAction = Ext.create('GeoExt.form.action.Search', {
 *          form: formPanel.getForm(),
 *          protocol: new OpenLayers.Protocol.WFS({
 *              url: "http://publicus.opengeo.org/geoserver/wfs",
 *              featureType: "tasmania_roads",
 *              featureNS: "http://www.openplans.org/topp"
 *          }),
 *          abortPrevious: true
 *      });
 *
 *     formPanel.getForm().doAction(searchAction, {
 *          callback: function(response) {
 *              // response.features includes the features read
 *              // from the server through the protocol
 *          }
 *      });
 *
 * @class GeoExt.form.action.Search
 */
Ext.define('GeoExt.form.action.Search', {
    extend: 'Ext.form.Action',
    alternateClassName: 'GeoExt.form.SearchAction',
    alias: 'formaction.search',
    requires: ['GeoExt.Form'],

    /**
     * The action type string.
     *
     * @property {String}
     * @private
     */
    type: "search",

    /**
     * A reference to the response resulting from the search request. Read-only.
     *
     * @property {OpenLayers.Protocol.Response} response
     */

    /**
     * The protocol to use for search requests.
     *
     * @cfg {OpenLayers.Protocol} protocol
     */

    /**
     * The protocol.
     *
     * @property {OpenLayers.Protocol} protocol
     */

    /**
     * (optional) Extra options passed to the protocol's read method.
     *
     * @cfg {Object} readOptions
     */

    /**
     * (optional) Callback function called when the response is received.
     *
     * @cfg {Function} callback
     */

    /**
     * (optional) Scope {@link #callback}.
     *
     * @cfg {Object} scope
     */

    /**
     * If set to true, the abort method will be called on the protocol
     * if there's a pending request. Default is `false`.
     *
     * @cfg {Boolean} abortPrevious
     */

    /**
     * Run the action.
     *
     * @private
     */
    run: function() {
        var form = this.form,
            f = GeoExt.Form.toFilter(form, this.logicalOp, this.wildcard);
        if(this.clientValidation === false || form.isValid()){

            if (this.abortPrevious && form.prevResponse) {
                this.protocol.abort(form.prevResponse);
            }

            this.form.prevResponse = this.protocol.read(
                Ext.applyIf({
                    filter: f,
                    callback: this.handleResponse,
                    scope: this
                }, this.readOptions)
            );

        } else if(this.clientValidation !== false){
            // client validation failed
            this.failureType = Ext.form.action.Action.CLIENT_INVALID;
            form.afterAction(this, false);
        }
    },

    /**
     * Handle the response to the search query.
     *
     * @param {OpenLayers.Protocol.Response} response The response object.
     * @private
     */
    handleResponse: function(response) {
        var form = this.form;
        form.prevResponse = null;
        this.response = response;
        if(response.success()) {
            form.afterAction(this, true);
        } else {
            form.afterAction(this, false);
        }
        if(this.callback) {
            this.callback.call(this.scope, response);
        }
    }
});
