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

package org.mapfish.print.attribute.map;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.geotools.referencing.CRS;
import org.mapfish.print.Constants;
import org.mapfish.print.attribute.ReflectiveAttribute;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.MapLayerFactoryPlugin;
import org.mapfish.print.map.Scale;
import org.mapfish.print.parser.HasDefaultValue;
import org.mapfish.print.parser.MapfishParser;
import org.mapfish.print.parser.OneOf;
import org.mapfish.print.parser.Requires;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.PObject;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.awt.Dimension;
import java.util.List;
import java.util.Map;

/**
 * The attributes for {@link org.mapfish.print.processor.map.CreateMapProcessor}.
 */
public final class MapAttribute extends ReflectiveAttribute<MapAttribute.MapAttributeValues> {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(MapAttribute.class);
    private static final double DEFAULT_SNAP_TOLERANCE = 0.05;

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private MapfishParser mapfishJsonParser;

    private double maxDpi;
    private ZoomLevels zoomLevels;
    private double zoomSnapTolerance = DEFAULT_SNAP_TOLERANCE;
    private ZoomLevelSnapStrategy zoomLevelSnapStrategy;

    private int width;
    private int height;

    @Override
    public MapAttributeValues createValue(final Template template) {
        MapAttributeValues result = new MapAttributeValues(template, new Dimension(this.width, this.height));
        return result;
    }

    public void setMaxDpi(final double maxDpi) {
        this.maxDpi = maxDpi;
    }

    public void setWidth(final int width) {
        this.width = width;
    }

    public void setHeight(final int height) {
        this.height = height;
    }

    public void setZoomLevels(final ZoomLevels zoomLevels) {
        this.zoomLevels = zoomLevels;
    }

    public void setZoomSnapTolerance(final double zoomSnapTolerance) {
        this.zoomSnapTolerance = zoomSnapTolerance;
    }

    public void setZoomLevelSnapStrategy(final ZoomLevelSnapStrategy zoomLevelSnapStrategy) {
        this.zoomLevelSnapStrategy = zoomLevelSnapStrategy;
    }

    @Override
    public void validate(final List<Throwable> validationErrors) {
        if (this.width < 1) {
            validationErrors.add(new ConfigurationException("width field is not legal: " + this.width + " in " + getClass().getName()));
        }

        if (this.height < 1) {
            validationErrors.add(new ConfigurationException("height field is not legal: " + this.height + " in " + getClass().getName()));
        }

        if (this.maxDpi < 1) {
            validationErrors.add(new ConfigurationException("maxDpi field is not legal: " + this.maxDpi + " in " + getClass().getName()));
        }
    }

    /**
     * The value of {@link MapAttribute}.
     */
    public final class MapAttributeValues {
        private static final String TYPE = "type";
        private final Dimension mapSize;
        private final Template template;
        private MapBounds mapBounds;
        private List<MapLayer> mapLayers;
        /**
         * An array of 4 doubles, minX, minY, maxX, maxY.  The bounding box of the map.
         * <p/>
         * Either the bbox or the center + scale must be defined
         * <p/>
         */
        @OneOf("MapBounds")
        public double[] bbox;
        /**
         * An array of 2 doubles, (x, y).  The center of the map.
         */
        @Requires("scale")
        @OneOf("MapBounds")
        public double[] center;
        /**
         * If center is defined then this is the scale of the map centered at center.
         */
        @HasDefaultValue
        public double scale;
        /**
         * The projection of the map.
         */
        @HasDefaultValue
        public String projection = "EPSG:3857";
        /**
         * The rotation of the map.
         */
        @HasDefaultValue
        public double rotation = 0;
        /**
         * The json with all the layer information.  This will be parsed in postConstruct into a list of layers and
         * therefore this field should not normally be accessed.
         */
        public PArray layers;
        /**
         * Indicates if the map should adjust its scale/zoom level to be equal to one of those defined in the configuration file.
         * <p/>
         *
         * @see #isUseNearestScale()
         */
        @HasDefaultValue
        public boolean useNearestScale = true;

        /**
         * Indicates if the map should adjust its bounds.
         * <p/>
         *
         * @see #isUseAdjustBounds()
         */
        @HasDefaultValue
        public boolean useAdjustBounds = false;

        /**
         * The output dpi of the printed map.
         */
        public double dpi;
        /**
         * By default the normal axis order as specified in EPSG code will be used when parsing projections.  However
         * the requestor can override this by explicitly declaring that longitude axis is first.
         */
        @HasDefaultValue
        public Boolean longitudeFirst = null;

        /**
         * Constructor.
         *
         * @param template the template this map is part of.
         * @param mapSize  the size of the map.
         */
        public MapAttributeValues(final Template template, final Dimension mapSize) {
            this.template = template;
            this.mapSize = mapSize;
        }

        /**
         * Validate the values provided by the request data and construct MapBounds and parse the layers.
         */
        public void postConstruct() throws FactoryException {
            if (this.dpi > MapAttribute.this.maxDpi) {
                throw new IllegalArgumentException("dpi parameter was " + this.dpi + " must be limited to " + MapAttribute.this.maxDpi
                                                   + ".  The path to the parameter is: " + this.dpi);
            }

            this.mapBounds = parseBounds();
            this.mapLayers = parseLayers();
        }

