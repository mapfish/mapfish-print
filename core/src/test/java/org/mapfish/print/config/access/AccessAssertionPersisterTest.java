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
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AccessAssertionPersisterTest extends AbstractMapfishSpringTest {
    @Autowired
    private AccessAssertionPersister persister;
    @Autowired
    private List<AccessAssertion> accessAssertions;

    @Test
    public void testMarshalUnmarshal() throws Exception {
        for (AccessAssertion assertion : this.accessAssertions) {
            try {
                final JSONObject marshalled = persister.marshal(assertion);
                final AccessAssertion unmarshalled = persister.unmarshal(marshalled);
                assertNotNull(unmarshalled);

                assertTrue(assertion.getClass() == unmarshalled.getClass());
            } catch (AssertionError e) {
                throw e;
            } catch (Exception e) {
                throw new AssertionError("Marshalling or unmarshalling access assertion: " + assertion.getClass() + " failed", e);
            }
        }
    }

}