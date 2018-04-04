package org.mapfish.print.processor.jasper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import javax.annotation.Nullable;

import net.sf.jasperreports.engine.data.JRTableModelDataSource;
import net.sf.jasperreports.engine.design.JRDesignField;

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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.annotation.DirtiesContext;

import com.google.common.base.Predicate;
import com.google.common.io.Files;

public class LegendProcessorTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "legend/";
    public static final String BASE_DIR_DYNAMIC = "legend_dynamic/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private ForkJoinPool forkJoinPool;
    @Autowired
    private TestHttpClientFactory httpRequestFactory;


    @Test
    @DirtiesContext
    public void testBasicLegendProperties() throws Exception {
        httpRequestFactory.registerHandler(new Predicate<URI>() {
            @Override
            public boolean apply(@Nullable URI input) {
                return input != null && input.getHost().equals("legend.com");
            }
        }, new TestHttpClientFactory.Handler() {
            @Override
            public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod)  {
                final MockClientHttpRequest request = new MockClientHttpRequest();
                request.setResponse(new MockClientHttpResponse(new byte[0],HttpStatus.OK));
                return request;
            }
        });
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
        final Template template = config.getTemplate("main");
        PJsonObject requestData = loadJsonRequestData();
        Values values = new Values("test", requestData, template, getTaskDirectory(), this.httpRequestFactory, new File("."));
        forkJoinPool.invoke(template.getProcessorGraph().createTask(values));

        final JRTableModelDataSource legend = values.getObject(
                "legendDataSource", JRTableModelDataSource.class);

        int count = 0;
        while (legend.next()) {
            count++;
        }

        assertEquals(9, count);
        assertEquals(9, values.getInteger("numberOfLegendRows").intValue());
    }


    @Test
    @DirtiesContext
    public void testDynamicLegendProperties() throws Exception {
        httpRequestFactory.registerHandler(new Predicate<URI>() {
            @Override
            public boolean apply(@Nullable URI input) {
                return input != null && input.getHost().equals("legend.com");
            }
        }, new TestHttpClientFactory.Handler() {
            @Override
            public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws IOException  {
                try {
                    byte[] bytes = Files.toByteArray(getFile(BASE_DIR_DYNAMIC + uri.getPath()));
                    return ok(uri, bytes, httpMethod);
                } catch (AssertionError e) {
                    return error404(uri, httpMethod);
                }
            }
        });
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR_DYNAMIC + "config.yaml"));
        final Template template = config.getTemplate("main");
        PJsonObject requestData = loadDynamicJsonRequestData();
        Values values = new Values("test", requestData, template, getTaskDirectory(), this.httpRequestFactory, new File("."));
        forkJoinPool.invoke(template.getProcessorGraph().createTask(values));

        final JRTableModelDataSource legend = values.getObject(
                "legendDataSource", JRTableModelDataSource.class);

        JRDesignField iconField = new JRDesignField();
        iconField.setName("icon");
        JRDesignField reportField = new JRDesignField();
        reportField.setName("report");

        List<Object> icons = new ArrayList<Object>();
        List<Object> reports = new ArrayList<Object>();
        int count = 0;
        while (legend.next()) {
            icons.add(legend.getFieldValue(iconField));
            reports.add(legend.getFieldValue(reportField));
            count++;
        }

        assertEquals(7, count);
        assertEquals(7, values.getInteger("numberOfLegendRows").intValue());

        assertTrue(icons.get(2) instanceof BufferedImage);
        assertTrue(icons.get(4) instanceof BufferedImage);
        assertTrue(icons.get(6) instanceof BufferedImage);

        assertNotNull(reports.get(2));
        assertNotNull(reports.get(4));
        assertNotNull(reports.get(6));
    }

    @Test
    @DirtiesContext
    public void testEmptyLegend() throws Exception {
        httpRequestFactory.registerHandler(new Predicate<URI>() {
            @Override
            public boolean apply(@Nullable URI input) {
                return input != null && input.getHost().equals("legend.com");
            }
        }, new TestHttpClientFactory.Handler() {
            @Override
            public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod)  {
                final MockClientHttpRequest request = new MockClientHttpRequest();
                request.setResponse(new MockClientHttpResponse(new byte[0],HttpStatus.OK));
                return request;
            }
        });
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
        final Template template = config.getTemplate("main");
        PJsonObject requestData = loadJsonRequestData();
        requestData.getJSONObject("attributes").getJSONObject("legend").getInternalObj().put("classes", new JSONArray());

        Values values = new Values("test", requestData, template, getTaskDirectory(), this.httpRequestFactory, new File("."));
        forkJoinPool.invoke(template.getProcessorGraph().createTask(values));

        final JRTableModelDataSource legend = values.getObject(
                "legendDataSource", JRTableModelDataSource.class);

        int count = 0;
        while (legend.next()) {
            count++;
        }

        assertEquals(0, count);
        assertEquals(0, values.getInteger("numberOfLegendRows").intValue());
    }

    private static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(LegendProcessorTest.class, BASE_DIR + "requestData.json");
    }

    private static PJsonObject loadDynamicJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(LegendProcessorTest.class, BASE_DIR_DYNAMIC + "requestData.json");
    }
}
