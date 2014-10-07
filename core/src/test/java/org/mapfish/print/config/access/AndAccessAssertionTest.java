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

import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.Assert.fail;

public class AndAccessAssertionTest extends AbstractMapfishSpringTest {

    @Autowired
    ApplicationContext applicationContext;

    @After
    public void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
    }

    @Test(expected = AssertionError.class)
    public void testSetPredicates() throws Exception {

        final AndAccessAssertion andAssertion = applicationContext.getBean(AndAccessAssertion.class);
        andAssertion.setPredicates(AlwaysAllowAssertion.INSTANCE, AlwaysAllowAssertion.INSTANCE);
        andAssertion.setPredicates(AlwaysAllowAssertion.INSTANCE);
    }
    @Test(expected = AccessDeniedException.class)
    public void testAssertAccessNotAllowed() throws Exception {
        final AndAccessAssertion andAssertion = applicationContext.getBean(AndAccessAssertion.class);
        AccessAssertion pred1 = new RoleAccessAssertion().setRequiredRoles(Sets.newHashSet("ROLE_USER"));
        AccessAssertion pred2 = new RoleAccessAssertion().setRequiredRoles(Sets.newHashSet("ROLE_OTHER"));
        andAssertion.setPredicates(pred1, pred2);

        AccessAssertionTestUtil.setCreds("ROLE_USER");
        andAssertion.assertAccess("", this);
    }
    @Test
    public void testAssertAccessAllowed() throws Exception {
        final AndAccessAssertion andAssertion = applicationContext.getBean(AndAccessAssertion.class);
        AccessAssertion pred1 = new RoleAccessAssertion().setRequiredRoles(Sets.newHashSet("ROLE_USER"));
        AccessAssertion pred2 = new RoleAccessAssertion().setRequiredRoles(Sets.newHashSet("ROLE_OTHER"));
        andAssertion.setPredicates(pred1, pred2);

        AccessAssertionTestUtil.setCreds("ROLE_USER", "ROLE_OTHER");
        andAssertion.assertAccess("", this);
    }

    @Test
    public void testMarshalUnmarshal() throws Exception {
        final AndAccessAssertion andAssertion = applicationContext.getBean(AndAccessAssertion.class);
        AccessAssertion pred1 = new RoleAccessAssertion().setRequiredRoles(Sets.newHashSet("ROLE_USER"));
        AccessAssertion pred2 = new RoleAccessAssertion().setRequiredRoles(Sets.newHashSet("ROLE_OTHER"));
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