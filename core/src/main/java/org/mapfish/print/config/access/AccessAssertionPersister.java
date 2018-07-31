package org.mapfish.print.config.access;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * Class for marshalling and unmarshalling AccessAssertionObjects to and from JSON.
 */
public final class AccessAssertionPersister {
    private static final String JSON_CLASS_NAME = "className";
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Load assertion from the provided json or throw exception if not possible.
     *
     * @param encodedAssertion the assertion as it was encoded in JSON.
     */
    public AccessAssertion unmarshal(final JSONObject encodedAssertion) {
        final String className;
        try {
            className = encodedAssertion.getString(JSON_CLASS_NAME);
            final Class<?> assertionClass =
                    Thread.currentThread().getContextClassLoader().loadClass(className);
            final AccessAssertion assertion =
                    (AccessAssertion) this.applicationContext.getBean(assertionClass);
            assertion.unmarshal(encodedAssertion);

            return assertion;
        } catch (JSONException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Marshal the assertion as a JSON object.
     *
     * @param assertion the assertion to marshal
     */
    public JSONObject marshal(final AccessAssertion assertion) {
        final JSONObject jsonObject = assertion.marshal();
        if (jsonObject.has(JSON_CLASS_NAME)) {
            throw new AssertionError("The toJson method in AccessAssertion: '" + assertion.getClass() +
                                             "' defined a JSON field " + JSON_CLASS_NAME +
                                             " which is a reserved keyword and is not permitted to be used " +
                                             "in toJSON method");
        }
        try {
            jsonObject.put(JSON_CLASS_NAME, assertion.getClass().getName());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return jsonObject;
    }
}
