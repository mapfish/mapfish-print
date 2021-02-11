package org.mapfish.print.map.tiled;


import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.GeneralEnvelope;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.StatsUtils;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.map.style.json.ColorParser;
import org.mapfish.print.map.tiled.TilePreparationInfo.SingleTilePreparationInfo;
import org.mapfish.print.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.RecursiveTask;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;


/**
 * The CoverageTask class.
 */
public final class CoverageTask implements Callable<GridCoverage2D> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoverageTask.class);

    private final TileCacheInformation tiledLayer;
    private final TilePreparationInfo tilePreparationInfo;
    private final boolean failOnError;
    private final MetricRegistry registry;
    private final Processor.ExecutionContext context;
    private final BufferedImage errorImage;


    /**
     * Constructor.
     *
     * @param tilePreparationInfo tileLoader Results.
     * @param failOnError fail on tile download error.
     * @param registry the metrics registry.
     * @param context the job ID.
     * @param tileCacheInfo the object used to create the tile requests.
     * @param configuration the configuration.
     */
    public CoverageTask(
            @Nonnull final TilePreparationInfo tilePreparationInfo,
            final boolean failOnError,
            @Nonnull final MetricRegistry registry,
            @Nonnull final Processor.ExecutionContext context,
            @Nonnull final TileCacheInformation tileCacheInfo,
            @Nonnull final Configuration configuration) {
        this.tilePreparationInfo = tilePreparationInfo;
        this.context = context;
        this.tiledLayer = tileCacheInfo;
        this.failOnError = failOnError;
        this.registry = registry;

        final Dimension tileSize = this.tiledLayer.getTileSize();
        this.errorImage = new BufferedImage(tileSize.width, tileSize.height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = this.errorImage.createGraphics();
        try {
            graphics.setBackground(ColorParser.toColor(configuration.getOpaqueTileErrorColor()));
            graphics.clearRect(0, 0, tileSize.width, tileSize.height);
        } finally {
            graphics.dispose();
        }
    }

    /**
     * Call the Coverage Task.
     */
    public GridCoverage2D call() {
        try {
            BufferedImage coverageImage = this.tiledLayer.createBufferedImage(
                    this.tilePreparationInfo.getImageWidth(),
                    this.tilePreparationInfo.getImageHeight());
            Graphics2D graphics = coverageImage.createGraphics();
            try {
                for (SingleTilePreparationInfo tileInfo: this.tilePreparationInfo.getSingleTiles()) {
                    final TileTask task;
                    if (tileInfo.getTileRequest() != null) {
                        task = new SingleTileLoaderTask(
                                tileInfo.getTileRequest(), this.errorImage, tileInfo.getTileIndexX(),
                                tileInfo.getTileIndexY(), this.failOnError, this.registry, this.context);
                    } else {
                        task = new PlaceHolderImageTask(this.tiledLayer.getMissingTileImage(),
                                                        tileInfo.getTileIndexX(), tileInfo.getTileIndexY());
                    }
                    Tile tile = task.call();
                    if (tile.getImage() != null) {
                    	// crop the image here
                    	BufferedImage noBufferTileImage;
                    	if (this.tiledLayer.getTileBufferWidth() > 0 || this.tiledLayer.getTileBufferHeight() > 0) {
                    		int noBufferWidth = Math.min(
                    				this.tiledLayer.getTileSize().width,
                    				tile.getImage().getWidth() - this.tiledLayer.getTileBufferWidth());
                    		int noBufferHeight = Math.min(
                    				this.tiledLayer.getTileSize().height,
                    				tile.getImage().getHeight() - this.tiledLayer.getTileBufferHeight());
                    		noBufferTileImage = tile.getImage().getSubimage(
                			this.tiledLayer.getTileBufferWidth(),
                			this.tiledLayer.getTileBufferHeight(),
                			noBufferWidth,
                			noBufferHeight);
                    	}else {
                    		noBufferTileImage = tile.getImage();
                    	}
                        graphics.drawImage(noBufferTileImage,
                                           tile.getxIndex() * this.tiledLayer.getTileSize().width,
                                           tile.getyIndex() * this.tiledLayer.getTileSize().height, null);
                    }
                }
            } finally {
                graphics.dispose();
            }

            GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
            GeneralEnvelope gridEnvelope = new GeneralEnvelope(this.tilePreparationInfo.getMapProjection());
            gridEnvelope.setEnvelope(this.tilePreparationInfo.getGridCoverageOrigin().x,
                                     this.tilePreparationInfo.getGridCoverageOrigin().y,
                                     this.tilePreparationInfo.getGridCoverageMaxX(),
                                     this.tilePreparationInfo.getGridCoverageMaxY());
            return factory.create(this.tiledLayer.createCommonUrl(), coverageImage, gridEnvelope,
                                  null, null, null);
        } catch (Exception e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
    }


    /**
     * Tile Task.
     */
    public abstract static class TileTask extends RecursiveTask<Tile> implements Callable<Tile> {
        private final int tileIndexX;
        private final int tileIndexY;

        /**
         * Constructor.
         *
         * @param tileIndexX tile index x
         * @param tileIndexY tile index y
         */
        public TileTask(final int tileIndexX, final int tileIndexY) {
            this.tileIndexX = tileIndexX;
            this.tileIndexY = tileIndexY;
        }

        public final int getTileIndexX() {
            return this.tileIndexX;
        }

        public final int getTileIndexY() {
            return this.tileIndexY;
        }

        @Override
        public final Tile call() {
            return this.compute();
        }
    }

    /**
     * Single Tile Loader Task.
     */
    public static final class SingleTileLoaderTask extends TileTask {

        private final ClientHttpRequest tileRequest;
        private final boolean failOnError;
        private final MetricRegistry registry;
        private final Processor.ExecutionContext context;
        private final BufferedImage errorImage;

        /**
         * Constructor.
         *
         * @param tileRequest tile request
         * @param errorImage error image
         * @param tileIndexX tile index x
         * @param tileIndexY tile index y
         * @param failOnError fail on error
         * @param registry registry
         * @param context the job ID
         */
        public SingleTileLoaderTask(
                final ClientHttpRequest tileRequest, final BufferedImage errorImage,
                final int tileIndexX, final int tileIndexY, final boolean failOnError,
                final MetricRegistry registry, final Processor.ExecutionContext context) {
            super(tileIndexX, tileIndexY);
            this.tileRequest = tileRequest;
            this.errorImage = errorImage;
            this.failOnError = failOnError;
            this.registry = registry;
            this.context = context;
        }

        @Override
        protected Tile compute() {
            return this.context.mdcContext(() -> {
                final String baseMetricName = TilePreparationTask.class.getName() + ".read." +
                        StatsUtils.quotePart(this.tileRequest.getURI().getHost());
                LOGGER.debug("\n\t{} -- {}", this.tileRequest.getMethod(), this.tileRequest.getURI());
                final Timer.Context timerDownload = this.registry.timer(baseMetricName).time();
                try (ClientHttpResponse response = this.tileRequest.execute()) {
                    final HttpStatus statusCode = response.getStatusCode();
                    if (statusCode == HttpStatus.NO_CONTENT || statusCode == HttpStatus.NOT_FOUND) {
                        if (statusCode == HttpStatus.NOT_FOUND) {
                            LOGGER.info(
                                    "The request {} returns a not found status code, we consider it as an " +
                                            "empty tile.", this.tileRequest.getURI());
                        }
                        // Empty response, nothing special to do
                        return new Tile(null, getTileIndexX(), getTileIndexY());
                    } else if (statusCode != HttpStatus.OK) {
                        String errorMessage = String.format(
                                "Error making tile request: %s\n\tStatus: %s\n\toutMessage: %s",
                                this.tileRequest.getURI(), statusCode, response.getStatusText());
                        this.registry.counter(baseMetricName + ".error").inc();
                        if (this.failOnError) {
                            throw new RuntimeException(errorMessage);
                        } else {
                            LOGGER.info(errorMessage);
                            return new Tile(this.errorImage, getTileIndexX(), getTileIndexY());
                        }
                    }

                    BufferedImage image = ImageIO.read(response.getBody());
                    if (image == null) {
                        LOGGER.warn("The URL: {} is an image format that cannot be decoded",
                                    this.tileRequest.getURI());
                        image = this.errorImage;
                        this.registry.counter(baseMetricName + ".error").inc();
                    } else {
                        timerDownload.stop();
                    }

                    return new Tile(image, getTileIndexX(), getTileIndexY());
                } catch (IOException e) {
                    this.registry.counter(baseMetricName + ".error").inc();
                    throw ExceptionUtils.getRuntimeException(e);
                }
            });
        }
    }

    /**
     * PlaceHolder Tile Loader Task.
     */
    public static class PlaceHolderImageTask extends TileTask {

        private final BufferedImage placeholderImage;

        /**
         * Constructor.
         *
         * @param placeholderImage placeholder image
         * @param tileOriginX tile origin x
         * @param tileOriginY tile origin y
         */
        public PlaceHolderImageTask(
                final BufferedImage placeholderImage, final int tileOriginX,
                final int tileOriginY) {
            super(tileOriginX, tileOriginY);
            this.placeholderImage = placeholderImage;
        }

        @Override
        protected final Tile compute() {
            return new Tile(this.placeholderImage, getTileIndexX(), getTileIndexY());
        }
    }

    /**
     * Tile.
     */
    public static final class Tile {
        /**
         * The tile image.
         */
        private final BufferedImage image;
        /**
         * The x index of the image.  the x coordinate to draw this tile is xIndex * tileSizeX
         */
        private final int xIndex;
        /**
         * The y index of the image.  the y coordinate to draw this tile is yIndex * tileSizeY
         */
        private final int yIndex;

        /**
         * Constructor.
         *
         * @param image
         * @param xIndex
         * @param yIndex
         */
        private Tile(final BufferedImage image, final int xIndex, final int yIndex) {
            this.image = image;
            this.xIndex = xIndex;
            this.yIndex = yIndex;
        }

        /**
         * Get image.
         *
         * @return image
         */
        public BufferedImage getImage() {
            return this.image;
        }

        /**
         * Get x index.
         *
         * @return x index
         */
        public int getxIndex() {
            return this.xIndex;
        }

        /**
         * Get y index.
         *
         * @return y index
         */
        public int getyIndex() {
            return this.yIndex;
        }
    }
}
