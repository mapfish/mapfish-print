/*
 * Copyright (C) 2009  Camptocamp
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.TimeLogger;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;

import com.lowagie.text.DocumentException;

/**
 * Print Output that generate a PNG. It will first generate a PDF ant convert it to PNG
 * using the convert command provide by ImageMagick.
 * 
 * It include an hack to correct the transparency layer opacity.
 * 
 * To use it the system property USE_IMAGEMAGICK should be set to true.
 *  
 * @author Stéphane Brunner
 */
public class ImageOutputImageMagickFactory implements OutputFormatFactory {
    
    @Override
    public List<String> formats() {
        return Collections.singletonList("png");
    }

    @Override
    public OutputFormat create(String format) {
        return new ImageOutput(format);
    }

    @Override
    public String enablementStatus() {
        return null;
    }

    public static class ImageOutput extends AbstractImageFormat {

        /**
         * The logger.
         */
        public static final Logger LOGGER = Logger.getLogger(ImageOutput.class);

        /**
         * Construct.
         * @param format
         */
        public ImageOutput(String format) {
            super(format);
        }

        @Override
        public RenderingContext print(MapPrinter printer, PJsonObject jsonSpec, OutputStream out, String referer) throws DocumentException {
            // Hack to correct the transparency
            {
                PJsonArray layers = jsonSpec.getJSONArray("layers");
                
                // a*x²+b*x+c
                final double a = -0.3;
                final double b = 0.9;
                final double c = 0.4;
                
                for (int i = 0 ; i < layers.size() ; i++) {
                    PJsonObject layer = layers.getJSONObject(i);
                    if (layer.has("opacity")) {
                        double opacity = layer.getDouble("opacity");
                        opacity = a*opacity*opacity+b*opacity+c;
                        try {
                            layer.getInternalObj().put("opacity", opacity);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            
            File tmpPdfFile = null;
            File tmpPngFile = null;
            try {
                tmpPdfFile = File.createTempFile("mapfishprint", ".pdf");
                FileOutputStream tmpOut = new FileOutputStream(tmpPdfFile);
                RenderingContext context;
                try {
                    TimeLogger timeLog = TimeLogger.info(LOGGER, "PDF Creation");
                    context = printer.print(jsonSpec, tmpOut, referer);
                    timeLog.done();
                } finally {
                    tmpOut.close();
                }

                TimeLogger timeLog = TimeLogger.info(LOGGER, "Pdf to image conversion");
                tmpPngFile = File.createTempFile("mapfishprint", ".png");
                createImage(jsonSpec, tmpPdfFile, tmpPngFile, context);
                timeLog.done();

                timeLog = TimeLogger.info(LOGGER, "Write Image");
                drawImage(out, tmpPngFile);
                timeLog.done();

                return context;
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (tmpPdfFile != null) {
                    if(!tmpPdfFile.delete()) {
                        LOGGER.warn(tmpPdfFile+" was not able to be deleted for unknown reason.  Will try again on shutdown");
                    }
                    tmpPdfFile.deleteOnExit();
                }
                if (tmpPngFile != null) {
                    if(!tmpPngFile.delete()) {
                        LOGGER.warn(tmpPngFile+" was not able to be deleted for unknown reason.  Will try again on shutdown");
                    }
                    tmpPngFile.deleteOnExit();
                }
            }
        }

        /**
         * Write the image from the temporary file to the output stream.
         * @param out the output stream
         * @param tmpPngFile the temporary file
         * @throws IOException on IO error
         */
        private void drawImage(OutputStream out, File tmpPngFile) throws IOException {
            FileInputStream input = new FileInputStream(tmpPngFile);
            byte[] buffer = new byte[1024*10]; // Adjust if you want
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1)
            {
                out.write(buffer, 0, bytesRead);
            }
        }

        /**
         * Creates a PNG image from a PDF file using the convert command provide by Image Magick 
         * @param jsonSpec the spec used to know the DPI value
         * @param tmpPdfFile the PDF file
         * @param tmpPngFile the PNG file
         * @param context the context used to know the DPI value
         * @throws IOException on IO error
         */
        private void createImage(PJsonObject jsonSpec, File tmpPdfFile, File tmpPngFile, RenderingContext context) throws IOException {
            int dpi = calculateDPI(context, jsonSpec);
            String cmd = "/usr/bin/convert -density " + dpi + "x" + dpi + " " 
                    + tmpPdfFile.getAbsolutePath() + " " + tmpPngFile.getAbsolutePath();
            LOGGER.info("Run: " + cmd);
            Process p = Runtime.getRuntime().exec(cmd);
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
