package org.mapfish.print.attribute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.json.data.JsonDataSource;
import org.junit.jupiter.api.Test;
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
            new HashMap<>(),
            requestData,
            template,
            config.getDirectory(),
            httpClientFactory,
            config.getDirectory(),
            HTTP_REQUEST_MAX_NUMBER_FETCH_RETRY,
            HTTP_REQUEST_FETCH_RETRY_INTERVAL_MILLIS,
            new AtomicBoolean(false));

    assertEquals(
        "s1", getValue(values.getObject("json", JsonDataSource.class), "a.b", String.class));
  }

  private PJsonObject loadJsonRequestData() throws IOException {
    return parseJSONObjectFromFile(
        JsonDatasourceAttributeTest.class, BASE_DIR + "requestData.json");
  }
}
