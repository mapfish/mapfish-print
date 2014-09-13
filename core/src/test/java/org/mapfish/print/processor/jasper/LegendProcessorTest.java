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

package org.mapfish.print.processor.jasper;

import com.google.common.base.Predicate;
import jsr166y.ForkJoinPool;
import net.sf.jasperreports.engine.data.JRTableModelDataSource;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.parser.MapfishParser;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.annotation.DirtiesContext;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;

/**
 * @author Jesse on 4/10/2014.
 */
public class LegendProcessorTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "legend/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private MapfishParser parser;
    @Autowired
    private ForkJoinPool forkJoinPool;
    @Autowired
    private TestHttpClientFactory httpRequestFactory;


    @Test
    @DirtiesContext
    public void testBasicLegendProperties() throws Exception {
        httpRequestFactory.registerHandler(new Predicate<URI>() {
            @Override
            public boolean apply(@Nullable URI input) {
                return input != null && input.getHost().equals("legend.com");
            }
        }, new TestHttpClientFactory.Handler() {
            @Override
            public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod)  {
                final MockClientHttpRequest request = new MockClientHttpRequest();
                request.setResponse(new MockClientHttpResponse(new byte[0],HttpStatus.OK));
                return request;
            }
        });
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
        final Template template = config.getTemplate("main");
        PJsonObject requestData = loadJsonRequestData();
        Values values = new Values(requestData, template, parser, getTaskDirectory(), this.httpRequestFactory, new File("."));
        forkJoinPool.invoke(template.getProcessorGraph().createTask(values));

        final JRTableModelDataSource legend = values.getObject("legend", JRTableModelDataSource.class);

        int count = 0;
        while (legend.next()) {
            count++;
        }

        assertEquals(9, count);
    }

    private static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(LegendProcessorTest.class, BASE_DIR + "requestData.json");
    }
}
