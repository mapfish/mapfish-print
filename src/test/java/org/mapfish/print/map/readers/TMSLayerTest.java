package org.mapfish.print.map.readers;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
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
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;

import static org.junit.Assert.assertNotNull;

public class TMSLayerTest extends PrintTestCase {

    private static final int MARGIN = 40;


    private TmsMapReader tmsreader;
    protected Document doc;
    protected RenderingContext context;
    private PdfWriter writer;
    private OutputStream outFile;

    PJsonObject tmsSpec;


    public TMSLayerTest(String name) {
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

            tmsSpec = MapPrinter.parseSpec(FileUtilities.readWholeTextFile(new File(TMSLayerTest.class.getClassLoader()
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

	public void testNoOrigin() throws JSONException{

        JSONObject tms_full = tmsSpec.getInternalObj();
        tms_full.accumulate("tileOrigin", null);
        tms_full.accumulate("origin", null);
        tmsSpec = new PJsonObject(tms_full, "");

        tmsreader = new TmsMapReader("foo", context, tmsSpec);

        assertEquals("Origin X is not initiated by default as expected",0.0f,tmsreader.tileCacheLayerInfo.originX);
        assertEquals("Origin Y is not initiated by default as expected",0.0f,tmsreader.tileCacheLayerInfo.originY);
    }

    public void testOriginXY() throws JSONException{
        JSONObject tms_full = tmsSpec.getInternalObj();
        Map<String, Float> origin = new HashMap<String, Float>();
        origin.put("x",-10.0f);
        origin.put("y",-20.0f);

        tms_full.accumulate("tileOrigin", null);
        tms_full.accumulate("origin", new JSONObject(origin));
        tmsSpec = new PJsonObject(tms_full, "");

        tmsreader = new TmsMapReader("foo", context, tmsSpec);

        assertEquals("Origin X is not set (via origin.x) as expected",-10.0f,tmsreader.tileCacheLayerInfo.originX);
        assertEquals("Origin Y is not set (via origin.y) as expected",-20.0f,tmsreader.tileCacheLayerInfo.originY);
    }

    public void testTileOriginXY() throws JSONException{
        JSONObject tms_full = tmsSpec.getInternalObj();
        Map<String, Float> origin = new HashMap<String, Float>();
        origin.put("x",-10.0f);
        origin.put("y",-20.0f);

        tms_full.accumulate("tileOrigin", new JSONObject(origin));
        tms_full.accumulate("origin", null);
        tmsSpec = new PJsonObject(tms_full, "");

        tmsreader = new TmsMapReader("foo", context, tmsSpec);

        assertEquals("Origin X is not set (via tileOrigin.x) as expected",-10.0f,tmsreader.tileCacheLayerInfo.originX);
        assertEquals("Origin Y is not set (via tileOrigin.y) as expected",-20.0f,tmsreader.tileCacheLayerInfo.originY);
    }

    public void testOriginLatLon() throws JSONException{
        JSONObject tms_full = tmsSpec.getInternalObj();
        Map<String, Float> origin = new HashMap<String, Float>();
        origin.put("lat",-20.0f);
        origin.put("lon",-10.0f);

        tms_full.accumulate("tileOrigin", new JSONObject(origin));
        tms_full.accumulate("origin", null);
        tmsSpec = new PJsonObject(tms_full, "");

        tmsreader = new TmsMapReader("foo", context, tmsSpec);

        assertEquals("Origin X is not set (via tileOrigin.lon) as expected",-10.0f,tmsreader.tileCacheLayerInfo.originX);
        assertEquals("Origin Y is not set (via tileOrigin.lat) as expected",-20.0f,tmsreader.tileCacheLayerInfo.originY);

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
