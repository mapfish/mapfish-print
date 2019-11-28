package org.mapfish.print.config;

import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.junit.After;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.Constants;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Tests for {@link org.mapfish.print.config.Configuration} class.
 */
public class ConfigurationTest {

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testGetTemplate() {

        final Configuration configuration = new Configuration();
        Map<String, Template> templates = new HashMap<>();
        final Template t1Template = new Template();
        templates.put("t1", t1Template);
        configuration.setTemplates(templates);
        assertEquals(t1Template, configuration.getTemplate("t1"));
        assertEquals(1, configuration.getTemplates().size());
        assertEquals(t1Template, configuration.getTemplates().values().iterator().next());
        assertNull(configuration.getTemplate("Doesn't exist"));
    }

    @Test
    public void testGetDefaultStyle_IsPresentInMap() {
        final Configuration configuration = new Configuration();
        Map<String, Style> styles = new HashMap<>();
        final Style pointStyle = Mockito.mock(Style.class);
        final Style lineStyle = Mockito.mock(Style.class);
        final Style polygonStyle = Mockito.mock(Style.class);
        final Style geomStyle = Mockito.mock(Style.class);
        styles.put("point", pointStyle);
        styles.put("line", lineStyle);
        styles.put("polygon", polygonStyle);
        styles.put("geometry", geomStyle);
        styles.put("grid", lineStyle);
        configuration.setDefaultStyle(styles);

        assertSame(pointStyle, configuration.getDefaultStyle("POINT"));
        assertSame(pointStyle, configuration.getDefaultStyle("MultiPOINT"));

        assertSame(lineStyle, configuration.getDefaultStyle("lIne"));
        assertSame(lineStyle, configuration.getDefaultStyle("lInestring"));
        assertSame(lineStyle, configuration.getDefaultStyle("linearRing"));
        assertSame(lineStyle, configuration.getDefaultStyle("multilInestring"));
        assertSame(lineStyle, configuration.getDefaultStyle("multiline"));
        assertSame(lineStyle, configuration.getDefaultStyle("grid"));

        assertSame(polygonStyle, configuration.getDefaultStyle("poly"));
        assertSame(polygonStyle, configuration.getDefaultStyle("polygon"));
        assertSame(polygonStyle, configuration.getDefaultStyle("multiPolygon"));

        assertSame(geomStyle, configuration.getDefaultStyle("geom"));
        assertSame(geomStyle, configuration.getDefaultStyle("geometry"));
        assertSame(geomStyle, configuration.getDefaultStyle("geometryCollection"));
        assertSame(geomStyle, configuration.getDefaultStyle("MultiGeometry"));

        assertSame(geomStyle, configuration.getDefaultStyle("other"));
    }

    @Test
    public void testGetDefaultStyle_NotInMap() {
        final Configuration configuration = new Configuration();
        Map<String, Style> styles = new HashMap<>();
        final Style geomStyle = Mockito.mock(Style.class);
        styles.put("geometry", geomStyle);
        configuration.setDefaultStyle(styles);


        assertStyleType(PointSymbolizer.class, configuration.getDefaultStyle("POINT"));
        assertStyleType(PointSymbolizer.class, configuration.getDefaultStyle("MultiPOINT"));

        assertStyleType(LineSymbolizer.class, configuration.getDefaultStyle("lIne"));
        assertStyleType(LineSymbolizer.class, configuration.getDefaultStyle("lInestring"));
        assertStyleType(LineSymbolizer.class, configuration.getDefaultStyle("linearRing"));
        assertStyleType(LineSymbolizer.class, configuration.getDefaultStyle("multilInestring"));
        assertStyleType(LineSymbolizer.class, configuration.getDefaultStyle("multiline"));

        assertStyleType(PolygonSymbolizer.class, configuration.getDefaultStyle("poly"));
        assertStyleType(PolygonSymbolizer.class, configuration.getDefaultStyle("polygon"));
        assertStyleType(PolygonSymbolizer.class, configuration.getDefaultStyle("multiPolygon"));

        assertStyleType(RasterSymbolizer.class, configuration.getDefaultStyle(Constants.Style.Raster.NAME));

        assertSame(geomStyle, configuration.getDefaultStyle("geom"));
        assertSame(geomStyle, configuration.getDefaultStyle("geometry"));
        assertSame(geomStyle, configuration.getDefaultStyle("geometryCollection"));
        assertSame(geomStyle, configuration.getDefaultStyle("MultiGeometry"));
    }

