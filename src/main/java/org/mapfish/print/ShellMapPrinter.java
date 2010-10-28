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

import com.lowagie.text.DocumentException;
import org.apache.log4j.*;
import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.print.output.OutputFactory;
import org.mapfish.print.output.OutputFormat;
import org.mapfish.print.utils.PJsonObject;
import org.pvalsecc.misc.FileUtilities;
import org.pvalsecc.opts.GetOptions;
import org.pvalsecc.opts.InvalidOption;
import org.pvalsecc.opts.Option;

import java.io.*;
import java.nio.charset.Charset;

/**
 * A shell version of the MapPrinter. Can be used for testing or for calling
 * from other languages than Java.
 */
public class ShellMapPrinter {
    public static final Logger LOGGER = Logger.getLogger(ShellMapPrinter.class);

    @Option(desc = "Filename for the configuration (templates&CO)", mandatory = true)
    private String config = null;

    @Option(desc = "The location of the description of what has to be printed. By default, STDIN")
    private String spec = null;

    @Option(desc = "Used only if log4jConfig is not specified. 3 if you want everything, 2 if you want the debug information (stacktraces are shown), 1 for infos and 0 for only warnings and errors")
    private int verbose = 1;

    @Option(desc = "The destination file. By default, STDOUT")
    private String output = null;

    @Option(desc = "Get the config for the client form. Doesn't generate a PDF")
    private boolean clientConfig = false;

    @Option(desc = "Referer address to use when doing queries")
    private String referer = null;

    @Option(desc = "Property file for the log4j configuration")
    private String log4jConfig = null;

    private final MapPrinter printer;

    public ShellMapPrinter(String[] args) throws IOException {
        try {
            GetOptions.parse(args, this);
        } catch (InvalidOption invalidOption) {
            help(invalidOption.getMessage());
        }
        configureLogs();
        printer = new MapPrinter(new File(config));
    }

    @SuppressWarnings({"UseOfSystemOutOrSystemErr"})
    private void help(String message) {
        System.err.println(message);
        System.err.println();
        System.err.println("Usage:");
        System.err.println("  " + getClass().getName() + " " + GetOptions.getShortList(this));
        System.err.println("Params:");
        try {
            System.err.println(GetOptions.getLongList(this));
        } catch (IllegalAccessException e) {
            e.printStackTrace(System.err);
        }
        System.exit(-1);
    }

    public void run() throws IOException, JSONException, DocumentException {
        OutputStream outFile = null;
        try {
            if (clientConfig) {
                outFile = getOutputStream("");
                final OutputStreamWriter writer = new OutputStreamWriter(outFile, Charset.forName("UTF-8"));
                JSONWriter json = new JSONWriter(writer);
                json.object();
                {
                    printer.printClientConfig(json);
                }
                json.endObject();

                writer.close();

            } else {
                final InputStream inFile = getInputStream();
                final PJsonObject jsonSpec = printer.parseSpec(FileUtilities.readWholeTextStream(inFile, "UTF-8"));
                final OutputFormat outputFormat = OutputFactory.create(printer.getConfig(), jsonSpec);
                outFile = getOutputStream(jsonSpec.optString("outputFormat", "pdf"));
                outputFormat.print(printer, jsonSpec, outFile, referer);
            }
        } finally {
            if(outFile != null) outFile.close();
            if(printer != null) printer.stop();
        }
    }



    private void configureLogs() {
        if (log4jConfig != null) {
            PropertyConfigurator.configure(log4jConfig);
        } else {
            final ConsoleAppender appender = new ConsoleAppender(
                    new PatternLayout("%d{HH:mm:ss.SSS} [%t] %-5p %30.30c - %m%n"),
                    "system.err");
            BasicConfigurator.configure(appender);
            final Level level;
            switch (verbose) {
                case 0:
                    level = Level.WARN;
                    break;
                case 1:
                    level = Level.INFO;
                    break;
                case 2:
                    level = Level.DEBUG;
                    Logger.getLogger("httpclient").setLevel(Level.INFO);
                    Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.INFO);
                    Logger.getLogger("org.apache.pdfbox").setLevel(Level.INFO);
                    break;
                default:
                    level = Level.TRACE;
                    break;
            }
            Logger.getRootLogger().setLevel(level);
        }
    }

    private OutputStream getOutputStream(String suffix) throws FileNotFoundException {
        final OutputStream outFile;
        if (output != null) {
            if(!output.endsWith("."+suffix)) {
                output = output + "."+suffix;
            }
            outFile = new FileOutputStream(output);
        } else {
            //noinspection UseOfSystemOutOrSystemErr
            outFile = System.out;
        }
        return outFile;
    }

    private InputStream getInputStream() throws FileNotFoundException {
        final InputStream file;
        if (spec != null) {
            file = new FileInputStream(spec);
        } else {
            file = System.in;
        }
        return file;
    }

    public static void main(String[] args) throws IOException, JSONException, DocumentException {
        ShellMapPrinter app = new ShellMapPrinter(args);
        try {
            app.run();
        } catch (PrintException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error("Cannot generate PDF", e);
            } else {
                LOGGER.error(e.toString());
            }
            System.exit(-2);
        }
    }
}
