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

package org.mapfish.print.cli;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

import com.google.common.io.CharStreams;
import com.sampullara.cli.Args;

import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.json.PJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mapfish.print.cli.CliDefinition.*;

/**
 * A shell version of the MapPrinter. Can be used for testing or for calling
 * from other languages than Java.
 */
public class Main {
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static final String DEFAULT_SPRING_CONTEXT = "mapfish-spring-application-context.xml";

    @Autowired
    private MapPrinter mapPrinter;


    public static void main(String[] args) throws IOException, InterruptedException, JSONException {
        try {
            List<String> unusedArguments = Args.parse(Main.class, args);

            if (!unusedArguments.isEmpty()) {
                System.out.println("The following arguments are not recognized: " + unusedArguments);
                printUsage();
                return;
            }
        } catch (IllegalArgumentException invalidOption) {
            printUsage();
            return;
        }
        configureLogs();
        AbstractXmlApplicationContext context = new ClassPathXmlApplicationContext(DEFAULT_SPRING_CONTEXT);

        if (springConfig != null) {
            context = new ClassPathXmlApplicationContext(new String[]{DEFAULT_SPRING_CONTEXT, springConfig});
        }

        try {
            context.getBean(Main.class).run();
        } finally {
            context.destroy();
        }
    }

    private static void printUsage() {
        Args.usage(CliDefinition.class);
        System.exit(1);
    }

    public void run() throws IOException, InterruptedException, JSONException {
        Configuration configuration = Configuration.loadFile(config);
        mapPrinter.setConfiguration(configuration);
        OutputStream outFile = null;
        try {
            if (clientConfig) {
                outFile = getOutputStream("");
                final OutputStreamWriter writer = new OutputStreamWriter(outFile, Charset.forName("UTF-8"));

                JSONWriter json = new JSONWriter(writer);
                json.object();
                {
                    mapPrinter.printClientConfig(json);
                }
                json.endObject();

                writer.close();

            } else {
                final InputStream inFile = getInputStream();
                final String jsonConfiguration = CharStreams.toString(new InputStreamReader(inFile, "UTF-8"));
                final PJsonObject jsonSpec = MapPrinter.parseSpec(jsonConfiguration);
                outFile = getOutputStream(mapPrinter.getOutputFormat(jsonSpec).getFileSuffix());
                Map<String, String> headers = new HashMap<String, String>();
                if (referer != null) {
                    headers.put("Referer", referer);
                }
                if (cookie != null) {
                    headers.put("Cookie", cookie);
                }
                mapPrinter.print(jsonSpec, outFile, headers);
            }
        } finally {
            if (outFile != null) outFile.close();
        }
    }


    private static void configureLogs() {
        final ClassLoader classLoader = Main.class.getClassLoader();
        URL logfile;
        switch (verbose) {
            case 0:
                logfile = classLoader.getResource("shell-quiet-log4j.properties");
                break;
            case 1:
                logfile = classLoader.getResource("shell-info-log4j.properties");
                break;
            case 2:
                logfile = classLoader.getResource("shell-default-log4j.properties");
                break;
            case 3:
                logfile = classLoader.getResource("shell-verbose-log4j.properties");
                break;
            default:
                logfile = classLoader.getResource("shell-default-log4j.properties");
                break;
        }

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            // Call context.reset() to clear any previous configuration, e.g. default
            // configuration. For multi-step configuration, omit calling context.reset().
            context.reset();
            configurator.doConfigure(logfile);
        } catch (JoranException je) {
            // StatusPrinter will handle this
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);

    }

    @SuppressWarnings("resource")
    private OutputStream getOutputStream(String suffix) throws FileNotFoundException {
        final OutputStream outFile;
        if (output != null) {
            if (!output.endsWith("." + suffix)) {
                output = output + "." + suffix;
            }
            outFile = new FileOutputStream(output);
        } else {
            //noinspection UseOfSystemOutOrSystemErr
            outFile = System.out;
        }
        return outFile;
    }

    @SuppressWarnings("resource")
    private InputStream getInputStream() throws FileNotFoundException {
        final InputStream file;
        if (spec != null) {
            file = new FileInputStream(spec);
        } else {
            file = System.in;
        }
        return file;
    }

}
