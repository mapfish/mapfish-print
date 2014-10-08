/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.config;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointPlacement;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.junit.After;
import org.junit.Test;
import org.mapfish.print.Constants;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for {@link org.mapfish.print.config.Configuration} class.
 *
 * @author Jesse on 3/27/14.
 */
public class ConfigurationTest {

    @After
    public void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testGetTemplate() throws Exception {

        final Configuration configuration = new Configuration();
        Map<String, Template> templates = Maps.newHashMap();
        final Template t1Template = new Template();
        templates.put("t1", t1Template);
        configuration.setTemplates(templates);
        assertEquals(t1Template, configuration.getTemplate("t1"));
        assertEquals(1, configuration.getTemplates().size());
        assertEquals(t1Template, configuration.getTemplates().values().iterator().next());

        try {
            configuration.getTemplate("Doesn't exist");
            fail("Exception should have been thrown");
        } catch (Exception e) {
            // good
        }
    }

    @Test
    public void testGetDefaultStyle_IsPresentInMap() throws Exception {
        final Configuration configuration = new Configuration();
        Map<String, Style> styles = new HashMap<String, Style>();
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
    public void testGetDefaultStyle_NotInMap() throws Exception {
        final Configuration configuration = new Configuration();
        Map<String, Style> styles = new HashMap<String, Style>();
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
    public void testGetDefaultStyle_GeomNotInMap() throws Exception {
        final Configuration configuration = new Configuration();

        assertStyleType(Symbolizer.class, configuration.getDefaultStyle("geom"));
        assertStyleType(Symbolizer.class, configuration.getDefaultStyle("geometry"));
        assertStyleType(Symbolizer.class, configuration.getDefaultStyle("geometryCollection"));
        assertStyleType(Symbolizer.class, configuration.getDefaultStyle("MultiGeometry"));
        assertStyleType(Symbolizer.class, configuration.getDefaultStyle(Constants.Style.Grid.NAME));
        assertStyleType(Symbolizer.class, configuration.getDefaultStyle(Constants.Style.Raster.NAME));
    }

    @Test
    public void testGridStyle() throws Exception {
        final Configuration configuration = new Configuration();
        final Style gridStyle = configuration.getDefaultStyle(Constants.Style.Grid.NAME);
        final AtomicInteger foundLineSymb = new AtomicInteger(0);
        final AtomicInteger foundTextSymb = new AtomicInteger(0);

        final AbstractStyleVisitor styleValidator = new AbstractStyleVisitor() {
            @Override
            public void visit(LineSymbolizer line) {
                foundLineSymb.incrementAndGet();
                super.visit(line);
            }

            @Override
            public void visit(TextSymbolizer text) {
                foundTextSymb.incrementAndGet();
                final PointPlacement labelPlacement = (PointPlacement) text.getLabelPlacement();
                assertNotNull(labelPlacement.getDisplacement());
                super.visit(text);
            }
        };

        styleValidator.visit(gridStyle);

        assertEquals(1, foundLineSymb.intValue());
        assertEquals(1, foundTextSymb.intValue());
    }

    @Test
    public void testTemplateAccess() throws Exception {
        Configuration configuration = new Configuration();
        Map<String, Template> templates = Maps.newHashMap();
        Template unrestricted = new Template();
        templates.put("unrestricted", unrestricted);
        Template restricted = new Template();
        restricted.setAccess(Lists.newArrayList("ROLE_USER", "ROLE_ADMIN"));
        templates.put("restricted", restricted);
        configuration.setTemplates(templates);

        assertCorrectTemplates(configuration, unrestricted, restricted, AuthenticationCredentialsNotFoundException.class);

        SimpleGrantedAuthority userAuth = new SimpleGrantedAuthority("ROLE_USER");
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(securityContext);
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("user", "", Lists.newArrayList(userAuth)));
        assertCorrectTemplates(configuration, unrestricted, restricted, null);

        securityContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(securityContext);
        assertCorrectTemplates(configuration, unrestricted, restricted, AuthenticationCredentialsNotFoundException.class);

        SimpleGrantedAuthority adminAuth = new SimpleGrantedAuthority("ROLE_ADMIN");
        securityContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(securityContext);
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("admin", "", Lists.newArrayList(adminAuth)));
        assertCorrectTemplates(configuration, unrestricted, restricted, null);

        SecurityContextHolder.clearContext();
        assertCorrectTemplates(configuration, unrestricted, restricted, AuthenticationCredentialsNotFoundException.class);


        SimpleGrantedAuthority otherAuth = new SimpleGrantedAuthority("ROLE_OTHER");
        securityContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(securityContext);
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("other", "", Lists.newArrayList(otherAuth)));
        assertCorrectTemplates(configuration, unrestricted, restricted, AccessDeniedException.class);
    }
    @Test
    public void testTemplateAccess_ConfigHasAccess() throws Exception {
        Configuration configuration = new Configuration();
        Map<String, Template> templates = Maps.newHashMap();
        Template template1 = new Template();
        templates.put("template1", template1);
        Template template2 = new Template();
        templates.put("template2", template2);
        configuration.setTemplates(templates);
        configuration.setAccess(Lists.newArrayList("ROLE_USER", "ROLE_ADMIN"));

        assertAccessTemplate_ConfigSecured(configuration, AuthenticationCredentialsNotFoundException.class);

        SimpleGrantedAuthority userAuth = new SimpleGrantedAuthority("ROLE_USER");
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(securityContext);
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("user", "", Lists.newArrayList(userAuth)));
        assertAccessTemplate_ConfigSecured(configuration, null);

        securityContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(securityContext);
        assertAccessTemplate_ConfigSecured(configuration, AuthenticationCredentialsNotFoundException.class);

        SimpleGrantedAuthority adminAuth = new SimpleGrantedAuthority("ROLE_ADMIN");
        securityContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(securityContext);
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("admin", "", Lists.newArrayList(adminAuth)));
        assertAccessTemplate_ConfigSecured(configuration, null);

        SecurityContextHolder.clearContext();
        assertAccessTemplate_ConfigSecured(configuration, AuthenticationCredentialsNotFoundException.class);


        SimpleGrantedAuthority otherAuth = new SimpleGrantedAuthority("ROLE_OTHER");
        securityContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(securityContext);
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("other", "", Lists.newArrayList(otherAuth)));
        assertAccessTemplate_ConfigSecured(configuration, AccessDeniedException.class);
    }

    private void assertAccessTemplate_ConfigSecured(Configuration configuration,
                                                    @Nullable Class<?> expectedException) throws Exception {
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

    private void assertCorrectTemplates(Configuration configuration,
                                        Template unrestricted,
                                        Template restricted,
                                        @Nullable Class<? extends Exception> expectedException) throws Exception {
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
    public void testRenderAsSvg() throws Exception {
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

    private void assertStyleType(Class<?> expectedSymbolizerType, Style style) {
        assertNotNull(style);
        final FeatureTypeStyle featureTypeStyle = style.featureTypeStyles().get(0);
        final Rule rule = featureTypeStyle.rules().get(0);
        final Class<? extends Symbolizer> symbClass = rule.symbolizers().get(0).getClass();
        assertTrue("Expected: " + expectedSymbolizerType.getName() + " but was: " + symbClass,
                expectedSymbolizerType.isAssignableFrom(symbClass));
    }
}
