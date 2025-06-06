package org.mapfish.print.attribute;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

public class BooleanAttributeTest extends AbstractMapfishSpringTest {

  private static final String BASE_DIR = "bool/";

  @Autowired private ConfigurationFactory configurationFactory;
  @Autowired private TestHttpClientFactory httpClientFactory;

  @Test
  public void testParsableByValues() throws Exception {
    final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
    PJsonObject requestData = loadJsonRequestData();

    Template template = config.getTemplate("main");
    Values values =
        new Values(
            new HashMap<String, String>(),
            requestData,
            template,
            config.getDirectory(),
            httpClientFactory,
            config.getDirectory(),
            HTTP_REQUEST_MAX_NUMBER_FETCH_RETRY,
            HTTP_REQUEST_FETCH_RETRY_INTERVAL_MILLIS,
            new AtomicBoolean(false));

    assertTrue(values.getBoolean("field1"));
    assertFalse(values.getBoolean("field2"));
    assertFalse(values.getBoolean("field3"));

    JSONObject field2Config =
        AbstractAttributeTest.getClientConfig(template.getAttributes().get("field2"), template);
    assertFalse(field2Config.has(ReflectiveAttribute.JSON_ATTRIBUTE_DEFAULT));

    JSONObject field3Config =
        AbstractAttributeTest.getClientConfig(template.getAttributes().get("field3"), template);
    assertTrue(field3Config.has(ReflectiveAttribute.JSON_ATTRIBUTE_DEFAULT));
    assertFalse(field3Config.getBoolean(ReflectiveAttribute.JSON_ATTRIBUTE_DEFAULT));
  }

  private PJsonObject loadJsonRequestData() throws IOException {
    return parseJSONObjectFromFile(BooleanAttributeTest.class, BASE_DIR + "requestData.json");
  }
}
