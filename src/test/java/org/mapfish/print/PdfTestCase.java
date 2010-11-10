/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Server
 *
 * MapFish Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MapFish Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
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
import java.util.TreeSet;

public abstract class PdfTestCase extends PrintTestCase {
    private static final int MARGIN = 40;

    protected Document doc;
    protected RenderingContext context;
    private PdfWriter writer;
    private OutputStream outFile;

    public PdfTestCase(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        PJsonObject spec = MapPrinter.parseSpec(FileUtilities.readWholeTextFile(new File("samples/spec.json")));
        spec.getInternalObj().put("units", "meters");

        doc = new Document(PageSize.A4);
        String baseDir = PdfTestCase.class.getClassLoader().getResource(".").getFile();
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
        config.setDpis(new TreeSet<Integer>(Arrays.asList(96, 190, 254)));
        config.setScales(new TreeSet<Integer>(Arrays.asList(20000, 25000, 100000, 500000, 4000000)));
        context = new RenderingContext(doc, writer, config, spec, null, layout, null);
    }
}