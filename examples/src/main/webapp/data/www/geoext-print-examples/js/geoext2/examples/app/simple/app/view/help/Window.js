/**
 * Help Window with static content using 'contentEl' property.
 * @extends Ext.window.Window
 */
Ext.define('CF.view.help.Window', {
    extend: 'Ext.window.Window',
    alias : 'widget.cf_helpwindow',
    initComponent: function() {
        Ext.apply(this, {
            bodyCls: "cf-helpwindow",
            closeAction: "hide",
            layout: 'fit',
            maxWidth: 600,
            title: "Help"
        });
        this.callParent(arguments);
    }
});
