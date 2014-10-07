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

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * Class for marshalling and unmarshalling AccessAssertionObjects to and from JSON.
 *
 * @author Jesse on 10/7/2014.
 */
public final class AccessAssertionPersister {
    private static final String JSON_CLASS_NAME = "className";
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Load assertion from the provided json or throw exception if not possible.
     *
     * @param encodedAssertion the assertion as it was encoded in JSON.
     */
    public AccessAssertion unmarshal(final JSONObject encodedAssertion) {
        final String className;
        try {
            className = encodedAssertion.getString(JSON_CLASS_NAME);
            final Class<?> assertionClass = Thread.currentThread().getContextClassLoader().loadClass(className);
            final AccessAssertion assertion = (AccessAssertion) this.applicationContext.getBean(assertionClass);
            assertion.unmarshal(encodedAssertion);

            return assertion;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Marshal the assertion as a JSON object.
     *
     * @param assertion the assertion to marshal
     */
    public JSONObject marshal(final AccessAssertion assertion) {
        final JSONObject jsonObject = assertion.marshal();
        if (jsonObject.has(JSON_CLASS_NAME)) {
            throw new AssertionError("The toJson method in AccessAssertion: '" + assertion.getClass() +
                                 "' defined a JSON field " + JSON_CLASS_NAME +
                                 " which is a reserved keyword and is not permitted to be used in toJSON method");
        }
        try {
            jsonObject.put(JSON_CLASS_NAME, assertion.getClass().getName());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return jsonObject;
    }
}
