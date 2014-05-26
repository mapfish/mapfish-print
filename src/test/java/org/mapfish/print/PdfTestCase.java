/*
 * Copyright (C) 2013  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print;

import com.codahale.metrics.MetricRegistry;
import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import org.junit.Before;
import org.mapfish.print.config.Config;
import org.mapfish.print.config.layout.Block;
import org.mapfish.print.config.layout.Layout;
import org.mapfish.print.config.layout.MainPage;
import org.mapfish.print.config.layout.MapBlock;
import org.mapfish.print.utils.PJsonObject;
import org.pvalsecc.misc.FileUtilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeSet;

public abstract class PdfTestCase extends PrintTestCase {
    private static final int MARGIN = 40;

    protected Document doc;
    protected RenderingContext context;
    private PdfWriter writer;
    private OutputStream outFile;
    protected ThreadResources threadResources;

    @SuppressWarnings("deprecation")
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        this.threadResources = new ThreadResources();
        this.threadResources.init();

        PJsonObject spec = MapPrinter.parseSpec(FileUtilities.readWholeTextFile(new File("samples/spec.json")));
        spec.getInternalObj().put("units", "meters");

        doc = new Document(PageSize.A4);

        //This test expects to be able to write files into the same directory the classes
        //are compiled to, in this case the build/classes/test directory
        String expectedPath = "build" + File.separator + "classes" + File.separator + "test";
        String baseDir = PdfTestCase.class.getClassLoader().getResource(".").getFile();
        if (baseDir.indexOf("pulse-java.jar") != -1) {
            String[] paths = System.getProperty("java.class.path").split(File.pathSeparator);

            for (String path : paths) {
                if (path.indexOf(expectedPath) != -1) {
                    baseDir = path;
                }
            }
        }
        outFile = new FileOutputStream(baseDir + getClass().getSimpleName() + "_" + name.getMethodName() + ".pdf");
        writer = PdfWriter.getInstance(doc, outFile);
        writer.setFullCompression();
        createContext(spec);
        doc.setMargins(MARGIN, MARGIN, MARGIN, MARGIN * 3);
        doc.open();
        doc.newPage();
        final Paragraph title = new Paragraph("Test class=" + getClass().getName() + " method=" + name.getMethodName());
        title.setSpacingAfter(20);
        doc.add(title);
    }

    @Override
    public void tearDown() throws Exception {
        doc.close();
        writer.close();
        outFile.close();
        this.threadResources.destroy();

        super.tearDown();
    }

    private void createContext(PJsonObject spec) {
        Layout layout = new Layout();
        MainPage mainPage = new MainPage();
        final MapBlock mapBlock = new MapBlock();
        mainPage.setItems(new ArrayList<Block>(Arrays.asList(mapBlock)));
        layout.setMainPage(mainPage);
        Config config = new Config();
        config.setThreadResources(this.threadResources);
        config.setMetricRegistry(new MetricRegistry());
        try {
            config.setDpis(new TreeSet<Integer>(Arrays.asList(96, 190, 254)));
            config.setScales(new TreeSet<Number>(Arrays.asList(20000.0, 25000.0, 100000.0, 500000.0, 4000000.0)));
            context = new RenderingContext(doc, writer, config, spec, null, layout, Collections.<String, String>emptyMap());
        } finally {
            config.close();
        }
    }
}
