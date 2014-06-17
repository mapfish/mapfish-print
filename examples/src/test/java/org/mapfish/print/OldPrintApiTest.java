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

package org.mapfish.print;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;

import org.json.JSONObject;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;

/**
 * Test the old print API.
 * 
 *  To run this test make sure that the test servers are running:
 * 
 *      ./gradlew examples:jettyRun
 *      
 * Or run the tests with the following task (which automatically starts the servers):
 * 
 *      ./gradlew examples:test
 */
public class OldPrintApiTest extends AbstractApiTest {
    
    @Test
    public void testInfo() throws Exception {
        ClientHttpRequest request = getPrintRequest("info.json", HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(getJsonMediaType(), response.getHeaders().getContentType());
        
        JSONObject info = new JSONObject(getBodyAsText(response));
        assertTrue(info.has("scales"));
        assertTrue(info.has("dpis"));
        assertTrue(info.has("outputFormats"));
        assertTrue(info.has("layouts"));
        assertTrue(info.has("printURL"));
        assertTrue(info.has("createURL"));
    }
    
    @Test
    public void testInfoVarAndUrl() throws Exception {
        ClientHttpRequest request = getPrintRequest(
                "info.json?var=printConfig&url=http://demo.mapfish.org/2.2/print/pdf/info.json", HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(getJsonMediaType(), response.getHeaders().getContentType());

        final String result = getBodyAsText(response);
        assertTrue(result.startsWith("var printConfig="));
        assertTrue(result.endsWith(";"));
        
        final JSONObject info = new JSONObject(
                result.replace("var printConfig=", "").replace(";", ""));
        
        assertTrue(info.has("scales"));
        assertEquals("http://demo.mapfish.org/2.2/print/pdf/print.pdf", info.getString("printURL"));
        assertEquals("http://demo.mapfish.org/2.2/print/pdf/create.json", info.getString("createURL"));
    }

    protected ClientHttpRequest getPrintRequest(String path, HttpMethod method) throws IOException,
            URISyntaxException {
        return getRequest("dep-pdf/" + path, method);
    }

}