    @Test
    public void testGetDefaultStyle_GeomNotInMap() {
        final Configuration configuration = new Configuration();

        assertStyleType(Symbolizer.class, configuration.getDefaultStyle("geom"));
        assertStyleType(Symbolizer.class, configuration.getDefaultStyle("geometry"));
        assertStyleType(Symbolizer.class, configuration.getDefaultStyle("geometryCollection"));
        assertStyleType(Symbolizer.class, configuration.getDefaultStyle("MultiGeometry"));
        assertStyleType(Symbolizer.class, configuration.getDefaultStyle(Constants.Style.Grid.NAME_LINES));
        assertStyleType(Symbolizer.class, configuration.getDefaultStyle(Constants.Style.Raster.NAME));
    }


    @Test
    public void testTemplateAccess() throws Exception {
        Configuration configuration = new Configuration();
        Map<String, Template> templates = new HashMap<>();
        Template unrestricted = new Template();
        templates.put("unrestricted", unrestricted);
        Template restricted = new Template();
        restricted.setAccess(Arrays.asList("ROLE_USER", "ROLE_ADMIN"));
        templates.put("restricted", restricted);
        configuration.setTemplates(templates);

        assertCorrectTemplates(configuration, unrestricted, restricted,
                               AuthenticationCredentialsNotFoundException.class);

        SimpleGrantedAuthority userAuth = new SimpleGrantedAuthority("ROLE_USER");
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(securityContext);
        securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "", Collections.singletonList(userAuth)));
        assertCorrectTemplates(configuration, unrestricted, restricted, null);

        securityContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(securityContext);
        assertCorrectTemplates(configuration, unrestricted, restricted,
                               AuthenticationCredentialsNotFoundException.class);

        SimpleGrantedAuthority adminAuth = new SimpleGrantedAuthority("ROLE_ADMIN");
        securityContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(securityContext);
        securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "", Collections.singletonList(adminAuth)));
        assertCorrectTemplates(configuration, unrestricted, restricted, null);

        SecurityContextHolder.clearContext();
        assertCorrectTemplates(configuration, unrestricted, restricted,
                               AuthenticationCredentialsNotFoundException.class);


        SimpleGrantedAuthority otherAuth = new SimpleGrantedAuthority("ROLE_OTHER");
        securityContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(securityContext);
        securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken("other", "", Collections.singletonList(otherAuth)));
        assertCorrectTemplates(configuration, unrestricted, restricted, AccessDeniedException.class);
    }

    @Test
    public void testTemplateAccess_ConfigHasAccess() throws Exception {
        Configuration configuration = new Configuration();
        Map<String, Template> templates = new HashMap<>();
        Template template1 = new Template();
        templates.put("template1", template1);
        Template template2 = new Template();
        templates.put("template2", template2);
        configuration.setTemplates(templates);
        configuration.setAccess(Arrays.asList("ROLE_USER", "ROLE_ADMIN"));

        assertAccessTemplate_ConfigSecured(configuration, AuthenticationCredentialsNotFoundException.class);

        SimpleGrantedAuthority userAuth = new SimpleGrantedAuthority("ROLE_USER");
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(securityContext);
        securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "", Collections.singletonList(userAuth)));
        assertAccessTemplate_ConfigSecured(configuration, null);

        securityContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(securityContext);
        assertAccessTemplate_ConfigSecured(configuration, AuthenticationCredentialsNotFoundException.class);

        SimpleGrantedAuthority adminAuth = new SimpleGrantedAuthority("ROLE_ADMIN");
        securityContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(securityContext);
        securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "", Collections.singletonList(adminAuth)));
        assertAccessTemplate_ConfigSecured(configuration, null);

        SecurityContextHolder.clearContext();
        assertAccessTemplate_ConfigSecured(configuration, AuthenticationCredentialsNotFoundException.class);


        SimpleGrantedAuthority otherAuth = new SimpleGrantedAuthority("ROLE_OTHER");
        securityContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(securityContext);
        securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken("other", "", Collections.singletonList(otherAuth)));
        assertAccessTemplate_ConfigSecured(configuration, AccessDeniedException.class);
    }

    private void assertAccessTemplate_ConfigSecured(
            Configuration configuration,
            @Nullable Class<?> expectedException) {
        final int numExpectedTemplates = expectedException == null ? 2 : 0;
        assertEquals(numExpectedTemplates, configuration.getTemplates().size());
        if (expectedException != null) {
            try {
                configuration.getTemplate("template1");
                fail("Expected " + expectedException + " to be thrown");
            } catch (Exception e) {
                if (!expectedException.isInstance(e)) {
                    throw e;
                }
            }
        } else {
            assertNotNull(configuration.getTemplate("template1"));
            assertNotNull(configuration.getTemplate("template2"));
        }
        final JSONArray layouts = getClientConfigJson(configuration);
        assertEquals(numExpectedTemplates, layouts.length());
    }

    private JSONArray getClientConfigJson(Configuration configuration) throws JSONException {
        final StringWriter w = new StringWriter();
        JSONWriter writer = new JSONWriter(w);
        writer.object();
        configuration.printClientConfig(writer);
        writer.endObject();
        final JSONObject clientConfig = new JSONObject(w.toString());
        return clientConfig.getJSONArray("layouts");
    }

    private void assertCorrectTemplates(
            Configuration configuration,
            Template unrestricted,
            Template restricted,
            @Nullable Class<? extends Exception> expectedException) {
        if (expectedException != null) {
            try {
                configuration.getTemplate("restricted");
                fail("Expected " + expectedException + " to be thrown");
            } catch (Exception e) {
                if (!expectedException.isInstance(e)) {
                    throw e;
                }
            }
            assertFalse(configuration.getTemplates().containsKey("restricted"));
        } else {
            assertEquals(restricted, configuration.getTemplate("restricted"));
            assertTrue(configuration.getTemplates().containsKey("restricted"));
        }

        assertEquals(unrestricted, configuration.getTemplate("unrestricted"));
        final int expectedNumTemplates = expectedException == null ? 2 : 1;
        assertEquals(expectedNumTemplates, configuration.getTemplates().size());
        assertTrue(configuration.getTemplates().containsKey("unrestricted"));
        final JSONArray layouts = getClientConfigJson(configuration);
        assertEquals(expectedNumTemplates, layouts.length());
    }

    @Test
    public void testRenderAsSvg() {
        final Configuration config = new Configuration();
        config.setDefaultToSvg(false);
        assertFalse(config.renderAsSvg(null));
        assertFalse(config.renderAsSvg(false));
        assertTrue(config.renderAsSvg(true));

        config.setDefaultToSvg(true);
        assertTrue(config.renderAsSvg(null));
        assertFalse(config.renderAsSvg(false));
        assertTrue(config.renderAsSvg(true));
    }

    @Test
    public void testJdbcDrivers() {
        final Configuration config = new Configuration();
        Map<String, Template> templates = new HashMap<>();
        config.setTemplates(templates);
        config.setConfigurationFile(AbstractMapfishSpringTest.getFile(ConfigurationTest.class,
                                                                      "/org/mapfish/print/config/config" +
                                                                              "-test-application-context" +
                                                                              ".xml"));
        List<Throwable> errors = config.validate();

        assertEquals(1, errors.size()); // no templates error

        config.setJdbcDrivers(Collections.singleton("non.existant.driver.Driver"));
        errors = config.validate();

        assertEquals(2, errors.size());

        config.setJdbcDrivers(Collections.singleton("org.hsqldb.jdbc.JDBCDriver"));
        errors = config.validate();

        assertEquals(1, errors.size());


    }

    private void assertStyleType(Class<?> expectedSymbolizerType, Style style) {
        assertNotNull(style);
        final FeatureTypeStyle featureTypeStyle = style.featureTypeStyles().get(0);
        final Rule rule = featureTypeStyle.rules().get(0);
        final Class<? extends Symbolizer> symbClass = rule.symbolizers().get(0).getClass();
        assertTrue("Expected: " + expectedSymbolizerType.getName() + " but was: " + symbClass,
                   expectedSymbolizerType.isAssignableFrom(symbClass));
    }
}
