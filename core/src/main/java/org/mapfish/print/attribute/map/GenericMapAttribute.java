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
import org.mapfish.print.parser.HasDefaultValue;
import org.mapfish.print.parser.MapfishParser;
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
 * Generic attributes for {@link org.mapfish.print.processor.map.CreateMapProcessor} and
 * {@link org.mapfish.print.processor.map.CreateOverviewMapProcessor}.
 * @param <GenericMapAttributeValues>
 */
public abstract class GenericMapAttribute<GenericMapAttributeValues>
        extends ReflectiveAttribute<GenericMapAttribute<?>.GenericMapAttributeValues> {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(GenericMapAttribute.class);

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private MapfishParser mapfishJsonParser;

    private Double maxDpi = null;
    private ZoomLevels zoomLevels = null;
    private Double zoomSnapTolerance = null;
    private ZoomLevelSnapStrategy zoomLevelSnapStrategy = null;

    private Integer width = null;
    private Integer height = null;

    public final Double getMaxDpi() {
        return this.maxDpi;
    }

    public final void setMaxDpi(final Double maxDpi) {
        this.maxDpi = maxDpi;
    }

    public final Integer getWidth() {
        return this.width;
    }

    public final void setWidth(final Integer width) {
        this.width = width;
    }

    public final Integer getHeight() {
        return this.height;
    }

    public final void setHeight(final Integer height) {
        this.height = height;
    }

    public final void setZoomLevels(final ZoomLevels zoomLevels) {
        this.zoomLevels = zoomLevels;
    }

    public final void setZoomSnapTolerance(final Double zoomSnapTolerance) {
        this.zoomSnapTolerance = zoomSnapTolerance;
    }

    public final void setZoomLevelSnapStrategy(final ZoomLevelSnapStrategy zoomLevelSnapStrategy) {
        this.zoomLevelSnapStrategy = zoomLevelSnapStrategy;
    }

    //CSOFF: DesignForExtension
    @Override
    public void validate(final List<Throwable> validationErrors) {
    //CSON: DesignForExtension
        if (this.width == null || this.width < 1) {
            validationErrors.add(new ConfigurationException("width field is not legal: " + this.width + " in " + getClass().getName()));
        }

        if (this.height == null || this.height < 1) {
            validationErrors.add(new ConfigurationException("height field is not legal: " + this.height + " in " + getClass().getName()));
        }

        if (this.getMaxDpi() == null || this.getMaxDpi() < 1) {
            validationErrors.add(
                    new ConfigurationException("maxDpi field is not legal: " + this.getMaxDpi() + " in " + getClass().getName()));
        }
    }

    /**
     * The value of {@link GenericMapAttribute}.
     */
    public abstract class GenericMapAttributeValues {
        private static final String TYPE = "type";
        private final Dimension mapSize;
        private final Template template;
        private List<MapLayer> mapLayers;

        /**
         * The projection of the map.
         */
        @HasDefaultValue
        public String projection = null;
        /**
         * The rotation of the map.
         */
        @HasDefaultValue
        public Double rotation = null;
        /**
         * Indicates if the map should adjust its scale/zoom level to be equal to one of those defined in the configuration file.
         * <p/>
         *
         * @see #isUseNearestScale()
         */
        @HasDefaultValue
        public Boolean useNearestScale = null;

        /**
         * Indicates if the map should adjust its bounds.
         * <p/>
         *
         * @see #isUseAdjustBounds()
         */
        @HasDefaultValue
        public Boolean useAdjustBounds = null;

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
        public GenericMapAttributeValues(final Template template, final Dimension mapSize) {
            this.template = template;
            this.mapSize = mapSize;
        }
        
        /**
         * Validate the values provided by the request data and construct MapBounds and parse the layers.
         */
        //CSOFF: DesignForExtension
        public void postConstruct() throws FactoryException {
        //CSON: DesignForExtension
            this.mapLayers = parseLayers();
        }

        private List<MapLayer> parseLayers() {
            List<MapLayer> layerList = Lists.newArrayList();

            for (int i = 0; i < this.getRawLayers().size(); i++) {
                try {
                    PObject layer = this.getRawLayers().getObject(i);
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
                    GenericMapAttribute.this.applicationContext.getBeansOfType(MapLayerFactoryPlugin.class);
            for (MapLayerFactoryPlugin layerParser : layerParsers.values()) {
                final boolean layerApplies = layerParser.getTypeNames().contains(layer.getString(TYPE).toLowerCase());
                if (layerApplies) {
                    Object param = layerParser.createParameter();

                    GenericMapAttribute.this.mapfishJsonParser.parse(this.template.getConfiguration()
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
            for (MapLayerFactoryPlugin<?> mapLayerFactoryPlugin : layerParsers.values()) {
                for (Object name : mapLayerFactoryPlugin.getTypeNames()) {
                    message.append("\n");
                    message.append("\t* ").append(name);
                }
            }

            throw new IllegalArgumentException(message.toString());
        }

        /**
         * Parse the projection from a string.
         * @return the crs
         */
        protected final CoordinateReferenceSystem parseProjection() {
            String epsgCode = getProjection();
            if (epsgCode.equalsIgnoreCase("EPSG:900913")) {
                epsgCode = "EPSG:3857";
            }

            try {
                if (this.longitudeFirst == null) {
                    return CRS.decode(epsgCode);
                } else {
                    return CRS.decode(epsgCode, this.longitudeFirst);
                }
            } catch (NoSuchAuthorityCodeException e) {
                throw new RuntimeException(epsgCode + "was not recognized as a crs code", e);
            } catch (FactoryException e) {
                throw new RuntimeException("Error occurred while parsing: " + epsgCode, e);
            }
        }

        /**
         * Return the DPI value for the map.
         * This method is abstract because the dpi value is optional for the overview map,
         * but must be given for the normal map. So, in the overview map the field is defined
         * with a @HasDefaultValue annotation.
         */
        public abstract Double getDpi();
        
        /**
         * Return the JSON layer definiton.
         * This method is abstract is abstract for the same reasons as {@link #getDpi()}.
         */
        protected abstract PArray getRawLayers();

        //CSOFF: DesignForExtension
        public List<MapLayer> getLayers() {
        //CSON: DesignForExtension
            return Lists.newArrayList(this.mapLayers);
        }

        public final Template getTemplate() {
            return this.template;
        }

        public final Dimension getMapSize() {
            return this.mapSize;
        }

        //CSOFF: DesignForExtension
        public Double getRotation() {
        //CSON: DesignForExtension
            return this.rotation;
        }

        //CSOFF: DesignForExtension
        public String getProjection() {
        //CSON: DesignForExtension
            return this.projection;
        }

        /**
         * Return true if requestData has useNearestScale and configuration has some zoom levels defined.
         */
        //CSOFF: DesignForExtension
        public Boolean isUseNearestScale() {
        //CSON: DesignForExtension
            return this.useNearestScale && GenericMapAttribute.this.zoomLevels != null;
        }

        /**
         * Return true if requestData has useNearestScale and configuration has some zoom levels defined.
         */
        //CSOFF: DesignForExtension
        public Boolean isUseAdjustBounds() {
        //CSON: DesignForExtension
            return this.useAdjustBounds;
        }

        //CSOFF: DesignForExtension
        public ZoomLevels getZoomLevels() {
        //CSON: DesignForExtension
            return GenericMapAttribute.this.zoomLevels;
        }

        //CSOFF: DesignForExtension
        public Double getZoomSnapTolerance() {
        //CSON: DesignForExtension
            return GenericMapAttribute.this.zoomSnapTolerance;
        }

        //CSOFF: DesignForExtension
        public ZoomLevelSnapStrategy getZoomLevelSnapStrategy() {
        //CSON: DesignForExtension
            return GenericMapAttribute.this.zoomLevelSnapStrategy;
        }

        //CSOFF: DesignForExtension
        public double getRequestorDPI() {
        //CSON: DesignForExtension
            // We are making the same assumption as Openlayers 2.x versions, that the DPI is 72.
            // In the future we probably need to change this assumption and allow the client software to
            // specify the DPI they are using for creating the bounds.
            // For the moment we require the client to convert their bounds to 72 DPI
            return Constants.PDF_DPI;
        }
        
    }
}
