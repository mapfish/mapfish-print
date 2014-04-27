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

package org.mapfish.print.servlet;

import java.util.UUID;

/**
 * A servlet info that can be configured through the spring configuration (or programmatically).
 * <p/>
 * A default random id will be created by default.
 *
 * @author Jesse on 4/26/2014.
 */
public final class ConfigBasedServletInfo implements ServletInfo {
    private String servletId = UUID.randomUUID().toString();

    public void setServletId(final String servletId) {
        this.servletId = servletId;
    }

    @Override
    public String getServletId() {
        return this.servletId;
    }
}
