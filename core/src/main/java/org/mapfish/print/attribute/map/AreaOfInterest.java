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

import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.util.Assert;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.geom.GeometryJSON;
import org.mapfish.print.Constants;
import org.mapfish.print.parser.HasDefaultValue;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import javax.annotation.Nonnull;

/**
 * Represents an area on the map which is of particular interest for some reason.  It consists of polygon geojson and a method
 * for displaying the area the geometry intersects with.
 *
 * @author Jesse on 8/26/2014.
 */
// CSOFF: VisibilityModifier
public final class AreaOfInterest {
    private Polygon polygon;

    /**
     * A Geojson geometry (can be string or GeoJson) that indicates the area of interest.
     * <p>
     *     The geojson must be a polygon representing the area of interest.
     * </p>
     */
    public String area;

    /**
     * The way that the Area of Interest will be represented on the map.  By default it will be drawn as a polygon with a solid
     * border and a translucent interior.  The style can be controlled by the style parameter.
     */
    @HasDefaultValue
    public AoiDisplay display = AoiDisplay.RENDER;

    /**
     * A string representing the style.  The {@link org.mapfish.print.map.style.StyleParserPlugin}s are used to load the style.
     * Because of this the styleRef can be JSON, SLD, URL, file path, etc...
     */
    @HasDefaultValue
    public String style;

    /**
     * If true the Area of Interest will be rendered as SVG (if display == RENDER).
     * <p/>
     * (will default to {@link org.mapfish.print.config.Configuration#defaultStyle}).
     */
    @HasDefaultValue
    public Boolean renderAsSvg;

    /**
     * Tests that the area is valid geojson, the style ref is valid or null and the display is non-null.
     */
    public void postConstruct() {
        parseGeometry();

        Assert.isTrue(this.polygon != null, "Polygon is null. 'area' string is: '" + this.area + "'");
        Assert.isTrue(this.display != null, "'display' is null");

        Assert.isTrue(this.style == null || this.display == AoiDisplay.RENDER,
                "'style' does not make sense unless 'display' == RENDER.  In this case 'display' == " + this.display);
    }

    private void parseGeometry() {
        GeometryJSON json = new GeometryJSON();
        byte[] bytes;
        try {
            bytes = this.area.getBytes(Constants.DEFAULT_ENCODING);
            final InputStream input = new ByteArrayInputStream(bytes);
            this.polygon = (Polygon) json.read(input);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the area polygon.  It will parse the polygon string representation in the polygon field.
     */
    public synchronized Polygon getArea() {
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
    public SimpleFeatureCollection areaToFeatureCollection(@Nonnull final MapAttribute.MapAttributeValues mapAttributes) {
        Assert.isTrue(mapAttributes.areaOfInterest == this, "map attributes passed in does not contain this area of interest object");

        final SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("aoi");
        CoordinateReferenceSystem crs = mapAttributes.getMapBounds().getProjection();
        typeBuilder.add("geom", this.polygon.getClass(), crs);
        final SimpleFeature feature = SimpleFeatureBuilder.build(typeBuilder.buildFeatureType(), new Object[]{this.polygon}, "aoi");
        final DefaultFeatureCollection features = new DefaultFeatureCollection();
        features.add(feature);
        return features;
    }

    public void setPolygon(final Polygon polygon) {
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
    public static enum AoiDisplay {
        /**
         * Draw the entire map and render the AOI polygon on the map. This is the default.
         */
        RENDER,
        /**
        * Set a clip for the area (IE only show the area of interest).
        */
        CLIP,
        /**
         * Do not show the Area of interest on the map.  In this case another processor will likely make use of the AOI.
         */
        NONE
    }
}
