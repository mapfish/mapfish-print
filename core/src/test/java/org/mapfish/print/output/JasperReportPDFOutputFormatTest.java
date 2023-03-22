package org.mapfish.print.output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.PDFConfig;
import org.mapfish.print.config.Template;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

public class JasperReportPDFOutputFormatTest extends AbstractMapfishSpringTest {
  public static final String BASE_DIR = "pdf-config/";
  @Autowired private ConfigurationFactory configurationFactory;
  @Autowired private Map<String, OutputFormat> outputFormat;

  public static PJsonObject loadJsonRequestData() throws IOException {
    return parseJSONObjectFromFile(
        JasperReportOutputFormatSimpleMapTest.class, BASE_DIR + "requestData.json");
  }

  @Test
  public void testPdfConfigDefaults() throws Exception {
    final Configuration config =
        configurationFactory.getConfig(getFile(BASE_DIR + "config-defaults.yaml"));
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
    format.print(
        "test",
        requestData,
        config,
        getFile(JasperReportPDFOutputFormatTest.class, BASE_DIR),
        getTaskDirectory(),
        outputStream);
    assertAllMethodsCalled(pdfConfigSpy);
    assertFalse(pdfConfigSpy.isCompressed());
    assertEquals("Mapfish Print", pdfConfigSpy.getAuthor());
    assertEquals("Mapfish Print", pdfConfigSpy.getCreator());
    assertEquals("Mapfish Print", pdfConfigSpy.getKeywordsAsString());
    assertEquals("Mapfish Print", pdfConfigSpy.getTitle());
    assertEquals("Mapfish Print", pdfConfigSpy.getSubject());
  }

  @Test
  public void testPdfConfigValuesFromConfig() throws Exception {
    final Configuration config =
        configurationFactory.getConfig(getFile(BASE_DIR + "config-settings-in-config.yaml"));
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
    format.print(
        "test",
        requestData,
        config,
        getFile(JasperReportPDFOutputFormatTest.class, BASE_DIR),
        getTaskDirectory(),
        outputStream);
    assertAllMethodsCalled(pdfConfigSpy);
    assertTrue(pdfConfigSpy.isCompressed());
    assertEquals("Config Author", pdfConfigSpy.getAuthor());
    assertEquals("Config Creator", pdfConfigSpy.getCreator());
    assertEquals("Config Keywords", pdfConfigSpy.getKeywordsAsString());
    assertEquals("Config Title", pdfConfigSpy.getTitle());
    assertEquals("Config Subject", pdfConfigSpy.getSubject());
  }

  @Test
  public void testPdfConfigValuesFromTemplate() throws Exception {
    final Configuration config =
        configurationFactory.getConfig(getFile(BASE_DIR + "config-settings-in-template.yaml"));
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
    format.print(
        "test",
        requestData,
        config,
        getFile(JasperReportPDFOutputFormatTest.class, BASE_DIR),
        getTaskDirectory(),
        outputStream);
    assertAllMethodsCalled(pdfConfigSpy);
    assertTrue(pdfConfigSpy.isCompressed());
    assertEquals("Template Author", pdfConfigSpy.getAuthor());
    assertEquals("Template Creator", pdfConfigSpy.getCreator());
    assertEquals("Template Keywords", pdfConfigSpy.getKeywordsAsString());
    assertEquals("Template Title", pdfConfigSpy.getTitle());
    assertEquals("Template Subject", pdfConfigSpy.getSubject());
  }

  @Test
  public void testPdfConfigValuesInTemplateOverrideValuesInConfig() throws Exception {
    final Configuration config =
        configurationFactory.getConfig(getFile(BASE_DIR + "config-template-override-config.yaml"));
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
    format.print(
        "test",
        requestData,
        config,
        getFile(JasperReportPDFOutputFormatTest.class, BASE_DIR),
        getTaskDirectory(),
        outputStream);
    assertAllMethodsCalled(pdfConfigSpy);
    assertFalse(pdfConfigSpy.isCompressed());
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
}
