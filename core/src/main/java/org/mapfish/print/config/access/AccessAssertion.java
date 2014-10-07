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
// *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.mapfish.print.config.access;

import org.json.JSONObject;
import org.mapfish.print.config.ConfigurationObject;

/**
 * An access assertion is a check that the current user has particular access/properties to allow access to a particular resource like
 * a template or completed report.
 * <p>
 * All implementations must be serializable.
 * </p>
 *
 * @author Jesse on 10/7/2014.
 */
public interface AccessAssertion extends ConfigurationObject {
    /**
     * Checks that the user can access the resource.
     * <p/>
     * Will throw {@link org.springframework.security.authentication.AuthenticationCredentialsNotFoundException} if the user has
     * not logged in or supplied credentials.
     * <p/>
     * Will throw {@link org.springframework.security.access.AccessDeniedException} if the user is logged in but may not access
     * the resource.
     *
     * @param resourceDescription a string describing the resource for logging and exception throwing purposes
     * @param protectedResource the resource being protected.
     */
    void assertAccess(String resourceDescription, Object protectedResource);

    /**
     * Encode the assertion as JSON for later loading.  Each call should return a unique JSON object.
     */
    JSONObject marshal();

    /**
     * Load the assertion properties from the JSON data.
     *
     * @param encodedAssertion the assertion encoded as JSON.
     */
    void unmarshal(JSONObject encodedAssertion);

}
