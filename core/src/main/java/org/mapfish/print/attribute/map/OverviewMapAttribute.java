package org.mapfish.print.attribute.map;

import org.json.JSONArray;
import org.mapfish.print.config.Template;
import org.mapfish.print.parser.HasDefaultValue;
import org.mapfish.print.parser.Requires;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.json.PJsonArray;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * <p>The attributes for an overview map (see
 * <a href="processors.html#!createOverviewMap">!createOverviewMap</a> processor).</p>
 * [[examples=verboseExample,overviewmap_tyger_ny_EPSG_3857]]
 */
public final class OverviewMapAttribute extends GenericMapAttribute {

    private static final double DEFAULT_ZOOM_FACTOR = 5.0;

    private double zoomFactor = DEFAULT_ZOOM_FACTOR;

    private String style = null;

    /**
     * The zoom factor by which the extent of the main map will be augmented to create an overview.
     *
     * @param zoomFactor The zoom-factor.
     */
    public void setZoomFactor(final double zoomFactor) {
        this.zoomFactor = zoomFactor;
    }

    /**
     * The style name of a style to apply to the bbox rectangle of the original map during rendering. The
     * style name must map to a style in the template or the configuration objects.
     *
     * @param style The style.
     */
    public void setStyle(final String style) {
        this.style = style;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<OverviewMapAttributeValues> getValueType() {
        return OverviewMapAttributeValues.class;
    }

    @Override
    public OverviewMapAttributeValues createValue(final Template template) {
        return new OverviewMapAttributeValues(template, this.getWidth(), this.getHeight());
    }

    /**
     * The value of {@link MapAttribute}.
     */
    public final class OverviewMapAttributeValues extends GenericMapAttribute.GenericMapAttributeValues {

        /**
         * The json with all the layer information.  This will be parsed in postConstruct into a list of
         * layers and therefore this field should not normally be accessed.
         */
        @HasDefaultValue
        public PArray layers = new PJsonArray(null, new JSONArray(), null);

        /**
         * The output dpi of the printed map.
         */
        @HasDefaultValue
        public Double dpi = null;
        /**
         * An array of 4 doubles, minX, minY, maxX, maxY.  The bounding box of the overview-map.
         *
         * If a bounding box is given, the overview-map shows a fixed extent. The configuration parameter
         * <code>zoomFactor</code> is ignored in this case.
         */
        @HasDefaultValue
        public double[] bbox;
        /**
         * An array of 2 doubles, (x, y).  The center of the overview-map.
         *
         * If center and scale are given, the overview-map shows a fixed extent. The configuration parameter
         * <code>zoomFactor</code> is ignored in this case.
         */
        @Requires("scale")
        @HasDefaultValue
        public double[] center;
        /**
         * If <code>center</code> is defined then this is the scale of the map centered at
         * <code>center</code>.
         */
        @HasDefaultValue
        public Double scale;
        private MapBounds mapBounds;

        /**
         * Constructor.
         *
         * @param template the template this map is part of.
         */
        public OverviewMapAttributeValues(final Template template) {
            super(template);
        }


        /**
         * Constructor.
         *
         * @param template the template this map is part of.
         * @param width the width of the map.
         * @param height the height of the map.
         */
        public OverviewMapAttributeValues(
                final Template template, final Integer width, final Integer height) {
            super(template, width, height);
        }

        @Override
        public void postConstruct() throws FactoryException {
            super.postConstruct();
            this.mapBounds = parseBounds();
        }

        private MapBounds parseBounds() {
            final CoordinateReferenceSystem crs = parseProjection();
            if (this.center != null && this.bbox != null) {
                throw new IllegalArgumentException("Cannot have both center and bbox defined");
            }
            MapBounds bounds = null;
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
            }

            return bounds;
        }

        public MapBounds getMapBounds() {
            return this.mapBounds;
        }

        @Override
        public Double getDpi() {
            return this.dpi;
        }

        @Override
        public PArray getRawLayers() {
            return this.layers;
        }

        @Override
        public void setRawLayers(final PArray newLayers) {
            this.layers = newLayers;
        }

        public double getZoomFactor() {
            return OverviewMapAttribute.this.zoomFactor;
        }

        public String getStyle() {
            return OverviewMapAttribute.this.style;
        }

        @Override
        public String getProjection() {
            return getValueOr(super.getProjection(), DEFAULT_PROJECTION);
        }

    }
}
