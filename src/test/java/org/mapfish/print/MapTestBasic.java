package org.mapfish.print;


import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;
import junit.framework.TestCase;
import org.apache.log4j.*;
import org.junit.runners.model.FrameworkMethod;
import org.mapfish.print.config.Config;
import org.mapfish.print.config.HostMatcher;
import org.mapfish.print.config.layout.Block;
import org.mapfish.print.config.layout.Layout;
import org.mapfish.print.config.layout.MainPage;
import org.mapfish.print.config.layout.MapBlock;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;

public abstract class MapTestBasic extends TestCase {

    private static final int MARGIN = 40;

    private final Logger logger = Logger.getLogger(MapTestBasic.class);

    protected Document doc;
    protected RenderingContext context;
    private PdfWriter writer;
    private OutputStream outFile;

    public MapTestBasic(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        BasicConfigurator.configure(new ConsoleAppender(
                new PatternLayout("%d{HH:mm:ss.SSS} [%t] %-5p %30.30c - %m%n")));
        Logger.getRootLogger().setLevel(Level.DEBUG);
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
            context = new RenderingContext(doc, writer, config, null, null, layout, Collections.<String, String>emptyMap());
        } finally {
            config.close();
        }
    }

    protected void tearDown() throws Exception {
        BasicConfigurator.resetConfiguration();

        writer.close();
        outFile.close();

        context = null;

        super.tearDown();
    }

    public void succeeded(FrameworkMethod method) {
        logger.info("Test " + method.getName() + "() succeeded.");
    }

    public void failed(Throwable e, FrameworkMethod method) {
        logger.error("Test " + method.getName() + "() failed. " + e.getMessage(), e);
    }

    private String getBaseDir() {
        //This test expects to be able to write files into the same directory the classes
        //are compiled to, in this case the build/classes/test directory
        String expectedPath = "build" + File.separator + "classes" + File.separator + "test";
        String baseDir = MapTestBasic.class.getClassLoader().getResource(".").getFile();
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
