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

import static org.junit.Assert.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.lowagie.text.DocumentException;
import org.junit.Test;

public class PDFUtilsTest extends PdfTestCase {
    public static final Logger LOGGER = Logger.getLogger(PDFUtilsTest.class);
    private FakeHttpd httpd;
    private final Random random = new Random();
    private int port;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        port = 8000 + random.nextInt(15000);
        Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.INFO);
        Logger.getLogger("httpclient").setLevel(Level.INFO);

        Map<String, FakeHttpd.HttpAnswerer> routings = new HashMap<String, FakeHttpd.HttpAnswerer>();
        routings.put("/500", new FakeHttpd.HttpAnswerer(500, "Server error", "text/plain", "Server error"));
        routings.put("/notImage", new FakeHttpd.HttpAnswerer(200, "OK", "text/plain", "Blahblah"));
        httpd = new FakeHttpd(port, routings);
        httpd.start();
    }

    @Override
    public void tearDown() throws Exception {
        httpd.shutdown();
        super.tearDown();
    }

    @Test
    public void testGetImageDirectWMSError() throws URISyntaxException, IOException, DocumentException {
        URI uri = new URI("http://localhost:" + port + "/notImage");
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
        URI uri = new URI("http://localhost:" + port + "/500");
        try {
            doc.newPage();
            PDFUtils.getImageDirect(context, uri);
            fail("Supposed to have thrown an IOException");
        } catch (IOException ex) {
            //expected
            assertEquals("Error (status=500) while reading the image from " + uri + ": Server error", ex.getMessage());
        }
    }

    @Test
    public void testPlaceholder() throws URISyntaxException, IOException, DocumentException {
        URI uri = new URI("http://localhost:" + port + "/500");
        try {
            doc.newPage();
            PDFUtils.getImageDirect(context, uri);
            fail("Supposed to have thrown an IOException");
        } catch (IOException ex) {
            //expected
            assertEquals("Error (status=500) while reading the image from " + uri + ": Server error", ex.getMessage());
        }
    }


}
