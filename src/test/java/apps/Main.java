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

package apps;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * An example of using the PagePanel class to show PDFs. For more advanced
 * usage including navigation and zooming, look ad the
 * com.sun.pdfview.PDFViewer class.
 *
 * @author joshua.marinacci@sun.com
 */
public class Main {


    public static void main(final String[] args) throws IOException {
        PDDocument document = null;
            try
            {
                document = PDDocument.load( new File("/tmp/print-out.pdf"));

                ImageType imageType = ImageType.ARGB;

                PDFRenderer pdfRenderer = new PDFRenderer(document);
                int pageCounter = 0;
                while (pageCounter < 3)
                {
                    BufferedImage bim = pdfRenderer.renderImageWithDPI(pageCounter, 56, imageType);
                    ImageIOUtil.writeImage(bim, "/tmp/img--" + (pageCounter++) + ".png", 56);
                }
            }
            catch (Exception e)
            {
                System.err.println(e);
            }
            finally
            {
                if( document != null )
                {
                    document.close();
                }
            }
        }
}