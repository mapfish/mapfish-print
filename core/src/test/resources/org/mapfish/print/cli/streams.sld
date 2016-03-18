<?xml version="1.0" encoding="UTF-8"?>

<StyledLayerDescriptor version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
    xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <NamedLayer> <Name> area landmarks </Name>
        <UserStyle>
            <FeatureTypeStyle>
                <FeatureTypeName>Feature</FeatureTypeName>
                <Rule>  <!-- thick line drawn first-->
                    <LineSymbolizer>
                        <Stroke>
                            <CssParameter name="stroke">
                                <ogc:Literal>#666666</ogc:Literal>
                            </CssParameter>
                            <CssParameter name="stroke-width">
                                <ogc:Literal>7</ogc:Literal>
                            </CssParameter>
                        </Stroke>
                    </LineSymbolizer>
                </Rule>
            </FeatureTypeStyle>
            <FeatureTypeStyle>
                <FeatureTypeName>Feature</FeatureTypeName>
                <Rule>  <!-- thin line drawn second -->
                    <LineSymbolizer>
                        <Stroke>
                            <CssParameter name="stroke">
                                <ogc:Literal>#FFFFFF</ogc:Literal>
                            </CssParameter>
                            <CssParameter name="stroke-width">
                                <ogc:Literal>4</ogc:Literal>
                            </CssParameter>
                        </Stroke>
                    </LineSymbolizer>
                </Rule>
            </FeatureTypeStyle>

        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>
