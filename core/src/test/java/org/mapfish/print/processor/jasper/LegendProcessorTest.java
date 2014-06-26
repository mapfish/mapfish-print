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

import jsr166y.ForkJoinPool;
import net.sf.jasperreports.engine.data.JRTableModelDataSource;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.parser.MapfishParser;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.io.IOException;

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
    private ClientHttpRequestFactory httpRequestFactory;


    @Test
    public void testBasicLegendProperties() throws Exception {
        // register "legends:" protocol
        System.setProperty("java.protocol.handler.pkgs", "org.mapfish.print.processor.jasper");
        
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
        final Template template = config.getTemplate("main");
        PJsonObject requestData = loadJsonRequestData();
        Values values = new Values(requestData, template, parser, getTaskDirectory(), this.httpRequestFactory);
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
