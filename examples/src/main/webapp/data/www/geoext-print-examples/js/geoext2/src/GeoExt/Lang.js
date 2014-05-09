/*
 * Copyright (c) 2008-2014 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/**
 * The GeoExt.Lang singleton is created when the library is loaded.
 * Include all relevant language files after this file in your build.
 *
 * @class GeoExt.Lang
 */
Ext.define('GeoExt.Lang', {
    extend: 'Ext.util.Observable',
    singleton: true,
    requires: [
        'GeoExt.Version'
    ],

    /**
     * The current language tag.  Use `#set` to set the locale. Defaults
     * to the browser language where available.
     *
     * @cfg {String}
     */
    locale: navigator.language || navigator.userLanguage,

    /**
     * Dictionary of string lookups per language.
     *
     * @property {Object}
     * @private
     */
    dict: null,

    /**
     * Construct the Lang singleton.
     *
     * @private
     */
    constructor: function() {
        this.addEvents(
            /**
             * Fires when localized strings are set.  Listeners will receive a
             * single `locale` event with the language tag.
             *
             * @event
             */
            "localize"
        );
        this.dict = {};
        this.callParent();
    },

    /**
     * Add translation strings to the dictionary.  This method can be called
     * multiple times with the same language tag (locale argument) to extend
     * a single dictionary.
     *
     * @param {String} locale A language tag that follows the "en-CA"
     *     convention (http://www.ietf.org/rfc/rfc3066.txt).
     * @param {Object} lookup An object with properties that are dot
     *     delimited names of objects with localizable strings (e.g.
     *     "GeoExt.VectorLegend.prototype").  The values for these properties
     *     are objects that will be used to extend the target objects with
     *     localized strings (e.g. {untitledPrefix: "Untitiled "})
     */
    add: function(locale, lookup) {
        var obj = this.dict[locale];
        if (!obj) {
            this.dict[locale] = Ext.apply({}, lookup);
        } else {
            for (var key in lookup) {
                obj[key] = Ext.apply(obj[key] || {}, lookup[key]);
            }
        }
        if (!locale || locale === this.locale) {
            this.set(locale);
        } else if (this.locale.indexOf(locale + "-") === 0) {
            // current locale is regional variation of added strings
            // call set so newly added strings are used where appropriate
            this.set(this.locale);
        }
    },

    /**
     * Set the language for all GeoExt components.  This will use any localized
     * strings in the dictionary (set with the `#add` method) that
     * correspond to the complete matching language tag or any "higher order"
     * tag (e.g. setting "en-CA" will use strings from the "en" dictionary if
     * matching strings are not found in the "en-CA" dictionary).
     *
     * @param {String} locale Language identifier tag following recommendations
     *     at http://www.ietf.org/rfc/rfc3066.txt.
     */
    set: function(locale) {
        // compile lookup based on primary and all subtags
        var tags = locale ? locale.split("-") : [];
        var id = "";
        var lookup = {}, parent, str, i, ii;
        for (i=0, ii=tags.length; i<ii; ++i) {
            id += (id && "-" || "") + tags[i];
            if (id in this.dict) {
                parent = this.dict[id];
                for (str in parent) {
                    if (str in lookup) {
                        Ext.apply(lookup[str], parent[str]);
                    } else {
                        lookup[str] = Ext.apply({}, parent[str]);
                    }
                }
            }
        }

        // now extend all objects given by dot delimited names in lookup
        for (str in lookup) {
            var obj = window;
            var parts = str.split(".");
            var missing = false;
            for (i=0, ii=parts.length; i<ii; ++i) {
                var name = parts[i];
                if (name in obj) {
                    obj = obj[name];
                } else {
                    missing = true;
                    break;
                }
            }
            if (!missing) {
                Ext.apply(obj, lookup[str]);
            }
        }
        this.locale = locale;
        this.fireEvent("localize", locale);
    }
});
