package org.mapfish.print.config.access;

import org.junit.After;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.Assert.fail;

public class AndAccessAssertionTest extends AbstractMapfishSpringTest {

    @Autowired
    ApplicationContext applicationContext;

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test(expected = AssertionError.class)
    public void testSetPredicates() {

        final AndAccessAssertion andAssertion = applicationContext.getBean(AndAccessAssertion.class);
        andAssertion.setPredicates(AlwaysAllowAssertion.INSTANCE, AlwaysAllowAssertion.INSTANCE);
        andAssertion.setPredicates(AlwaysAllowAssertion.INSTANCE);
    }

    @Test(expected = AccessDeniedException.class)
    public void testAssertAccessNotAllowed() {
        final AndAccessAssertion andAssertion = applicationContext.getBean(AndAccessAssertion.class);
        AccessAssertion pred1 =
                new RoleAccessAssertion().setRequiredRoles(Collections.singleton("ROLE_USER"));
        AccessAssertion pred2 =
                new RoleAccessAssertion().setRequiredRoles(Collections.singleton("ROLE_OTHER"));
        andAssertion.setPredicates(pred1, pred2);

        AccessAssertionTestUtil.setCreds("ROLE_USER");
        andAssertion.assertAccess("", this);
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
