Ext.define('CF.view.summit.Chart', {
    extend: 'Ext.chart.Chart',
    alias : 'widget.summitchart',
    requires: ['Ext.chart.*'],

    // i18n properties
    bottomAxeTitleText: "Summit",
    leftAxeTitleText: "Elevation",

    initComponent: function() {
        Ext.apply(this, {
            flex: 1,
            legend: {
                position: 'right'
            },
            shadow: true,
            store: 'Summits',
            style: 'background:#fff',
            axes: [{
                type: 'Numeric',
                position: 'left',
                fields: ['elevation'],
                title: this.leftAxeTitleText,
                grid: true,
                minimum: 0
            }, {
                type: 'Category',
                position: 'bottom',
                fields: ['name'],
                title: this.bottomAxeTitleText
            }],
            series: [{
                type: 'line',
                axis: 'left',
                fill: true,
                listeners: {
                    itemmousedown: function(e) {
                        CF.view.summit.Grid.selectionModel.select(e.storeItem);
                    }
                },
                highlight: {
                    size: 7,
                    radius: 7
                },
                markerConfig: {
                    type: 'circle',
                    size: 4,
                    radius: 4,
                    'stroke-width': 0
                },
                smooth: true,
                tips: {
                    trackMouse: true,
                    width: 130,
                    height: 40,
                    renderer: function(storeItem, item) {
                        this.setTitle(
                            storeItem.get('name') +
                            '<br />' +
                            item.value[1] + ' meters'
                        );
                    }
                },
                title: [
                    this.leftAxeTitleText
                ],
                xField: 'name',
                yField: ['elevation']
            }]
        });
        this.callParent(arguments);
    }
});
