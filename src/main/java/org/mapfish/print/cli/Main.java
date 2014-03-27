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

package org.mapfish.print.cli;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.google.common.io.CharStreams;
import com.sampullara.cli.Args;
import org.json.JSONWriter;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.json.PJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mapfish.print.cli.CliDefinition.clientConfig;
import static org.mapfish.print.cli.CliDefinition.config;
import static org.mapfish.print.cli.CliDefinition.cookie;
import static org.mapfish.print.cli.CliDefinition.output;
import static org.mapfish.print.cli.CliDefinition.referer;
import static org.mapfish.print.cli.CliDefinition.spec;
import static org.mapfish.print.cli.CliDefinition.springConfig;
import static org.mapfish.print.cli.CliDefinition.verbose;

/**
 * A shell version of the MapPrinter. Can be used for testing or for calling
 * from other languages than Java.
 */
public final class Main {
    private Main() {
        // intentionally empty
    }
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final int LOGLEVEL_QUIET = 0;
    private static final int LOGLEVEL_INFO = 1;
    private static final int LOGLEVEL_DEFAULT = 2;
    private static final int LOGLEVEL_VERBOSE = 3;

    /**
     * Name of the default spring context file.
     */
    public static final String DEFAULT_SPRING_CONTEXT = "mapfish-spring-application-context.xml";

    @Autowired
    private MapPrinter mapPrinter;

    /**
     * Main method.
     *
     * @param args the cli arguments
     * @throws Exception
     */
    public static void main(final String[] args) throws Exception {
        try {
            List<String> unusedArguments = Args.parse(CliDefinition.class, args);

            if (!unusedArguments.isEmpty()) {
                System.out.println("The following arguments are not recognized: " + unusedArguments);
                printUsage();
                return;
            }
        } catch (IllegalArgumentException invalidOption) {
            System.out.println(invalidOption.getMessage());
            printUsage();
            return;
        }
        AbstractXmlApplicationContext context = new ClassPathXmlApplicationContext(DEFAULT_SPRING_CONTEXT);

        if (springConfig != null) {
            context = new ClassPathXmlApplicationContext(DEFAULT_SPRING_CONTEXT, springConfig);
        }
        configureLogs();

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

    private void run() throws Exception {
        final File configFile = new File(config);
        this.mapPrinter.setConfiguration(configFile);
        OutputStream outFile = null;
        try {
            if (clientConfig) {
                outFile = getOutputStream("");
                final OutputStreamWriter writer = new OutputStreamWriter(outFile, Charset.forName("UTF-8"));

                JSONWriter json = new JSONWriter(writer);
                json.object();
                this.mapPrinter.printClientConfig(json);
                json.endObject();

                writer.close();

            } else {
                final InputStream inFile = getInputStream();
                final String jsonConfiguration = CharStreams.toString(new InputStreamReader(inFile, "UTF-8"));
                final PJsonObject jsonSpec = MapPrinter.parseSpec(jsonConfiguration);
                outFile = getOutputStream(this.mapPrinter.getOutputFormat(jsonSpec).getFileSuffix());
                Map<String, String> headers = new HashMap<String, String>();
                if (referer != null) {
                    headers.put("Referer", referer);
                }
                if (cookie != null) {
                    headers.put("Cookie", cookie);
                }
                this.mapPrinter.print(jsonSpec, outFile, headers);
            }
        } finally {
            if (outFile != null) {
                outFile.close();
            }
        }
    }


    private static void configureLogs() {
        final ClassLoader classLoader = Main.class.getClassLoader();
        URL logfile;
        switch (verbose) {
            case LOGLEVEL_QUIET:
                logfile = classLoader.getResource("shell-quiet-log.xml");
                break;
            case LOGLEVEL_INFO:
                logfile = classLoader.getResource("shell-info-log.xml");
                break;
            case LOGLEVEL_DEFAULT:
                logfile = classLoader.getResource("shell-default-log.xml");
                break;
            case LOGLEVEL_VERBOSE:
                logfile = classLoader.getResource("shell-verbose-log.xml");
                break;
            default:
                logfile = classLoader.getResource("shell-default-log.xml");
                break;
        }

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(loggerContext);
            // Call context.reset() to clear any previous configuration, e.g. default
            // configuration. For multi-step configuration, omit calling context.reset().
            loggerContext.reset();
            configurator.doConfigure(logfile);
        } catch (JoranException je) {
            // StatusPrinter will handle this
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
    }

    private OutputStream getOutputStream(final String suffix) throws FileNotFoundException {
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
