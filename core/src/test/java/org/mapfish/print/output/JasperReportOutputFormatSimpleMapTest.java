package org.mapfish.print.output;

import net.sf.jasperreports.engine.JasperPrint;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.test.util.ImageSimilarity;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class JasperReportOutputFormatSimpleMapTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "simple_map/";

    @Autowired
    private ConfigurationFactory configurationFactory;

    @Autowired
    private Map<String, OutputFormat> outputFormat;

    @Test
    public void testPrint() throws Exception {
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
        PJsonObject requestData = loadJsonRequestData();

        final AbstractJasperReportOutputFormat format = (AbstractJasperReportOutputFormat) this.outputFormat.get("pngOutputFormat");
        JasperPrint print = format.getJasperPrint("test", requestData, config,
                getFile(JasperReportOutputFormatSimpleMapTest.class, BASE_DIR), getTaskDirectory()).print;

        // note that we are using a sample size of 50, because the image is quite big.
        // otherwise small differences are not detected!
        new ImageSimilarity(getFile(BASE_DIR + "expectedReport.png"))
                .assertSimilarity(print, 0, 5);
    }

    @Test
    public void testAllOutputFormats() throws Exception {
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
        PJsonObject requestData = loadJsonRequestData();

        for (OutputFormat format : this.outputFormat.values()) {
            OutputStream outputStream = new ByteArrayOutputStream();
            format.print("test", requestData, config,
                    getFile(JasperReportOutputFormatSimpleMapTest.class, BASE_DIR),
                    getTaskDirectory(), outputStream);
            // no error?  its a pass


        }
    }

    public static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(JasperReportOutputFormatSimpleMapTest.class, BASE_DIR + "requestData.json");
    }

}
