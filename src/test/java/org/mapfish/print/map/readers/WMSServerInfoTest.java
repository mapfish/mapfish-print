/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Server
 *
 * MapFish Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MapFish Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.map.readers;

import org.mapfish.print.PrintTestCase;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class WMSServerInfoTest extends PrintTestCase {
    public WMSServerInfoTest(String name) {
        super(name);
    }

    public void testParseTileCache() throws IOException, SAXException, ParserConfigurationException {
        String response = "<?xml version='1.0' encoding=\"ISO-8859-1\" standalone=\"no\" ?>\n" +
                "        <!DOCTYPE WMT_MS_Capabilities SYSTEM \n" +
                "            \"http://schemas.opengeospatial.net/wms/1.1.1/WMS_MS_Capabilities.dtd\" [\n" +
                "              <!ELEMENT VendorSpecificCapabilities (TileSet*) >\n" +
                "              <!ELEMENT TileSet (SRS, BoundingBox?, Resolutions,\n" +
                "                                 Width, Height, Format, Layers*, Styles*) >\n" +
                "              <!ELEMENT Resolutions (#PCDATA) >\n" +
                "              <!ELEMENT Width (#PCDATA) >\n" +
                "              <!ELEMENT Height (#PCDATA) >\n" +
                "              <!ELEMENT Layers (#PCDATA) >\n" +
                "              <!ELEMENT Styles (#PCDATA) >\n" +
                "        ]> \n" +
                "        <WMT_MS_Capabilities version=\"1.1.1\">\n" +
                "\n" +
                "          <Service>\n" +
                "            <Name>OGC:WMS</Name>\n" +
                "            <Title></Title>\n" +
                "            <OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"http://www.example.com?\"/>\n" +
                "          </Service>\n" +
                "        \n" +
                "          <Capability>\n" +
                "            <Request>\n" +
                "              <GetCapabilities>\n" +
                "\n" +
                "                <Format>application/vnd.ogc.wms_xml</Format>\n" +
                "                <DCPType>\n" +
                "                  <HTTP>\n" +
                "                    <Get><OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"http://www.example.com?\"/></Get>\n" +
                "                  </HTTP>\n" +
                "                </DCPType>\n" +
                "              </GetCapabilities>\n" +
                "              <GetMap>\n" +
                "\n" +
                "                <Format>image/png</Format>\n" +
                "\n" +
                "                <DCPType>\n" +
                "                  <HTTP>\n" +
                "                    <Get><OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"http://www.example.com?\"/></Get>\n" +
                "                  </HTTP>\n" +
                "                </DCPType>\n" +
                "              </GetMap>\n" +
                "            </Request>\n" +
                "\n" +
                "            <Exception>\n" +
                "              <Format>text/plain</Format>\n" +
                "            </Exception>\n" +
                "            <VendorSpecificCapabilities>\n" +
                "              <TileSet>\n" +
                "                <SRS>EPSG:21781</SRS>\n" +
                "                <BoundingBox SRS=\"EPSG:21781\" minx=\"155000.000000\" miny=\"-253050.000000\"\n" +
                "                                      maxx=\"1365000.000000\" maxy=\"583050.000000\" />\n" +
                "                <Resolutions>800.00000000000000000000 400.00000000000000000000 200.00000000000000000000 100.00000000000000000000 50.00000000000000000000 20.00000000000000000000 10.00000000000000000000 5.00000000000000000000 2.50000000000000000000</Resolutions>\n" +
                "\n" +
                "                <Width>256</Width>\n" +
                "                <Height>256</Height>\n" +
                "                <Format>image/png</Format>\n" +
                "                <Layers>cn</Layers>\n" +
                "                <Styles></Styles>\n" +
                "              </TileSet>\n" +
                "            </VendorSpecificCapabilities>\n" +
                "            <UserDefinedSymbolization SupportSLD=\"0\" UserLayer=\"0\"\n" +
                "                                      UserStyle=\"0\" RemoteWFS=\"0\"/>\n" +
                "            <Layer>\n" +
                "              <Title>TileCache Layers</Title>\n" +
                "            <Layer queryable=\"0\" opaque=\"0\" cascaded=\"1\">\n" +
                "\n" +
                "              <Name>cn</Name>\n" +
                "              <Title>cn</Title>\n" +
                "              <SRS>EPSG:21781</SRS>\n" +
                "              <BoundingBox SRS=\"EPSG:21781\" minx=\"155000.000000\" miny=\"-253050.000000\"\n" +
                "                                    maxx=\"1365000.000000\" maxy=\"583050.000000\" />\n" +
                "            </Layer>\n" +
                "            </Layer>\n" +
                "          </Capability>\n" +
                "        </WMT_MS_Capabilities>";

        InputStream stream = new ByteArrayInputStream(response.getBytes("ISO-8859-1"));
        WMSServerInfo info = WMSServerInfo.parseCapabilities(stream);
        assertEquals(true, info.isTileCache());
        TileCacheLayerInfo layerInfo = info.getTileCacheLayer("cn");
        assertNotNull(layerInfo);
        assertEquals(256, layerInfo.getWidth());
        assertEquals(256, layerInfo.getHeight());
        final float[] resolutions = layerInfo.getResolutions();
        final float[] expectedResolutions = {
                800.0F,
                400.0F,
                200.0F,
                100.0F,
                50.0F,
                20.0F,
                10.0F,
                5.0F,
                2.5F};
        assertTrue(Arrays.equals(expectedResolutions, resolutions));

        final TileCacheLayerInfo.ResolutionInfo higherRes = new TileCacheLayerInfo.ResolutionInfo(8, 2.5F);
        final TileCacheLayerInfo.ResolutionInfo midRes = new TileCacheLayerInfo.ResolutionInfo(7, 5.0F);
        final TileCacheLayerInfo.ResolutionInfo lowerRes = new TileCacheLayerInfo.ResolutionInfo(0, 800.0F);

        assertEquals(higherRes, layerInfo.getNearestResolution(0.1F));
        assertEquals(higherRes, layerInfo.getNearestResolution(2.5F));
        assertEquals(higherRes, layerInfo.getNearestResolution(2.6F));
        assertEquals(midRes, layerInfo.getNearestResolution(4.99999F));
        assertEquals(midRes, layerInfo.getNearestResolution(5.0F));
        assertEquals(lowerRes, layerInfo.getNearestResolution(1000.0F));

        assertEquals(155000.0F, layerInfo.getMinX());
        assertEquals(-253050.0F, layerInfo.getMinY());
        assertEquals("png", layerInfo.getExtension());
    }

    /**
     * Tilecache with resolutions not in the correct order.
     */
    public void testParseWeirdTileCache() throws IOException, SAXException, ParserConfigurationException {
        String response = "<?xml version='1.0' encoding=\"ISO-8859-1\" standalone=\"no\" ?>\n" +
                "        <!DOCTYPE WMT_MS_Capabilities SYSTEM \n" +
                "            \"http://schemas.opengeospatial.net/wms/1.1.1/WMS_MS_Capabilities.dtd\" [\n" +
                "              <!ELEMENT VendorSpecificCapabilities (TileSet*) >\n" +
                "              <!ELEMENT TileSet (SRS, BoundingBox?, Resolutions,\n" +
                "                                 Width, Height, Format, Layers*, Styles*) >\n" +
                "              <!ELEMENT Resolutions (#PCDATA) >\n" +
                "              <!ELEMENT Width (#PCDATA) >\n" +
                "              <!ELEMENT Height (#PCDATA) >\n" +
                "              <!ELEMENT Layers (#PCDATA) >\n" +
                "              <!ELEMENT Styles (#PCDATA) >\n" +
                "        ]> \n" +
                "        <WMT_MS_Capabilities version=\"1.1.1\">\n" +
                "\n" +
                "          <Service>\n" +
                "            <Name>OGC:WMS</Name>\n" +
                "            <Title></Title>\n" +
                "            <OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"http://www.example.com?\"/>\n" +
                "          </Service>\n" +
                "        \n" +
                "          <Capability>\n" +
                "            <Request>\n" +
                "              <GetCapabilities>\n" +
                "\n" +
                "                <Format>application/vnd.ogc.wms_xml</Format>\n" +
                "                <DCPType>\n" +
                "                  <HTTP>\n" +
                "                    <Get><OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"http://www.example.com?\"/></Get>\n" +
                "                  </HTTP>\n" +
                "                </DCPType>\n" +
                "              </GetCapabilities>\n" +
                "              <GetMap>\n" +
                "\n" +
                "                <Format>image/png</Format>\n" +
                "\n" +
                "                <DCPType>\n" +
                "                  <HTTP>\n" +
                "                    <Get><OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"http://www.example.com?\"/></Get>\n" +
                "                  </HTTP>\n" +
                "                </DCPType>\n" +
                "              </GetMap>\n" +
                "            </Request>\n" +
                "\n" +
                "            <Exception>\n" +
                "              <Format>text/plain</Format>\n" +
                "            </Exception>\n" +
                "            <VendorSpecificCapabilities>\n" +
                "              <TileSet>\n" +
                "                <SRS>EPSG:21781</SRS>\n" +
                "                <BoundingBox SRS=\"EPSG:21781\" minx=\"155000.000000\" miny=\"-253050.000000\"\n" +
                "                                      maxx=\"1365000.000000\" maxy=\"583050.000000\" />\n" +
                "                <Resolutions>400.00000000000000000000 800.00000000000000000000 200.00000000000000000000 100.00000000000000000000 50.00000000000000000000 20.00000000000000000000 10.00000000000000000000 5.00000000000000000000 2.50000000000000000000</Resolutions>\n" +
                "\n" +
                "                <Width>256</Width>\n" +
                "                <Height>256</Height>\n" +
                "                <Format>image/png</Format>\n" +
                "                <Layers>cn</Layers>\n" +
                "                <Styles></Styles>\n" +
                "              </TileSet>\n" +
                "            </VendorSpecificCapabilities>\n" +
                "            <UserDefinedSymbolization SupportSLD=\"0\" UserLayer=\"0\"\n" +
                "                                      UserStyle=\"0\" RemoteWFS=\"0\"/>\n" +
                "            <Layer>\n" +
                "              <Title>TileCache Layers</Title>\n" +
                "            <Layer queryable=\"0\" opaque=\"0\" cascaded=\"1\">\n" +
                "\n" +
                "              <Name>cn</Name>\n" +
                "              <Title>cn</Title>\n" +
                "              <SRS>EPSG:21781</SRS>\n" +
                "              <BoundingBox SRS=\"EPSG:21781\" minx=\"155000.000000\" miny=\"-253050.000000\"\n" +
                "                                    maxx=\"1365000.000000\" maxy=\"583050.000000\" />\n" +
                "            </Layer>\n" +
                "            </Layer>\n" +
                "          </Capability>\n" +
                "        </WMT_MS_Capabilities>";

        InputStream stream = new ByteArrayInputStream(response.getBytes("ISO-8859-1"));
        WMSServerInfo info = WMSServerInfo.parseCapabilities(stream);
        assertEquals(true, info.isTileCache());
        TileCacheLayerInfo layerInfo = info.getTileCacheLayer("cn");
        assertNotNull(layerInfo);
        assertEquals(256, layerInfo.getWidth());
        assertEquals(256, layerInfo.getHeight());
        final float[] resolutions = layerInfo.getResolutions();
        final float[] expectedResolutions = {
                800.0F,
                400.0F,
                200.0F,
                100.0F,
                50.0F,
                20.0F,
                10.0F,
                5.0F,
                2.5F};
        assertTrue(Arrays.equals(expectedResolutions, resolutions));

        final TileCacheLayerInfo.ResolutionInfo higherRes = new TileCacheLayerInfo.ResolutionInfo(8, 2.5F);
        final TileCacheLayerInfo.ResolutionInfo midRes = new TileCacheLayerInfo.ResolutionInfo(7, 5.0F);
        final TileCacheLayerInfo.ResolutionInfo lowerRes = new TileCacheLayerInfo.ResolutionInfo(0, 800.0F);

        assertEquals(higherRes, layerInfo.getNearestResolution(0.1F));
        assertEquals(higherRes, layerInfo.getNearestResolution(2.5F));
        assertEquals(higherRes, layerInfo.getNearestResolution(2.6F));
        assertEquals(midRes, layerInfo.getNearestResolution(4.99999F));
        assertEquals(midRes, layerInfo.getNearestResolution(5.0F));
        assertEquals(lowerRes, layerInfo.getNearestResolution(1000.0F));

        assertEquals(155000.0F, layerInfo.getMinX());
        assertEquals(-253050.0F, layerInfo.getMinY());
        assertEquals("png", layerInfo.getExtension());
    }

    public void testParseMapServer() throws IOException, SAXException, ParserConfigurationException {
        String response = "<?xml version='1.0' encoding=\"UTF-8\" standalone=\"no\" ?>\n" +
                "<!DOCTYPE WMT_MS_Capabilities SYSTEM \"http://schemas.opengis.net/wms/1.1.1/WMS_MS_Capabilities.dtd\"\n" +
                " [\n" +
                " <!ELEMENT VendorSpecificCapabilities EMPTY>\n" +
                " ]>  <!-- end of DOCTYPE declaration -->\n" +
                "\n" +
                "<WMT_MS_Capabilities version=\"1.1.1\">\n" +
                "\n" +
                "<!-- MapServer version 5.0.3 OUTPUT=GIF OUTPUT=PNG OUTPUT=JPEG OUTPUT=WBMP OUTPUT=SVG SUPPORTS=PROJ SUPPORTS=AGG SUPPORTS=FREETYPE SUPPORTS=WMS_SERVER SUPPORTS=WMS_CLIENT SUPPORTS=WFS_SERVER SUPPORTS=WFS_CLIENT SUPPORTS=WCS_SERVER SUPPORTS=FASTCGI SUPPORTS=THREADS SUPPORTS=GEOS INPUT=EPPL7 INPUT=POSTGIS INPUT=OGR INPUT=GDAL INPUT=SHAPEFILE -->\n" +
                "\n" +
                "<Service>\n" +
                "  <Name>OGC:WMS</Name>\n" +
                "  <Title>SwissTopo raster WMS Server</Title>\n" +
                "  <Abstract>WMS Server serving swisstopo raster maps</Abstract>\n" +
                "  <OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"http://www.example.com/cgi-bin/mapserver?\"/>\n" +
                "  <ContactInformation>\n" +
                "  </ContactInformation>\n" +
                "</Service>\n" +
                "\n" +
                "<Capability>\n" +
                "  <Request>\n" +
                "    <GetCapabilities>\n" +
                "      <Format>application/vnd.ogc.wms_xml</Format>\n" +
                "      <DCPType>\n" +
                "        <HTTP>\n" +
                "          <Get><OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"http://www.example.com/cgi-bin/mapserver?\"/></Get>\n" +
                "          <Post><OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"http://www.example.com/cgi-bin/mapserver?\"/></Post>\n" +
                "        </HTTP>\n" +
                "      </DCPType>\n" +
                "    </GetCapabilities>\n" +
                "    <GetMap>\n" +
                "      <Format>image/tiff</Format>\n" +
                "      <Format>image/gif</Format>\n" +
                "      <Format>image/png; mode=24bit</Format>\n" +
                "      <Format>image/wbmp</Format>\n" +
                "      <Format>image/svg+xml</Format>\n" +
                "      <DCPType>\n" +
                "        <HTTP>\n" +
                "          <Get><OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"http://www.example.com/cgi-bin/mapserver?\"/></Get>\n" +
                "          <Post><OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"http://www.example.com/cgi-bin/mapserver?\"/></Post>\n" +
                "        </HTTP>\n" +
                "      </DCPType>\n" +
                "    </GetMap>\n" +
                "    <GetFeatureInfo>\n" +
                "      <Format>text/plain</Format>\n" +
                "      <Format>application/vnd.ogc.gml</Format>\n" +
                "      <DCPType>\n" +
                "        <HTTP>\n" +
                "          <Get><OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"http://www.example.com/cgi-bin/mapserver?\"/></Get>\n" +
                "          <Post><OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"http://www.example.com/cgi-bin/mapserver?\"/></Post>\n" +
                "        </HTTP>\n" +
                "      </DCPType>\n" +
                "    </GetFeatureInfo>\n" +
                "    <DescribeLayer>\n" +
                "      <Format>text/xml</Format>\n" +
                "      <DCPType>\n" +
                "        <HTTP>\n" +
                "          <Get><OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"http://www.example.com/cgi-bin/mapserver?\"/></Get>\n" +
                "          <Post><OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"http://www.example.com/cgi-bin/mapserver?\"/></Post>\n" +
                "        </HTTP>\n" +
                "      </DCPType>\n" +
                "    </DescribeLayer>\n" +
                "    <GetLegendGraphic>\n" +
                "      <Format>image/gif</Format>\n" +
                "      <Format>image/png; mode=24bit</Format>\n" +
                "      <Format>image/wbmp</Format>\n" +
                "      <DCPType>\n" +
                "        <HTTP>\n" +
                "          <Get><OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"http://www.example.com/cgi-bin/mapserver?\"/></Get>\n" +
                "          <Post><OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"http://www.example.com/cgi-bin/mapserver?\"/></Post>\n" +
                "        </HTTP>\n" +
                "      </DCPType>\n" +
                "    </GetLegendGraphic>\n" +
                "    <GetStyles>\n" +
                "      <Format>text/xml</Format>\n" +
                "      <DCPType>\n" +
                "        <HTTP>\n" +
                "          <Get><OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"http://www.example.com/cgi-bin/mapserver?\"/></Get>\n" +
                "          <Post><OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"http://www.example.com/cgi-bin/mapserver?\"/></Post>\n" +
                "        </HTTP>\n" +
                "      </DCPType>\n" +
                "    </GetStyles>\n" +
                "  </Request>\n" +
                "  <Exception>\n" +
                "    <Format>application/vnd.ogc.se_xml</Format>\n" +
                "    <Format>application/vnd.ogc.se_inimage</Format>\n" +
                "    <Format>application/vnd.ogc.se_blank</Format>\n" +
                "  </Exception>\n" +
                "  <VendorSpecificCapabilities />\n" +
                "  <UserDefinedSymbolization SupportSLD=\"1\" UserLayer=\"0\" UserStyle=\"1\" RemoteWFS=\"0\"/>\n" +
                "  <Layer>\n" +
                "    <Name>SwissTopo</Name>\n" +
                "    <Title>SwissTopo raster WMS Server</Title>\n" +
                "    <SRS>epsg:21781</SRS>\n" +
                "    <SRS>epsg:4326</SRS>\n" +
                "    <LatLonBoundingBox minx=\"1.20539\" miny=\"42.4702\" maxx=\"18.1119\" maxy=\"50.3953\" />\n" +
                "    <BoundingBox SRS=\"EPSG:21781\"\n" +
                "                minx=\"155000\" miny=\"-253050\" maxx=\"1.365e+06\" maxy=\"583050\" />\n" +
                "    <Layer>\n" +
                "      <Name>cn</Name>\n" +
                "      <Title>SwissTopo</Title>\n" +
                "      <Abstract>cn</Abstract>\n" +
                "      <Layer queryable=\"0\" opaque=\"0\" cascaded=\"0\">\n" +
                "        <Name>cn25k</Name>\n" +
                "        <Title>cn25k</Title>\n" +
                "        <SRS>epsg:21781</SRS>\n" +
                "        <SRS>epsg:4326</SRS>\n" +
                "        <ScaleHint min=\"0.0707106399349092\" max=\"5.23258735518328\" />\n" +
                "      </Layer>\n" +
                "    </Layer>\n" +
                "  </Layer>\n" +
                "\n" +
                "</Capability>\n" +
                "</WMT_MS_Capabilities>";

        InputStream stream = new ByteArrayInputStream(response.getBytes("UTF-8"));
        WMSServerInfo info = WMSServerInfo.parseCapabilities(stream);
        assertEquals(false, info.isTileCache());
    }

    public void testParseGeoServer() throws IOException, SAXException, ParserConfigurationException {
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE WMT_MS_Capabilities SYSTEM \"http://wms.example.com:8080/geoserver/schemas/wms/1.1.1/WMS_MS_Capabilities.dtd\">\n" +
                "<WMT_MS_Capabilities version=\"1.1.1\">\n" +
                "  <Service>\n" +
                "    <Name>OGC:WMS</Name>\n" +
                "    <Title>GeoNetwork opensource embedded Web Map Server</Title>\n" +
                "    <Abstract>\n" +
                "Web Map Services provided by GeoServer for GeoNetwork opensource.\n" +
                "     </Abstract>\n" +
                "    <KeywordList>\n" +
                "      <Keyword>WFS</Keyword>\n" +
                "      <Keyword>WMS</Keyword>\n" +
                "      <Keyword>GEOSERVER</Keyword>\n" +
                "      <Keyword>GEONETWORK</Keyword>\n" +
                "      <Keyword>OSGeo</Keyword>\n" +
                "    </KeywordList>\n" +
                "    <OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:type=\"simple\" xlink:href=\"http://geonetwork-opensource.org/\"/>\n" +
                "    <ContactInformation>\n" +
                "      <ContactPersonPrimary>\n" +
                "        <ContactPerson/>\n" +
                "        <ContactOrganization/>\n" +
                "      </ContactPersonPrimary>\n" +
                "      <ContactPosition/>\n" +
                "      <ContactAddress>\n" +
                "        <AddressType/>\n" +
                "        <Address/>\n" +
                "        <City/>\n" +
                "        <StateOrProvince/>\n" +
                "        <PostCode/>\n" +
                "        <Country/>\n" +
                "      </ContactAddress>\n" +
                "      <ContactVoiceTelephone/>\n" +
                "      <ContactFacsimileTelephone/>\n" +
                "      <ContactElectronicMailAddress/>\n" +
                "    </ContactInformation>\n" +
                "    <Fees>NONE</Fees>\n" +
                "    <AccessConstraints>NONE</AccessConstraints>\n" +
                "  </Service>\n" +
                "  <Capability>\n" +
                "    <Request>\n" +
                "      <GetCapabilities>\n" +
                "        <Format>application/vnd.ogc.wms_xml</Format>\n" +
                "        <DCPType>\n" +
                "          <HTTP>\n" +
                "            <Get>\n" +
                "              <OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:type=\"simple\" xlink:href=\"http://wms.example.com:8080/geoserver/wms?SERVICE=WMS&amp;\"/>\n" +
                "            </Get>\n" +
                "            <Post>\n" +
                "              <OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:type=\"simple\" xlink:href=\"http://wms.example.com:8080/geoserver/wms?SERVICE=WMS&amp;\"/>\n" +
                "            </Post>\n" +
                "          </HTTP>\n" +
                "        </DCPType>\n" +
                "      </GetCapabilities>\n" +
                "      <GetMap>\n" +
                "        <Format>image/png</Format>\n" +
                "        <Format>application/atom+xml</Format>\n" +
                "        <Format>application/openlayers</Format>\n" +
                "        <Format>application/pdf</Format>\n" +
                "        <Format>application/rss+xml</Format>\n" +
                "        <Format>application/vnd.google-earth.kml+xml</Format>\n" +
                "        <Format>application/vnd.google-earth.kmz</Format>\n" +
                "        <Format>image/geotiff</Format>\n" +
                "        <Format>image/geotiff8</Format>\n" +
                "        <Format>image/gif</Format>\n" +
                "        <Format>image/jpeg</Format>\n" +
                "        <Format>image/png8</Format>\n" +
                "        <Format>image/svg+xml</Format>\n" +
                "        <Format>image/tiff</Format>\n" +
                "        <Format>image/tiff8</Format>\n" +
                "        <DCPType>\n" +
                "          <HTTP>\n" +
                "            <Get>\n" +
                "              <OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:type=\"simple\" xlink:href=\"http://wms.example.com:8080/geoserver/wms?SERVICE=WMS&amp;\"/>\n" +
                "            </Get>\n" +
                "          </HTTP>\n" +
                "        </DCPType>\n" +
                "      </GetMap>\n" +
                "      <GetFeatureInfo>\n" +
                "        <Format>text/plain</Format>\n" +
                "        <Format>text/html</Format>\n" +
                "        <Format>application/vnd.ogc.gml</Format>\n" +
                "        <DCPType>\n" +
                "          <HTTP>\n" +
                "            <Get>\n" +
                "              <OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:type=\"simple\" xlink:href=\"http://wms.example.com:8080/geoserver/wms?SERVICE=WMS&amp;\"/>\n" +
                "            </Get>\n" +
                "            <Post>\n" +
                "              <OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:type=\"simple\" xlink:href=\"http://wms.example.com:8080/geoserver/wms?SERVICE=WMS&amp;\"/>\n" +
                "            </Post>\n" +
                "          </HTTP>\n" +
                "        </DCPType>\n" +
                "      </GetFeatureInfo>\n" +
                "      <DescribeLayer>\n" +
                "        <Format>application/vnd.ogc.wms_xml</Format>\n" +
                "        <DCPType>\n" +
                "          <HTTP>\n" +
                "            <Get>\n" +
                "              <OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:type=\"simple\" xlink:href=\"http://wms.example.com:8080/geoserver/wms?SERVICE=WMS&amp;\"/>\n" +
                "            </Get>\n" +
                "          </HTTP>\n" +
                "        </DCPType>\n" +
                "      </DescribeLayer>\n" +
                "      <GetLegendGraphic>\n" +
                "        <Format>image/png</Format>\n" +
                "        <Format>image/jpeg</Format>\n" +
                "        <Format>image/gif</Format>\n" +
                "        <DCPType>\n" +
                "          <HTTP>\n" +
                "            <Get>\n" +
                "              <OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:type=\"simple\" xlink:href=\"http://wms.example.com:8080/geoserver/wms?SERVICE=WMS&amp;\"/>\n" +
                "            </Get>\n" +
                "          </HTTP>\n" +
                "        </DCPType>\n" +
                "      </GetLegendGraphic>\n" +
                "    </Request>\n" +
                "    <Exception>\n" +
                "      <Format>application/vnd.ogc.se_xml</Format>\n" +
                "    </Exception>\n" +
                "    <UserDefinedSymbolization SupportSLD=\"1\" UserLayer=\"1\" UserStyle=\"1\" RemoteWFS=\"0\"/>\n" +
                "    <Layer>\n" +
                "      <Title>GeoNetwork opensource embedded Web Map Server</Title>\n" +
                "      <Abstract>\n" +
                "Web Map Services provided by GeoServer for GeoNetwork opensource.\n" +
                "     </Abstract>\n" +
                "      <!--common SRS:-->\n" +
                "      <SRS>EPSG:21781</SRS>\n" +
                "      <!--All supported EPSG projections:-->\n" +
                "      <SRS>EPSG:2000</SRS>\n" +
                "      <SRS>EPSG:2001</SRS>\n" +
                "      <SRS>EPSG:2002</SRS>  <!-- ...cut... -->\n" +
                "      <SRS>EPSG:42304</SRS>\n" +
                "      <SRS>EPSG:42303</SRS>\n" +
                "      <LatLonBoundingBox minx=\"-180.0\" miny=\"45.78874927621686\" maxx=\"10.558901428148609\" maxy=\"180.0\"/>\n" +
                "      <Layer queryable=\"1\">\n" +
                "        <Name>gn:countries</Name>\n" +
                "        <Title>countries_Type</Title>\n" +
                "        <Abstract>Generated from countries</Abstract>\n" +
                "        <KeywordList>\n" +
                "          <Keyword>countries</Keyword>\n" +
                "        </KeywordList>\n" +
                "        <SRS>EPSG:21781</SRS>\n" +
                "        <!--WKT definition of this CRS:\n" +
                "PROJCS[\"CH1903 / LV03\", \n" +
                "  GEOGCS[\"CH1903\", \n" +
                "    DATUM[\"CH1903\", \n" +
                "      SPHEROID[\"Bessel 1841\", 6377397.155, 299.1528128, AUTHORITY[\"EPSG\",\"7004\"]], \n" +
                "      TOWGS84[674.4, 15.1, 405.3, 0.0, 0.0, 0.0, 0.0], \n" +
                "      AUTHORITY[\"EPSG\",\"6149\"]], \n" +
                "    PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], \n" +
                "    UNIT[\"degree\", 0.017453292519943295], \n" +
                "    AXIS[\"Geodetic longitude\", EAST], \n" +
                "    AXIS[\"Geodetic latitude\", NORTH], \n" +
                "    AUTHORITY[\"EPSG\",\"4149\"]], \n" +
                "  PROJECTION[\"Oblique Mercator\", AUTHORITY[\"EPSG\",\"9815\"]], \n" +
                "  PARAMETER[\"longitude_of_center\", 7.439583333333333], \n" +
                "  PARAMETER[\"latitude_of_center\", 46.952405555555565], \n" +
                "  PARAMETER[\"azimuth\", 90.0], \n" +
                "  PARAMETER[\"scale_factor\", 1.0], \n" +
                "  PARAMETER[\"false_easting\", 600000.0], \n" +
                "  PARAMETER[\"false_northing\", 200000.0], \n" +
                "  PARAMETER[\"rectified_grid_angle\", 90.0], \n" +
                "  UNIT[\"m\", 1.0], \n" +
                "  AXIS[\"Easting\", EAST], \n" +
                "  AXIS[\"Northing\", NORTH], \n" +
                "  AUTHORITY[\"EPSG\",\"21781\"]]-->\n" +
                "        <LatLonBoundingBox minx=\"5.956640769345093\" miny=\"45.81975202969038\" maxx=\"10.493459252966687\" maxy=\"47.810475823557454\"/>\n" +
                "        <BoundingBox SRS=\"EPSG:21781\" minx=\"5.956640769345093\" miny=\"45.81975202969038\" maxx=\"10.493459252966687\" maxy=\"47.810475823557454\"/>\n" +
                "        <Style>\n" +
                "          <Name>Selection</Name>\n" +
                "          <Title>A style to show the selected feature</Title>\n" +
                "          <Abstract>A yellow line with a 2 pixel width</Abstract>\n" +
                "          <LegendURL width=\"20\" height=\"20\">\n" +
                "            <Format>image/png</Format>\n" +
                "            <OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:type=\"simple\" xlink:href=\"http://wms.example.com:8080/geoserver/wms/GetLegendGraphic?VERSION=1.0.0&amp;FORMAT=image/png&amp;WIDTH=20&amp;HEIGHT=20&amp;LAYER=gn:countries\"/>\n" +
                "          </LegendURL>\n" +
                "        </Style>\n" +
                "        <Style>\n" +
                "          <Name>Selection</Name>\n" +
                "          <Title>A style to show the selected feature</Title>\n" +
                "          <Abstract>A yellow line with a 2 pixel width</Abstract>\n" +
                "          <LegendURL width=\"20\" height=\"20\">\n" +
                "            <Format>image/png</Format>\n" +
                "            <OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:type=\"simple\" xlink:href=\"http://wms.example.com:8080/geoserver/wms/GetLegendGraphic?VERSION=1.0.0&amp;FORMAT=image/png&amp;WIDTH=20&amp;HEIGHT=20&amp;LAYER=gn:countries\"/>\n" +
                "          </LegendURL>\n" +
                "        </Style>\n" +
                "      </Layer>\n" +
                "      <Layer queryable=\"1\">\n" +
                "        <Name>gn:gemeindenBB</Name>\n" +
                "        <Title>gemeindenBB_Type</Title>\n" +
                "        <Abstract>Generated from gemeindenBB</Abstract>\n" +
                "        <KeywordList>\n" +
                "          <Keyword>gemeindenBB</Keyword>\n" +
                "        </KeywordList>\n" +
                "        <SRS>EPSG:21781</SRS>\n" +
                "        <!--WKT definition of this CRS:\n" +
                "PROJCS[\"CH1903 / LV03\", \n" +
                "  GEOGCS[\"CH1903\", \n" +
                "    DATUM[\"CH1903\", \n" +
                "      SPHEROID[\"Bessel 1841\", 6377397.155, 299.1528128, AUTHORITY[\"EPSG\",\"7004\"]], \n" +
                "      TOWGS84[674.4, 15.1, 405.3, 0.0, 0.0, 0.0, 0.0], \n" +
                "      AUTHORITY[\"EPSG\",\"6149\"]], \n" +
                "    PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], \n" +
                "    UNIT[\"degree\", 0.017453292519943295], \n" +
                "    AXIS[\"Geodetic longitude\", EAST], \n" +
                "    AXIS[\"Geodetic latitude\", NORTH], \n" +
                "    AUTHORITY[\"EPSG\",\"4149\"]], \n" +
                "  PROJECTION[\"Oblique Mercator\", AUTHORITY[\"EPSG\",\"9815\"]], \n" +
                "  PARAMETER[\"longitude_of_center\", 7.439583333333333], \n" +
                "  PARAMETER[\"latitude_of_center\", 46.952405555555565], \n" +
                "  PARAMETER[\"azimuth\", 90.0], \n" +
                "  PARAMETER[\"scale_factor\", 1.0], \n" +
                "  PARAMETER[\"false_easting\", 600000.0], \n" +
                "  PARAMETER[\"false_northing\", 200000.0], \n" +
                "  PARAMETER[\"rectified_grid_angle\", 90.0], \n" +
                "  UNIT[\"m\", 1.0], \n" +
                "  AXIS[\"Easting\", EAST], \n" +
                "  AXIS[\"Northing\", NORTH], \n" +
                "  AUTHORITY[\"EPSG\",\"21781\"]]-->\n" +
                "        <LatLonBoundingBox minx=\"5.956610444770297\" miny=\"45.81975202969038\" maxx=\"10.493459252966687\" maxy=\"47.810475823557454\"/>\n" +
                "        <BoundingBox SRS=\"EPSG:21781\" minx=\"484807.6327910628\" miny=\"74247.28126117215\" maxx=\"837389.5575765288\" maxy=\"300004.7975591116\"/>\n" +
                "        <Style>\n" +
                "          <Name>Selection</Name>\n" +
                "          <Title>A style to show the selected feature</Title>\n" +
                "          <Abstract>A yellow line with a 2 pixel width</Abstract>\n" +
                "          <LegendURL width=\"20\" height=\"20\">\n" +
                "            <Format>image/png</Format>\n" +
                "            <OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:type=\"simple\" xlink:href=\"http://wms.example.com:8080/geoserver/wms/GetLegendGraphic?VERSION=1.0.0&amp;FORMAT=image/png&amp;WIDTH=20&amp;HEIGHT=20&amp;LAYER=gn:gemeindenBB\"/>\n" +
                "          </LegendURL>\n" +
                "        </Style>\n" +
                "      </Layer>\n" +
                "      <Layer queryable=\"1\">\n" +
                "        <Name>gn:kantoneBB</Name>\n" +
                "        <Title>kantoneBB_Type</Title>\n" +
                "        <Abstract>Generated from kantoneBB</Abstract>\n" +
                "        <KeywordList>\n" +
                "          <Keyword>kantoneBB</Keyword>\n" +
                "        </KeywordList>\n" +
                "        <SRS>EPSG:21781</SRS>\n" +
                "        <!--WKT definition of this CRS:\n" +
                "PROJCS[\"CH1903 / LV03\", \n" +
                "  GEOGCS[\"CH1903\", \n" +
                "    DATUM[\"CH1903\", \n" +
                "      SPHEROID[\"Bessel 1841\", 6377397.155, 299.1528128, AUTHORITY[\"EPSG\",\"7004\"]], \n" +
                "      TOWGS84[674.4, 15.1, 405.3, 0.0, 0.0, 0.0, 0.0], \n" +
                "      AUTHORITY[\"EPSG\",\"6149\"]], \n" +
                "    PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], \n" +
                "    UNIT[\"degree\", 0.017453292519943295], \n" +
                "    AXIS[\"Geodetic longitude\", EAST], \n" +
                "    AXIS[\"Geodetic latitude\", NORTH], \n" +
                "    AUTHORITY[\"EPSG\",\"4149\"]], \n" +
                "  PROJECTION[\"Oblique Mercator\", AUTHORITY[\"EPSG\",\"9815\"]], \n" +
                "  PARAMETER[\"longitude_of_center\", 7.439583333333333], \n" +
                "  PARAMETER[\"latitude_of_center\", 46.952405555555565], \n" +
                "  PARAMETER[\"azimuth\", 90.0], \n" +
                "  PARAMETER[\"scale_factor\", 1.0], \n" +
                "  PARAMETER[\"false_easting\", 600000.0], \n" +
                "  PARAMETER[\"false_northing\", 200000.0], \n" +
                "  PARAMETER[\"rectified_grid_angle\", 90.0], \n" +
                "  UNIT[\"m\", 1.0], \n" +
                "  AXIS[\"Easting\", EAST], \n" +
                "  AXIS[\"Northing\", NORTH], \n" +
                "  AUTHORITY[\"EPSG\",\"21781\"]]-->\n" +
                "        <LatLonBoundingBox minx=\"5.908953517650008\" miny=\"45.78874927621686\" maxx=\"10.558901428148609\" maxy=\"47.81382548271046\"/>\n" +
                "        <BoundingBox SRS=\"EPSG:21781\" minx=\"485410.0\" miny=\"75270.0\" maxx=\"833840.7\" maxy=\"295935.0\"/>\n" +
                "        <Style>\n" +
                "          <Name>Selection</Name>\n" +
                "          <Title>A style to show the selected feature</Title>\n" +
                "          <Abstract>A yellow line with a 2 pixel width</Abstract>\n" +
                "          <LegendURL width=\"20\" height=\"20\">\n" +
                "            <Format>image/png</Format>\n" +
                "            <OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:type=\"simple\" xlink:href=\"http://wms.example.com:8080/geoserver/wms/GetLegendGraphic?VERSION=1.0.0&amp;FORMAT=image/png&amp;WIDTH=20&amp;HEIGHT=20&amp;LAYER=gn:kantoneBB\"/>\n" +
                "          </LegendURL>\n" +
                "        </Style>\n" +
                "        <Style>\n" +
                "          <Name>Selection</Name>\n" +
                "          <Title>A style to show the selected feature</Title>\n" +
                "          <Abstract>A yellow line with a 2 pixel width</Abstract>\n" +
                "          <LegendURL width=\"20\" height=\"20\">\n" +
                "            <Format>image/png</Format>\n" +
                "            <OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:type=\"simple\" xlink:href=\"http://wms.example.com:8080/geoserver/wms/GetLegendGraphic?VERSION=1.0.0&amp;FORMAT=image/png&amp;WIDTH=20&amp;HEIGHT=20&amp;LAYER=gn:kantoneBB\"/>\n" +
                "          </LegendURL>\n" +
                "        </Style>\n" +
                "      </Layer>\n" +
                "      <Layer queryable=\"1\">\n" +
                "        <Name>gn:xlinks</Name>\n" +
                "        <Title>xlinks</Title>\n" +
                "        <Abstract>xlinks</Abstract>\n" +
                "        <KeywordList>\n" +
                "          <Keyword>xlinks</Keyword>\n" +
                "        </KeywordList>\n" +
                "        <SRS>EPSG:21781</SRS>\n" +
                "        <!--WKT definition of this CRS:\n" +
                "PROJCS[\"CH1903 / LV03\", \n" +
                "  GEOGCS[\"CH1903\", \n" +
                "    DATUM[\"CH1903\", \n" +
                "      SPHEROID[\"Bessel 1841\", 6377397.155, 299.1528128, AUTHORITY[\"EPSG\",\"7004\"]], \n" +
                "      TOWGS84[674.4, 15.1, 405.3, 0.0, 0.0, 0.0, 0.0], \n" +
                "      AUTHORITY[\"EPSG\",\"6149\"]], \n" +
                "    PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], \n" +
                "    UNIT[\"degree\", 0.017453292519943295], \n" +
                "    AXIS[\"Geodetic longitude\", EAST], \n" +
                "    AXIS[\"Geodetic latitude\", NORTH], \n" +
                "    AUTHORITY[\"EPSG\",\"4149\"]], \n" +
                "  PROJECTION[\"Oblique Mercator\", AUTHORITY[\"EPSG\",\"9815\"]], \n" +
                "  PARAMETER[\"longitude_of_center\", 7.439583333333333], \n" +
                "  PARAMETER[\"latitude_of_center\", 46.952405555555565], \n" +
                "  PARAMETER[\"azimuth\", 90.0], \n" +
                "  PARAMETER[\"scale_factor\", 1.0], \n" +
                "  PARAMETER[\"false_easting\", 600000.0], \n" +
                "  PARAMETER[\"false_northing\", 200000.0], \n" +
                "  PARAMETER[\"rectified_grid_angle\", 90.0], \n" +
                "  UNIT[\"m\", 1.0], \n" +
                "  AXIS[\"Easting\", EAST], \n" +
                "  AXIS[\"Northing\", NORTH], \n" +
                "  AUTHORITY[\"EPSG\",\"21781\"]]-->\n" +
                "        <LatLonBoundingBox minx=\"-180.0\" miny=\"90.0\" maxx=\"-90.0\" maxy=\"180.0\"/>\n" +
                "        <BoundingBox SRS=\"EPSG:21781\" minx=\"9.626899504917674\" miny=\"47.12853893946158\" maxx=\"9.82825134054292\" maxy=\"47.24631077121012\"/>\n" +
                "        <Style>\n" +
                "          <Name>polygon</Name>\n" +
                "          <Title>A boring default style</Title>\n" +
                "          <Abstract>A sample style that just prints out a transparent red interior with a red outline</Abstract>\n" +
                "          <LegendURL width=\"20\" height=\"20\">\n" +
                "            <Format>image/png</Format>\n" +
                "            <OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:type=\"simple\" xlink:href=\"http://wms.example.com:8080/geoserver/wms/GetLegendGraphic?VERSION=1.0.0&amp;FORMAT=image/png&amp;WIDTH=20&amp;HEIGHT=20&amp;LAYER=gn:xlinks\"/>\n" +
                "          </LegendURL>\n" +
                "        </Style>\n" +
                "      </Layer>\n" +
                "      <Layer queryable=\"0\">\n" +
                "        <Name>gn:world</Name>\n" +
                "        <Title>Blue Marble world image</Title>\n" +
                "        <Abstract>Blue Marble world image</Abstract>\n" +
                "        <KeywordList>\n" +
                "          <Keyword>Blue</Keyword>\n" +
                "          <Keyword>Marble</Keyword>\n" +
                "          <Keyword>world</Keyword>\n" +
                "          <Keyword>topography</Keyword>\n" +
                "          <Keyword>bathymetry</Keyword>\n" +
                "          <Keyword>200407</Keyword>\n" +
                "        </KeywordList>\n" +
                "        <!--WKT definition of this CRS:\n" +
                "GEOGCS[\"WGS 84\", \n" +
                "  DATUM[\"World Geodetic System 1984\", \n" +
                "    SPHEROID[\"WGS 84\", 6378137.0, 298.257223563, AUTHORITY[\"EPSG\",\"7030\"]], \n" +
                "    AUTHORITY[\"EPSG\",\"6326\"]], \n" +
                "  PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], \n" +
                "  UNIT[\"degree\", 0.017453292519943295], \n" +
                "  AXIS[\"Geodetic longitude\", EAST], \n" +
                "  AXIS[\"Geodetic latitude\", NORTH], \n" +
                "  AUTHORITY[\"EPSG\",\"4326\"]]-->\n" +
                "        <SRS>EPSG:4326</SRS>\n" +
                "        <LatLonBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/>\n" +
                "        <BoundingBox SRS=\"EPSG:4326\" minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/>\n" +
                "        <Style>\n" +
                "          <Name>raster</Name>\n" +
                "          <Title>A boring default style</Title>\n" +
                "          <Abstract>A sample style for rasters</Abstract>\n" +
                "          <LegendURL width=\"20\" height=\"20\">\n" +
                "            <Format>image/png</Format>\n" +
                "            <OnlineResource xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:type=\"simple\" xlink:href=\"http://wms.example.com:8080/geoserver/wms/GetLegendGraphic?VERSION=1.0.0&amp;FORMAT=image/png&amp;WIDTH=20&amp;HEIGHT=20&amp;LAYER=gn:world\"/>\n" +
                "          </LegendURL>\n" +
                "        </Style>\n" +
                "      </Layer>\n" +
                "    </Layer>\n" +
                "  </Capability>\n" +
                "\n" +
                "</WMT_MS_Capabilities>";

        InputStream stream = new ByteArrayInputStream(response.getBytes("UTF-8"));
        WMSServerInfo info = WMSServerInfo.parseCapabilities(stream);
        assertEquals(false, info.isTileCache());
    }
}
