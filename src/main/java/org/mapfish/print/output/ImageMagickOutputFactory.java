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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
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
    
	private String imageMagickCmd;
	private List<String> imageMagickArgs = new ArrayList<String>();
	private List<String> formats = new ArrayList<String>();

	public ImageMagickOutputFactory() {
		
		if(java.lang.management.ManagementFactory.getOperatingSystemMXBean().getName().toLowerCase().contains("win")) {
			imageMagickCmd = "convert";
		} else {
			imageMagickCmd = "/usr/bin/convert";
		}
		
		imageMagickArgs.add("-density");
		imageMagickArgs.add("${dpi}x${dpi}");
		imageMagickArgs.add("-append");
		imageMagickArgs.add("${sourceFile}");
		imageMagickArgs.add("${targetFile}");
		
		formats.add("jpg");
		formats.add("gif");
		formats.add("png");
		formats.add("bmp");
		formats.add("tif");
		formats.add("tiff");
	}
	/**
	 * Set the path and command of the image magic convert command.  
	 * 
	 * Default value is /usr/bin/convert on linux and just convert on windows (assumes it is on the path)
	 * 
	 * value is typically injected by spring dependency injection
	 */
	public void setImageMagickCmd(String imageMagickCmd) {
		this.imageMagickCmd = imageMagickCmd;
	}
	/**
	 * Set arguments when executing the imageMagickCmd.  
	 * 
	 * Default parameters are "-density", "${dpi}x${dpi}", "${sourceFile}" and "${targetFile}"
	 * 
	 * value is typically injected by spring dependency injection
	 */
	public void setImageMagickArgs(List<String> imageMagickArgs) {
		this.imageMagickArgs = imageMagickArgs;
	}
	public void setFormats(List<String> formats) {
		this.formats = formats;
	}
	
    @Override
    public List<String> formats() {
        return formats;
    }

    @Override
    public OutputFormat create(String format) {
        return new ImageOutput(format, imageMagickCmd, imageMagickArgs);
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
		private List<String> imageMagickArgs;
		private String cmd;
        /**
         * Construct.
         * @param format
         */
        public ImageOutput(String format, String cmd, List<String> imageMagickArgs) {
            super(format);
            this.cmd = cmd;
            this.imageMagickArgs = imageMagickArgs;
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
            FileInputStream inputStream = new FileInputStream(tmpPngFile);
			FileChannel channel = inputStream.getChannel();
            try {
	            channel.transferTo(0, tmpPngFile.length(), Channels.newChannel(out));
            } finally {
            	closeQuiet(channel);
            	closeQuiet(inputStream);
            }
        }

        private void closeQuiet(Closeable c) {
        	try {
        		if(c != null) c.close();
        	} catch(Throwable e) {
        		LOGGER.error("Error closing resource", e);
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
            
            String[] finalCommands = new String[imageMagickArgs.size()+1];
            finalCommands[0] = cmd;
//            FileChannel in = new FileInputStream(tmpPdfFile).getChannel();
//            FileChannel out = new FileOutputStream(new File("p.pdf")).getChannel();
//            out.transferFrom(in, 0, tmpPdfFile.length());
//            in.close();
//            out.close();
            
            for (int i = 1; i < finalCommands.length; i++) {
				String arg = imageMagickArgs.get(i-1)
						.replace("${dpi}", ""+dpi)
						.replace("${targetFile}", tmpPngFile.getAbsolutePath())
						.replace("${sourceFile}", tmpPdfFile.getAbsolutePath());
				
				finalCommands[i] = arg;
			}
            
            ProcessBuilder builder = new ProcessBuilder(finalCommands);
            LOGGER.info("Executing process: " + builder.command());
            
            Process p = builder.start();
            
            writeOut(p, false);
            writeOut(p, true);
            try {
                int exitCode = p.waitFor();
                
                p.destroy();
                if(exitCode != 0) {
                	LOGGER.error("Image magick failed to create image from pdf.  Exit code was "+exitCode);
                } else {
                	LOGGER.info("Image magick exited correctly from image conversion process.  Exit code was "+exitCode);
                }
            } catch (InterruptedException e) {
                LOGGER.error("Process interrupted", e);
            }
        }

		private void writeOut(Process p, boolean errorStream) throws IOException {
			InputStream stream;
			if(errorStream) {
				stream = p.getErrorStream();
			} else {
				stream = p.getInputStream();
			}
			BufferedReader reader = new BufferedReader (new InputStreamReader(stream));
            String line = null;
            
            while((line = reader.readLine()) != null) {
            	if(errorStream) {
            		LOGGER.error(line);
            	} else {
            		LOGGER.info(line);
            	}
            }
		}
    }

    
}
