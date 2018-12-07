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

import org.json.JSONWriter;
import org.json.JSONException;

/**
 *
 * @author Eric Lemoine, Camptocamp.
 */
public abstract class MfFeature implements MfGeo {
    /**
     * Creates a new instance of MfFeature
     */
    protected MfFeature() {
    }
    
    public GeoType getGeoType() {
        return GeoType.FEATURE;
    }
    
    // Subclasses must implement these methods. -----------

    public abstract String getFeatureId();
    public abstract MfGeometry getMfGeometry();

    /**
     * Add the "key" and "value" pairs to the provided builder.
     */
    public abstract void toJSON(JSONWriter builder) throws JSONException;
}
