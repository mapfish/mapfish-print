package org.mapfish.print.attribute.map;

import org.geotools.referencing.CRS;
import org.junit.Before;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OverviewMapAttributeTest extends AbstractMapfishSpringTest {

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private TestHttpClientFactory httpRequestFactory;

    @Before
    public void setUp() throws Exception {
        this.configurationFactory.setDoValidation(false);
    }

    @Test
    public void testAttributesFromJson() throws Exception {
        final File configFile = getFile(OverviewMapAttributeTest.class, "overviewmap_attributes/config-json.yaml");
        final Configuration config = configurationFactory.getConfig(configFile);
        final Template template = config.getTemplate("main");
        final PJsonObject pJsonObject = parseJSONObjectFromFile(OverviewMapAttributeTest.class, "overviewmap_attributes/requestData-json.json");

        final Values values = new Values("test", pJsonObject, template, getTaskDirectory(), this.httpRequestFactory,
                new File("."));
        final MapAttribute.MapAttributeValues mapValue = values.getObject("map", MapAttribute.MapAttributeValues.class);
        final OverviewMapAttribute.OverviewMapAttributeValues overviewMapValue =
                values.getObject("overviewMap", OverviewMapAttribute.OverviewMapAttributeValues.class);
        final MapAttribute.OverriddenMapAttributeValues value = mapValue.getWithOverrides(overviewMapValue);

        assertEquals(300.0, value.getDpi(), 0.1);
        assertNotNull(value.getLayers());
        assertEquals(2, value.getLayers().size());
        Object proj = value.getOriginalBounds().getProjection();
        CoordinateReferenceSystem expected = CRS.decode("CRS:84");
        assertTrue(CRS.equalsIgnoreMetadata(expected, proj));
        assertEquals(0.0, value.getRotation(), 0.1);
        assertEquals(200, value.getMapSize().width);
        assertEquals(100, value.getMapSize().height);
    }

    @Test
    public void testAttributesFromYaml() throws Exception {
        final File configFile = getFile(OverviewMapAttributeTest.class, "overviewmap_attributes/config-yaml.yaml");
        final Configuration config = configurationFactory.getConfig(configFile);
        final Template template = config.getTemplate("main");
        final PJsonObject pJsonObject = parseJSONObjectFromFile(OverviewMapAttributeTest.class, "overviewmap_attributes/requestData-yaml.json");

        final Values values = new Values("test", pJsonObject, template, getTaskDirectory(), this.httpRequestFactory, new File("."));
        final MapAttribute.MapAttributeValues mapValue = values.getObject("map", MapAttribute.MapAttributeValues.class);
        final OverviewMapAttribute.OverviewMapAttributeValues overviewMapValue =
                values.getObject("overviewMap", OverviewMapAttribute.OverviewMapAttributeValues.class);
        final MapAttribute.OverriddenMapAttributeValues value = mapValue.getWithOverrides(overviewMapValue);

        assertEquals(80.0, value.getDpi(), 0.1);
        assertNotNull(value.getLayers());

        Object proj = value.getOriginalBounds().getProjection();
        CoordinateReferenceSystem expected = CRS.decode("CRS:84");
        assertTrue(CRS.equalsIgnoreMetadata(expected, proj));
        assertEquals(Math.toRadians(10.0), value.getRotation(), 0.1);
        assertEquals(200, value.getMapSize().width);
        assertEquals(100, value.getMapSize().height);
        assertEquals(7.0, overviewMapValue.getZoomFactor(), 0.1);
    }

    @Test
    public void testAttributesFromBoth() throws Exception {
        final File configFile = getFile(OverviewMapAttributeTest.class, "overviewmap_attributes/config-yaml.yaml");
        final Configuration config = configurationFactory.getConfig(configFile);
        final Template template = config.getTemplate("main");
        final PJsonObject pJsonObject = parseJSONObjectFromFile(OverviewMapAttributeTest.class, "overviewmap_attributes/requestData-json.json");

        final Values values = new Values("test", pJsonObject, template, getTaskDirectory(), this.httpRequestFactory, new File("."));
        final MapAttribute.MapAttributeValues mapValue = values.getObject("map", MapAttribute.MapAttributeValues.class);
        final OverviewMapAttribute.OverviewMapAttributeValues overviewMapValue =
                values.getObject("overviewMap", OverviewMapAttribute.OverviewMapAttributeValues.class);
        final MapAttribute.OverriddenMapAttributeValues value = mapValue.getWithOverrides(overviewMapValue);

        assertEquals(300.0, value.getDpi(), 0.1);
        assertNotNull(value.getLayers());

        Object proj = value.getOriginalBounds().getProjection();
        CoordinateReferenceSystem expected = CRS.decode("CRS:84");
        assertTrue(CRS.equalsIgnoreMetadata(expected, proj));
        assertEquals(0.0, value.getRotation(), 0.1);
        assertEquals(200, value.getMapSize().width);
        assertEquals(100, value.getMapSize().height);
        assertEquals(7.0, overviewMapValue.getZoomFactor(), 0.1);
    }
}
