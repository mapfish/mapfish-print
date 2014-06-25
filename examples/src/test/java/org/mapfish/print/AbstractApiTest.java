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

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        ExamplesTest.DEFAULT_SPRING_XML
})
public abstract class AbstractApiTest {
    
    protected static final String PRINT_SERVER = "http://localhost:8080/print-servlet/";
    
    @Autowired
    protected ClientHttpRequestFactory httpRequestFactory;

    protected ClientHttpResponse response;
    
    @After
    public void tearDown() {
        if (response != null) {
            response.close();
        }
        response = null;
    } 

    protected ClientHttpRequest getRequest(String path, HttpMethod method) throws IOException,
            URISyntaxException {
        return httpRequestFactory.createRequest(new URI(PRINT_SERVER + path), method);
    }
    
    protected String getBodyAsText(ClientHttpResponse response) throws IOException {
        return IOUtils.toString(response.getBody(), "UTF-8");
    }

    protected String getPrintSpec(String file) throws IOException {
        return Resources.toString(Resources.getResource(file), Charsets.UTF_8);
    }

    protected void setPrintSpec(String printSpec, ClientHttpRequest request) throws IOException {
        request.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        OutputStreamWriter writer = new OutputStreamWriter(request.getBody());
        writer.write(URLEncoder.encode(printSpec, Constants.DEFAULT_ENCODING));
        writer.flush();
    }

    protected MediaType getJsonMediaType() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("charset", "utf-8");
        return new MediaType("application", "json", params);
    }

    protected MediaType getJavaScriptMediaType() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("charset", "utf-8");
        return new MediaType("application", "javascript", params);
    }
}
