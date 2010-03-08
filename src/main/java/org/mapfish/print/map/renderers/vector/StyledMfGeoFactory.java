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

import org.mapfish.geo.MfGeoFactory;
import org.mapfish.geo.MfFeature;
import org.mapfish.geo.MfGeometry;
import org.mapfish.print.utils.PJsonObject;
import org.json.JSONObject;

/**
 * MfFactory that affects a styling object to the Features.
 */
public class StyledMfGeoFactory extends MfGeoFactory {
    /**
     * Available styles.
     */
    private PJsonObject styles;
    private String styleProperty;

    public StyledMfGeoFactory(PJsonObject styles, String styleProperty) {
        this.styles = styles;
        this.styleProperty = styleProperty;
    }

    public MfFeature createFeature(String id, MfGeometry geometry, JSONObject properties) {
        PJsonObject style = null;
        if (styles != null) {
            JSONObject direct = properties.optJSONObject(styleProperty);
            if (direct != null) {
                style = new PJsonObject(direct, "feature.properties." + styleProperty);
            } else {
                final String styleName = properties.optString(styleProperty);
                if (styleName != null) {
                    style = styles.getJSONObject(styleName);
                }
            }
        }
        return new StyledMfFeature(id, geometry, style);
    }
}
