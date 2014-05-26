/*
 * Copyright (C) 2013  Camptocamp
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

package org.mapfish.print;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.Test;
import org.mapfish.print.config.Config;
import org.mapfish.print.config.ConfigFactory;
import org.mapfish.print.config.ConfigTest;
import org.mapfish.print.utils.PJsonObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PDFUtilsTest extends PdfTestCase {
    private static final String FIVE_HUNDRED_ROUTE = "/500";
    private static final String NOT_IMAGE_ROUTE = "/notImage";
    private FakeHttpd httpd;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.INFO);
        Logger.getLogger("httpclient").setLevel(Level.INFO);

        httpd = new FakeHttpd(
                FakeHttpd.Route.errorResponse(FIVE_HUNDRED_ROUTE, 500, "Server error"),
                FakeHttpd.Route.textResponse(NOT_IMAGE_ROUTE, "Blahblah")
                );
        httpd.start();

    }

    @Override
    public void tearDown() throws Exception {
        httpd.shutdown();
        super.tearDown();
    }

    @Test
    public void testGetImageDirectWMSError() throws URISyntaxException, IOException, DocumentException {
        URI uri = new URI("http://localhost:" + httpd.getPort() + NOT_IMAGE_ROUTE);
        try {
            doc.newPage();
            PDFUtils.getImageDirect(context, uri);
            fail("Supposed to have thrown an IOException");
        } catch (IOException ex) {
            //expected
            assertEquals("Didn't receive an image while reading: " + uri, ex.getMessage());
        }
    }

    @Test
    public void testGetImageDirectHTTPError() throws URISyntaxException, IOException, DocumentException {
        URI uri = new URI("http://localhost:" + httpd.getPort() + FIVE_HUNDRED_ROUTE);
        try {
            doc.newPage();
            PDFUtils.getImageDirect(context, uri);
            fail("Supposed to have thrown an IOException");
        } catch (IOException ex) {
            //expected
            assertEquals("Error (status=500) while reading the image from " + uri + ": Internal Server Error", ex.getMessage());
        }
    }

    @Test
    public void testPlaceholder() throws URISyntaxException, IOException, DocumentException {
        URI uri = new URI("http://localhost:" + httpd.getPort() + FIVE_HUNDRED_ROUTE);
        try {
            doc.newPage();
            PDFUtils.getImageDirect(context, uri);
            fail("Supposed to have thrown an IOException");
        } catch (IOException ex) {
            //expected
            assertEquals("Error (status=500) while reading the image from " + uri + ": Internal Server Error", ex.getMessage());
        }
    }

    @Test
    public void testRenderString_Scale() throws Exception {
        final File file = ConfigTest.getSampleConfigFiles().get(ConfigTest.GEORCHESTRA_YAML);
        Config config = new ConfigFactory(this.threadResources).fromYaml(file);
        context = new RenderingContext(doc, context.getWriter(), config, context.getGlobalParams(), file.getParent(),
                context.getLayout(), context.getHeaders());
        JSONObject internal = new JSONObject();
        internal.accumulate("scaleLbl", "Scale Label");
        internal.append("bbox", "-10");
        internal.append("bbox", "-10");
        internal.append("bbox", "10");
        internal.append("bbox", "10");
        PJsonObject params = new PJsonObject(internal, "params");
        Font font = new Font();
        context.getLayout().getMainPage().getMap(null).setWidth("300");
        context.getLayout().getMainPage().getMap(null).setHeight("600");
        PDFUtils.renderString(context, params, "${scaleLbl}1:${format %,d scale}", font, null);

    }
    
    @Test
    public void testRenderString_ScaleForMultipleMaps() throws Exception {
        final File file = ConfigTest.getSampleConfigFiles().get("configMultipleMaps.yaml");
        Config config = new ConfigFactory(this.threadResources).fromYaml(file);
        context = new RenderingContext(doc, context.getWriter(), config, context.getGlobalParams(), file.getParent(),
                config.getLayout("A4 portrait"), context.getHeaders());
        JSONObject internal = new JSONObject();
        
        
        
        internal.accumulate("scaleLbl", "Scale Label");
        
        JSONObject mainMap = new JSONObject();
        mainMap.append("bbox", "-10");
        mainMap.append("bbox", "-10");
        mainMap.append("bbox", "10");
        mainMap.append("bbox", "10");
        
        JSONObject otherMap = new JSONObject();
        otherMap.append("bbox", "-10000");
        otherMap.append("bbox", "-10000");
        otherMap.append("bbox", "10000");
        otherMap.append("bbox", "10000");
        
        JSONObject maps = new JSONObject();
        maps.accumulate("main", mainMap);
        maps.accumulate("other", otherMap);
        
        internal.accumulate("maps", maps);
        
        
        PJsonObject params = new PJsonObject(internal, "params");
        Font font = new Font();
        
        context.getLayout().getMainPage().getMap("main").setWidth("300");
        context.getLayout().getMainPage().getMap("main").setHeight("600");
        context.getLayout().getMainPage().getMap("other").setWidth("300");
        context.getLayout().getMainPage().getMap("other").setHeight("600");
        
        assertTrue(PDFUtils.renderString(context, params, "${scaleLbl}1:${format %,d scale.main}", font, null).getContent().contains("1:25"));
        assertTrue(PDFUtils.renderString(context, params, "${scaleLbl}1:${format %,d scale.other}", font, null).getContent().contains("1:200"));
    }
}
