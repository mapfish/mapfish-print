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

import org.json.JSONObject;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/*
 * Test the servlet metrics API.
 * 
 *  To run this test make sure that the test servers are running:
 * 
 *      ./gradlew examples:jettyRun
 *      
 * Or run the tests with the following task (which automatically starts the servers):
 * 
 *      ./gradlew examples:test
 */
public class MetricsApiTest extends AbstractApiTest {

    @Test
    public void testMetrics() throws Exception {
        ClientHttpRequest request = getMetricsRequest("metrics", HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        JSONObject sampleRequest = new JSONObject(getBodyAsText(response));
        assertTrue(sampleRequest.has("version"));
    }

    @Test
    public void testPing() throws Exception {
        ClientHttpRequest request = getMetricsRequest("ping", HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("pong", getBodyAsText(response).trim());
    }

    @Test
    public void testThreads() throws Exception {
        ClientHttpRequest request = getMetricsRequest("threads", HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
        assertFalse(getBodyAsText(response).isEmpty());
    }

    @Test
    public void testHealthcheck() throws Exception {
        ClientHttpRequest request = getMetricsRequest("healthcheck", HttpMethod.GET);
        response = request.execute();
        // TODO not implemented?
        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
//        assertNotNull(new JSONObject(getBodyAsText(response)));
    }

    private ClientHttpRequest getMetricsRequest(String path, HttpMethod method) throws IOException,
            URISyntaxException {
        return getRequest("metrics/" + path, method);
    }

}
