package org.mapfish.print.config.access;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.List;
import org.json.JSONObject;
import org.junit.Test;
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
        final AccessAssertion unmarshaled = persister.unmarshal(marshaled);
        assertNotNull(unmarshaled);

        assertSame(assertion.getClass(), unmarshaled.getClass());
      } catch (AssertionError e) {
        throw e;
      } catch (Exception e) {
        e.printStackTrace();
        throw new AssertionError(
            "Marshaling or unmarshaling access assertion: " + assertion.getClass() + " failed");
      }
    }
  }
}
