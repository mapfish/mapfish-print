package org.mapfish.print.attribute.map;

import com.google.common.base.Function;
import com.vividsolutions.jts.geom.Envelope;

import org.json.JSONArray;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.attribute.map.OverviewMapAttribute.OverviewMapAttributeValues;
import org.mapfish.print.attribute.map.ZoomToFeatures.ZoomType;
import org.mapfish.print.config.Template;
import org.mapfish.print.parser.CanSatisfyOneOf;
import org.mapfish.print.parser.HasDefaultValue;
import org.mapfish.print.parser.OneOf;
import org.mapfish.print.parser.Requires;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.json.PJsonArray;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * <p>The attributes for {@link org.mapfish.print.processor.map.CreateMapProcessor} (see
 * <a href="processors.html#!createMap">!createMap</a> processor).</p>
 * [[examples=verboseExample]]
 */
public final class MapAttribute extends GenericMapAttribute {

    private static final double DEFAULT_SNAP_TOLERANCE = 0.05;
    private static final ZoomLevelSnapStrategy DEFAULT_SNAP_STRATEGY = ZoomLevelSnapStrategy.CLOSEST_LOWER_SCALE_ON_TIE;
    private static final boolean DEFAULT_SNAP_GEODETIC = false;

    @SuppressWarnings("unchecked")
    @Override
    public Class<MapAttributeValues> getValueType() {
        return MapAttributeValues.class;
    }

    @Override
    public MapAttributeValues createValue(final Template template) {
        return new MapAttributeValues(template, getWidth(), getHeight());
    }

    /**
     * The value of {@link MapAttribute}.
     */
    public class MapAttributeValues extends GenericMapAttribute.GenericMapAttributeValues {

        private static final boolean DEFAULT_ADJUST_BOUNDS = false;
        private static final double DEFAULT_ROTATION = 0.0;

        private MapBounds mapBounds;

        /**
         * An array of 4 doubles, minX, minY, maxX, maxY.  The bounding box of the map.
         * <p></p>
         * Either the bbox or the center + scale must be defined
         * <p></p>
         */
        @OneOf("MapBounds")
        public double[] bbox;
        /**
         * A GeoJSON geometry that is essentially the area of the area to draw on the map.
         * <p></p>
         */
        @CanSatisfyOneOf("MapBounds")
        @HasDefaultValue
        public AreaOfInterest areaOfInterest;
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
        public Double scale;

        /**
         * Zoom the map to the features of a specific layer or all features of the map.
         */
        @OneOf("MapBounds")
        public ZoomToFeatures zoomToFeatures;

        /**
         * The json with all the layer information.  This will be parsed in postConstruct into a list of layers and
         * therefore this field should not normally be accessed.
         *
         * The first layer in the array will be the top layer in the map.  The last layer in the array will be the bottom
         * layer in the map.  There for the last layer will be hidden by the first layer (where not transparent).
         */
        @HasDefaultValue
        public PArray layers = new PJsonArray(null, new JSONArray(), null);

        /**
         * The output dpi of the printed map.
         */
        public double dpi;

        /**
         * Constructor.
         *
         * @param template the template this map is part of.
         * @param width the width of the map.
         * @param height the height of the map.
         */
        public MapAttributeValues(final Template template, final Integer width, final Integer height) {
            super(template, width, height);
        }

        /**
         * Constructor.
         *
         * @param template the template this map is part of.
         */
        public MapAttributeValues(final Template template) {
            super(template);
        }

        @Override
        public Double getDpi() {
            return this.dpi;
        }

        @Override
        public final PArray getRawLayers() {
            return this.layers;
        }

        @Override
        public void setRawLayers(final PArray newLayers) {
            this.layers = newLayers;
        }

        @Override
        public final void postConstruct() throws FactoryException {
            super.postConstruct();

            if (getDpi() > getMaxDpi()) {
                throw new IllegalArgumentException(
                        "dpi parameter was " + getDpi() + " must be limited to " + getMaxDpi()
                        + ".  The path to the parameter is: " + getDpi());
            }

            if (this.zoomToFeatures != null) {
                if (this.zoomToFeatures.zoomType == ZoomType.CENTER) {
                    if (this.scale == null && this.zoomToFeatures.minScale == null) {
                        throw new IllegalArgumentException(
                                "When using 'zoomToFeatures.zoom.Type: center' either 'scale' " +
                                "or 'zoomToFeatures.minScale' has to be given.");
                    }
                } else if (this.zoomToFeatures.zoomType == ZoomType.EXTENT
                            && this.zoomToFeatures.minScale == null) {
                    throw new IllegalArgumentException(
                            "When using 'zoomToFeatures.zoom.Type: extent' 'zoomToFeatures.minScale'" +
                            "has to be given.");
                }
            }

            this.mapBounds = parseBounds();
        }

