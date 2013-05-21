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

import java.awt.image.BufferedImage;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFImageWriter;

/**
 * An example of using the PagePanel class to show PDFs. For more advanced
 * usage including navigation and zooming, look ad the 
 * com.sun.pdfview.PDFViewer class.
 *
 * @author joshua.marinacci@sun.com
 */
public class Main {


    public static void main(final String[] args) throws IOException {
        String color = "rgba";
        PDDocument document = null;
            try
            {
                document = PDDocument.load( "/tmp/print-out.pdf" );


                int imageType = 24;
                if ("bilevel".equalsIgnoreCase(color))
                {
                    imageType = BufferedImage.TYPE_BYTE_BINARY;
                }
                else if ("indexed".equalsIgnoreCase(color))
                {
                    imageType = BufferedImage.TYPE_BYTE_INDEXED;
                }
                else if ("gray".equalsIgnoreCase(color))
                {
                    imageType = BufferedImage.TYPE_BYTE_GRAY;
                }
                else if ("rgb".equalsIgnoreCase(color))
                {
                    imageType = BufferedImage.TYPE_INT_RGB;
                }
                else if ("rgba".equalsIgnoreCase(color))
                {
                    imageType = BufferedImage.TYPE_INT_ARGB;
                }
                else
                {
                    System.err.println( "Error: the number of bits per pixel must be 1, 8 or 24." );
                    System.exit( 2 );
                }

                //Make the call
                PDFImageWriter imageWriter = new PDFImageWriter();
                boolean success = imageWriter.writeImage(document, "png", "",
                        1, 3, "/tmp/img--", imageType, 56);
                if (!success)
                {
                    System.err.println( "Error: no writer found for image format '"
                            + "png" + "'" );
                    System.exit(1);
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