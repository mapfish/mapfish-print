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

import org.locationtech.jts.geom.Geometry;
import org.json.JSONObject;

import java.util.Collection;

public abstract class MfGeoFactory {
    public MfFeatureCollection createFeatureCollection(Collection<MfFeature> collection) {
        return new MfFeatureCollection(collection);
    }

    public abstract MfFeature createFeature(String id, MfGeometry geometry, JSONObject properties);

    public MfGeometry createGeometry(Geometry jtsGeometry) {
        return new MfGeometry(jtsGeometry);
    }
}
