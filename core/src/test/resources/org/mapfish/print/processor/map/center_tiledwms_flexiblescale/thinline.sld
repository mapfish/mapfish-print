<?xml version="1.0" encoding="ISO-8859-1"?>
<!--
  ~ Copyright (C) 2014  Camptocamp
  ~
  ~ This file is part of MapFish Print
  ~
  ~ MapFish Print is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation, either version 3 of the License, or
  ~ (at your option) any later version.
  ~
  ~ MapFish Print is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~ GNU General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
  -->

<StyledLayerDescriptor version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
    xmlns="http://www.opengis.net/sld" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <!-- a Named Layer is the basic building block of an SLD document -->
    <NamedLayer>
        <Name>default_polygon</Name>
        <UserStyle>
            <!-- Styles can have names, titles and abstracts -->
            <Title>Default Polygon</Title>
            <Abstract>A sample style that draws a polygon</Abstract>
            <!-- FeatureTypeStyles describe how to render different features -->
            <!-- A FeatureTypeStyle for rendering polygons -->
            <FeatureTypeStyle>
                <Rule>
                    <Name>rule1</Name>
                    <Title>Gray Polygon with Black Outline</Title>
                    <Abstract>A polygon with a gray fill and a 1 pixel black outline</Abstract>
                    <PolygonSymbolizer>
                        <Fill>
                            <CssParameter name="fill">#0000FF</CssParameter>
                            <CssParameter name="fill-opacity">0.5</CssParameter>
                        </Fill>
                        <Stroke>
                            <CssParameter name="stroke">#000000</CssParameter>
                            <CssParameter name="stroke-width">2</CssParameter>
                        </Stroke>
                    </PolygonSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>