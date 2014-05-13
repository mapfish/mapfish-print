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

package org.mapfish.print.output;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRGraphics2DExporter;
import net.sf.jasperreports.engine.export.JRGraphics2DExporterParameter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import javax.imageio.ImageIO;

/**
 * An PDF output format that uses Jasper reports to generate the result.
 *
 * @author Jesse
 * @author sbrunner
 */
public final class JasperReportPNGOutputFormat extends AbstractJasperReportOutputFormat implements OutputFormat {

    private int imageType = BufferedImage.TYPE_INT_ARGB;

    @Override
    public String getContentType() {
        return "image/png";
    }

    @Override
    public String getFileSuffix() {
        return "png";
    }

    @Override
    protected void doExport(final OutputStream outputStream, final JasperPrint print) throws JRException, IOException {
        final int numPages = print.getPages().size();
        final int pageHeightOnImage = print.getPageHeight() + 1;
        final int pageWidthOnImage = print.getPageWidth() + 1;
        BufferedImage pageImage = new BufferedImage(pageWidthOnImage, numPages * pageHeightOnImage, this.imageType);

        Graphics2D graphics2D = pageImage.createGraphics();
        try {
            JRGraphics2DExporter exporter = new JRGraphics2DExporter();
            exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
            exporter.setParameter(JRGraphics2DExporterParameter.GRAPHICS_2D, graphics2D);
            for (int pageIndex = 0; pageIndex < numPages; pageIndex++) {
                exporter.setParameter(JRExporterParameter.PAGE_INDEX, pageIndex);

                exporter.exportReport();
                graphics2D.setColor(Color.black);
                graphics2D.drawLine(0, pageHeightOnImage, pageWidthOnImage, pageHeightOnImage);
                graphics2D.translate(0, pageHeightOnImage);
            }
        } finally {
            graphics2D.dispose();
        }

        ImageIO.write(pageImage, getFileSuffix(), outputStream);
    }

    /**
     * One of {@link java.awt.image.BufferedImage} TYPE_ values.
     *
     * @param imageType the buffered image type to create.
     */
    public void setImageType(final int imageType) {
        this.imageType = imageType;
    }
}
