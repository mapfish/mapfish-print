/**
 * The grid in which summits are displayed
 * @extends Ext.grid.Panel
 */
Ext.define('CF.view.summit.Grid' ,{
    extend: 'Ext.grid.Panel',
    alias : 'widget.summitgrid',
    requires: [
        'GeoExt.selection.FeatureModel',
        'GeoExt.grid.column.Symbolizer',
        'Ext.grid.plugin.CellEditing',
        'Ext.form.field.Number'
    ],
    initComponent: function() {
        Ext.apply(this, {
            border: false,
            columns: [
                {
                    header: '',
                    dataIndex: 'symbolizer',
                    menuDisabled: true,
                    sortable: false,
                    xtype: 'gx_symbolizercolumn',
                    width: 30
                },
                {header: 'ID', dataIndex: 'fid', width: 40},
                {header: 'Name', dataIndex: 'name', flex: 3},
                {
                    header: 'Elevation',
                    dataIndex: 'elevation',
                    width: 60,
                    editor: {xtype: 'numberfield'}
                },
                {header: 'Title', dataIndex: 'title', flex: 4},
                {header: 'Latitude', dataIndex: 'lat', flex: 2},
                {header: 'Longitude', dataIndex: 'lon', flex: 2}
            ],
            flex: 1,
            store: 'Summits',
            selType: 'featuremodel',
            plugins: [
                Ext.create('Ext.grid.plugin.CellEditing', {
                    clicksToEdit: 2
                })
            ]
        });
        this.callParent(arguments);
        // store singleton selection model instance
        CF.view.summit.Grid.selectionModel = this.getSelectionModel();
    }
});
