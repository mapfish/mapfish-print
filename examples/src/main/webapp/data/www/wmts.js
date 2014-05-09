var map;

function init() {
    
    map = new OpenLayers.Map({
        div: "map",
        projection: "EPSG:900913"
    });    
    
    var osm = new OpenLayers.Layer.OSM();

    // If tile matrix identifiers differ from zoom levels (0, 1, 2, ...)
    // then they must be explicitly provided.
    var matrixIds = new Array(31);
    for (var i=0; i<31; ++i) {
        matrixIds[i] = "EPSG:900913:" + i;
    }

    var states = new OpenLayers.Layer.WMTS({
        name: "States",
        url: "http://localhost:9876/e2egeoserver/gwc/service/wmts/",
        layer: "topp:states",
        matrixSet: "EPSG:900913",
        matrixIds: matrixIds,
        format: "image/png",
        style: "pophatch",
        opacity: 0.7,
        isBaseLayer: false
    });                

    var roads = new OpenLayers.Layer.WMTS({
        name: "Roads",
        url: "http://localhost:9876/e2egeoserver/gwc/service/wmts/",
        layer: "sf:roads",
        matrixSet: "EPSG:900913",
        matrixIds: matrixIds,
        format: "image/png",
        style: "line",
        opacity: 0.7,
        isBaseLayer: false
    });                

    map.addLayers([osm, states, roads]);
    map.setCenter(new OpenLayers.LonLat(-11529014.982992, 5523802.3781164), 8);
    
}
