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

package org.mapfish.print.processor.http;

import org.json.JSONException;
import org.json.JSONObject;
import org.mapfish.print.attribute.HttpHeadersAttribute;
import org.mapfish.print.output.Values;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.test.context.ContextConfiguration;

import java.net.URI;
import javax.annotation.Nullable;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@ContextConfiguration(locations = {
        "classpath:org/mapfish/print/processor/http/forward-headers/add-custom-processor-application-context.xml"
})
public class ForwardHeadersProcessorTest extends AbstractHttpProcessorTest {

    @Override
    protected String baseDir() {
        return "forward-headers";
    }

    @Override
    protected Class<TestProcessor> testProcessorClass() {
        return TestProcessor.class;
    }

    @Override
    protected Class<? extends HttpProcessor> classUnderTest() {
        return ForwardHeadersProcessor.class;
    }

    @Override
    protected void addExtraValues(Values values) throws JSONException {
        HttpHeadersAttribute.Value headers = new HttpHeadersAttribute.Value();
        JSONObject inner = new JSONObject("{\"header1\": [\"header1-v1\",\"header1-v2\"], \"header2\": [\"header2-value\"]}");
        headers.headers = new PJsonObject(inner, "headers");
        values.put("httpHeaders", headers);
    }

    public static class TestProcessor extends AbstractTestProcessor {
        @Nullable
        @Override
        public Void execute(TestParam values, ExecutionContext context) throws Exception {
            final URI uri = new URI("http://localhost:8080/path?query#fragment");
            final ClientHttpRequest request = values.clientHttpRequestFactory.createRequest(uri, HttpMethod.GET);
            final URI finalUri = request.getURI();

            assertEquals("http", finalUri.getScheme());
            assertEquals("localhost", finalUri.getHost());
            assertEquals("/path", finalUri.getPath());
            assertEquals(8080, finalUri.getPort());
            assertEquals("query", finalUri.getQuery());
            assertEquals("fragment", finalUri.getFragment());

            assertEquals(2, request.getHeaders().size());
            assertArrayEquals(new Object[]{"header1-v1", "header1-v2"}, request.getHeaders().get("header1").toArray());
            assertArrayEquals(new Object[]{"header2-value"}, request.getHeaders().get("header2").toArray());
            return null;
        }
    }
}