package org.mapfish.print;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Tools function to get java fonts families and interface font config.
 */
public final class FontTools {
    private static final Logger LOGGER = LoggerFactory.getLogger(FontTools.class);

    /**
     * List of java font families.
     */
    public static final Set<String> FONT_FAMILIES;
    static {
        Set<String> ff = new HashSet<>();
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (java.awt.Font font : graphicsEnvironment.getAllFonts()) {
          ff.add(font.getFamily());
        }
        FONT_FAMILIES = Collections.unmodifiableSet(ff);
    }

    private FontTools() {
    }

    /**
     * Description of font get from font config.
     */
    public static class FontConfigDescription {
        /** The font families. */
        public String[] family;
        /** The font style. */
        public String[] style;
        /** The font name. */
        public String name;
        /**
         * CSS like font weight.
         */
        public int weight;
    }

    /**
     * Get matched font from a font family name.
     *
     * @param family the font family name.
     * @return The matched cont config attributes.
     */
    public static List<FontConfigDescription> listFontConfigFonts(final String family) {
        List<FontConfigDescription> descriptions = new ArrayList<>();
        if (SystemUtils.IS_OS_LINUX) {
            InputStreamReader inputStreamReader = null;
            BufferedReader stdInput = null;
            try {
                String[] commands = {"fc-list", "-b", family};
                Process process = Runtime.getRuntime().exec(commands);

                inputStreamReader = new InputStreamReader(process.getInputStream(), "utf-8");
                stdInput = new BufferedReader(inputStreamReader);
                String inputLine = null;
                FontConfigDescription description = null;
                while ((inputLine = stdInput.readLine()) != null) {
                    if (inputLine.startsWith("Pattern ")) {
                        description = new FontConfigDescription();
                        descriptions.add(description);
                    } else if (description != null) {
                        augmentFontConfigDescription(description, inputLine);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Unable to get font config font list", e);
            } finally {
                if (inputStreamReader != null) {
                    try {
                        inputStreamReader.close();
                    } catch (IOException e) {
                        LOGGER.error("Unable to close stream", e);
                    }
                }
                if (stdInput != null) {
                    try {
                        stdInput.close();
                    } catch (IOException e) {
                        LOGGER.error("Unable to close stream", e);
                    }
                }
            }
        }
        return descriptions;
    }

    /**
     * parses the inputLine and adds information to the FontConfigDescription.
     * 
     * @param description
     * @param inputLine
     */
    private static void augmentFontConfigDescription(final FontConfigDescription description,
            final String inputLine) {
        String[] split = inputLine.trim().split(": ");
        if (split[0].equals("family")) {
            description.family = split[1].substring(1, split[1].length() - 4)
                .split(Pattern.quote("\"(s) \""));
        } else if (split[0].equals("style")) {
            description.style = split[1].substring(1, split[1].length() - 4)
                .split(Pattern.quote("\"(s) \""));
        } else if (split[0].equals("fullname")) {
            description.name = split[1].substring(1, split[1].length() - 4);
        } else if (split[0].equals("weight")) {
            final int weight = Integer.parseInt(split[1]
                .substring(0, split[1].length() - 6));
            description.weight = recalculateWeight(weight);
        }
    }

    private static int recalculateWeight(final int weight) {
        int result;
        // See more information:
        // https://work.lisk.in/2020/07/18/font-weight-300.html
        // https://developer.mozilla.org/en-US/docs/Web/CSS/font-weight
        // #Common_weight_name_mapping
        if (weight < 20) {
            result = 100;
        } else if (weight < 45) {
            result = 200;
        } else if (weight < 65) {
            result = 300;
        } else if (weight < 90) {
            result = 400;
        } else if (weight < 140) {
            result = 500;
        } else if (weight < 190) {
            result = 600;
        } else if (weight < 203) {
            result = 700;
        } else if (weight < 208) {
            result = 800;
        } else {
            result = 900;
        }

        return result;
    }
}
