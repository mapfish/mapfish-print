/*
 *
 *  * Copyright (C) 2014  Camptocamp
 *  *
 *  * This file is part of MapFish Print
 *  *
 *  * MapFish Print is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * MapFish Print is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.mapfish.print.config.access;

import org.json.JSONObject;
import org.mapfish.print.config.Configuration;

import java.util.List;

/**
 * This assertion always allows access.
 *
 * @author Jesse on 10/7/2014.
 */
public final class AlwaysAllowAssertion implements AccessAssertion {
    /**
     * A public instance that can be used by all resource in the default case.
     */
    public static final AlwaysAllowAssertion INSTANCE = new AlwaysAllowAssertion();

    @Override
    public void assertAccess(final String resourceDescription, final Object protectedResource) {
        // do nothing
    }

    @Override
    public JSONObject marshal() {
        return new JSONObject();
    }

    @Override
    public void unmarshal(final JSONObject encodedAssertion) {
        // nothing to do
    }

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        // do nothing
    }
}
