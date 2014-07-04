/*
 * Copyright (C) 2014  Camptocamp
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

package org.mapfish.print.processor.jasper;

import com.google.common.collect.Lists;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JRDesignImage;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.util.JRLoader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.awt.Dimension;
import java.io.File;
import java.net.URI;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MapSubReportTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testCompile() throws Exception {
        File layer0Tiff = new File("/tmp/mfp/3/layer_0.tiff").getAbsoluteFile();
        File layer1SVG = new File("/tmp/mfp/3/layer_1.svg").getAbsoluteFile();
        File layer2Tiff = new File("/tmp/mfp/3/layer_2.tiff").getAbsoluteFile();
        File layer3SVG = new File("/tmp/mfp/3/layer_3.svg").getAbsoluteFile();
        List<URI> layerImages = Lists.newArrayList(
                layer0Tiff.toURI(),
                layer1SVG.toURI(),
                layer2Tiff.toURI(),
                layer3SVG.toURI());

        MapSubReport subReport = new MapSubReport(layerImages, new Dimension(400, 500), 72);

        JasperDesign report = subReport.getReportDesign();

        assertEquals(400, report.getPageWidth());
        assertEquals(500, report.getPageHeight());

        assertEquals(4, report.getNoData().getChildren().size());

        JRDesignImage image0 = (JRDesignImage) report.getNoData().getChildren().get(0);
        assertEquals(400, image0.getWidth());
        assertEquals(500, image0.getHeight());
        assertEquals('"' + layer0Tiff.getPath().replace('\\', '/') + '"', image0.getExpression().getText());

        JRDesignImage image3 = (JRDesignImage) report.getNoData().getChildren().get(3);
        assertEquals(400, image3.getWidth());
        assertEquals(500, image3.getHeight());
        assertEquals("net.sf.jasperreports.renderers.BatikRenderer.getInstance(new java.io.File(\"" + layer3SVG.getPath().replace('\\',
                        '/') + "\"))",
                image3.getExpression().getText());

        File compiledReportFile = folder.newFile();
        subReport.compile(compiledReportFile);

        JasperReport jasperReport = (JasperReport) JRLoader.loadObject(compiledReportFile);
        assertEquals("report can be loaded from compiled file",
                "map", jasperReport.getName());
    }

}
