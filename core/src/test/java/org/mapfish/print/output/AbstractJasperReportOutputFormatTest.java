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

package org.mapfish.print.output;

import com.vividsolutions.jts.util.AssertionFailedException;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AbstractJasperReportOutputFormatTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "simple_map/";
    public static final String TABLE_BASE_DIR = "../processor/jasper/table/";

    @Autowired
    private ConfigurationFactory configurationFactory;

    @Autowired
    private Map<String, OutputFormat> outputFormat;

    @Test
    @DirtiesContext
    public void testParameterValidation_WrongType() throws Exception {
        configurationFactory.setDoValidation(false);
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config-map-wrong-type.yaml"));
        PJsonObject requestData = loadJsonRequestData();

        final AbstractJasperReportOutputFormat format = (AbstractJasperReportOutputFormat) this.outputFormat.get("pngOutputFormat");
        try {
            format.getJasperPrint(requestData, config,
                    getFile(JasperReportOutputFormatSimpleMapTest.class, BASE_DIR), getTaskDirectory());
            fail("Expected a " + AssertionFailedException.class);
        } catch (AssertionFailedException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("does not match the class of the actual object"));
        }
    }

    @Test
    @DirtiesContext
    public void testParameterValidation_MissingParameter() throws Exception {
        configurationFactory.setDoValidation(false);
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config-missing-map.yaml"));
        PJsonObject requestData = loadJsonRequestData();

        final AbstractJasperReportOutputFormat format = (AbstractJasperReportOutputFormat) this.outputFormat.get("pngOutputFormat");
        try {
            format.getJasperPrint(requestData, config,
                    getFile(JasperReportOutputFormatSimpleMapTest.class, BASE_DIR), getTaskDirectory());
            fail("Expected a " + AssertionFailedException.class);
        } catch (AssertionFailedException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("but are not output values of processors or attributes"));
        }
    }

    @Test
    @DirtiesContext
    public void testFieldValidation_WrongType() throws Exception {
        configurationFactory.setDoValidation(false);
        final Configuration config = configurationFactory.getConfig(getFile(TABLE_BASE_DIR + "config.yaml"));
        config.getTemplate("main").setReportTemplate("simpleReport-wrong-field-type.jrxml");
        PJsonObject requestData = loadTableJsonRequestData();

        final AbstractJasperReportOutputFormat format = (AbstractJasperReportOutputFormat) this.outputFormat.get("pngOutputFormat");
        try {
            format.getJasperPrint(requestData, config,
                    getFile(JasperReportOutputFormatSimpleMapTest.class, TABLE_BASE_DIR), getTaskDirectory());
            fail("Expected a " + AssertionFailedException.class);
        } catch (AssertionFailedException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("does not match the class of the actual object"));
        }
    }

    public static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(JasperReportOutputFormatSimpleMapTest.class, BASE_DIR + "requestData.json");
    }
    public static PJsonObject loadTableJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(JasperReportOutputFormatSimpleMapTest.class, TABLE_BASE_DIR + "requestData.json");
    }
}