/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/**
 * Action class to create GeoExt.Action
 *
 * Sample code to create a toolbar with an OpenLayers control into it.
 *
 * Example:
 *
 *     var action = Ext.create('GeoExt.Action', {
 *         text: "max extent",
 *         control: new OpenLayers.Control.ZoomToMaxExtent(),
 *         map: map
 *     });
 *     var toolbar = Ext.create('Ext.toolbar.Toolbar', action);
 *
 * @class GeoExt.Action
 */
Ext.define('GeoExt.Action', {
    extend: 'Ext.Action',
    alias : 'widget.gx_action',
    requires: [
        'GeoExt.Version'
    ],

    /**
     * The OpenLayers control wrapped in this action.
     *
     * @cfg {OpenLayers.Control}
     */
    control: null,

    /**
     * Activate the action's control when the action is enabled.
     *
     * @property {Boolean} activateOnEnable
     */
    /**
     * Activate the action's control when the action is enabled.
     *
     * @cfg {Boolean} activateOnEnable
     */
    activateOnEnable: false,

    /**
     * Deactivate the action's control when the action is disabled.
     *
     * @property {Boolean} deactivateOnDisable
     */
    /**
     * Deactivate the action's control when the action is disabled.
     *
     * @cfg {Boolean} deactivateOnDisable
     */
    deactivateOnDisable: false,

    /**
     * The OpenLayers map that the control should be added to. For controls that
     * don't need to be added to a map or have already been added to one, this
     * config property may be omitted.
     *
     * @cfg {OpenLayers.Map}
     */
    map: null,

    /**
     * The user-provided scope, used when calling uHandler, uToggleHandler,
     * and uCheckHandler.
     *
     * @property {Object}
     * @private
     */
    uScope: null,

    /**
     * References the function the user passes through the "handler" property.
     *
     * @property {Function}
     * @private
     */
    uHandler: null,

    /**
     * References the function the user passes through the "toggleHandler"
     * property.
     *
     * @property {Function}
     * @private
     */
    uToggleHandler: null,

    /**
     * References the function the user passes through the "checkHandler"
     * property.
     *
     * @property {Function}
     * @private
     */
    uCheckHandler: null,

    /**
     * Create a GeoExt.Action instance. A GeoExt.Action is created to insert
     * an OpenLayers control in a toolbar as a button or in a menu as a menu
     * item. A GeoExt.Action instance can be used like a regular Ext.Action,
     * look at the Ext.Action API doc for more detail.
     *
     * @param {Object} config (optional) Config object.
     * @private
     */
    constructor: function(config){
        // store the user scope and handlers
        this.uScope = config.scope;
        this.uHandler = config.handler;
        this.uToggleHandler = config.toggleHandler;
        this.uCheckHandler = config.checkHandler;

        config.scope = this;
        config.handler = this.pHandler;
        config.toggleHandler = this.pToggleHandler;
        config.checkHandler = this.pCheckHandler;

        // set control in the instance, the Ext.Action
        // constructor won't do it for us
        this.control = config.control;
        var ctrl = this.control;
        delete config.control;

        this.activateOnEnable = !!config.activateOnEnable;
        delete config.activateOnEnable;
        this.deactivateOnDisable = !!config.deactivateOnDisable;
        delete config.deactivateOnDisable;

        // register "activate" and "deactivate" listeners
        // on the control
        if (ctrl) {
            // If map is provided in config, add control to map.
            if (config.map) {
                config.map.addControl(ctrl);
                delete config.map;
            }
            if((config.pressed || config.checked) && ctrl.map) {
                ctrl.activate();
            }
            if (ctrl.active) {
                config.pressed = true;
                config.checked = true;
            }
            ctrl.events.on({
                activate: this.onCtrlActivate,
                deactivate: this.onCtrlDeactivate,
                scope: this
            });

        }

        this.callParent(arguments);

    },

    /**
     * The private handler.
     *
     * @param {Ext.Component} The component that triggers the handler.
     * @private
     */
    pHandler: function(cmp){
        var ctrl = this.control;
        if (ctrl &&
        ctrl.type == OpenLayers.Control.TYPE_BUTTON) {
            ctrl.trigger();
        }
        if (this.uHandler) {
            this.uHandler.apply(this.uScope, arguments);
        }
    },

    /**
     * The private toggle handler.
     *
     * @param {Ext.Component} cmp The component that triggers the toggle 
     *     handler.
     * @param {Boolean} state The state of the toggle.
     * @private
     */
    pToggleHandler: function(cmp, state){
        this.changeControlState(state);
        if (this.uToggleHandler) {
            this.uToggleHandler.apply(this.uScope, arguments);
        }
    },

    /**
     * The private check handler.
     *
     * @param {Ext.Component} cmp The component that triggers the check handler.
     * @param {Boolean} state The state of the toggle.
     * @private
     */
    pCheckHandler: function(cmp, state){
        this.changeControlState(state);
        if (this.uCheckHandler) {
            this.uCheckHandler.apply(this.uScope, arguments);
        }
    },

    /**
     * Change the control state depending on the state boolean.
     *
     * @param {Boolean} state The state of the toggle.
     * @private
     */
    changeControlState: function(state){
        if(state) {
            if(!this._activating) {
                this._activating = true;
                this.control.activate();
                // update initialConfig for next component created from this action
                this.initialConfig.pressed = true;
                this.initialConfig.checked = true;
                this._activating = false;
            }
        } else {
            if(!this._deactivating) {
                this._deactivating = true;
                this.control.deactivate();
                // update initialConfig for next component created from this action
                this.initialConfig.pressed = false;
                this.initialConfig.checked = false;
                this._deactivating = false;
            }
        }
    },

    /**
     * Called when this action's control is activated.
     *
     * @private
     */
    onCtrlActivate: function(){
        var ctrl = this.control;
        if(ctrl.type == OpenLayers.Control.TYPE_BUTTON) {
            this.enable();
        } else {
            // deal with buttons
            this.safeCallEach("toggle", [true]);
            // deal with check items
            this.safeCallEach("setChecked", [true]);
        }
    },

    /**
     * Called when this action's control is deactivated.
     *
     * @private
     */
    onCtrlDeactivate: function(){
        var ctrl = this.control;
        if(ctrl.type == OpenLayers.Control.TYPE_BUTTON) {
            this.disable();
        } else {
            // deal with buttons
            this.safeCallEach("toggle", [false]);
            // deal with check items
            this.safeCallEach("setChecked", [false]);
        }
    },

   /**
    * Called when the control which should get toggled
    * is not of type OpenLayers.Control.TYPE_BUTTON
    *
    * @private
    */
   safeCallEach: function(fnName, args) {
       var cs = this.items;
       for(var i = 0, len = cs.length; i < len; i++){
           if(cs[i][fnName]) {
               cs[i].rendered ?
                   cs[i][fnName].apply(cs[i], args) :
                   cs[i].on({
                       "render": Ext.Function.bind(cs[i][fnName], cs[i], args),
                       single: true
                   });
           }
       }
   },

   /**
    * Override method on super to optionally deactivate controls on disable.
    *
    * @param {Boolean} v Disable the action's components.
    * @private
    */
   setDisabled : function(v) {
       if (!v && this.activateOnEnable && this.control && !this.control.active) {
           this.control.activate();
       }
       if (v && this.deactivateOnDisable && this.control && this.control.active) {
           this.control.deactivate();
       }
       return GeoExt.Action.superclass.setDisabled.apply(this, arguments);
   }

});
