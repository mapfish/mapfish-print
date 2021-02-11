package org.mapfish.print.map.tiled.wms;
import org.mapfish.print.parser.HasDefaultValue;

import org.locationtech.jts.util.Assert;
import org.mapfish.print.map.image.wms.WmsLayerParam;

import java.awt.Dimension;
import java.net.URISyntaxException;

/**
 * The parameters for configuration a Tiled WMS layer.
 * <p>
 * What is meant by a "tiled wms layer" is a layer based on a WMS layer but instead of a single large image
 * for the layer multiple wms requests are made and the resulting images are combined as tiles.
 * </p>
 */
public final class TiledWmsLayerParam extends WmsLayerParam {
    /**
     * A two element array of integers indicating the x and y size of each tile.
     */
    public int[] tileSize;

    /**
     * A two element array of integers indicating the width and height tile buffer.
     */
    @HasDefaultValue
    public int[] tileBufferSize = new int[] {0, 0};

    /**
     * Constructor.
     */
    public TiledWmsLayerParam() {
        super();
    }

    /**
     * Convert WmsLayerParam into it's tiled equivalent.
     *
     * @param params the WMS parameters to convert
     * @param tileSize The size of the tiles
     */
    public TiledWmsLayerParam(final WmsLayerParam params, final Dimension tileSize, final int tileBufferWidth, final int tileBufferHeight) {
        super(params);
        this.tileSize = new int[]{tileSize.width, tileSize.height};
        this.tileBufferSize = new int[]{tileBufferWidth, tileBufferHeight};
    }

    @Override
    public void postConstruct() throws URISyntaxException {
        super.postConstruct();
        Assert.isTrue(this.tileSize.length == 2,
                      "The tileSize parameter must have exactly two elements, x,y tile size.  " +
                              "Actual number of elements was: " + this.tileSize.length);
    }

    public Dimension getTileSize() {
        return new Dimension(this.tileSize[0], this.tileSize[1]);
    }

    public int getTileBufferWidth() {
    	return this.tileBufferSize[0];
    }

    public int getTileBufferHeight() {
    	return this.tileBufferSize[1];
    }

}
