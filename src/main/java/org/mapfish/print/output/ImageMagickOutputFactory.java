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
 *  
 * @author Stéphane Brunner
 */
public class ImageMagickOutputFactory implements OutputFormatFactory {
    
	private String imageMagickCommand;

	public ImageMagickOutputFactory() {
		if(java.lang.management.ManagementFactory.getOperatingSystemMXBean().getName().toLowerCase().contains("win")) {
			imageMagickCommand = "convert";
		} else {
			imageMagickCommand = "/usr/bin/convert";
		}
	}
	/**
	 * Set the path and command of the image magic convert command.  
	 * 
	 * Default value is /usr/bin/convert on linux and just convert on windows (assumes it is on the path)
	 *
	 * value is typically injected by spring dependency injection
	 */
	public void setImageMagickCommand(String imageMagickCommand) {
		this.imageMagickCommand = imageMagickCommand;
	}
	
    @Override
    public List<String> formats() {
        return Collections.singletonList("png");
    }

    @Override
    public OutputFormat create(String format) {
        return new ImageOutput(format, imageMagickCommand);
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
		private String imageMagickCmd;

        /**
         * Construct.
         * @param format
         */
        public ImageOutput(String format, String imageMagickCmd) {
            super(format);
            this.imageMagickCmd = imageMagickCmd;
        }

        @Override
        public RenderingContext print(PrintParams params) throws DocumentException {
            // Hack to correct the transparency
            {
                PJsonArray layers = params.jsonSpec.getJSONArray("layers");
                
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
                    context = doPrint(params.withOutput(tmpOut));
                    timeLog.done();
                } finally {
                    tmpOut.close();
                }

                TimeLogger timeLog = TimeLogger.info(LOGGER, "Pdf to image conversion");
                tmpPngFile = File.createTempFile("mapfishprint", ".png");
                createImage(params.jsonSpec, tmpPdfFile, tmpPngFile, context);
                timeLog.done();

                timeLog = TimeLogger.info(LOGGER, "Write Image");
                drawImage(params.outputStream, tmpPngFile);
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
            String cmd = imageMagickCmd+" -density " + dpi + "x" + dpi + " " 
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
