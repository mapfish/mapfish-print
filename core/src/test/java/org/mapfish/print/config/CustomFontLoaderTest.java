package org.mapfish.print.config;

import static org.junit.Assert.assertTrue;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.util.Map;

import net.sf.jasperreports.engine.JasperPrint;

import org.apache.commons.lang.ArrayUtils;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.output.AbstractJasperReportOutputFormat;
import org.mapfish.print.output.OutputFormat;
import org.mapfish.print.test.util.ImageSimilarity;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

public class CustomFontLoaderTest extends AbstractMapfishSpringTest {

    public static final String BASE_DIR = "font/";

    @Autowired
    private Map<String, OutputFormat> outputFormat;

    @Autowired
    ConfigurationFactory configurationFactory;

    /**
     * Tests that custom font defined in `test-mapfish-spring-custom-fonts.xml` are loaded.
     */
    @Test
    public void testLoadingFontFromConfig() throws Exception {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        assertTrue(ArrayUtils.contains(ge.getAvailableFontFamilyNames(), "Coming Soon"));
    }

    /**
     * Tests that a custom-loaded font can be used.
     */
    public void testPrint() throws Exception {
        final Configuration config = configurationFactory.getConfig(getFile(CustomFontLoaderTest.class, BASE_DIR + "config.yaml"));
        PJsonObject requestData = loadJsonRequestData();

        final AbstractJasperReportOutputFormat format = (AbstractJasperReportOutputFormat) this.outputFormat.get("pngOutputFormat");
        JasperPrint print = format.getJasperPrint("test", requestData, config,
                getFile(CustomFontLoaderTest.class, BASE_DIR), getTaskDirectory()).print;
        ImageSimilarity.exportReportToImage(print, 0);
        // no error, ok
    }

    public static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(CustomFontLoaderTest.class, BASE_DIR + "requestData.json");
    }
}
