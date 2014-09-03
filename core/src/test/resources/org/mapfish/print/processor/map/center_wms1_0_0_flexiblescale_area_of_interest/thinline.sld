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
        <Name>default_line</Name>
        <UserStyle>
            <!-- Styles can have names, titles and abstracts -->
            <Title>Default Line</Title>
            <Abstract>A sample style that draws a line</Abstract>
            <!-- FeatureTypeStyles describe how to render different features -->
            <!-- A FeatureTypeStyle for rendering lines -->
            <FeatureTypeStyle>
                <Rule>
                    <Name>rule1</Name>
                    <Title>White Line</Title>
                    <Abstract>A solid white line with a 1 pixel width</Abstract>
                    <LineSymbolizer>
                        <Stroke>
                            <CssParameter name="stroke">#D95F02</CssParameter>
                        </Stroke>
                    </LineSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>

