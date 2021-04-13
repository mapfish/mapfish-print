# ChangeLog

### 3.28.1

* The community mailing list no longer exists. Links to it were removed.
* [WMTS Template Variables are now case-insensitive](https://github.com/mapfish/mapfish-print/pull/1803).
* [Updated documentation for buffered Tiles](https://github.com/mapfish/mapfish-print/pull/1793).
* [Better Error Handling](https://github.com/mapfish/mapfish-print/pull/1790).

### 3.28.0

* ChangeLog started.
* Add Parameter tileBufferSize to TiledWmsLayer.
  - [Docs](http://mapfish.github.io/mapfish-print-doc/layers.html#Tiled%20Wms%20Layer)
  - [Example](https://github.com/mapfish/mapfish-print/blob/ed26974b8f274ee469b55b7fc7e38490673bf2a2/examples/src/test/resources/examples/printtiledwms/requestData-bbox-meta-tiled-wms1_1_1.json#L32)
* Add new config Parameters tileBufferWidth and tileBufferHeight.
  - [Docs](http://mapfish.github.io/mapfish-print-doc/processors.html#!setTiledWms)
  - [Example](https://github.com/mapfish/mapfish-print/blob/ed26974b8f274ee469b55b7fc7e38490673bf2a2/examples/src/test/resources/examples/printtiledwms/config.yaml#L48)