        private MapBounds parseBounds() throws FactoryException {
            final CoordinateReferenceSystem crs = parseProjection();
            if (this.center != null && this.bbox != null) {
                throw new IllegalArgumentException("Cannot have both center and bbox defined");
            }
            MapBounds bounds;
            if (this.center != null) {
                double centerX = this.center[0];
                double centerY = this.center[1];

                bounds = new CenterScaleMapBounds(crs, centerX, centerY, this.scale);
            } else if (this.bbox != null) {
                final int maxYIndex = 3;
                double minX = this.bbox[0];
                double minY = this.bbox[1];
                double maxX = this.bbox[2];
                double maxY = this.bbox[maxYIndex];
                bounds = new BBoxMapBounds(crs, minX, minY, maxX, maxY);
            } else if (this.areaOfInterest != null) {
                Envelope area = this.areaOfInterest.getArea().getEnvelopeInternal();
                bounds = new BBoxMapBounds(crs, area);
            } else if (this.zoomToFeatures != null) {
                bounds = new BBoxMapBounds(crs, 0, 0, 10, 10);
            } else {
                throw new IllegalArgumentException("Expected either: center and scale, bbox, or an " +
                        "areaOfInterest defined in order to calculate the map bounds");
            }
            return bounds;
        }

        public MapBounds getMapBounds() {
            return this.mapBounds;
        }

        public void setMapBounds(final MapBounds mapBounds) {
            this.mapBounds = mapBounds;
        }

        /**
         * Recalculate the bounds after center or bounds have changed.
         */
        public void recalculateBounds() {
            try {
                this.mapBounds = parseBounds();
            } catch (FactoryException e) {
                throw ExceptionUtils.getRuntimeException(e);
            }
        }

        @Override
        public String getProjection() {
            return getValueOr(super.getProjection(), DEFAULT_PROJECTION);
        }

        @Override
        public Double getZoomSnapTolerance() {
            return getValueOr(super.getZoomSnapTolerance(), DEFAULT_SNAP_TOLERANCE);
        }

        @Override
        public ZoomLevelSnapStrategy getZoomLevelSnapStrategy() {
            return getValueOr(super.getZoomLevelSnapStrategy(), DEFAULT_SNAP_STRATEGY);
        }

        @Override
        public Boolean getZoomSnapGeodetic() {
            return getValueOr(super.getZoomSnapGeodetic(), DEFAULT_SNAP_GEODETIC);
        }

        @Override
        public Double getRotation() {
            return getValueOr(super.getRotation(), DEFAULT_ROTATION);
        }

        @Override
        public Boolean isUseNearestScale() {
            return (this.useNearestScale == null || this.useNearestScale)
                        && getZoomLevels() != null;
        }

        @Override
        public Boolean isUseAdjustBounds() {
            return getValueOr(super.isUseAdjustBounds(), DEFAULT_ADJUST_BOUNDS);
        }

        /**
         * Creates an {@link org.mapfish.print.attribute.map.MapAttribute.OverriddenMapAttributeValues} instance with the current object
         * and a given {@link org.mapfish.print.attribute.map.OverviewMapAttribute.OverviewMapAttributeValues} instance.
         *
         * @param paramOverrides Attributes set in this instance will override attributes in
         *  the current instance.
         */
        public final OverriddenMapAttributeValues getWithOverrides(
                final OverviewMapAttributeValues paramOverrides) {
            return new OverriddenMapAttributeValues(this, paramOverrides, getTemplate());
        }

        /**
         * Create a copy of this instance. Should be overridden for each subclass.
         * @param width the width of the new map attribute values to create
         * @param height the height of the new map attribute values to create
         * @param updater a function which will be called after copy is made but before postConstruct is called in order
         *                to do other configuration changes.
         */
        public MapAttribute.MapAttributeValues copy(final int width, final int height,
                                                    @Nonnull final Function<MapAttributeValues, Void> updater) {
            MapAttributeValues copy = new MapAttributeValues(getTemplate(), width, height);
            copy.areaOfInterest = this.areaOfInterest.copy();
            copy.bbox = this.bbox;
            copy.center = this.center;
            copy.scale = this.scale;
            copy.layers = this.layers;
            copy.dpi = this.getDpi();
            copy.projection = this.getProjection();
            copy.rotation = this.getRotation();
            copy.useNearestScale = this.isUseNearestScale();
            copy.useAdjustBounds = this.useAdjustBounds;
            copy.longitudeFirst = this.longitudeFirst;
            copy.zoomToFeatures = (this.zoomToFeatures == null) ? null : this.zoomToFeatures.copy();
            updater.apply(copy);
            try {
                copy.postConstruct();
            } catch (FactoryException e) {
                throw new RuntimeException(e);
            }
            return copy;
        }
    }

