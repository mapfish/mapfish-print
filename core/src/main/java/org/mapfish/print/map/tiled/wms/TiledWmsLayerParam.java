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

package org.mapfish.print.map.tiled.wms;

import com.vividsolutions.jts.util.Assert;

import org.mapfish.print.map.image.wms.WmsLayerParam;
import org.mapfish.print.parser.HasDefaultValue;

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

    /**
     * The format of the image. for example image/png, image/jpeg, etc...
     */
    @HasDefaultValue
    public String imageFormat = "image/png";

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
