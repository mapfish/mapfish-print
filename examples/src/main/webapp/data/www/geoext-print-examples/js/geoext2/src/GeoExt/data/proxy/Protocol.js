/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/**
 * A data proxy for use with OpenLayers.Protocol objects.
 *
 * @class GeoExt.data.proxy.Protocol
 */
Ext.define('GeoExt.data.proxy.Protocol', {
    extend: 'Ext.data.proxy.Server',
    requires: [
        'GeoExt.Version'
    ],
    alias: 'proxy.gx_protocol',

    /**
     * The protocol used to fetch features.
     *
     * @cfg {OpenLayers.Protocol}
     */
    protocol: null,

    /**
     * Abort any previous request before issuing another.
     *
     * @cfg {Boolean}
     */
    abortPrevious: true,

    /**
     * Should options.params be set directly on options before passing it into
     * the protocol's read method?
     *
     * @cfg {Boolean}
     */
    setParamsAsOptions: false,

    /**
     * The response returned by the read call on the protocol.
     *
     * @property {OpenLayers.Protocol.Response}
     * @private
     */
    response: null,

    /**
     * Send the request.
     *
     * @param {Ext.data.Operation} operation The Ext.data.Operation object.
     * @param {Function} callback The callback function to call when the
     *     operation has completed.
     * @param {Object} scope The scope in which to execute the callback.
     * @private
     */
    doRequest: function(operation, callback, scope) {
        var me = this,
            params = Ext.applyIf(operation.params || {}, me.extraParams || {}),
            request;

        //copy any sorters, filters etc into the params so they can be sent over the wire
        params = Ext.applyIf(params, me.getParams(operation));

        var o = {
            params: params || {},
            operation: operation,
            request: {
                callback: callback,
                scope: scope,
                arg: operation.arg
            },
            reader: this.getReader()
        };
        var cb = OpenLayers.Function.bind(this.loadResponse, this, o);
        if (this.abortPrevious) {
            this.abortRequest();
        }
        var options = {
            params: params,
            callback: cb,
            scope: this
        };
        Ext.applyIf(options, operation.arg);
        if (this.setParamsAsOptions === true) {
            Ext.applyIf(options, options.params);
            delete options.params;
        }
        this.response = this.protocol.read(options);
    },

    /**
     * Called to abort any ongoing request.
     *
     * @private
     */
    abortRequest: function() {
        if (this.response) {
            this.protocol.abort(this.response);
            this.response = null;
        }
    },

    /**
     * Handle response from the protocol.
     *
     * @param {Object} o
     * @param {OpenLayers.Protocol.Response} response
     * @private
     */
    loadResponse: function(o, response) {
        var me = this;
        var operation = o.operation;
        var scope = o.request.scope;
        var callback = o.request.callback;
        if (response.success()) {
            var result = o.reader.read(response);
            Ext.apply(operation, {
                response: response,
                resultSet: result
            });

            operation.commitRecords(result.records);
            operation.setCompleted();
            operation.setSuccessful();
        } else {
            me.setException(operation, response);
            me.fireEvent('exception', this, response, operation);
        }
        if (typeof callback == 'function') {
            callback.call(scope || me, operation);
        }
    }
});
