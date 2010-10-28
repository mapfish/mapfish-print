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

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.ByteBuffer;
import com.lowagie.text.pdf.PdfStream;
import com.lowagie.text.pdf.PdfWriter;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mapfish.print.config.Config;
import org.mapfish.print.config.layout.Layout;
import org.mapfish.print.utils.PJsonObject;

import java.io.*;
import java.util.TreeSet;

/**
 * The main class for printing maps. Will parse the spec, create the PDF
 * document and generate it.
 */
public class MapPrinter {
    /**
     * The parsed configuration file.
     */
    private final Config config;

    /**
     * The directory where the configuration file sits (used for ${configDir})
     */
    private String configDir;

    static {
        //configure iText to use a higher precision for floats
        ByteBuffer.HIGH_PRECISION = true;
    }

    public MapPrinter(File config) throws FileNotFoundException {
        this.config = Config.fromYaml(config);
        configDir = config.getParent();
        if (configDir == null) {
            try {
                configDir = new File(".").getCanonicalPath();
            } catch (IOException e) {
                configDir = ".";
            }
        }
        initFonts();
    }

    public MapPrinter(InputStream instreamConfig, String configDir) {
        this.config = Config.fromInputStream(instreamConfig);
        this.configDir = configDir;

        initFonts();
    }


    public MapPrinter(String strConfig, String configDir) {
        this.config = Config.fromString(strConfig);
        this.configDir = configDir;

        initFonts();
    }

    /**
     * Register the user specified fonts in iText.
     */
    private void initFonts() {
        //we don't do that since it takes ages and that would hurt the perfs for
        //the python controller:
        //FontFactory.registerDirectories();

        FontFactory.defaultEmbedding = true;

        final TreeSet<String> fontPaths = config.getFonts();
        if (fontPaths != null) {
            for (String fontPath : fontPaths) {
                fontPath = fontPath.replaceAll("\\$\\{configDir\\}", configDir);
                File fontFile = new File(fontPath);
                if (fontFile.isDirectory()) {
                    FontFactory.registerDirectory(fontPath, true);
                } else {
                    FontFactory.register(fontPath);
                }
            }
        }
    }

    /**
     * Generate the PDF using the given spec.
     *
     * @return The context that was used for printing.
     */
    public RenderingContext print(PJsonObject jsonSpec, OutputStream outFile, String referer) throws DocumentException {


        final String layoutName = jsonSpec.getString(Constants.JSON_LAYOUT_KEY);
        Layout layout = config.getLayout(layoutName);
        if (layout == null) {
            throw new RuntimeException("Unknown layout '" + layoutName + "'");
        }

        Document doc = new Document(layout.getFirstPageSize(null,jsonSpec));
        PdfWriter writer = PdfWriter.getInstance(doc, outFile);
        if (!layout.isSupportLegacyReader()) {
            writer.setFullCompression();
            writer.setPdfVersion(PdfWriter.PDF_VERSION_1_5);
            writer.setCompressionLevel(PdfStream.BEST_COMPRESSION);
        }
        RenderingContext context = new RenderingContext(doc, writer, config, jsonSpec, configDir, layout, referer);

        layout.render(jsonSpec, context);

        doc.close();
        writer.close();
        return context;
    }

    public static PJsonObject parseSpec(String spec) {
        final JSONObject jsonSpec;
        try {
            jsonSpec = new JSONObject(spec);
        } catch (JSONException e) {
            throw new RuntimeException("Cannot parse the spec file", e);
        }
        return new PJsonObject(jsonSpec, "spec");
    }

    /**
     * Use by /info.json to generate its returned content.
     */
    public void printClientConfig(JSONWriter json) throws JSONException {
        config.printClientConfig(json);
    }

    /**
     * Stop the thread pool or others.
     */
    public void stop() {
        config.stop();
    }

    public String getOutputFilename(String layout, String defaultName) {
        final String name = config.getOutputFilename(layout);
        return name == null ? defaultName : name;
    }

    public Config getConfig() {
        return config;
    }
}
