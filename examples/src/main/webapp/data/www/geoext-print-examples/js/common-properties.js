
var Env = {};
Env.baseUrl = 'http://localhost:9876/e2egeoserver';
Env.wmsUrl = Env.baseUrl + '/wms';
Env.wmtsUrl = Env.baseUrl + '/gwc/service/wmts';
Env.tmsUrl = Env.baseUrl + '/gwc/service/tms/';
Env.layers = {
    states: {name:'EPSG:4326 State Population', id:'topp:states', style: 'pophatch'},
    roads: {name:"EPSG:26713 Roads", id:'sf:roads', style: 'line'},
    nyRoads: {name:"EPSG:4326 New York Tiger Roads", id: 'tiger:tiger_roads', style: "line"},
    newYork: {name:"EPSG:4326 New York Tiger", id:'tiger-ny', style: ''}
}