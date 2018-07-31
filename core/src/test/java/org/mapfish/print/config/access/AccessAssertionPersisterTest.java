package org.mapfish.print.config.access;

import org.json.JSONObject;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class AccessAssertionPersisterTest extends AbstractMapfishSpringTest {
    @Autowired
    private AccessAssertionPersister persister;
    @Autowired
    private List<AccessAssertion> accessAssertions;

    @Test
    public void testMarshalUnmarshal() {
        for (AccessAssertion assertion: this.accessAssertions) {
            try {
                final JSONObject marshalled = persister.marshal(assertion);
                final AccessAssertion unmarshalled = persister.unmarshal(marshalled);
                assertNotNull(unmarshalled);

                assertSame(assertion.getClass(), unmarshalled.getClass());
            } catch (AssertionError e) {
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
                throw new AssertionError(
                        "Marshalling or unmarshalling access assertion: " + assertion.getClass() + " failed");
            }
        }
    }

}
