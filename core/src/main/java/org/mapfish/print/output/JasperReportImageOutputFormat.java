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
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Stroke;
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
public final class JasperReportImageOutputFormat extends AbstractJasperReportOutputFormat implements OutputFormat {

    private int imageType = BufferedImage.TYPE_INT_ARGB;

    private String fileSuffix;

    @Override
    public String getContentType() {
        return "image/" + this.fileSuffix;
    }

    public void setFileSuffix(final String fileSuffix) {
        this.fileSuffix = fileSuffix;
    }

    @Override
    public String getFileSuffix() {
        return this.fileSuffix;
    }

    @Override
    protected void doExport(final OutputStream outputStream, final Print print) throws JRException, IOException {
        JasperPrint jasperPrint = print.print;
        final int numPages = jasperPrint.getPages().size();

        final float dpiRatio = (float) (print.dpi / print.requestorDpi);
        final int pageHeightOnImage = (int) (jasperPrint.getPageHeight() * dpiRatio);
        final int pageWidthOnImage = (int) (jasperPrint.getPageWidth() * dpiRatio);
        final int separatorHeight = 1;
        final int separatorHeightOnImage = (int) (separatorHeight * dpiRatio);

        BufferedImage reportImage = new BufferedImage(
                pageWidthOnImage, numPages * pageHeightOnImage + (numPages - 1) * separatorHeightOnImage,
                this.imageType);

        Graphics2D graphics2D = reportImage.createGraphics();
        try {
            JasperPrintManager printManager = JasperPrintManager.getInstance(print.context);

            for (int pageIndex = 0; pageIndex < numPages; pageIndex++) {
                Image pageImage = printManager.printToImage(jasperPrint, pageIndex, dpiRatio);

                graphics2D.drawImage(pageImage,
                        0, (pageHeightOnImage + separatorHeight) * pageIndex,
                        pageWidthOnImage, (pageHeightOnImage + separatorHeight) * pageIndex + pageHeightOnImage,
                        0, 0,
                        pageWidthOnImage, pageHeightOnImage, null);
            }

            // draw separator line between the pages
            final Stroke stroke = new BasicStroke(separatorHeightOnImage);
            for (int pageIndex = 0; pageIndex < numPages - 1; pageIndex++) {
                graphics2D.setColor(Color.black);
                graphics2D.setStroke(stroke);
                int y = (pageHeightOnImage + separatorHeight) * pageIndex + pageHeightOnImage;
                graphics2D.drawLine(
                        0, y,
                        pageWidthOnImage, y);
            }
        } finally {
            graphics2D.dispose();
        }

        ImageIO.write(reportImage, getFileSuffix(), outputStream);
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
