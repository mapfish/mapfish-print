package org.mapfish.print.map.readers;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.PrintTestCase;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.ShellMapPrinter;
import org.mapfish.print.config.Config;
import org.mapfish.print.config.HostMatcher;
import org.mapfish.print.config.layout.Block;
import org.mapfish.print.config.layout.Layout;
import org.mapfish.print.config.layout.MainPage;
import org.mapfish.print.config.layout.MapBlock;
import org.mapfish.print.utils.PJsonObject;
import org.pvalsecc.misc.FileUtilities;
import org.pvalsecc.misc.MatchAllSet;
import org.pvalsecc.misc.URIUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class XYZLayerTest extends PrintTestCase {

    private static final int MARGIN = 40;


    private XyzMapReader xyzreader;
    protected Document doc;
    protected RenderingContext context;
    private PdfWriter writer;
    private OutputStream outFile;

    PJsonObject xyzSpec;


    public XYZLayerTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        //  PJsonObject spec = MapPrinter.parseSpec(FileUtilities.readWholeTextFile(new File(XYZLayerTest.class.getClassLoader().getResource("samples/spec.json").getFile())));
        // spec.getInternalObj().put("units", "meters");

        doc = new Document(PageSize.A4);
        String baseDir = getBaseDir();
        outFile = new FileOutputStream(baseDir + getClass().getSimpleName() + "_" + getName() + ".pdf");
        writer = PdfWriter.getInstance(doc, outFile);
        writer.setFullCompression();
        Layout layout = new Layout();
        MainPage mainPage = new MainPage();
        final MapBlock mapBlock = new MapBlock();
        mainPage.setItems(new ArrayList<Block>(Arrays.asList(mapBlock)));
        layout.setMainPage(mainPage);
        Config config = new Config();
        try {
            config.setDpis(new TreeSet<Integer>(Arrays.asList(96, 190, 254)));
            config.setScales(new TreeSet<Integer>(Arrays.asList(20000, 25000, 100000, 500000, 4000000)));
            List<HostMatcher> hosts = new ArrayList<HostMatcher>(1);
            hosts.add(HostMatcher.ACCEPT_ALL);
            config.setHosts(hosts);
            context = new RenderingContext(doc, writer, config, null, null, layout, Collections.<String, String> emptyMap());

            xyzSpec = MapPrinter.parseSpec(FileUtilities.readWholeTextFile(new File(XYZLayerTest.class.getClassLoader()
                    .getResource("layers/layer_spec.json").getFile())));
        } finally {
            config.close();
        }

    }

    protected void tearDown() throws Exception {
        writer.close();
        outFile.close();

        context = null;

        super.tearDown();
    }

    public void testUriWithoutFormat() throws IOException, JSONException, URISyntaxException {
        String test_format = null;
        String expected_url = xyzSpec.getString("baseURL") + "/07/64/63.gif";

        JSONObject xyz_full = xyzSpec.getInternalObj();
        xyz_full.accumulate("path_format", test_format);
        xyzSpec = new PJsonObject(xyz_full, "");

        xyzreader = new XyzMapReader("foo", context, xyzSpec);

        URI outputuri = xyzreader.getTileUri(URIUtils.addParams(xyzreader.baseUrl, new HashMap<String, List<String>>(), new MatchAllSet<String>()), null, -180, -90, 180, 90, 256, 256);
        String url = outputuri.getScheme() +"://"+ outputuri.getHost() + outputuri.getPath();

        assertEquals("Default format (null path_format) did not get created correctly", expected_url, url);
    }

    public void testUriWithFormat() throws IOException, JSONException, URISyntaxException {
        String test_format = "${z}_${x}_${y}_static.${extension}";
        String expected_url = xyzSpec.getString("baseURL") + "/07_64_63_static.gif";


        JSONObject xyz_full = xyzSpec.getInternalObj();
        xyz_full.accumulate("path_format", test_format);
        xyzSpec = new PJsonObject(xyz_full, "");

        xyzreader = new XyzMapReader("foo", context, xyzSpec);

        URI outputuri = xyzreader.getTileUri(URIUtils.addParams(xyzreader.baseUrl, new HashMap<String, List<String>>(), new MatchAllSet<String>()), null, -180, -90, 180, 90, 256, 256);
        String url = outputuri.getScheme() +"://"+ outputuri.getHost() + outputuri.getPath();

        assertEquals("Custom format did not get created correctly", expected_url, url);
    }

    private String getBaseDir() {
        //This test expects to be able to write files into the same directory the classes
        //are compiled to, in this case the build/classes/test directory
        String expectedPath = "build" + File.separator + "classes" + File.separator + "test";
        String baseDir = XYZLayerTest.class.getClassLoader().getResource(".").getFile();
        if (baseDir.indexOf("pulse-java.jar") != -1) {
            String[] paths = System.getProperty("java.class.path").split(File.pathSeparator);

            for (String path : paths) {
                if (path.indexOf(expectedPath) != -1 || path.indexOf("out/test/mapfish-print") != -1) {
                    baseDir = path;
                }
            }
        }
        return baseDir;
    }

}
