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
