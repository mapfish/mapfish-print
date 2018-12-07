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

package org.mapfish.geo;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

public class MfGeoJSONReader {
    private final MfGeoFactory mfFactory;
    private final GeometryFactory jtsFactory;

    public MfGeoJSONReader(MfGeoFactory mfFactory) {
        this(mfFactory, new GeometryFactory());
    }

    public MfGeoJSONReader(MfGeoFactory mfFactory, GeometryFactory jtsFactory) {
        this.mfFactory = mfFactory;
        this.jtsFactory = jtsFactory;
    }

    public MfGeo decode(InputStream input) throws JSONException {
        JSONObject json = new JSONObject(input);
        return decode(json);
    }

    public MfGeo decode(String input) throws JSONException {
        JSONObject json = new JSONObject(input);
        return decode(json);
    }

    public MfGeo decode(JSONObject json) throws JSONException {
        final String type = json.getString("type");
        if (type.equals("FeatureCollection")) {
            return decodeFeatureCollection(json);
        } else if (type.equals("Feature")) {
            return decodeFeature(json);
        } else {
            return decodeGeometry(json);
        }
    }

    private MfFeatureCollection decodeFeatureCollection(JSONObject json) throws JSONException {
        JSONArray features = json.getJSONArray("features");
        Collection<MfFeature> collection = new ArrayList<MfFeature>(features.length());
        for (int cpt = 0; cpt < features.length(); ++cpt) {
            collection.add(decodeFeature(features.getJSONObject(cpt)));
        }
        return mfFactory.createFeatureCollection(collection);
    }

    private MfFeature decodeFeature(JSONObject json) throws JSONException {
        JSONObject geometry = json.optJSONObject("geometry");
        String id = json.optString("id", null);
        JSONObject properties = json.getJSONObject("properties");
        return mfFactory.createFeature(id, decodeGeometry(geometry), properties);
    }

    private MfGeometry decodeGeometry(JSONObject json) throws JSONException {
        return mfFactory.createGeometry(decodeJtsGeometry(json));
    }

    private Geometry decodeJtsGeometry(JSONObject json) throws JSONException {
        if (json == null) {
          return null;
        }
        String type = json.getString("type");
        final Geometry geometry;


        if (type.equals("GeometryCollection")) {
            JSONArray geoCoords = json.getJSONArray("geometries");
            Geometry[] geometries = new Geometry[geoCoords.length()];
            for (int i = 0; i < geometries.length; ++i) {
                geometries[i] = decodeJtsGeometry(geoCoords.getJSONObject(i));
            }
            geometry = jtsFactory.createGeometryCollection(geometries);

        } else {
            JSONArray coordinates = json.getJSONArray("coordinates");

            if (type.equals("Point")) {
                geometry = jtsFactory.createPoint(decodeCoordinate(coordinates));

            } else if (type.equals("LineString")) {
                geometry = jtsFactory.createLineString(decodeCoordinates(coordinates));

            } else if (type.equals("Polygon")) {
                geometry = decodePolygon(coordinates);

            } else if (type.equals("MultiPoint")) {
                Point[] points = new Point[coordinates.length()];
                for (int i = 0; i < points.length; ++i) {
                    points[i] = jtsFactory.createPoint(decodeCoordinate(coordinates.getJSONArray(i)));

                }
                geometry = jtsFactory.createMultiPoint(points);

            } else if (type.equals("MultiLineString")) {
                LineString[] lineStrings = new LineString[coordinates.length()];
                for (int i = 0; i < lineStrings.length; ++i) {
                    lineStrings[i] = jtsFactory.createLineString(decodeCoordinates(coordinates.getJSONArray(i)));
                }
                geometry = jtsFactory.createMultiLineString(lineStrings);

            } else if (type.equals("MultiPolygon")) {
                Polygon[] polygons = new Polygon[coordinates.length()];
                for (int i = 0; i < polygons.length; ++i) {
                    polygons[i] = decodePolygon(coordinates.getJSONArray(i));

                }
                geometry = jtsFactory.createMultiPolygon(polygons);

            } else {
                return null;
            }
        }

        return geometry;
    }

    private Polygon decodePolygon(JSONArray coordinates) throws JSONException {
        LinearRing outer = jtsFactory.createLinearRing(decodeCoordinates(coordinates.getJSONArray(0)));
        LinearRing[] holes = new LinearRing[coordinates.length() - 1];
        for (int i = 1; i < coordinates.length(); ++i) {
            holes[i - 1] = jtsFactory.createLinearRing(decodeCoordinates(coordinates.getJSONArray(i)));
        }
        return jtsFactory.createPolygon(outer, holes);
    }

    private Coordinate[] decodeCoordinates(JSONArray coordinates) throws JSONException {
        Coordinate[] result = new Coordinate[coordinates.length()];
        for (int i = 0; i < result.length; ++i) {
            result[i] = decodeCoordinate(coordinates.getJSONArray(i));
        }
        return result;
    }

    private Coordinate decodeCoordinate(JSONArray coord) throws JSONException {
        if (coord.length() > 2) {
            return new Coordinate(coord.getDouble(0), coord.getDouble(1), coord.getDouble(2));
        } else {
            return new Coordinate(coord.getDouble(0), coord.getDouble(1));
        }
    }

}
