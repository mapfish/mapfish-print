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

package org.mapfish.print.config.layout;

import com.lowagie.text.DocumentException;
import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.print.InvalidJsonValueException;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.utils.PJsonObject;

/**
 * Bean to configure the pages added for each requested maps.
 * It's "mainPage" in in the layout definition.
 * <p/>
 * See http://trac.mapfish.org/trac/mapfish/wiki/PrintModuleServer#ServersideConfiguration
 */
public class MainPage extends Page {
    private boolean rotation = false;

    public void setRotation(boolean rotation) {
        this.rotation = rotation;
    }

    public void printClientConfig(JSONWriter json) throws JSONException {
        MapBlock map = getMap();
        if (map != null) {
            json.key("map");
            map.printClientConfig(json);
        }

        json.key("rotation").value(rotation);
    }

    /**
     * Called for each map requested by the client.
     */
    public void render(PJsonObject params, RenderingContext context) throws DocumentException {
        //validate the rotation parameter
        final float rotation = params.optFloat("rotation", 0.0F);
        if (rotation != 0.0F && !this.rotation) {
            throw new InvalidJsonValueException(params, "rotation", rotation);
        }

        super.render(params, context);
    }

    public MapBlock getMap() {
        MapBlock result = null;
        for (int i = 0; i < items.size() && result == null; i++) {
            Block block = items.get(i);
            result = block.getMap();
        }
        return result;
    }
}
