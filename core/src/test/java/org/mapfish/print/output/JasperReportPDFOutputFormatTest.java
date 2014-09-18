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

import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.PDFConfig;
import org.mapfish.print.config.Template;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class JasperReportPDFOutputFormatTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "pdf-config/";
    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private Map<String, OutputFormat> outputFormat;

    @Test
    public void testPdfConfigDefaults() throws Exception {
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config-defaults.yaml"));
        final Template rawTemplate = config.getTemplate("main");
        final PDFConfig pdfConfigSpy = Mockito.spy(rawTemplate.getPdfConfig());
        Template templateSpy = Mockito.spy(rawTemplate);
        Mockito.when(templateSpy.getPdfConfig()).thenReturn(pdfConfigSpy);

        final Map<String, Template> templates = config.getTemplates();
        templates.put("main", templateSpy);
        config.setTemplates(templates);

        PJsonObject requestData = loadJsonRequestData();
        OutputStream outputStream = new ByteArrayOutputStream();
        OutputFormat format = this.outputFormat.get("pdfOutputFormat");
        format.print(requestData, config,
                getFile(JasperReportPDFOutputFormatTest.class, BASE_DIR), getTaskDirectory(),
                outputStream);
        assertAllMethodsCalled(pdfConfigSpy);
        assertEquals(false, pdfConfigSpy.isCompressed());
        assertEquals("Mapfish Print", pdfConfigSpy.getAuthor());
        assertEquals("Mapfish Print", pdfConfigSpy.getCreator());
        assertEquals("Mapfish Print", pdfConfigSpy.getKeywordsAsString());
        assertEquals("Mapfish Print", pdfConfigSpy.getTitle());
        assertEquals("Mapfish Print", pdfConfigSpy.getSubject());
    }

    @Test
    public void testPdfConfigValuesFromConfig() throws Exception {
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config-settings-in-config.yaml"));
        final Template rawTemplate = config.getTemplate("main");
        final PDFConfig pdfConfigSpy = Mockito.spy(rawTemplate.getPdfConfig());
        Template templateSpy = Mockito.spy(rawTemplate);
        Mockito.when(templateSpy.getPdfConfig()).thenReturn(pdfConfigSpy);

        final Map<String, Template> templates = config.getTemplates();
        templates.put("main", templateSpy);
        config.setTemplates(templates);

        PJsonObject requestData = loadJsonRequestData();
        OutputStream outputStream = new ByteArrayOutputStream();
        OutputFormat format = this.outputFormat.get("pdfOutputFormat");
        format.print(requestData, config,
                getFile(JasperReportPDFOutputFormatTest.class, BASE_DIR), getTaskDirectory(),
                outputStream);
        assertAllMethodsCalled(pdfConfigSpy);
        assertEquals(true, pdfConfigSpy.isCompressed());
        assertEquals("Config Author", pdfConfigSpy.getAuthor());
        assertEquals("Config Creator", pdfConfigSpy.getCreator());
        assertEquals("Config Keywords", pdfConfigSpy.getKeywordsAsString());
        assertEquals("Config Title", pdfConfigSpy.getTitle());
        assertEquals("Config Subject", pdfConfigSpy.getSubject());
    }

    @Test
    public void testPdfConfigValuesFromTemplate() throws Exception {
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config-settings-in-template.yaml"));
        final Template rawTemplate = config.getTemplate("main");
        final PDFConfig pdfConfigSpy = Mockito.spy(rawTemplate.getPdfConfig());
        Template templateSpy = Mockito.spy(rawTemplate);
        Mockito.when(templateSpy.getPdfConfig()).thenReturn(pdfConfigSpy);

        final Map<String, Template> templates = config.getTemplates();
        templates.put("main", templateSpy);
        config.setTemplates(templates);

        PJsonObject requestData = loadJsonRequestData();
        OutputStream outputStream = new ByteArrayOutputStream();
        OutputFormat format = this.outputFormat.get("pdfOutputFormat");
        format.print(requestData, config,
                getFile(JasperReportPDFOutputFormatTest.class, BASE_DIR), getTaskDirectory(),
                outputStream);
        assertAllMethodsCalled(pdfConfigSpy);
        assertEquals(true, pdfConfigSpy.isCompressed());
        assertEquals("Template Author", pdfConfigSpy.getAuthor());
        assertEquals("Template Creator", pdfConfigSpy.getCreator());
        assertEquals("Template Keywords", pdfConfigSpy.getKeywordsAsString());
        assertEquals("Template Title", pdfConfigSpy.getTitle());
        assertEquals("Template Subject", pdfConfigSpy.getSubject());
    }

    @Test
    public void testPdfConfigValuesInTemplateOverrideValuesInConfig() throws Exception {
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config-template-override-config.yaml"));
        final Template rawTemplate = config.getTemplate("main");
        final PDFConfig pdfConfigSpy = Mockito.spy(rawTemplate.getPdfConfig());
        Template templateSpy = Mockito.spy(rawTemplate);
        Mockito.when(templateSpy.getPdfConfig()).thenReturn(pdfConfigSpy);

        final Map<String, Template> templates = config.getTemplates();
        templates.put("main", templateSpy);
        config.setTemplates(templates);

        PJsonObject requestData = loadJsonRequestData();
        OutputStream outputStream = new ByteArrayOutputStream();
        OutputFormat format = this.outputFormat.get("pdfOutputFormat");
        format.print(requestData, config,
                getFile(JasperReportPDFOutputFormatTest.class, BASE_DIR), getTaskDirectory(),
                outputStream);
        assertAllMethodsCalled(pdfConfigSpy);
        assertEquals(false, pdfConfigSpy.isCompressed());
        assertEquals("Template Author", pdfConfigSpy.getAuthor());
        assertEquals("Config Creator", pdfConfigSpy.getCreator());
        assertEquals("Config Keywords", pdfConfigSpy.getKeywordsAsString());
        assertEquals("Template Title", pdfConfigSpy.getTitle());
        assertEquals("Config Subject", pdfConfigSpy.getSubject());
    }

    private void assertAllMethodsCalled(PDFConfig pdfConfigSpy) {
        Mockito.verify(pdfConfigSpy).isCompressed();
        Mockito.verify(pdfConfigSpy).getAuthor();
        Mockito.verify(pdfConfigSpy).getCreator();
        Mockito.verify(pdfConfigSpy).getKeywordsAsString();
        Mockito.verify(pdfConfigSpy).getTitle();
        Mockito.verify(pdfConfigSpy).getSubject();
    }

    public static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(JasperReportOutputFormatSimpleMapTest.class, BASE_DIR + "requestData.json");
    }
}