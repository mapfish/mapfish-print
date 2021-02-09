package org.mapfish.print.processor.map;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.math.IntMath;
import org.mapfish.print.attribute.map.GenericMapAttribute.GenericMapAttributeValues;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.map.image.wms.WmsLayer;
import org.mapfish.print.map.image.wms.WmsLayerParam;
import org.mapfish.print.map.tiled.wms.TiledWmsLayer;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.InputOutputValue;
import org.mapfish.print.processor.http.matcher.URIMatcher;
import org.mapfish.print.processor.http.matcher.UriMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.awt.Dimension;
import java.math.RoundingMode;
import java.net.URI;
import java.util.List;

import static org.mapfish.print.Constants.PDF_DPI;

/**
 * <p>Processor that transforms WMS layers that are too big into tiled WMS layers.</p>
 *
 * <p>This processor will reduce the given max tile size to best match the dimensions of the map. This is
 * to reduce the amount of extra data that is fetched from the WMS server.</p>
 */
public class SetTiledWmsProcessor extends AbstractProcessor<SetTiledWmsProcessor.Input, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetTiledWmsProcessor.class);

    /**
     * The matchers that chooses if the processor is applied or not.
     */
    protected final UriMatchers matchers = new UriMatchers();
    /**
     * The maximum width in pixels.
     */
    private int maxWidth;
    /**
     * The maximum height in pixels.
     */
    private int maxHeight;

    /**
     * Constructor.
     */
    protected SetTiledWmsProcessor() {
        super(Void.class);
    }

    @VisibleForTesting
    static int adaptTileDimension(final int pixels, final int maxTile) {
        final int nb = IntMath.divide(pixels, maxTile, RoundingMode.CEILING);
        return IntMath.divide(pixels, nb, RoundingMode.CEILING);
    }

    /**
     * Adapt the size of the tiles so that we have the same amount of tiles as we would have had with maxWidth
     * and maxHeight, but with the smallest tiles as possible.
     */
    private static Dimension adaptTileDimensions(
            final Dimension pixels, final int maxWidth, final int maxHeight) {
        return new Dimension(adaptTileDimension(pixels.width, maxWidth),
                             adaptTileDimension(pixels.height, maxHeight));
    }

    @Override
    public final Input createInputParameter() {
        return new Input();
    }

    @Override
    public final Void execute(final Input values, final ExecutionContext context) throws Exception {
        final Dimension size = values.map.getMapSize();
        final double dpiRatio = values.map.getDpi() / PDF_DPI;
        final Dimension pixels = new Dimension((int) Math.ceil(size.width * dpiRatio),
                                               (int) Math.ceil(size.height * dpiRatio));

        if (pixels.height <= maxHeight && pixels.width <= maxWidth) {
            return null;
        }

        final Dimension tileSize = adaptTileDimensions(pixels, maxWidth, maxHeight);
        final int tileBufferWidth = 10;
        final int tileBufferHeight = 10;

        final List<MapLayer> layers = values.map.getLayers();
        for (int i = 0; i < layers.size(); i++) {
            final MapLayer layer = layers.get(i);

            context.stopIfCanceled();
            if (layer instanceof WmsLayer) {
                final WmsLayer wmsLayer = (WmsLayer) layer;
                final WmsLayerParam params = wmsLayer.getParams();
                if (matchers.matches(new URI(params.baseURL), HttpMethod.GET)) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Converting layer {}[{}] into a tiled WMS layer with tileSize={}x{}",
                                    wmsLayer.getParams().baseURL,
                                    String.join(", ", wmsLayer.getParams().layers), tileSize.width,
                                    tileSize.height);
                    }
                    values.map.replaceLayer(i, new TiledWmsLayer(wmsLayer, tileSize, tileBufferWidth, tileBufferHeight));
                }
            }
        }
        return null;
    }

    @Override
    protected final void extraValidation(
            final List<Throwable> validationErrors, final Configuration configuration) {
        if (this.maxHeight < 256) {
            validationErrors.add(new ConfigurationException(
                    "The maxHeight must be >=256 in " + getClass().getName()));
        }
        if (this.maxWidth < 256) {
            validationErrors.add(new ConfigurationException(
                    "The maxWidth must be >=256 in " + getClass().getName()));
        }
    }

    /**
     * Set the maximum width in pixels.
     *
     * @param maxWidth the value
     */
    public void setMaxWidth(final int maxWidth) {
        this.maxWidth = maxWidth;
    }

    /**
     * Set the maximum height in pixels.
     *
     * @param maxHeight the value
     */
    public void setMaxHeight(final int maxHeight) {
        this.maxHeight = maxHeight;
    }

    /**
     * The matchers used to select the WMS urls that are going to be modified by the processor. For example:
     * <pre><code>
     * - !restrictUris
     *   matchers:
     *     - !dnsMatch
     *       host: labs.metacarta.com
     *       port: 80
     * </code></pre>
     *
     * @param matchers the list of matcher to use to check if a url is permitted
     */
    public final void setMatchers(final List<? extends URIMatcher> matchers) {
        this.matchers.setMatchers(matchers);
    }

    /**
     * The input parameter object for {@link SetFeaturesProcessor}.
     */
    public static final class Input {
        /**
         * The map to update.
         */
        @InputOutputValue
        public GenericMapAttributeValues map;
    }
}
