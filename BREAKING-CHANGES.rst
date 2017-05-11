Known breaking changes
======================

Version 3.9
-----------

The processor graph is more strict
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

- Unable to override an attribute
- The order is important, unable to get the output from a processor placed after
  e.g. the !createScaleBar should be after the !createMap processor.
- In the !prepareLegend processor, the legend output is renamed to legendDataSource
- In the !prepareTable processor, the table output is renamed to tableDataSource
- In the mapContext the method getRoundedScale is renamed to getRoundedScaleDenominator
- The longtime deprecated imageFormat on OSM layer is removed
- The fake projection EPSG:900913 is no more supported, you should use the projection EPSG:3857
- The KVP WMTS layers should specify a real mime type in the FORMAT
