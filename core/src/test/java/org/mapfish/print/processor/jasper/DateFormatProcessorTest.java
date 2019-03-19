package org.mapfish.print.processor.jasper;

import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.ForkJoinPool;

import static org.junit.Assert.assertEquals;

public class DateFormatProcessorTest extends AbstractMapfishSpringTest {
    private final static String DIR = "date-format";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private ForkJoinPool forkJoinPool;
    @Autowired
    private TestHttpClientFactory httpRequestFactory;

    @Test
    public void testBasic() throws IOException {
        final PJsonObject requestData = parseJSONObjectFromFile(DateFormatProcessorTest.class, DIR +
                "/requestData.json");
        final Configuration config = configurationFactory.getConfig(getFile(DIR + "/config.yaml"));
        final Template template = config.getTemplate("main");
        Values values = new Values("test", requestData, template, getTaskDirectory(), this.httpRequestFactory,
                                   new File("."));
        forkJoinPool.invoke(template.getProcessorGraph().createTask(values));

        final DateFormat dateFormat = values.getObject("dateFormat", DateFormat.class);
        Calendar gmt = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        gmt.set(2018, Calendar.MARCH, 18, 9, 15, 12);
        assertEquals("dimanche 18 mars 2018 02:15 -0700", dateFormat.format(gmt.getTime()));
    }
}
