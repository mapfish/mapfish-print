package org.mapfish.print.processor;

import jsr166y.ForkJoinPool;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.geotools.AbstractFeatureSourceLayer;
import org.mapfish.print.output.Values;
import org.mapfish.print.parser.MapfishParser;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

public class SetStyleProcessorTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "setstyle/";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private MapfishParser parser;
    @Autowired
    private ForkJoinPool forkJoinPool;


    @Test
    public void testBasicTableProperties() throws Exception {
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
        final Template template = config.getTemplate("main");
        PJsonObject requestData = parseJSONObjectFromFile(SetStyleProcessorTest.class, BASE_DIR + "request.json");
        Values values = new Values(requestData, template, parser, this.folder.getRoot());
        forkJoinPool.invoke(template.getProcessorGraph().createTask(values));

        final MapAttribute.MapAttributeValues map = values.getObject("map", MapAttribute.MapAttributeValues.class);
        final AbstractFeatureSourceLayer layer = (AbstractFeatureSourceLayer)map.getLayers().get(0);

        //assertEquals("<?xml version=\"1.0\" encoding=\"UTF8\"?><StyledLayerDescriptor version=\"1.0.0\" xsi:schemaLocation=\"http://www.opengis.net/sld StyledLayerDescriptor.xsd\" xmlns=\"http://www.opengis.net/sld\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> <NamedLayer> <Name>default_line</Name> <UserStyle> <Title>Default Line</Title> <Abstract>A sample style that draws a line</Abstract> <FeatureTypeStyle> <Rule> <Name>01000</Name> <Filter> <PropertyIsEqualTo> <PropertyName>cp</PropertyName> <Literal>01000</Literal> </PropertyIsEqualTo> </Filter> <PointSymbolizer> <Graphic> <Mark> <WellKnownName>circle</WellKnownName> <Fill>  <CssParameter name=\"fill\">#0000ff</CssParameter> </Fill> <Stroke>  <CssParameter name=\"stroke\">#000000</CssParameter>  <CssParameter name=\"stroke-width\">2</CssParameter> </Stroke> </Mark> <Size>5</Size> </Graphic> </PointSymbolizer> <TextSymbolizer> <Label>count</Label> <Font> <!--CssParameter name=\"font-family\"></CssParameter> <CssParameter name=\"font-weight\"></CssParameter> <CssParameter name=\"font-style\"></CssParameter--> </Font> <Fill> <CssParameter name=\"fill\">#111111</CssParameter> </Fill> <Halo> <Radius>5</Radius> <CssParameter name=\"fill\">#000000</CssParameter> </Halo> </TextSymbolizer> </Rule> <Rule> <Name>01000</Name> <Filter> <PropertyIsEqualTo> <PropertyName>cp</PropertyName> <Literal>01000</Literal> </PropertyIsEqualTo> </Filter> <PointSymbolizer> <Graphic> <Mark> <WellKnownName>circle</WellKnownName> <Fill>  <CssParameter name=\"fill\">#0000ff</CssParameter> </Fill> <Stroke>  <CssParameter name=\"stroke\">#ff0000</CssParameter>  <CssParameter name=\"stroke-width\">2</CssParameter> </Stroke> </Mark> <Size>20</Size> </Graphic> </PointSymbolizer> <TextSymbolizer> <Label>count</Label> <Font> <!--CssParameter name=\"font-family\"></CssParameter> <CssParameter name=\"font-weight\"></CssParameter> <CssParameter name=\"font-style\"></CssParameter--> </Font> <Fill> <CssParameter name=\"fill\">#111111</CssParameter> </Fill> <Halo> <Radius>5</Radius> <CssParameter name=\"fill\">#000000</CssParameter> </Halo> </TextSymbolizer> </Rule> </FeatureTypeStyle> </UserStyle> </NamedLayer></StyledLayerDescriptor>", layer.getStyle());
    }
}
