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
import com.google.common.collect.Sets;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mapfish.print.config.access.AccessAssertionTestUtil.setCreds;

public class RoleAccessAssertionTest {

    @After
    public void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
    }

    @Test(expected = AssertionError.class)
    public void testSetRequiredRoles() throws Exception {
        final RoleAccessAssertion assertion = new RoleAccessAssertion();
        assertion.setRequiredRoles(Sets.newHashSet("ROLE_USER"));
        assertion.setRequiredRoles(Sets.newHashSet("ROLE_USER"));
    }

    @Test (expected = AuthenticationCredentialsNotFoundException.class)
    public void testAssertAccessNoCredentials() throws Exception {
        final RoleAccessAssertion assertion = new RoleAccessAssertion();
        assertion.setRequiredRoles(Sets.newHashSet("ROLE_USER"));

        assertion.assertAccess("", this);
    }

    @Test (expected = AccessDeniedException.class)
    public void testAssertAccessWrongCreds() throws Exception {
        final RoleAccessAssertion assertion = new RoleAccessAssertion();
        assertion.setRequiredRoles(Sets.newHashSet("ROLE_USER"));

        setCreds("ROLE_USER2");
        assertion.assertAccess("", this);
    }

    @Test
    public void testAssertAccessAllowed() throws Exception {
        final RoleAccessAssertion assertion = new RoleAccessAssertion();
        assertion.setRequiredRoles(Sets.newHashSet("ROLE_USER"));

        setCreds("ROLE_USER");
        assertion.assertAccess("", this);

        setCreds("ROLE_USER", "ROLE_OTHER");
        assertion.assertAccess("", this);
    }
    @Test
    public void testAssertAccessOneOf() throws Exception {
        final RoleAccessAssertion assertion = new RoleAccessAssertion();
        assertion.setRequiredRoles(Sets.newHashSet("ROLE_USER", "ROLE_USER2"));

        setCreds("ROLE_USER");
        assertion.assertAccess("", this);

        setCreds("ROLE_USER2");
        assertion.assertAccess("", this);

        setCreds("ROLE_OTHER", "ROLE_USER2");
        assertion.assertAccess("", this);

    }

    @Test (expected = AccessDeniedException.class)
    public void testAssertAccessOneOfFailed() throws Exception {
        final RoleAccessAssertion assertion = new RoleAccessAssertion();
        assertion.setRequiredRoles(Sets.newHashSet("ROLE_USER", "ROLE_USER2"));

        setCreds("ROLE_OTHER");
        assertion.assertAccess("", this);

    }

    @Test (expected = AuthenticationCredentialsNotFoundException.class)
    public void testAssertNoRolesNoCreds() throws Exception {
        final RoleAccessAssertion assertion = new RoleAccessAssertion();
        assertion.setRequiredRoles(Sets.<String>newHashSet());

        assertion.assertAccess("", this);
        setCreds("ROLE_OTHER", "ROLE_USER2");
        assertion.assertAccess("", this);
    }

    @Test
    public void testAssertNoRolesSomeCreds() throws Exception {
        final RoleAccessAssertion assertion = new RoleAccessAssertion();
        assertion.setRequiredRoles(Sets.<String>newHashSet());

        setCreds("ROLE_OTHER");
        assertion.assertAccess("", this);

        setCreds("ROLE_USER");
        assertion.assertAccess("", this);
    }

    @Test (expected = AuthenticationCredentialsNotFoundException.class)
    public void testMarshalUnmarshalNoAuth() throws Exception {
        final RoleAccessAssertion assertion = new RoleAccessAssertion();
        assertion.setRequiredRoles(Sets.newHashSet("ROLE_USER"));
        final JSONObject marshalData = assertion.marshal();

        RoleAccessAssertion newAssertion = new RoleAccessAssertion();
        newAssertion.unmarshal(marshalData);
        newAssertion.assertAccess("", this);
    }

    @Test (expected = AccessDeniedException.class)
    public void testMarshalUnmarshalNotPermitted() throws Exception {
        setCreds("ROLE_OTHER");
        final RoleAccessAssertion assertion = new RoleAccessAssertion();
        assertion.setRequiredRoles(Sets.newHashSet("ROLE_USER"));
        final JSONObject marshalData = assertion.marshal();

        RoleAccessAssertion newAssertion = new RoleAccessAssertion();
        newAssertion.unmarshal(marshalData);
        newAssertion.assertAccess("", this);
    }


    @Test
    public void testMarshalUnmarshalAllowed() throws Exception {
        setCreds("ROLE_USER");
        final RoleAccessAssertion assertion = new RoleAccessAssertion();
        assertion.setRequiredRoles(Sets.newHashSet("ROLE_USER"));
        final JSONObject marshalData = assertion.marshal();

        RoleAccessAssertion newAssertion = new RoleAccessAssertion();
        newAssertion.unmarshal(marshalData);
        newAssertion.assertAccess("", this);
    }


    @Test
    public void testValidate() throws Exception {
        List<Throwable> errors = Lists.newArrayList();
        final RoleAccessAssertion assertion = new RoleAccessAssertion();
        assertion.validate(errors, null);
        assertEquals(1, errors.size());
        errors.clear();
        assertion.setRequiredRoles(Sets.newHashSet("ROLE_USER"));
        assertion.validate(errors, null);
        assertEquals(0, errors.size());
    }
}