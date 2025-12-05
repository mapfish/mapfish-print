package org.mapfish.print.config.access;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

public class AndAccessAssertionTest extends AbstractMapfishSpringTest {

  @Autowired ApplicationContext applicationContext;

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void testSetPredicates() {
    assertThrows(AssertionError.class, () -> {

      final AndAccessAssertion andAssertion = applicationContext.getBean(AndAccessAssertion.class);
      andAssertion.setPredicates(AlwaysAllowAssertion.INSTANCE, AlwaysAllowAssertion.INSTANCE);
      andAssertion.setPredicates(AlwaysAllowAssertion.INSTANCE);
    });
  }

  @Test
  public void testAssertAccessNotAllowed() {
    assertThrows(AccessDeniedException.class, () -> {
      final AndAccessAssertion andAssertion = applicationContext.getBean(AndAccessAssertion.class);
      AccessAssertion pred1 =
          new RoleAccessAssertion().setRequiredRoles(Collections.singleton("ROLE_USER"));
      AccessAssertion pred2 =
          new RoleAccessAssertion().setRequiredRoles(Collections.singleton("ROLE_OTHER"));
      andAssertion.setPredicates(pred1, pred2);

      AccessAssertionTestUtil.setCreds("ROLE_USER");
      andAssertion.assertAccess("", this);
    });
  }

  @Test
  public void testAssertAccessAllowed() {
    final AndAccessAssertion andAssertion = applicationContext.getBean(AndAccessAssertion.class);
    AccessAssertion pred1 =
        new RoleAccessAssertion().setRequiredRoles(Collections.singleton("ROLE_USER"));
    AccessAssertion pred2 =
        new RoleAccessAssertion().setRequiredRoles(Collections.singleton("ROLE_OTHER"));
    andAssertion.setPredicates(pred1, pred2);

    AccessAssertionTestUtil.setCreds("ROLE_USER", "ROLE_OTHER");
    andAssertion.assertAccess("", this);
  }

  @Test
  public void testMarshalUnmarshal() {
    final AndAccessAssertion andAssertion = applicationContext.getBean(AndAccessAssertion.class);
    AccessAssertion pred1 =
        new RoleAccessAssertion().setRequiredRoles(Collections.singleton("ROLE_USER"));
    AccessAssertion pred2 =
        new RoleAccessAssertion().setRequiredRoles(Collections.singleton("ROLE_OTHER"));
    andAssertion.setPredicates(pred1, pred2);

    AccessAssertionTestUtil.setCreds("ROLE_USER", "ROLE_OTHER");
    andAssertion.assertAccess("", this);

    try {
      AccessAssertionTestUtil.setCreds("ROLE_USER");
      andAssertion.assertAccess("", this);

      fail("Expected an AccessDeniedException exception");
    } catch (AccessDeniedException e) {
      // good
    }
  }
}
