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

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closer;
import jsr166y.ForkJoinPool;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.mapfish.print.Constants;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.FileUtils;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.MapLayerFactoryPlugin;
import org.mapfish.print.parser.HasDefaultValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    public GeotiffLayer(final Function<MfClientHttpRequestFactory, AbstractGridCoverage2DReader> reader,
                        final StyleSupplier<AbstractGridCoverage2DReader> style,
                        final ExecutorService executorService) {
        super(reader, style, executorService);
    }

    /**
     * Parser for creating {@link org.mapfish.print.map.geotools.GeotiffLayer} layers from request data.
     */
    public static final class Plugin extends AbstractGridCoverageLayerPlugin implements MapLayerFactoryPlugin<GeotiffParam> {
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
        public GeotiffLayer parse(final Template template,
                              @Nonnull final GeotiffParam param) throws IOException {
            Function<MfClientHttpRequestFactory, AbstractGridCoverage2DReader> geotiffReader = getGeotiffReader(template, param.url);

            String styleRef = param.style;

            return new GeotiffLayer(geotiffReader,
                    super.<AbstractGridCoverage2DReader>createStyleSupplier(template, styleRef),
                    this.forkJoinPool);
        }

        private Function<MfClientHttpRequestFactory, AbstractGridCoverage2DReader> getGeotiffReader(final Template template,
                                                                                                  final String geotiffUrl) throws
                IOException {
            final URL url = FileUtils.testForLegalFileUrl(template.getConfiguration(), new URL(geotiffUrl));
            return new Function<MfClientHttpRequestFactory, AbstractGridCoverage2DReader>() {
                @Nullable
                @Override
                public AbstractGridCoverage2DReader apply(final MfClientHttpRequestFactory requestFactory) {
                    try {
                        final File geotiffFile;
                        if (url.getProtocol().equalsIgnoreCase("file")) {
                            geotiffFile = new File(url.toURI());
                        } else {
                            geotiffFile = File.createTempFile("downloadedGeotiff", ".tiff");
                            Closer closer = Closer.create();

                            try {
                                final ClientHttpRequest request = requestFactory.createRequest(url.toURI(), HttpMethod.GET);
                                final ClientHttpResponse httpResponse = closer.register(request.execute());
                                FileOutputStream output = closer.register(new FileOutputStream(geotiffFile));
                                ByteStreams.copy(httpResponse.getBody(), output);
                            } finally {
                                closer.close();
                            }
                        }

                        return new GeoTiffFormat().getReader(geotiffFile);
                    } catch (Throwable t) {
                        throw ExceptionUtils.getRuntimeException(t);
                    }

                }
            };
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
        public String style = Constants.Style.Raster.NAME;
    }
}
