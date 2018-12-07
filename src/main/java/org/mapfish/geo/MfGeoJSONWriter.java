/*
 * Copyright (C) 2008  Camptocamp
 *
 * This file is part of MapFish Server
 *
 * MapFish Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MapFish Server.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

/*
 * MfGeoJSON.java
 */

package org.mapfish.geo;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.json.JSONException;
import org.json.JSONWriter;

import java.util.Iterator;


/**
 * This class is coded against the version 1.0 of the spec
 * on http://geojson.org.
 *
 * The code of this class is greatly inspired from the GeoJSONBuilder
 * class from the GeoServer code base. The author of that class is
 * Chris Holmes, from the Open Planning Project. See the above
 * copyright.
 *
 * @author Eric Lemoine, Camptocamp.
 * @version $Id$
 *
 */
public class MfGeoJSONWriter {
    private final JSONWriter builder;

    public MfGeoJSONWriter(JSONWriter builder) {
        this.builder = builder;
    }

    public void encode(MfGeo o) throws JSONException {
        switch (o.getGeoType()) {
            case FEATURE:
                MfFeature f = (MfFeature)o;
                encodeFeature(f);
                break;
            case FEATURECOLLECTION:
                MfFeatureCollection c = (MfFeatureCollection)o;
                encodeFeatureCollection(c);
                break;
            case GEOMETRY:
                MfGeometry g = (MfGeometry)o;
                encodeGeometry(g.getInternalGeometry());
                break;
             default:
                throw new RuntimeException("No implementation for " + o.getGeoType());
        }
    }

    public void encodeFeatureCollection(MfFeatureCollection c) throws JSONException {
        builder.object();
        builder.key("type").value("FeatureCollection");
        builder.key("features");
        builder.array();

        Iterator<MfFeature> i = c.getCollection().iterator();
        while (i.hasNext()) {
            MfFeature f = i.next();
            encodeFeature(f);
        }

        builder.endArray();
        builder.endObject();
    }

    public void encodeFeature(MfFeature f) throws JSONException {
        builder.object();
        builder.key("type").value("Feature");
        builder.key("id").value(f.getFeatureId());
        builder.key("geometry");

        Geometry g;
        MfGeometry mfg;
        if (((mfg = f.getMfGeometry()) != null) &&
            ((g = mfg.getInternalGeometry()) != null)) {
            encodeGeometry(g);
        } else {
            builder.value(null);
        }

        builder.key("properties");
        builder.object();
        f.toJSON(builder);
        builder.endObject();
        builder.endObject();
    }

    public void encodeGeometry(Geometry g) throws JSONException {

        builder.object();
        builder.key("type");
        builder.value(getGeometryName(g));

        GeometryType geometryType = getGeometryType(g);

        if (geometryType != GeometryType.MULTIGEOMETRY) {
            builder.key("coordinates");

            switch (geometryType) {
            case POINT:
                encodeCoordinate(g.getCoordinate());
                break;

            case LINESTRING:
            case MULTIPOINT:
                encodeCoordinates(g.getCoordinates());
                break;

            case POLYGON:
                encodePolygon((Polygon) g);
                break;

            case MULTILINESTRING:
                builder.array();
                for (int i = 0, n = g.getNumGeometries(); i < n; i++) {
                    encodeCoordinates(g.getGeometryN(i).getCoordinates());
                }
                builder.endArray();
                break;

            case MULTIPOLYGON:
                builder.array();
                for (int i = 0, n = g.getNumGeometries(); i < n; i++) {
                    encodePolygon((Polygon) g.getGeometryN(i));
                }
                builder.endArray();
                break;

            default:
                //should never happen.
                throw new RuntimeException("No implementation for "+geometryType);
            }
        } else {
            encodeGeomCollection((GeometryCollection) g);
        }

        builder.endObject();
    }

    private void encodeGeomCollection(GeometryCollection collection) throws JSONException {
        builder.array();
        builder.key("geometries");

        for (int i = 0, n = collection.getNumGeometries(); i < n; i++) {
            encodeGeometry(collection.getGeometryN(i));
        }

        builder.endArray();
    }

    /**
     * Write the coordinates of a geometry
     * @param coords The coordinates to encode
     * @throws JSONException
     */
    private void encodeCoordinates(Coordinate[] coords)
        throws JSONException {
        builder.array();

        for (int i = 0; i < coords.length; i++) {
            Coordinate coord = coords[i];
            encodeCoordinate(coord);
        }

        builder.endArray();
    }

    private void encodeCoordinate(Coordinate coord) throws JSONException {
        builder.array();
        builder.value(coord.x);
        builder.value(coord.y);
        builder.endArray();
    }

    /**
     * Turns an envelope into an array [minX,minY,maxX,maxY]
     * @param env envelope representing bounding box
     */
    protected void encodeBoundingBox(Envelope env) throws JSONException {
    	builder.key("bbox");
    	builder.array();
    	builder.value(env.getMinX());
    	builder.value(env.getMinY());
    	builder.value(env.getMaxX());
    	builder.value(env.getMaxY());
    	builder.endArray();
    }

    /**
     * Writes a polygon
     * @param geometry The polygon to encode
     * @throws JSONException
     */
    private void encodePolygon(Polygon geometry) throws JSONException {
        builder.array();
        encodeCoordinates(geometry.getExteriorRing().getCoordinates());

        for (int i = 0, ii = geometry.getNumInteriorRing(); i < ii; i++) {
            encodeCoordinates(geometry.getInteriorRingN(i).getCoordinates());
        }

        builder.endArray(); //end the linear ring
    }

    public static enum GeometryType {
        POINT("Point"),
        LINESTRING("LineString"),
        POLYGON("Polygon"),
        MULTIPOINT("MultiPoint"),
        MULTILINESTRING("MultiLineString"),
        MULTIPOLYGON("MultiPolygon"),
        MULTIGEOMETRY("GeometryCollection");

        private final String name;

        private GeometryType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static String getGeometryName(Geometry geometry) {
        final GeometryType type = getGeometryType(geometry);
        return type != null ? type.getName() : null;
    }

    /**
     * Gets the internal representation for the given Geometry
     *
     * @param geometry a Geometry
     *
     * @return int representation of Geometry
     */
    public static GeometryType getGeometryType(Geometry geometry) {
        final Class<?> geomClass = geometry.getClass();
        final GeometryType returnValue;

        if (geomClass.equals(Point.class)) {
            returnValue = GeometryType.POINT;
        } else if (geomClass.equals(LineString.class)) {
            returnValue = GeometryType.LINESTRING;
        } else if (geomClass.equals(Polygon.class)) {
            returnValue = GeometryType.POLYGON;
        } else if (geomClass.equals(MultiPoint.class)) {
            returnValue = GeometryType.MULTIPOINT;
        } else if (geomClass.equals(MultiLineString.class)) {
            returnValue = GeometryType.MULTILINESTRING;
        } else if (geomClass.equals(MultiPolygon.class)) {
            returnValue = GeometryType.MULTIPOLYGON;
        } else if (geomClass.equals(GeometryCollection.class)) {
            returnValue = GeometryType.MULTIGEOMETRY;
        } else {
            returnValue = null;
            //HACK!!! throw exception.
        }

        return returnValue;
    }
}
