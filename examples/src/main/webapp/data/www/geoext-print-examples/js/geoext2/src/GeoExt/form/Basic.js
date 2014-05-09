/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include GeoExt/widgets/form/action/Search.js
 */

/**
 * A specific `Ext.form.Basic` whose `#doAction` method creates
 * a GeoExt.form.action.Search if it is passed the string
 * "search" as its first argument.
 *
 * In most cases one would not use this class directly, but
 * `GeoExt.form.Panel` instead.
 *
 * @class GeoExt.form.Basic
 */
Ext.define('GeoExt.form.Basic', {
    extend: 'Ext.form.Basic',
    requires: ['GeoExt.form.action.Search'],

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
     * The response return by a call to  protocol.read method.
     *
     * @property {OpenLayers.Protocol.Response} prevResponse
     * @private
     */

    /**
     * Tells if pending requests should be aborted when a new action
     * is performed. Default is `true`.
     *
     * @cfg {Boolean}
     */
    autoAbort: true,

    /**
     * Performs the action, if the string "search" is passed as the
     * first argument then a {@link GeoExt.form.action.Search} is created.
     *
     * @param {String/Ext.form.action.Action} action Either the name
     *     of the action or a `Ext.form.action.Action` instance.
     * @param {Object} options The options passed to the Action constructor.
     * @return {GeoExt.form.Basic} This form.
     *
     */
    doAction: function(action, options) {
        if(action == "search") {
            options = Ext.applyIf(options || {}, {
                form: this,
                protocol: this.protocol,
                abortPrevious: this.autoAbort
            });
            action = Ext.create('GeoExt.form.action.Search', options);
        }
        return this.callParent([action, options]);
    },

    /**
     * Shortcut to do a search action.
     *
     * @param {Object} options The options passed to the Action constructor.
     * @return {GeoExt.form.Basic} This form.
     */
    search: function(options) {
        return this.doAction("search", options);
    }
});
