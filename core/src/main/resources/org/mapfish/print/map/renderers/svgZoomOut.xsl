<?xml version="1.0" encoding="utf-8"?>
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

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan" xmlns:custom="Custom"
    xmlns:svg="http://www.w3.org/2000/svg" version="1.0">
    <!-- Some XSLT kungfu for adjusting line width in SVG files -->

    <xalan:component prefix="custom" functions="factorArray,factorValue">
        <xalan:script lang="javaclass" src="org.mapfish.print.CustomXPath" />
    </xalan:component>

    <xsl:param name="zoomFactor">1</xsl:param>

    <xsl:preserve-space elements="text" />

    <xsl:template match="/*">
        <svg:svg xmlns:svg="http://www.w3.org/2000/svg">
            <xsl:for-each select="@*">
                <xsl:attribute name="{name(.)}">
                    <xsl:value-of select="." />
                </xsl:attribute>
            </xsl:for-each>
            <xsl:apply-templates />
        </svg:svg>
    </xsl:template>

    <xsl:template match="*">
        <xsl:element name="svg:{name(.)}">
            <xsl:apply-templates select="@*" />
            <xsl:apply-templates select="*" />
            <xsl:apply-templates select="text()" />
        </xsl:element>
    </xsl:template>

    <xsl:template match="@stroke-width|@rx|@ry|@font-size">
        <xsl:attribute name="{name(.)}">
            <xsl:value-of select="custom:factorValue(., $zoomFactor)" />
        </xsl:attribute>
    </xsl:template>

    <xsl:template match="@stroke-dasharray">
        <xsl:attribute name="{name(.)}">
            <xsl:value-of select="custom:factorArray(., $zoomFactor)" />
        </xsl:attribute>
    </xsl:template>

    <xsl:template match="@*">
        <xsl:attribute name="{name(.)}">
            <xsl:value-of select="." />
        </xsl:attribute>
    </xsl:template>

    <xsl:template match="text()">
        <xsl:value-of select="." />
    </xsl:template>
</xsl:stylesheet>