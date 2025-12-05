package org.mapfish.print.config.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mapfish.print.config.access.AccessAssertionTestUtil.setCreds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mapfish.print.SetsUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;

public class RoleAccessAssertionTest {

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void testSetRequiredRoles() {
    assertThrows(AssertionError.class, () -> {
      final RoleAccessAssertion assertion = new RoleAccessAssertion();
      assertion.setRequiredRoles(Collections.singleton("ROLE_USER"));
      assertion.setRequiredRoles(Collections.singleton("ROLE_USER"));
    });
  }

  @Test
  public void testAssertAccessNoCredentials() {
    assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
      final RoleAccessAssertion assertion = new RoleAccessAssertion();
      assertion.setRequiredRoles(Collections.singleton("ROLE_USER"));

      assertion.assertAccess("", this);
    });
  }

  @Test
  public void testAssertAccessWrongCreds() {
    assertThrows(AccessDeniedException.class, () -> {
      final RoleAccessAssertion assertion = new RoleAccessAssertion();
      assertion.setRequiredRoles(Collections.singleton("ROLE_USER"));

      setCreds("ROLE_USER2");
      assertion.assertAccess("", this);
    });
  }

  @Test
  public void testAssertAccessAllowed() {
    final RoleAccessAssertion assertion = new RoleAccessAssertion();
    assertion.setRequiredRoles(Collections.singleton("ROLE_USER"));

    setCreds("ROLE_USER");
    assertion.assertAccess("", this);

    setCreds("ROLE_USER", "ROLE_OTHER");
    assertion.assertAccess("", this);
  }

  @Test
  public void testAssertAccessOneOf() {
    final RoleAccessAssertion assertion = new RoleAccessAssertion();
    assertion.setRequiredRoles(SetsUtils.create("ROLE_USER", "ROLE_USER2"));

    setCreds("ROLE_USER");
    assertion.assertAccess("", this);

    setCreds("ROLE_USER2");
    assertion.assertAccess("", this);

    setCreds("ROLE_OTHER", "ROLE_USER2");
    assertion.assertAccess("", this);
  }

  @Test
  public void testAssertAccessOneOfFailed() {
    assertThrows(AccessDeniedException.class, () -> {
      final RoleAccessAssertion assertion = new RoleAccessAssertion();
      assertion.setRequiredRoles(SetsUtils.create("ROLE_USER", "ROLE_USER2"));

      setCreds("ROLE_OTHER");
      assertion.assertAccess("", this);
    });
  }

  @Test
  public void testAssertNoRolesNoCreds() {
    assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
      final RoleAccessAssertion assertion = new RoleAccessAssertion();
      assertion.setRequiredRoles(new HashSet<>());

      assertion.assertAccess("", this);
      setCreds("ROLE_OTHER", "ROLE_USER2");
      assertion.assertAccess("", this);
    });
  }

  @Test
  public void testAssertNoRolesSomeCreds() {
    final RoleAccessAssertion assertion = new RoleAccessAssertion();
    assertion.setRequiredRoles(new HashSet<>());

    setCreds("ROLE_OTHER");
    assertion.assertAccess("", this);

    setCreds("ROLE_USER");
    assertion.assertAccess("", this);
  }

  @Test
  public void testMarshalUnmarshalNoAuth() {
    assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
      final RoleAccessAssertion assertion = new RoleAccessAssertion();
      assertion.setRequiredRoles(Collections.singleton("ROLE_USER"));
      final JSONObject marshalData = assertion.marshal();

      RoleAccessAssertion newAssertion = new RoleAccessAssertion();
      newAssertion.unmarshal(marshalData);
      newAssertion.assertAccess("", this);
    });
  }

  @Test
  public void testMarshalUnmarshalNotPermitted() {
    assertThrows(AccessDeniedException.class, () -> {
      setCreds("ROLE_OTHER");
      final RoleAccessAssertion assertion = new RoleAccessAssertion();
      assertion.setRequiredRoles(Collections.singleton("ROLE_USER"));
      final JSONObject marshalData = assertion.marshal();

      RoleAccessAssertion newAssertion = new RoleAccessAssertion();
      newAssertion.unmarshal(marshalData);
      newAssertion.assertAccess("", this);
    });
  }

  @Test
  public void testMarshalUnmarshalAllowed() {
    setCreds("ROLE_USER");
    final RoleAccessAssertion assertion = new RoleAccessAssertion();
    assertion.setRequiredRoles(Collections.singleton("ROLE_USER"));
    final JSONObject marshalData = assertion.marshal();

    RoleAccessAssertion newAssertion = new RoleAccessAssertion();
    newAssertion.unmarshal(marshalData);
    newAssertion.assertAccess("", this);
  }

  @Test
  public void testValidate() {
    List<Throwable> errors = new ArrayList<>();
    final RoleAccessAssertion assertion = new RoleAccessAssertion();
    assertion.validate(errors, null);
    assertEquals(1, errors.size());
    errors.clear();
    assertion.setRequiredRoles(Collections.singleton("ROLE_USER"));
    assertion.validate(errors, null);
    assertEquals(0, errors.size());
  }
}
