<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
    xmlns="http://www.opengis.net/sld" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <NamedLayer>
        <Name>overview-map</Name>
        <UserStyle>
            <FeatureTypeStyle>
                <Rule>
                    <Name>rule1</Name>
                    <PolygonSymbolizer>
                     <Fill>
                        <CssParameter name="fill">#009933</CssParameter>
                        <CssParameter name="fill-opacity">0.3</CssParameter>
                     </Fill>
                     <Stroke>
                        <CssParameter name="stroke">#009933</CssParameter>
                        <CssParameter name="stroke-width">1</CssParameter>
                        <CssParameter name="stroke-dasharray">5 2</CssParameter>
                     </Stroke>
                    </PolygonSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>

