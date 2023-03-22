package org.mapfish.print.attribute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JsonDataSource;
import net.sf.jasperreports.engine.design.JRDesignField;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

public class JsonDatasourceAttributeTest extends AbstractMapfishSpringTest {

  private static final String BASE_DIR = "jsonDatasource/";

  @Autowired private ConfigurationFactory configurationFactory;
  @Autowired private TestHttpClientFactory httpClientFactory;

  @SuppressWarnings("unchecked")
  private <T> T getValue(
      final JsonDataSource datasource, final String expression, final Class<T> type)
      throws JRException {
    assertNotNull(datasource);
    JRDesignField field = new JRDesignField();
    field.setName(expression);
    field.setValueClass(type);
    return (T) datasource.getFieldValue(field);
  }

  @Test
  public void testParsableByValues() throws Exception {
    final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
    PJsonObject requestData = loadJsonRequestData();

    Template template = config.getTemplate("main");
    Values values =
        new Values(
            "test",
            requestData,
            template,
            config.getDirectory(),
            httpClientFactory,
            config.getDirectory());

    assertEquals(
        "s1", getValue(values.getObject("json", JsonDataSource.class), "a.b", String.class));
  }

  private PJsonObject loadJsonRequestData() throws IOException {
    return parseJSONObjectFromFile(
        JsonDatasourceAttributeTest.class, BASE_DIR + "requestData.json");
  }
}
