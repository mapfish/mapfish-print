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

package org.mapfish.print.map.geotools;

import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import jsr166y.ForkJoinPool;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.styling.Style;
import org.mapfish.print.Constants;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.MapLayerFactoryPlugin;
import org.mapfish.print.map.style.StyleParser;
import org.mapfish.print.processor.HasDefaultValue;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;

import static org.mapfish.print.Constants.RASTER_STYLE_NAME;

/**
 * Reads a Geotiff file from a URL.
 *
 * @author Jesse on 3/26/14.
 */
public final class GeotiffLayer extends AbstractGridCoverage2DReaderLayer {

    /**
     * Constructor.
     *
     * @param reader          the reader to use for reading the geotiff.
     * @param style           style to use for rendering the data.
     * @param executorService the thread pool for doing the rendering.
     */
    public GeotiffLayer(final GeoTiffReader reader, final Style style, final ExecutorService executorService) {
        super(reader, style, executorService);
    }

    /**
     * Parser for creating {@link org.mapfish.print.map.geotools.GeotiffLayer} layers from request data.
     */
    public static final class Plugin implements MapLayerFactoryPlugin<GeotiffParam> {
        @Autowired
        private StyleParser parser;
        @Autowired
        private ForkJoinPool forkJoinPool;

        private Set<String> typeNames = Sets.newHashSet("geotiff");

        @Override
        public Set<String> getTypeNames() {
            return this.typeNames;
        }

        @Override
        public GeotiffParam createParameter() {
            return new GeotiffParam();
        }

        @Nonnull
        @Override
        public MapLayer parse(final Template template, @Nonnull final GeotiffParam param) throws IOException {
            GeoTiffReader geotiffReader = getGeotiffReader(template, param.url);

            String styleRef = param.style;
            Style style = template.getStyle(styleRef)
                    .or(this.parser.loadStyle(template.getConfiguration(), styleRef))
                    .or(template.getConfiguration().getDefaultStyle(RASTER_STYLE_NAME));

            return new GeotiffLayer(geotiffReader, style, this.forkJoinPool);
        }

        private GeoTiffReader getGeotiffReader(final Template template, final String geotiffUrl) throws IOException {
            URL url = new URL(geotiffUrl);
            final String protocol = url.getProtocol();
            final File geotiffFile;
            if (protocol.equalsIgnoreCase("file")) {
                geotiffFile = new File(template.getConfiguration().getDirectory(), geotiffUrl.substring("file://".length()));
                if (!geotiffFile.exists() || !geotiffFile.isFile()) {
                    throw new IllegalArgumentException("The url in the geotiff layer: " + geotiffUrl + " is a file url but does not " +
                                                       "reference a file within the configuration directory.  All file urls must be " +
                                                       "relative urls to the configuration directory and may not contain ..");
                }
                assertFileIsInConfigDir(template, geotiffFile);
            } else {
                geotiffFile = File.createTempFile("downloadedGeotiff", ".tiff");
                OutputStream output = null;
                try {
                    output = new FileOutputStream(geotiffFile);
                    Resources.copy(url, output);
                } finally {
                    if (output != null) {
                        output.close();
                    }
                }
            }

            final GeoTiffReader reader = new GeoTiffFormat().getReader(geotiffFile);
            return reader;
        }

        private void assertFileIsInConfigDir(final Template template, final File file) {
            final String configurationDir = template.getConfiguration().getDirectory().getAbsolutePath();
            if (!file.getAbsolutePath().startsWith(configurationDir)) {
                throw new IllegalArgumentException("The url attribute is a file url but indicates a file that is not within the" +
                                                   " configurationDirectory: " + file.getAbsolutePath());
            }
        }

    }

    /**
     * The parameters for reading a Geotiff file, either from the server or from a URL.
     */
    public static final class GeotiffParam {
        /**
         * The url of the geotiff.  It can be a file but if it is the file must be contained within the config directory.
         */
        public String url;
        /**
         * A string identifying a style to use when rendering the raster.
         */
        @HasDefaultValue
        public String style = Constants.RASTER_STYLE_NAME;
    }
}
