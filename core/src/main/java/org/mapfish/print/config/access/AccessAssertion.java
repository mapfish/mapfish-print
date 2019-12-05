package org.mapfish.print.config.access;

import org.json.JSONObject;
import org.mapfish.print.config.ConfigurationObject;

/**
 * An access assertion is a check that the current user has particular access/properties to allow access to a
 * particular resource like a template or completed report.
 * <p>
 * All implementations must be serializable.
 * </p>
 */
public interface AccessAssertion extends ConfigurationObject {
    /**
     * Checks that the user can access the resource.
     *
     * Will throw
     * {@link org.springframework.security.authentication.AuthenticationCredentialsNotFoundException}
     * if the user has not logged in or supplied credentials.
     *
     * Will throw {@link org.springframework.security.access.AccessDeniedException} if the user is logged in
     * but may not access the resource.
     *
     * @param resourceDescription a string describing the resource for logging and exception throwing
     *         purposes
     * @param protectedResource the resource being protected.
     */
    void assertAccess(String resourceDescription, Object protectedResource);

    /**
     * Encode the assertion as JSON for later loading.  Each call should return a unique JSON object.
     */
    JSONObject marshal();

    /**
     * Load the assertion properties from the JSON data.
     *
     * @param encodedAssertion the assertion encoded as JSON.
     */
    void unmarshal(JSONObject encodedAssertion);

    /**
     * Deep copy of this access assertion.
     *
     * @return the copy
     */
    AccessAssertion copy();

}
