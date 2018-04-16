package org.mapfish.print.attribute;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

import static org.junit.Assert.assertNotNull;

public class StyleAttributeTest extends AbstractMapfishSpringTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private TestHttpClientFactory clientHttpRequestFactory;

    @Test
    public void testAttributesFromJson() throws Exception {
        configurationFactory.setDoValidation(false);
        final File configFile = getFile(StyleAttributeTest.class, "style_attributes/config.yaml");
        final Configuration config = configurationFactory.getConfig(configFile);
        final Template template = config.getTemplate("main");
        final PJsonObject pJsonObject = parseJSONObjectFromFile(StyleAttributeTest.class, "style_attributes/request.json");
        final Values values = new Values("test", pJsonObject, template, this.folder.getRoot(),
                this.clientHttpRequestFactory, new File("."));
        final StyleAttribute.StylesAttributeValues value = values.getObject("styleDef", StyleAttribute.StylesAttributeValues.class);

        assertNotNull(value.style);
    }
}
