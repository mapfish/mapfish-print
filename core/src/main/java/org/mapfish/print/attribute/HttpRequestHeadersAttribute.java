package org.mapfish.print.attribute;

import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.PObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Attribute representing the headers from the request.
 * <p>
 * This is an internal attribute and is added to the system automatically.  It does not need to be added in
 * the config.yaml file.
 */
@InternalAttribute
public final class HttpRequestHeadersAttribute
        extends ReflectiveAttribute<HttpRequestHeadersAttribute.Value> {
    /**
     * Constructor that calls init.
     */
    public HttpRequestHeadersAttribute() {
        init();
    }

    @Override
    public Class<Value> getValueType() {
        return Value.class;
    }

    @Override
    public Value createValue(final Template template) {
        return new Value();
    }

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        // nothing to do
    }

    /**
     * The object containing the attribute data.
     */
    public static final class Value {
        /**
         * The headers from the request.
         */
        public PObject requestHeaders;

        /**
         * Get all the headers in map form.
         */
        public Map<String, List<String>> getHeaders() {
            Map<String, List<String>> headerMap = new HashMap<>();

            final Iterator<String> keys = this.requestHeaders.keys();

            while (keys.hasNext()) {
                List<String> valuesAsList = new ArrayList<>();

                String headerName = keys.next();
                final PArray values = this.requestHeaders.optArray(headerName);
                if (values != null) {
                    for (int i = 0; i < values.size(); i++) {
                        valuesAsList.add(values.getString(i));
                    }
                } else {
                    valuesAsList.add(this.requestHeaders.getString(headerName));
                }

                headerMap.put(headerName, valuesAsList);
            }

            return headerMap;
        }
    }
}
