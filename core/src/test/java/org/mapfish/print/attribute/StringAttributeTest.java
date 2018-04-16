package org.mapfish.print.attribute;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

public class StringAttributeTest extends AbstractMapfishSpringTest {

    private static final String BASE_DIR = "string/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private TestHttpClientFactory httpClientFactory;

    @Test
    public void testParsableByValues() throws Exception {
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
        PJsonObject requestData = loadJsonRequestData();

        Template template = config.getTemplate("main");
        Values values = new Values("test", requestData, template, config.getDirectory(), httpClientFactory, config.getDirectory());

        assertEquals("a loooooooooooooooooooong text", values.getString("field1"));
        assertEquals("a short text", values.getString("field2"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testParsableByValuesError() throws Exception {
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
        PJsonObject requestData = loadJsonRequestDataError();

        Template template = config.getTemplate("main");
        new Values("test", requestData, template, config.getDirectory(), httpClientFactory, config.getDirectory());
    }

    private PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(StringArrayAttributeTest.class, BASE_DIR + "requestData.json");
    }

    private PJsonObject loadJsonRequestDataError() throws IOException {
        return parseJSONObjectFromFile(StringArrayAttributeTest.class, BASE_DIR + "requestDataError.json");
    }
}
