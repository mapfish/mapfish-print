<?xml version="1.0" encoding="UTF-8"?>
<!-- XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX -->
<!DOCTYPE x [
  <!ENTITY xxe SYSTEM "file:///mapfish-print-xxe-regression-secret">
]>
<wfs:FeatureCollection xmlns:wfs="http://www.opengis.net/wfs" xmlns:gml="http://www.opengis.net/gml">
  <gml:boundedBy>
    <gml:null>&xxe;</gml:null>
  </gml:boundedBy>
  <gml:featureMember>XXE_REGRESSION_SENTINEL</gml:featureMember>
</wfs:FeatureCollection>
