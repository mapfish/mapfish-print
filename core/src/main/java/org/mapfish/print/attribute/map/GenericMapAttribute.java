package org.mapfish.print.attribute.map;

import org.geotools.referencing.CRS;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapfish.print.Constants;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.attribute.ReflectiveAttribute;
import org.mapfish.print.config.Configuration;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generic attributes for {@link org.mapfish.print.processor.map.CreateMapProcessor} and {@link
 * org.mapfish.print.processor.map.CreateOverviewMapProcessor}.
 */
public abstract class GenericMapAttribute
        extends ReflectiveAttribute<GenericMapAttribute.GenericMapAttributeValues> {

    /**
     * The json key for the suggested DPI values in the client config.
     */
    public static final String JSON_DPI_SUGGESTIONS = "dpiSuggestions";
    /**
     * The json key for the max DPI value in the client config.
     */
    public static final String JSON_MAX_DPI = "maxDPI";
    /**
     * The json key for the width of the map in the client config.
     */
    public static final String JSON_MAP_WIDTH = "width";
    /**
     * The json key for the height of the map in the client config.
     */
    public static final String JSON_MAP_HEIGHT = "height";
    /**
     * The json key for the max width of the map in the client config (for mapExport).
     */
    public static final String JSON_MAX_WIDTH = "maxWidth";
    /**
     * The json key for the max height of the map in the client config (for mapExport).
     */
    public static final String JSON_MAX_HEIGHT = "maxHeight";
    static final String JSON_ZOOM_LEVEL_SUGGESTIONS = "scales";
    private static final double[] DEFAULT_DPI_VALUES = {72, 120, 200, 254, 300, 600, 1200, 2400};
    @Autowired
    private ApplicationContext applicationContext;

    private Double maxDpi = null;
    private double[] dpiSuggestions = null;
    private ZoomLevels zoomLevels = null;
    private Double zoomSnapTolerance = null;
    private ZoomLevelSnapStrategy zoomLevelSnapStrategy = null;
    private Boolean zoomSnapGeodetic = null;

    private Integer width = null;
    private Integer height = null;

    private Integer maxWidth = null;
    private Integer maxHeight = null;

    /**
     * Parse the given projection.
     *
     * @param projection The projection string.
     * @param longitudeFirst longitudeFirst
     */
    public static CoordinateReferenceSystem parseProjection(
            final String projection, final Boolean longitudeFirst) {
        try {
            if (longitudeFirst == null) {
                return CRS.decode(projection);
            } else {
                return CRS.decode(projection, longitudeFirst);
            }
        } catch (NoSuchAuthorityCodeException e) {
            throw new RuntimeException(projection + " was not recognized as a crs code", e);
        } catch (FactoryException e) {
            throw new RuntimeException("Error occurred while parsing: " + projection, e);
        }
    }

    public final Double getMaxDpi() {
        return this.maxDpi;
    }

    /**
     * The maximum DPI allowed for maps.
     * <p>
     * If a request is made with a higher DPI, the request fails.
     *
     * @param maxDpi the value
     */
    public final void setMaxDpi(final Double maxDpi) {
        this.maxDpi = maxDpi;
    }

    /**
     * Get DPI suggestions.
     *
     * @return DPI suggestions
     */
    public final double[] getDpiSuggestions() {
        if (this.dpiSuggestions == null) {
            List<Double> list = new ArrayList<>();
            for (double suggestion: DEFAULT_DPI_VALUES) {
                if (suggestion <= this.maxDpi) {
                    list.add(suggestion);
                }
            }
            double[] suggestions = new double[list.size()];
            for (int i = 0; i < suggestions.length; i++) {
                suggestions[i] = list.get(i);
            }
            return suggestions;
        }
        return this.dpiSuggestions;
    }

    /**
     * Suggestions for DPI values to use. Typically these are used by the client to create a UI for a user.
     *
     * @param dpiSuggestions DPI suggestions
     */
    public final void setDpiSuggestions(final double[] dpiSuggestions) {
        this.dpiSuggestions = dpiSuggestions;
    }

    public final Integer getWidth() {
        return this.width;
    }

    /**
     * The width of the map in pixels. This value should match the width of the sub-report in the JasperReport
     * template.
     *
     * @param width Width
     */
    public final void setWidth(final Integer width) {
        this.width = width;
    }

    public final Integer getHeight() {
        return this.height;
    }

    /**
     * The height of the map in pixels. This value should match the height of the sub-report in the
     * JasperReport template.
     *
     * @param height Height
     */
    public final void setHeight(final Integer height) {
        this.height = height;
    }

    public final Integer getMaxWidth() {
        return this.maxWidth;
    }

    /**
     * Limits the maximum width of the map.
     *
     * @param maxWidth the value
     */
    public final void setMaxWidth(final Integer maxWidth) {
        this.maxWidth = maxWidth;
    }

    public final Integer getMaxHeight() {
        return this.maxHeight;
    }

    /**
     * Limits the maximum height of the map.
     *
     * @param maxHeight the value
     */
    public final void setMaxHeight(final Integer maxHeight) {
        this.maxHeight = maxHeight;
    }

    /**
     * The list of Zoom Levels.
     * <p>
     * Depending on the zoomLevelSnapStrategy, this will be used to compute the actual zoom level of the map.
     *
     * @param zoomLevels the value
     */
    public final void setZoomLevels(final ZoomLevels zoomLevels) {
        this.zoomLevels = zoomLevels;
    }

    /**
     * The zoom level tolerance.
     * <p>
     * Used in ZoomLevelSnapStrategy.HIGHER_SCALE and ZoomLevelSnapStrategy.LOWER_SCALE to specify the cutoff
     * value.
     *
     * @param zoomSnapTolerance the value
     */
    public final void setZoomSnapTolerance(final Double zoomSnapTolerance) {
        this.zoomSnapTolerance = zoomSnapTolerance;
    }

    /**
     * The strategy to use to compute the actual zoom level to use.
     * <p>
     * If you specify this value, you must set the zoomLevels as well.
     * <p>
     * Possible values are:
     * <ul>
     * <li>CLOSEST_LOWER_SCALE_ON_TIE: Find the closest zoom level.  If the targetScale is directly between
     * two zoomLevels then the smaller/higher resolution scale will be chosen.</li>
     * <li>CLOSEST_HIGHER_SCALE_ON_TIE: Find the closest zoom level.  If the targetScale is directly
     * between two zoomLevels then the larger/lower resolution scale will be chosen.</li>
     * <li>HIGHER_SCALE: Always choose the zoom-level that is just higher than the target value.</li>
     * <li>LOWER_SCALE: Always choose the zoom-level that is just lower than the target value.</li>
     * </ul>
     * <p>
     *
     * @param zoomLevelSnapStrategy the value
     * @see ZoomLevelSnapStrategy
     */
    public final void setZoomLevelSnapStrategy(final ZoomLevelSnapStrategy zoomLevelSnapStrategy) {
        this.zoomLevelSnapStrategy = zoomLevelSnapStrategy;
    }

    /**
     * If true, snap to geodetic scales.
     *
     * @param zoomSnapGeodetic the value
     */
    public final void setZoomSnapGeodetic(final Boolean zoomSnapGeodetic) {
        this.zoomSnapGeodetic = zoomSnapGeodetic;
    }

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {

        if (this.width != null && this.maxWidth != null) {
            validationErrors.add(new ConfigurationException(
                    "cannot set both width and maxWidth in " + getClass().getName()));
        }

        if (this.height != null && this.maxHeight != null) {
            validationErrors.add(new ConfigurationException(
                    "cannot set both height and maxHeight in " + getClass().getName()));
        }

        if (this.width != null && this.width < 1) {
            validationErrors.add(new ConfigurationException(
                    "width field is not legal: " + this.width + " in " + getClass().getName()));
        }

        if (this.height != null && this.height < 1) {
            validationErrors.add(new ConfigurationException(
                    "height field is not legal: " + this.height + " in " + getClass().getName()));
        }

        if (this.maxWidth != null && this.maxWidth < 1) {
            validationErrors.add(new ConfigurationException(
                    "max width field is not legal: " + this.width + " in " + getClass().getName()));
        }

        if (this.maxHeight != null && this.maxHeight < 1) {
            validationErrors
                    .add(new ConfigurationException("max height field is not legal: " + this.height + " in " +
                                                            getClass().getName()));
        }

        if (this.getMaxDpi() == null || this.getMaxDpi() < 1) {
            validationErrors.add(
                    new ConfigurationException("maxDpi field is not legal: " + this.getMaxDpi() + " in " +
                                                       getClass().getName()));
        }

        if (this.getMaxDpi() != null && this.getDpiSuggestions() != null) {
            for (double dpi: this.getDpiSuggestions()) {
                if (dpi < 1 || dpi > this.getMaxDpi()) {
                    validationErrors.add(new ConfigurationException(
                            "dpiSuggestions contains an invalid value: " + dpi + " in " +
                                    getClass().getName()));

                }
            }
        }
    }

    @Override
    protected final Optional<JSONObject> getClientInfo() throws JSONException {
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put(JSON_DPI_SUGGESTIONS, getDpiSuggestions());
        if (this.zoomLevels != null) {
            jsonObject.put(JSON_ZOOM_LEVEL_SUGGESTIONS, this.zoomLevels.getScaleDenominators());
        }
        jsonObject.put(JSON_MAX_DPI, this.maxDpi);
        jsonObject.put(JSON_MAP_WIDTH, this.width);
        jsonObject.put(JSON_MAP_HEIGHT, this.height);
        jsonObject.put(JSON_MAX_WIDTH, this.maxWidth);
        jsonObject.put(JSON_MAX_HEIGHT, this.maxHeight);
        return Optional.of(jsonObject);
    }

    /**
     * The value of {@link GenericMapAttribute}.
     */
    public abstract class GenericMapAttributeValues {
        /**
         * The default projection.
         */
        protected static final String DEFAULT_PROJECTION = "EPSG:3857";
        private static final String TYPE = "type";
        private final Template template;
        /**
         * The width of the map.
         */
        @HasDefaultValue
        public Integer width = null;
        /**
         * The height of the map.
         */
        @HasDefaultValue
        public Integer height = null;
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
         * Indicates if the map should adjust its scale/zoom level to be equal to one of those defined in the
         * configuration file.
         *
         *
         * @see #isUseNearestScale()
         */
        @HasDefaultValue
        public Boolean useNearestScale = null;
        /**
         * Indicates if the map should adjust its bounds.
         *
         *
         * @see #isUseAdjustBounds()
         */
        @HasDefaultValue
        public Boolean useAdjustBounds = null;
        /**
         * By default the normal axis order as specified in EPSG code will be used when parsing projections.
         * However the requestor can override this by explicitly declaring that longitude axis is first.
         */
        @HasDefaultValue
        public Boolean longitudeFirst = null;
        /**
         * Should the vector style definitions be adapted to the target DPI resolution? (Default: true)
         *
         * The style definitions are often optimized for a use with OpenLayers (which uses a DPI value of 72).
         * When these styles are used to print with a higher DPI value, lines often look too thin, label are
         * too small, etc.
         *
         * If this property is set to `true`, the style definitions will be scaled to the target DPI value.
         */
        @HasDefaultValue
        public boolean dpiSensitiveStyle = true;
        private List<MapLayer> mapLayers;

        /**
         * Whether to merge all layers into one.
         * This is great to reduce pdf size but needs layers having an imagebufferscaling of 1.
         * Also see the pdfA option of the print config.
         */
        @HasDefaultValue
        public Boolean pdfA = null;

        /**
         * Constructor.
         *
         * @param template the template this map is part of.
         */
        public GenericMapAttributeValues(final Template template) {
            this.template = template;
        }

        /**
         * Constructor.
         *
         * @param template the template this map is part of.
         * @param width the width of the map.
         * @param height the height of the map.
         */
        public GenericMapAttributeValues(final Template template, final Integer width, final Integer height) {
            this.template = template;
            this.width = width;
            this.height = height;
        }

        /**
         * Validate the values provided by the request data and construct MapBounds and parse the layers.
         */
        public void postConstruct() throws FactoryException {

            if (this.width == null) {
                throw new IllegalArgumentException("width parameter was not set.");
            }

            if (this.height == null) {
                throw new IllegalArgumentException("height parameter was not set.");
            }

            //check maximum dimensions
            if (getMaxWidth() != null && this.width > getMaxWidth()) {
                throw new IllegalArgumentException(
                        "width parameter was " + getWidth() + " must be limited to "
                                + getMaxWidth() + ".");
            }
            if (getMaxHeight() != null && this.height > getMaxHeight()) {
                throw new IllegalArgumentException(
                        "height parameter was " + getHeight() + " must be limited to "
                                + getMaxHeight() + ".");
            }

            this.mapLayers = parseLayers();
        }

        private List<MapLayer> parseLayers() {
            List<MapLayer> layerList = new ArrayList<>();

            for (int i = 0; i < this.getRawLayers().size(); i++) {
                try {
                    PObject layer = this.getRawLayers().getObject(i);
                    // only render if  the opacity is greater than 0
                    if (Math.abs(layer.optDouble("opacity", 1.0)) > Constants.OPACITY_PRECISION) {
                        parseSingleLayer(layerList, layer);
                    }
                } catch (Throwable throwable) {
                    throw ExceptionUtils.getRuntimeException(throwable);
                }
            }

            return layerList;
        }

        @SuppressWarnings("unchecked")
        private void parseSingleLayer(
                final List<MapLayer> layerList,
                final PObject layer) throws Throwable {

            final Map<String, MapLayerFactoryPlugin> layerParsers =
                    GenericMapAttribute.this.applicationContext.getBeansOfType(MapLayerFactoryPlugin.class);
            for (MapLayerFactoryPlugin layerParser: layerParsers.values()) {
                final boolean layerApplies = layerParser.getTypeNames().contains(layer.getString(TYPE).
                        toLowerCase());
                if (layerApplies) {
                    Object param = layerParser.createParameter();

                    MapfishParser.parse(this.template.getConfiguration().isThrowErrorOnExtraParameters(),
                                        layer, param, TYPE);

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

            StringBuilder message = new StringBuilder(String.format(
                    "\nLayer with type: '%s' is not currently supported.  Options include: ",
                    layer.getString(TYPE)));
            for (MapLayerFactoryPlugin<?> mapLayerFactoryPlugin: layerParsers.values()) {
                for (Object name: mapLayerFactoryPlugin.getTypeNames()) {
                    message.append("\n");
                    message.append("\t* ").append(name);
                }
            }

            throw new IllegalArgumentException(message.toString());
        }

        /**
         * Parse the projection from a string.
         *
         * @return the crs
         */
        protected final CoordinateReferenceSystem parseProjection() {
            return GenericMapAttribute.parseProjection(getProjection(), this.longitudeFirst);
        }

        /**
         * Return the DPI value for the map. This method is abstract because the dpi value is optional for the
         * overview map, but must be given for the normal map. So, in the overview map the field is defined
         * with a @HasDefaultValue annotation.
         */
        public abstract Double getDpi();

        /**
         * Return the JSON layer definition. This method is abstract for the same reasons as {@link
         * #getDpi()}.
         */
        public abstract PArray getRawLayers();

        /**
         * Set the JSON layer definition. This method is abstract for the same reasons as {@link #getDpi()}.
         *
         * @param layers the new layers
         */
        public abstract void setRawLayers(PArray layers);

        public List<MapLayer> getLayers() {
            return new ArrayList<>(this.mapLayers);
        }

        /**
         * Replace a layer with another.
         *
         * @param pos The position of the layer to replace
         * @param layer The new layer
         */
        public void replaceLayer(final int pos, final MapLayer layer) {
            this.mapLayers.set(pos, layer);
        }

        public final Template getTemplate() {
            return this.template;
        }

        public final Dimension getMapSize() {
            return new Dimension(this.width, this.height);
        }

        public final Integer getWidth() {
            return this.width;
        }

        public final Integer getHeight() {
            return this.height;
        }

        /**
         * Gets the rotation.
         *
         * @return the rotation
         */
        public Double getRotation() {
            if (this.rotation == null) {
                return null;
            }
            return Math.toRadians(this.rotation);
        }

        public String getProjection() {
            return this.projection;
        }

        /**
         * Return true if requestData has useNearestScale and configuration has some zoom levels defined.
         */
        public Boolean isUseNearestScale() {
            return this.useNearestScale && zoomLevels != null;
        }

        /**
         * Return true if requestData has useNearestScale and configuration has some zoom levels defined.
         */
        public Boolean isUseAdjustBounds() {
            return this.useAdjustBounds;
        }

        public final boolean isDpiSensitiveStyle() {
            return this.dpiSensitiveStyle;
        }

        public ZoomLevels getZoomLevels() {
            return zoomLevels;
        }

        public Double getZoomSnapTolerance() {
            return GenericMapAttribute.this.zoomSnapTolerance;
        }

        public ZoomLevelSnapStrategy getZoomLevelSnapStrategy() {
            return zoomLevelSnapStrategy;
        }

        public Boolean getZoomSnapGeodetic() {
            return GenericMapAttribute.this.zoomSnapGeodetic;
        }

        public double[] getDpiSuggestions() {
            return GenericMapAttribute.this.getDpiSuggestions();
        }

        /**
         * @param value The value or null.
         * @param defaultValue The default value.
         * @param <T> A type.
         */
        protected final <T extends Object> T getValueOr(final T value, final T defaultValue) {
            if (value != null) {
                return value;
            } else {
                return defaultValue;
            }
        }
    }
}
