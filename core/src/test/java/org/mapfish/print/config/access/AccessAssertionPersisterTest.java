package org.mapfish.print.config.access;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.springframework.beans.factory.annotation.Autowired;

public class AccessAssertionPersisterTest extends AbstractMapfishSpringTest {
  @Autowired private AccessAssertionPersister persister;
  @Autowired private List<AccessAssertion> accessAssertions;

  @Test
  public void testMarshalUnmarshal() {
    for (AccessAssertion assertion : this.accessAssertions) {
      try {
        final JSONObject marshaled = persister.marshal(assertion);
        final AccessAssertion unmarshalled = persister.unmarshal(marshaled);

        assertNotNull(unmarshalled);
        assertSame(assertion.getClass(), unmarshalled.getClass());
      } catch (Exception e) {
        throw new AssertionError(
            "Marshalling or unmarshalling access assertion: " + assertion.getClass() + " failed",
            e);
      }
    }
  }
}
