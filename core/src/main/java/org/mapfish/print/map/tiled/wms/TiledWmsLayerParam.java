package org.mapfish.print.map.tiled.wms;

import com.vividsolutions.jts.util.Assert;

import org.mapfish.print.map.image.wms.WmsLayerParam;

import java.awt.Dimension;
import java.net.URISyntaxException;

/**
 * The parameters for configuration a Tiled WMS layer.
 * <p>
 * What is meant by a "tiled wms layer" is a layer based on a WMS layer but instead of a single large image for the layer multiple wms
 * requests are made and the resulting images are combined as tiles.
 * </p>
 */
public final class TiledWmsLayerParam extends WmsLayerParam {
    /**
     * A two element array of integers indicating the x and y size of each tile.
     */
    public int[] tileSize;

    @Override
    public void postConstruct() throws URISyntaxException {
        super.postConstruct();
        Assert.isTrue(this.tileSize.length == 2, "The tileSize parameter must have exactly two elements, x,y tile size.  " +
                                                 "Actual number of elements was: " + this.tileSize.length);
    }

    public Dimension getTileSize() {
        return new Dimension(this.tileSize[0], this.tileSize[1]);
    }

}
