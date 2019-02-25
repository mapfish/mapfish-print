package org.mapfish.print.map.geotools;

import org.apache.commons.io.IOUtils;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.mapfish.print.Constants;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.FileUtils;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.AbstractLayerParams;
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
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import javax.annotation.Nonnull;

/**
 * <p>Reads a GeoTIFF file from an URL.</p>
 */
public final class GeotiffLayer extends AbstractGridCoverage2DReaderLayer {

    /**
     * Constructor.
     *
     * @param reader the reader to use for reading the geotiff.
     * @param style style to use for rendering the data.
     * @param executorService the thread pool for doing the rendering.
     * @param params the parameters for this layer
     */
    public GeotiffLayer(
            final Function<MfClientHttpRequestFactory, AbstractGridCoverage2DReader> reader,
            final StyleSupplier<AbstractGridCoverage2DReader> style,
            final ExecutorService executorService,
            final AbstractLayerParams params) {
        super(reader::apply, style, executorService, params);
    }

    @Override
    public RenderType getRenderType() {
        return RenderType.TIFF;
    }

    /**
     * <p>Renders a GeoTIFF image as layer.</p>
     * <p>Type: <code>geotiff</code></p>
     */
    public static final class Plugin extends AbstractGridCoverageLayerPlugin
            implements MapLayerFactoryPlugin<GeotiffParam> {
        private static final Set<String> TYPENAMES = Collections.singleton("geotiff");
        @Autowired
        private ExecutorService forkJoinPool;

        @Override
        public Set<String> getTypeNames() {
            return TYPENAMES;
        }

        @Override
        public GeotiffParam createParameter() {
            return new GeotiffParam();
        }

        @Nonnull
        @Override
        public GeotiffLayer parse(
                @Nonnull final Template template,
                @Nonnull final GeotiffParam param) throws IOException {
            Function<MfClientHttpRequestFactory, AbstractGridCoverage2DReader>
                    geotiffReader =
                    getGeotiffReader(template, param.url);

            String styleRef = param.style;

            return new GeotiffLayer(geotiffReader::apply,
                                    super.<AbstractGridCoverage2DReader>createStyleSupplier(template,
                                                                                            styleRef),
                                    this.forkJoinPool,
                                    param);
        }

        private Function<MfClientHttpRequestFactory, AbstractGridCoverage2DReader> getGeotiffReader(
                final Template template,
                final String geotiffUrl) throws IOException {
            final URL url = FileUtils.testForLegalFileUrl(template.getConfiguration(), new URL(geotiffUrl));
            return (final MfClientHttpRequestFactory requestFactory) -> {
                try {
                    final File geotiffFile;
                    if (url.getProtocol().equalsIgnoreCase("file")) {
                        geotiffFile = new File(url.toURI());
                    } else {
                        geotiffFile = File.createTempFile("downloadedGeotiff", ".tiff");

                        final ClientHttpRequest request = requestFactory.createRequest(
                                url.toURI(), HttpMethod.GET);
                        try (ClientHttpResponse httpResponse = request.execute();

                             FileOutputStream output = new FileOutputStream(geotiffFile)) {
                            IOUtils.copy(httpResponse.getBody(), output);
                        }
                    }

                    return new GeoTiffFormat().getReader(geotiffFile);
                } catch (Throwable t) {
                    throw ExceptionUtils.getRuntimeException(t);
                }
            };
        }
    }

    /**
     * The parameters for reading a Geotiff file, either from the server or from a URL.
     */
    public static final class GeotiffParam extends AbstractLayerParams {
        /**
         * The url of the geotiff.  It can be a file but if it is the file must be contained within the config
         * directory.
         */
        public String url;
        /**
         * A string identifying a style to use when rendering the raster.
         */
        @HasDefaultValue
        public String style = Constants.Style.Raster.NAME;
    }
}