        private List<MapLayer> parseLayers() {
            List<MapLayer> layerList = Lists.newArrayList();

            for (int i = 0; i < this.layers.size(); i++) {
                try {
                    PObject layer = this.layers.getObject(i);
                    parseSingleLayer(layerList, layer);
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }

            return layerList;
        }

        @SuppressWarnings("unchecked")
        private void parseSingleLayer(final List<MapLayer> layerList,
                                      final PObject layer) throws Throwable {

            final Map<String, MapLayerFactoryPlugin> layerParsers =
                    MapAttribute.this.applicationContext.getBeansOfType(MapLayerFactoryPlugin.class);
            for (MapLayerFactoryPlugin layerParser : layerParsers.values()) {
                final boolean layerApplies = layerParser.getTypeNames().contains(layer.getString(TYPE).toLowerCase());
                if (layerApplies) {
                    Object param = layerParser.createParameter();

                    MapAttribute.this.mapfishJsonParser.parse(this.template.getConfiguration()
                                    .isThrowErrorOnExtraParameters(),
                            layer, param, TYPE
                    );

                    final MapLayer newLayer = layerParser.parse(this.template, param);
                    if (layerList.isEmpty()) {
                        layerList.add(newLayer);
                    } else {
                        final int lastLayerIndex = layerList.size() - 1;
                        final MapLayer lastLayer = layerList.get(lastLayerIndex);
                        Optional<MapLayer> combinedLayer = lastLayer.tryAddLayer(newLayer);
                        if (combinedLayer.isPresent()) {
                            layerList.remove(lastLayerIndex);
                            layerList.add(lastLayerIndex, combinedLayer.get());
                        } else {
                            layerList.add(newLayer);
                        }
                    }
                    return;
                }
            }

            StringBuilder message = new StringBuilder("\nLayer with type: '" + layer.getString(TYPE) + "' is not currently " +
                                                      "supported.  Options include: ");
            for (MapLayerFactoryPlugin mapLayerFactoryPlugin : layerParsers.values()) {
                for (Object name : mapLayerFactoryPlugin.getTypeNames()) {
                    message.append("\n");
                    message.append("\t* ").append(name);
                }
            }

            throw new IllegalArgumentException(message.toString());
        }

        private CoordinateReferenceSystem parseProjection() {
            if (this.projection.equalsIgnoreCase("EPSG:900913")) {
                this.projection = "EPSG:3857";
            }

            try {
                if (this.longitudeFirst == null) {
                    return CRS.decode(this.projection);
                } else {
                    return CRS.decode(this.projection, this.longitudeFirst);
                }
            } catch (NoSuchAuthorityCodeException e) {
                throw new RuntimeException(this.projection + "was not recognized as a crs code", e);
            } catch (FactoryException e) {
                throw new RuntimeException("Error occurred while parsing: " + this.projection, e);
            }
        }

        //
        private MapBounds parseBounds() throws FactoryException {
            final CoordinateReferenceSystem crs = parseProjection();
            if (this.center != null && this.bbox != null) {
                throw new IllegalArgumentException("Cannot have both center and bbox defined");
            }
            MapBounds bounds;
            if (this.center != null) {
                double centerX = this.center[0];
                double centerY = this.center[1];
                Scale scaleObject = new Scale(this.scale);

                bounds = new CenterScaleMapBounds(crs, centerX, centerY, scaleObject);
            } else if (this.bbox != null) {
                final int maxYIndex = 3;
                double minX = this.bbox[0];
                double minY = this.bbox[1];
                double maxX = this.bbox[2];
                double maxY = this.bbox[maxYIndex];
                bounds = new BBoxMapBounds(crs, minX, minY, maxX, maxY);
            } else {
                throw new IllegalArgumentException("Expected either center and scale or bbox for the map bounds");
            }
            return bounds;
        }

        public MapBounds getMapBounds() {
            return this.mapBounds;
        }

        public List<MapLayer> getLayers() {
            return Lists.newArrayList(this.mapLayers);
        }

        public Dimension getMapSize() {
            return this.mapSize;
        }

        public double getDpi() {
            return this.dpi;
        }

        public double getRotation() {
            return this.rotation;
        }

        /**
         * Return true if requestData has useNearestScale and configuration has some zoom levels defined.
         *
         * @return
         */
        public boolean isUseNearestScale() {
            return this.useNearestScale && MapAttribute.this.zoomLevels != null;
        }

        /**
         * Return true if requestData has useNearestScale and configuration has some zoom levels defined.
         *
         * @return
         */
        public boolean isUseAdjustBounds() {
            return this.useAdjustBounds;
        }

        public ZoomLevels getZoomLevels() {
            return MapAttribute.this.zoomLevels;
        }

        public double getZoomSnapTolerance() {
            return MapAttribute.this.zoomSnapTolerance;
        }

        public ZoomLevelSnapStrategy getZoomLevelSnapStrategy() {
            return MapAttribute.this.zoomLevelSnapStrategy;
        }

        public double getRequestorDPI() {
            // We are making the same assumption as Openlayers 2.x versions, that the DPI is 72.
            // In the future we probably need to change this assumption and allow the client software to
            // specify the DPI they are using for creating the bounds.
            // For the moment we require the client to convert their bounds to 72 DPI
            return Constants.PDF_DPI;
        }
    }
}
