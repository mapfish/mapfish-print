package org.mapfish.print.attribute;

import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.simple.JSONArray;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

public class StringArrayAttributeTest extends AbstractMapfishSpringTest {

  private static final String BASE_DIR = "stringarray/";

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

    String[] array = (String[]) values.getObject("stringarray", Object.class);

    assertArrayEquals(new String[] {"s1", "s2"}, array);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testWrongType() throws Exception {
    final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
    PJsonObject requestData = loadJsonRequestData();

    final JSONArray intArray = new JSONArray();
    intArray.add(1);
    intArray.add(2);
    intArray.add(3);
    requestData.getJSONObject("attributes").getInternalObj().put("stringarray", intArray);

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

    String[] array = (String[]) values.getObject("stringarray", Object.class);

    assertArrayEquals(new String[] {"1", "2", "3"}, array);
  }

  private PJsonObject loadJsonRequestData() throws IOException {
    return parseJSONObjectFromFile(StringArrayAttributeTest.class, BASE_DIR + "requestData.json");
  }
}
