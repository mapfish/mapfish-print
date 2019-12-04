package org.mapfish.print.attribute.map;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.util.Assert;
import org.mapfish.print.Constants;
import org.mapfish.print.parser.HasDefaultValue;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nonnull;

/**
 * Represents an area on the map which is of particular interest for some reason.  It consists of polygon
 * geojson and a method for displaying the area the geometry intersects with.
 */
public final class AreaOfInterest {
    /**
     * A Geojson geometry (can be string or GeoJson) that indicates the area of interest.
     * <p>
     * The geojson must be a polygon representing the area of interest.
     * </p>
     */
    public String area;
    /**
     * The way that the Area of Interest will be represented on the map.  By default it will be drawn as a
     * polygon with a solid border and a translucent interior.  The style can be controlled by the style
     * parameter.
     */
    @HasDefaultValue
    public AoiDisplay display = AoiDisplay.RENDER;
    /**
     * A string representing the style.  The {@link org.mapfish.print.map.style.StyleParserPlugin}s are used
     * to load the style. Because of this the styleRef can be JSON, SLD, URL, file path, etc...
     */
    @HasDefaultValue
    public String style;
    /**
     * If true the Area of Interest will be rendered as SVG (if display == RENDER).
     *
     * (will default to {@link org.mapfish.print.config.Configuration#defaultStyle}).
     */
    @HasDefaultValue
    public Boolean renderAsSvg;
    private Geometry polygon;

    /**
     * Tests that the area is valid geojson, the style ref is valid or null and the display is non-null.
     */
    public void postConstruct() {
        parseGeometry();

        Assert.isTrue(this.polygon != null, "Polygon is null. 'area' string is: '" + this.area + "'");
        Assert.isTrue(this.display != null, "'display' is null");

        Assert.isTrue(this.style == null || this.display == AoiDisplay.RENDER,
                      "'style' does not make sense unless 'display' == RENDER.  In this case 'display' == " +
                              this.display);
    }

    private void parseGeometry() {
        GeometryJSON json = new GeometryJSON();
        byte[] bytes;
        try {
            bytes = this.area.getBytes(Constants.DEFAULT_ENCODING);
            final InputStream input = new ByteArrayInputStream(bytes);
            this.polygon = json.read(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the area polygon.  It will parse the polygon string representation in the polygon field.
     */
    public synchronized Geometry getArea() {
        if (this.polygon == null) {
            parseGeometry();
        }
        return this.polygon;
    }

    /**
     * Return the area polygon as the only feature in the feature collection.
     *
     * @param mapAttributes the attributes that this aoi is part of.
     */
    public SimpleFeatureCollection areaToFeatureCollection(
            @Nonnull final MapAttribute.MapAttributeValues mapAttributes) {
        Assert.isTrue(mapAttributes.areaOfInterest == this,
                      "map attributes passed in does not contain this area of interest object");

        final SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("aoi");
        CoordinateReferenceSystem crs = mapAttributes.getMapBounds().getProjection();
        typeBuilder.add("geom", this.polygon.getClass(), crs);
        final SimpleFeature feature =
                SimpleFeatureBuilder.build(typeBuilder.buildFeatureType(), new Object[]{this.polygon}, "aoi");
        final DefaultFeatureCollection features = new DefaultFeatureCollection();
        features.add(feature);
        return features;
    }

    public void setPolygon(final Geometry polygon) {
        this.polygon = polygon;
    }

    /**
     * Make a copy of this Area of Interest.
     */
    public AreaOfInterest copy() {
        AreaOfInterest aoi = new AreaOfInterest();
        aoi.display = this.display;
        aoi.area = this.area;
        aoi.polygon = this.polygon;
        aoi.style = this.style;
        aoi.renderAsSvg = this.renderAsSvg;
        return aoi;
    }

    /**
     * Represents the ways that the area of interest (aoi) can be rendered on the map.
     */
    public enum AoiDisplay {
        /**
         * Draw the entire map and render the AOI polygon on the map. This is the default.
         */
        RENDER,
        /**
         * Set a clip for the area (IE only show the area of interest).
         */
        CLIP,
        /**
         * Do not show the Area of interest on the map.  In this case another processor will likely make use
         * of the AOI.
         */
        NONE
    }
}
