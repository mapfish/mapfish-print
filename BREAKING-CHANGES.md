# Known breaking changes

## Version 3.25

- The allowTransparency property is renamed to pdfA. It is now also possible to pass that value
  at impression time.

## Version 3.24

- Removing JsonP support.

## Version 3.13

- If throwErrorOnExtraParameters is set to true and the JSON contains extra attributes,
  the print job will have now an error.

## Version 3.10

The DB schema in multi-instance mode has been changed. You can drop the
printjobresultimpl and printjobstatusimpl tables since they are not used anymore.

## Version 3.9

### The processor graph is more strict

- No more able to override an attribute.
- The order is important, unable to get the output from a processor placed after
  e.g. the `!createScaleBar` should be after the `!createMap` processor.
- In the `!prepareLegend` processor, the legend output is renamed to `legendDataSource`.
- In the `!prepareTable` processor, the table output is renamed to `tableDataSource`.
- In the mapContext the method `getRoundedScale` is renamed to `getRoundedScaleDenominator`.
- In the Jasper template `$P{mapContext}.getScale().getDenominator()` should be replace by
  `$P{mapContext}.getRoundedScaleDenominator()`.
- The longtime deprecated imageFormat on OSM layer is removed.
- The fake projection `EPSG:900913` is no more supported, you should use the projection `EPSG:3857`.
- The KVP WMTS layers should specify a real mime type in the `FORMAT`.
- For native WMS rotation, the angle or map_angle shouldn't be in the customParams,
  and the `serverType` is required, to disable it `useNativeAngle` to `false` on the layer.
