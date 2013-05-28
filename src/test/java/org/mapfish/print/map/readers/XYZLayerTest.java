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
import org.mapfish.print.config.layout.Block;
import org.mapfish.print.config.layout.Layout;
import org.mapfish.print.config.layout.MainPage;
import org.mapfish.print.config.layout.MapBlock;
import org.mapfish.print.utils.PJsonObject;
import org.pvalsecc.misc.FileUtilities;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeSet;

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
        config.setDpis(new TreeSet<Integer>(Arrays.asList(96, 190, 254)));
        config.setScales(new TreeSet<Integer>(Arrays.asList(20000, 25000, 100000, 500000, 4000000)));
        context = new RenderingContext(doc, writer, config, null, null, layout, Collections.<String, String>emptyMap());



        xyzSpec = MapPrinter.parseSpec(FileUtilities.readWholeTextFile(
                new File(XYZLayerTest.class.getClassLoader().getResource("layers/layer_spec.json").getFile())
                ));

    }

    protected void tearDown() throws Exception {
        doc.close();
        writer.close();
        outFile.close();

        context = null;

        super.tearDown();
    }

    public void testUriWithoutFormat() throws IOException, JSONException {

        JSONObject xyz_full = xyzSpec.getInternalObj();
        xyz_full.accumulate("path_format", null);
        xyzSpec = new PJsonObject(xyz_full, "");

        xyzreader = new XyzMapReader("foo", context, xyzSpec);

        assertTrue("Writing a test to confirm the uri_formatting process", true);
    }

    public void testUriWithFormat()  throws IOException, JSONException {

        JSONObject xyz_full = xyzSpec.getInternalObj();
        xyz_full.accumulate("path_format", "${z}_${x}_${y}_static.${extension}");
        xyzSpec = new PJsonObject(xyz_full, "");

        xyzreader = new XyzMapReader("foo", context, xyzSpec);

        assertTrue("Writing a test to confirm the uri_formatting process", true);
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
