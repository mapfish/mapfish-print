package org.mapfish.print.config.access;

import org.json.JSONObject;
import org.mapfish.print.config.Configuration;

import java.util.List;

/**
 * This assertion always allows access.
 *
 * @author Jesse on 10/7/2014.
 */
public final class AlwaysAllowAssertion implements AccessAssertion {
    /**
     * A public instance that can be used by all resource in the default case.
     */
    public static final AlwaysAllowAssertion INSTANCE = new AlwaysAllowAssertion();

    @Override
    public void assertAccess(final String resourceDescription, final Object protectedResource) {
        // do nothing
    }

    @Override
    public JSONObject marshal() {
        return new JSONObject();
    }

    @Override
    public void unmarshal(final JSONObject encodedAssertion) {
        // nothing to do
    }

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        // do nothing
    }
}
