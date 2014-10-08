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

import com.google.common.collect.Lists;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapfish.print.config.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * An access assertion that throws fails if any of the encapsulated assertions fail.
 *
 * @author Jesse on 10/7/2014.
 */
public final class AndAccessAssertion implements AccessAssertion {
    private static final String JSON_ARRAY = "data";
    private List<AccessAssertion> predicates;

    @Autowired
    private AccessAssertionPersister persister;

    /**
     * Set all the Predicates/AccessAssertion that have to all pass in order for this assertion to pass.
     * <p>
     *     An exception is thrown if this method is called more than once.
     * </p>
     * @param predicates the Predicates/AccessAssertion
     */
    public void setPredicates(@Nonnull final AccessAssertion... predicates) {
        if (this.predicates != null) {
            throw new AssertionError("Predicates can only be set a single time");
        }
        if (predicates.length < 1) {
            throw new IllegalArgumentException("There must be at least 1 predicate");
        }
        this.predicates = Arrays.asList(predicates);
    }

    @Override
    public void assertAccess(final String resourceDescription, final Object protectedResource) {
        for (AccessAssertion predicate : this.predicates) {
            predicate.assertAccess(resourceDescription, protectedResource);
        }
    }

    @Override
    public JSONObject marshal() {
        try {
            JSONObject marshalData = new JSONObject();
            JSONArray array = new JSONArray();
            marshalData.put(JSON_ARRAY, array);

            if (this.predicates != null) {
                for (AccessAssertion predicate : this.predicates) {
                    final JSONObject predicateMarshalData = this.persister.marshal(predicate);
                    array.put(predicateMarshalData);
                }
            }
            return marshalData;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void unmarshal(final JSONObject encodedAssertion) {
        try {
            this.predicates = Lists.newArrayList();

            JSONArray marshalData = encodedAssertion.getJSONArray(JSON_ARRAY);
            for (int i = 0; i < marshalData.length(); i++) {
                JSONObject predicateData = marshalData.getJSONObject(i);
                final AccessAssertion predicate = this.persister.unmarshal(predicateData);
                this.predicates.add(predicate);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        for (AccessAssertion predicate : this.predicates) {
            predicate.validate(validationErrors, configuration);
        }
    }
}
