package org.mapfish.print.output;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Map;
import org.junit.Test;
import org.locationtech.jts.util.AssertionFailedException;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.ExtraPropertyException;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

public class AbstractJasperReportOutputFormatTest extends AbstractMapfishSpringTest {
  public static final String BASE_DIR = "simple_map/";
  public static final String TABLE_BASE_DIR = "../processor/jasper/table/";

  @Autowired private ConfigurationFactory configurationFactory;

  @Autowired private Map<String, OutputFormat> outputFormat;

  public static PJsonObject loadJsonRequestData() throws IOException {
    return parseJSONObjectFromFile(
        JasperReportOutputFormatSimpleMapTest.class, BASE_DIR + "requestData.json");
  }

  public static PJsonObject loadTableJsonRequestData() throws IOException {
    return parseJSONObjectFromFile(
        JasperReportOutputFormatSimpleMapTest.class, TABLE_BASE_DIR + "requestData.json");
  }

  @Test
  @DirtiesContext
  public void testParameterValidation_WrongType() throws Exception {
    configurationFactory.setDoValidation(false);
    final Configuration config =
        configurationFactory.getConfig(getFile(BASE_DIR + "config-map-wrong-type.yaml"));
    PJsonObject requestData = loadJsonRequestData();

    final AbstractJasperReportOutputFormat format =
        (AbstractJasperReportOutputFormat) this.outputFormat.get("pngOutputFormat");
    try {
      format.getJasperPrint(
          "test",
          requestData,
          config,
          getFile(JasperReportOutputFormatSimpleMapTest.class, BASE_DIR),
          getTaskDirectory());
      fail("Expected a " + AssertionFailedException.class);
    } catch (AssertionFailedException e) {
      assertTrue(
          e.getMessage(), e.getMessage().contains("does not match the class of the actual object"));
    }
  }

  @Test
  @DirtiesContext
  public void testParameterValidation_MissingParameter() throws Exception {
    configurationFactory.setDoValidation(false);
    final Configuration config =
        configurationFactory.getConfig(getFile(BASE_DIR + "config-missing-map.yaml"));
    PJsonObject requestData = loadJsonRequestData();

    final AbstractJasperReportOutputFormat format =
        (AbstractJasperReportOutputFormat) this.outputFormat.get("pngOutputFormat");
    try {
      format.getJasperPrint(
          "test",
          requestData,
          config,
          getFile(JasperReportOutputFormatSimpleMapTest.class, BASE_DIR),
          getTaskDirectory());
      fail("Expected a " + ExtraPropertyException.class);
    } catch (ExtraPropertyException e) {
      assertTrue(
          e.getMessage(),
          e.getMessage().contains("Extra properties found in the request attributes"));
    }
  }

  @Test
  @DirtiesContext
  public void testFieldValidation_WrongType() throws Exception {
    configurationFactory.setDoValidation(false);
    final Configuration config =
        configurationFactory.getConfig(getFile(TABLE_BASE_DIR + "config.yaml"));
    config.getTemplate("main").setReportTemplate("simpleReport-wrong-field-type.jrxml");
    PJsonObject requestData = loadTableJsonRequestData();

    final AbstractJasperReportOutputFormat format =
        (AbstractJasperReportOutputFormat) this.outputFormat.get("pngOutputFormat");
    try {
      format.getJasperPrint(
          "test",
          requestData,
          config,
          getFile(JasperReportOutputFormatSimpleMapTest.class, TABLE_BASE_DIR),
          getTaskDirectory());
      fail("Expected a " + AssertionFailedException.class);
    } catch (AssertionFailedException e) {
      assertTrue(
          e.getMessage(), e.getMessage().contains("does not match the class of the actual object"));
    }
  }
}
