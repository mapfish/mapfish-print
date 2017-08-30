package org.mapfish.print.cli;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.CharStreams;
import com.sampullara.cli.Args;

import org.json.JSONWriter;
import org.mapfish.print.Constants;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.servlet.oldapi.OldAPIRequestConverter;
import org.mapfish.print.url.data.Handler;
import org.mapfish.print.wrapper.json.PJsonObject;
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
import java.util.List;

/**
 * A shell version of the MapPrinter. Can be used for testing or for calling
 * from other languages than Java.
 */
public final class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final int LOGLEVEL_QUIET = 0;
    private static final int LOGLEVEL_INFO = 1;
    private static final int LOGLEVEL_DEFAULT = 2;
    private static final int LOGLEVEL_VERBOSE = 3;

    /**
     * Name of the default spring context file.
     */
    public static final String DEFAULT_SPRING_CONTEXT = "/mapfish-cli-spring-application-context.xml";

    @Autowired
    private MapPrinter mapPrinter;

    static {
        Handler.configureProtocolHandler();
    }

    private static boolean exceptionOnFailure;

    private Main() {
        // intentionally empty
    }

    /**
     * Main method.
     *
     * @param args the cli arguments
     * @throws Exception
     */
    public static void main(final String[] args) throws Exception {
        runMain(args);
        System.exit(0);
    }

    /**
     * Runs the print.
     *
     * @param args the cli arguments
     * @throws Exception
     */
    @VisibleForTesting
    public static void runMain(final String[] args) throws Exception {
        final CliHelpDefinition helpCli = new CliHelpDefinition();

        try {
            Args.parse(helpCli, args);
            if (helpCli.help) {
                printUsage(0);
                return;
            }
        } catch (IllegalArgumentException invalidOption) {
            // Ignore because it is probably one of the non-help options.
        }

        final CliDefinition cli = new CliDefinition();
        try {
            List<String> unusedArguments = Args.parse(cli, args);

            if (!unusedArguments.isEmpty()) {
                System.out.println("\n\nThe following arguments are not recognized: " + unusedArguments);
                printUsage(1);
                return;
            }
        } catch (IllegalArgumentException invalidOption) {
            System.out.println("\n\n" + invalidOption.getMessage());
            printUsage(1);
            return;
        }
        configureLogs(cli.verbose);

        AbstractXmlApplicationContext context = new ClassPathXmlApplicationContext(DEFAULT_SPRING_CONTEXT);

        if (cli.springConfig != null) {
            context = new ClassPathXmlApplicationContext(DEFAULT_SPRING_CONTEXT, cli.springConfig);
        }
        try {
            context.getBean(Main.class).run(cli);
        } finally {
            context.destroy();
        }
    }

    private static void printUsage(final int exitCode) {
        Args.usage(new CliDefinition());
        if (Main.exceptionOnFailure) {
            throw new Error("Printing Usage: " + exitCode);
        } else {
            System.exit(exitCode);
        }
    }

    private void run(final CliDefinition cli) throws Exception {
        final File configFile = new File(cli.config);
        this.mapPrinter.setConfiguration(configFile);
        OutputStream outFile = null;
        try {
            if (cli.clientConfig) {
                outFile = getOutputStream(cli.output, ".yaml");
                final OutputStreamWriter writer = new OutputStreamWriter(outFile, Charset.forName(Constants.DEFAULT_ENCODING));

                JSONWriter json = new JSONWriter(writer);
                json.object();
                this.mapPrinter.printClientConfig(json);
                json.endObject();

                writer.close();

            } else {
                final InputStream inFile = getInputStream(cli.spec);
                final String jsonConfiguration = CharStreams.toString(new InputStreamReader(inFile, Constants.DEFAULT_ENCODING));
                PJsonObject jsonSpec = MapPrinter.parseSpec(jsonConfiguration);

                if (cli.v2Api) {
                    PJsonObject oldApiSpec = jsonSpec;
                    LOGGER.info("Converting request data from V2 API request data to V3 API");
                    jsonSpec = OldAPIRequestConverter.convert(oldApiSpec, this.mapPrinter.getConfiguration());
                }

                outFile = getOutputStream(cli.output, this.mapPrinter.getOutputFormat(jsonSpec).getFileSuffix());
                LOGGER.debug("Request Data: \n" + jsonSpec.getInternalObj().toString(2));
                this.mapPrinter.print("main", jsonSpec, outFile);
            }
        } finally {
            if (outFile != null) {
                outFile.close();
            }
        }
    }


    private static void configureLogs(final String verbose) {
        final ClassLoader classLoader = Main.class.getClassLoader();
        URL logfile;
        switch (Integer.parseInt(verbose)) {
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

    private OutputStream getOutputStream(final String output, final String suffix) throws FileNotFoundException {
        String outputPath = output;
        final OutputStream outFile;
        if (outputPath != null) {
            if (!outputPath.endsWith("." + suffix)) {
                outputPath = outputPath + "." + suffix;
            }
            outFile = new FileOutputStream(outputPath);
        } else {
            //noinspection UseOfSystemOutOrSystemErr
            outFile = System.out;
        }
        return outFile;
    }

    private InputStream getInputStream(final String spec) throws FileNotFoundException {
        final InputStream file;
        if (spec != null) {
            file = new FileInputStream(spec);
        } else {
            file = System.in;
        }
        return file;
    }

    /**
     * Instead of calling system.exit an exception will be thrown.  This is useful for testing so a test won't shutdown jvm.
     *
     * @param exceptionOnFailure if true then an exception will be thrown instead of system.exit being called.
     */
    @VisibleForTesting
    static void setExceptionOnFailure(final boolean exceptionOnFailure) {
        Main.exceptionOnFailure = exceptionOnFailure;
    }
}
