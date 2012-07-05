<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:custom="Custom"
                xmlns:svg="http://www.w3.org/2000/svg"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                version="1.0">
  <!-- Some XSLT kungfu for adjusting line width in SVG files -->

  <xalan:component prefix="custom" functions="factorArray,factorValue">
    <xalan:script lang="javaclass" src="org.mapfish.print.CustomXPath"/>
  </xalan:component>

  <xsl:param name="zoomFactor">1</xsl:param>

  <xsl:preserve-space elements="text"/>

  <xsl:template match="/*">
    <svg:svg xmlns:svg="http://www.w3.org/2000/svg"
             xmlns:xlink="http://www.w3.org/1999/xlink">
      <xsl:for-each select="@*">
        <xsl:attribute name="{name(.)}">
          <xsl:value-of select="."/>
        </xsl:attribute>
      </xsl:for-each>
      <xsl:apply-templates/>
    </svg:svg>
  </xsl:template>

  <xsl:template match="*">
    <xsl:element name="svg:{name(.)}">
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates select="*"/>
      <xsl:apply-templates select="text()"/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="@stroke-width|@rx|@ry|@font-size">
    <xsl:attribute name="{name(.)}">
      <xsl:value-of select="custom:factorValue(., $zoomFactor)"/>
    </xsl:attribute>
  </xsl:template>

  <xsl:template match="@stroke-dasharray">
    <xsl:attribute name="{name(.)}">
      <xsl:value-of select="custom:factorArray(., $zoomFactor)"/>
    </xsl:attribute>
  </xsl:template>

  <xsl:template match="@*">
    <xsl:attribute name="{name(.)}">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>

  <xsl:template match="text()">
    <xsl:value-of select="."/>
  </xsl:template>
</xsl:stylesheet>