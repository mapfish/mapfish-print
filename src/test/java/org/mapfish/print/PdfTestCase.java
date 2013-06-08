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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.Collections;

import org.mapfish.print.config.Config;
import org.mapfish.print.config.layout.Block;
import org.mapfish.print.config.layout.Layout;
import org.mapfish.print.config.layout.MainPage;
import org.mapfish.print.config.layout.MapBlock;
import org.mapfish.print.utils.PJsonObject;
import org.pvalsecc.misc.FileUtilities;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

public abstract class PdfTestCase extends PrintTestCase {
    private static final int MARGIN = 40;

    protected Document doc;
    protected RenderingContext context;
    private PdfWriter writer;
    private OutputStream outFile;

    public PdfTestCase(String name) {
        super(name);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        PJsonObject spec = MapPrinter.parseSpec(FileUtilities.readWholeTextFile(new File("samples/spec.json")));
        spec.getInternalObj().put("units", "meters");

        doc = new Document(PageSize.A4);

        //This test expects to be able to write files into the same directory the classes
        //are compiled to, in this case the build/classes/test directory
        String expectedPath = "build"+File.separator + "classes" + File.separator + "test";
        String baseDir = PdfTestCase.class.getClassLoader().getResource(".").getFile();
        if(baseDir.indexOf("pulse-java.jar") != -1){
            String[] paths = System.getProperty("java.class.path").split(File.pathSeparator);

            for(String path : paths){
               if(path.indexOf(expectedPath) != -1){
                   baseDir = path;
               }
           }
        }
        outFile = new FileOutputStream(baseDir + getClass().getSimpleName() + "_" + getName() + ".pdf");
        writer = PdfWriter.getInstance(doc, outFile);
        writer.setFullCompression();
        createContext(spec);
        doc.setMargins(MARGIN, MARGIN, MARGIN, MARGIN * 3);
        doc.open();
        doc.newPage();
        final Paragraph title = new Paragraph("Test class=" + getClass().getName() + " method=" + getName());
        title.setSpacingAfter(20);
        doc.add(title);
    }

    @Override
    protected void tearDown() throws Exception {
        doc.close();
        writer.close();
        outFile.close();

        super.tearDown();
    }

    private void createContext(PJsonObject spec) {
        Layout layout = new Layout();
        MainPage mainPage = new MainPage();
        final MapBlock mapBlock = new MapBlock();
        mainPage.setItems(new ArrayList<Block>(Arrays.asList(mapBlock)));
        layout.setMainPage(mainPage);
        Config config = new Config();
        try {
        config.setDpis(new TreeSet<Integer>(Arrays.asList(96, 190, 254)));
        config.setScales(new TreeSet<Integer>(Arrays.asList(20000, 25000, 100000, 500000, 4000000)));
        context = new RenderingContext(doc, writer, config, spec, null, layout, Collections.<String, String>emptyMap());
        } finally {
            config.close();
        }
    }
}
