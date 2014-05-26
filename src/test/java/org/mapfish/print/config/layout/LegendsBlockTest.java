package org.mapfish.print.config.layout;

import com.codahale.metrics.MetricRegistry;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mapfish.print.Constants;
import org.mapfish.print.FakeHttpd;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.ThreadResources;
import org.mapfish.print.config.Config;
import org.mapfish.print.utils.PJsonObject;
import java.net.URL;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for Legends Block class
 *
 * User: Jesse
 * Date: 8/29/13
 * Time: 8:19 AM
 */
public class LegendsBlockTest {
    private FakeHttpd httpd;
    private ThreadResources threadResources;

    @Before
    public void setUp() throws Exception {
        Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.INFO);
        Logger.getLogger("httpclient").setLevel(Level.INFO);

        httpd = new FakeHttpd(
                FakeHttpd.Route.errorResponse("/500", 500, "Server error"),
                FakeHttpd.Route.textResponse("/notImage", "Blahblah")
                );
        httpd.start();
        this.threadResources = new ThreadResources();
        this.threadResources.init();
    }
    @After
    public void tearDown() throws Exception {
        httpd.shutdown();
        this.threadResources.destroy();
    }

    @Test
    public void testBrokenUrl() throws DocumentException, JSONException {

        LegendsBlock legendBlock = new LegendsBlock();
        PJsonObject globalParams = new PJsonObject(new JSONObject("{\n" +
                "    \"layout\": \"A4 portrait\",\n" +
                "    \"title\": \"A simple example\",\n" +
                "    \"srs\": \"EPSG:4326\",\n" +
                "    \"dpi\": 254,\n" +
                "    \"units\": \"degrees\",\n" +
                "    \"outputFormat\": \"png\",\n" +
                "\"legends\" :\n" +
                "      [\n" +
                "         {\n" +
                "            \"classes\" :\n" +
                "               [\n" +
                "                  {\n" +
                "                     \"icons\" :\n" +
                "                        [\n" +
                "                           \""+"http://localhost:" + httpd.getPort() + "/notImage"+"\"\n" +
                "                        ],\n" +
                "                     \"name\" : \"name\",\n" +
                "                     \"iconBeforeName\" : true\n" +
                "                  }\n" +
                "               ],\n" +
                "            \"name\" : \"a class name\"\n" +
                "         }\n" +
                "      ]" +
                "}"), "global");
        PJsonObject params = new PJsonObject(new JSONObject("{\"legends\" :\n" +
                "      [\n" +
                "         {\n" +
                "            \"classes\" :\n" +
                "               [\n" +
                "                  {\n" +
                "                     \"icons\" :\n" +
                "                        [\n" +
                "                           \""+"http://localhost:" + httpd.getPort() + "/notImage"+"\"\n" +
                "                        ],\n" +
                "                     \"name\" : \"name\",\n" +
                "                     \"iconBeforeName\" : true\n" +
                "                  }\n" +
                "               ],\n" +
                "            \"name\" : \"a class name\"\n" +
                "         }\n" +
                "      ]}"), "legend");
        Block.PdfElement target = new Block.PdfElement() {
            @Override
            public void add(Element element) throws DocumentException {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };
        Config config = new Config();
        config.setThreadResources(this.threadResources);
        config.setBrokenUrlPlaceholder(Constants.ImagePlaceHolderConstants.DEFAULT);
        config.setMetricRegistry(new MetricRegistry());

        RenderingContext context = mock(RenderingContext.class);

        PdfContentByte dc = mock(PdfContentByte.class);
        PdfTemplate template = mock(PdfTemplate.class);
        when(dc.createTemplate(anyFloat(), anyFloat())).thenReturn(template);

        when(context.getGlobalParams()).thenReturn(globalParams);
        when(context.getConfig()).thenReturn(config);
        when(context.getPdfLock()).thenReturn(new Object());
        when(context.getDirectContent()).thenReturn(dc);

        legendBlock.render(params, target, context);

        try {
            config.setBrokenUrlPlaceholder(Constants.ImagePlaceHolderConstants.THROW);
            legendBlock.render(params, target, context);
            fail("expected an exception");
        } catch (Exception e) {
            // correct behaviour
        }

        URL placeholder = Constants.class.getClassLoader().getResource(Constants.ImagePlaceHolderConstants
                .DEFAULT_ERROR_IMAGE);
        config.setBrokenUrlPlaceholder(placeholder.toExternalForm());
        legendBlock.render(params, target, context);
    }
}