    /**
     * A wrapper around a {@link MapAttributeValues} instance and an
     * {@link org.mapfish.print.attribute.map.OverviewMapAttribute.OverviewMapAttributeValues} instance,
     * which is used to render the overview map.
     *
     * If attributes on the {@link org.mapfish.print.attribute.map.OverviewMapAttribute.OverviewMapAttributeValues} instance are set, those
     * attributes will be returned, otherwise the ones on {@link MapAttributeValues}.
     */
    public class OverriddenMapAttributeValues extends MapAttributeValues {

        private final MapAttributeValues params;
        private final OverviewMapAttributeValues paramOverrides;
        private MapBounds zoomedOutBounds = null;
        private MapLayer mapExtentLayer = null;

        /**
         * Constructor.
         * @param params The fallback parameters.
         * @param paramOverrides The parameters explicitly defined for the overview map.
         * @param template The template this map is part of.
         */
        public OverriddenMapAttributeValues(
                final MapAttributeValues params,
                final OverviewMapAttributeValues paramOverrides,
                final Template template) {
            super(template, paramOverrides.getWidth(), paramOverrides.getHeight());
            this.params = params;
            this.paramOverrides = paramOverrides;
        }

        /**
         * The bounds used to render the overview-map.
         */
        @Override
        public final MapBounds getMapBounds() {
            return this.zoomedOutBounds;
        }

        /**
         * Custom bounds for the overview-map. Overwrites the bounds of the original map.
         */
        public final MapBounds getCustomBounds() {
            return this.paramOverrides.getMapBounds();
        }

        /**
         * The bounds of the original map that this overview-map is associated to.
         */
        public final MapBounds getOriginalBounds() {
            return this.params.getMapBounds();
        }

        public final void setZoomedOutBounds(final MapBounds zoomedOutBounds) {
            this.zoomedOutBounds = zoomedOutBounds;
        }

        @Override
        public final Double getDpi() {
            return getValueOr(this.paramOverrides.getDpi(), this.params.getDpi());
        }

        @Override
        public final Double getZoomSnapTolerance() {
            return getValueOr(this.paramOverrides.getZoomSnapTolerance(), this.params.getZoomSnapTolerance());
        }

        @Override
        public final ZoomLevelSnapStrategy getZoomLevelSnapStrategy() {
            return getValueOr(this.paramOverrides.getZoomLevelSnapStrategy(), this.params.getZoomLevelSnapStrategy());
        }

        @Override
        public final ZoomLevels getZoomLevels() {
            return getValueOr(this.paramOverrides.getZoomLevels(), this.params.getZoomLevels());
        }

        @Override
        public final Double getRotation() {
            return getValueOr(this.paramOverrides.getRotation(), this.params.getRotation());
        }

        @Override
        public final Boolean isUseAdjustBounds() {
            return getValueOr(this.paramOverrides.isUseAdjustBounds(), this.params.isUseAdjustBounds());
        }

        @Override
        public final Boolean isUseNearestScale() {
            final Boolean useNearestScale = getValueOr(this.paramOverrides.useNearestScale, this.params.useNearestScale);
            return (useNearestScale == null || useNearestScale)
                    && getZoomLevels() != null;
        }

        public final void setMapExtentLayer(final MapLayer mapExtentLayer) {
            this.mapExtentLayer = mapExtentLayer;
        }

        @Override
        public final List<MapLayer> getLayers() {
            // return the layers together with a layer for the bbox rectangle of the map
            List<MapLayer> layers = new ArrayList<>();
            if (this.mapExtentLayer != null) {
                layers.add(this.mapExtentLayer);
            }

            if (!this.paramOverrides.getLayers().isEmpty()) {
                layers.addAll(this.paramOverrides.getLayers());
            } else {
                layers.addAll(this.params.getLayers());
            }

            return layers;
        }
    }
}
