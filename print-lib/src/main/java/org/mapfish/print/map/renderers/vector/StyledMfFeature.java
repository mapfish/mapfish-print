/*
 * Copyright (C) 2009  Camptocamp
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

package org.mapfish.print.map.renderers.vector;

import org.mapfish.geo.MfFeature;
import org.mapfish.geo.MfGeometry;
import org.mapfish.print.utils.PJsonObject;
import org.json.JSONWriter;
import org.json.JSONException;

/**
 * A geo JSON feature with styling information.
 */
public class StyledMfFeature extends MfFeature {
    private final String id;
    private final MfGeometry geometry;
    private PJsonObject style;

    public StyledMfFeature(String id, MfGeometry geometry, PJsonObject style) {
        this.id = id;
        this.geometry = geometry;
        this.style = style;
    }

    public String getFeatureId() {
        return id;
    }

    public MfGeometry getMfGeometry() {
        return geometry;
    }

    public void toJSON(JSONWriter builder) throws JSONException {
        throw new RuntimeException("Not implemented");
    }

    public PJsonObject getStyle() {
        return style;
    }

    public boolean isDisplayed() {
        return style==null || !style.optString("display", "yes").equalsIgnoreCase("none");
    }
}
