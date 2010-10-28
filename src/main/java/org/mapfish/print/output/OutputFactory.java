package org.mapfish.print.output;

import org.apache.log4j.Logger;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.config.Config;
import org.mapfish.print.utils.PJsonObject;

import java.util.*;

/**
 * User: jeichar
 * Date: Oct 18, 2010
 * Time: 2:07:54 PM
 */
public class OutputFactory {
    private static final Logger LOGGER = Logger.getLogger(OutputFactory.class);
    private final static List<OutputFormatFactory> formats = new ArrayList<OutputFormatFactory>();

    static {

        // order matters.  first match will get used
        formats.add(new PdfOutput());
        formats.add(new ImageOutputScalableFactory());
        formats.add(new ImageOutputFactory());
    }

    public static OutputFormat create(Config config, PJsonObject spec) {
        String id = spec.optString("outputFormat", "pdf");

        for (OutputFormatFactory formatFactory : formats) {
            String enablementMsg = formatFactory.enablementStatus();
            if(enablementMsg == null) {
                for (String supportedFormat : formatFactory.formats()) {
                    if(permitted(supportedFormat, config) && supportedFormat.equalsIgnoreCase(id)) {
                        final OutputFormat outputFormat = formatFactory.create(id);
                        LOGGER.info("OutputFormat chosen for " + id + " is " + (outputFormat.getClass().getSimpleName()));
                        return outputFormat;
                    }
                }
            } else {
                LOGGER.warn("OutputFormatFactory " + (formatFactory.getClass().getSimpleName()) + " is disabled: " + enablementMsg);
            }
        }

        if (id.equalsIgnoreCase("pdf")) {
            throw new Error("There must be a format that can output PDF");
        } else {
            StringBuilder allFormats = new StringBuilder();
            for (String format : getSupportedFormats(config)) {
                if(allFormats.length() > 0) allFormats.append(", ");
                allFormats.append(format.toLowerCase());
            }

            throw new IllegalArgumentException(id + " is not a supported format. Supported formats: "+allFormats);
        }
    }

    public static Set<String> getSupportedFormats(Config config) {
        Set<String> supported = new HashSet<String>();
        for (OutputFormatFactory formatFactory : formats) {
            if(formatFactory.enablementStatus() == null) {
                for (String format : formatFactory.formats()) {
                    if(permitted(format, config)) {
                        supported.add(format.toLowerCase());
                    }
                }
            }
        }

        return supported;
    }

    private static boolean permitted(String supportedFormat, Config config) {
        TreeSet<String> configuredFormats = config.getFormats();
        if(configuredFormats.size() == 1 && configuredFormats.iterator().next().trim().equals("*")) {
            return true;
        }

        if(configuredFormats.isEmpty()) {
            return "pdf".equalsIgnoreCase(supportedFormat);
        }

        for (String configuredFormat : configuredFormats) {
            if(configuredFormat.equalsIgnoreCase(supportedFormat)) return true;
        }

        return false;
    }

}
