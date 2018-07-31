package org.mapfish.print;

import java.util.Map;
import java.util.Set;

/**
 * Indicates one or more properties are missing either from a config.yaml configuration file or from request
 * json.
 */
public final class MissingPropertyException extends RuntimeException {
    private final Map<String, Class<?>> missingProperties;
    private final Set<String> attributeNames;

    /**
     * Constructor.
     *
     * @param message the error message. A textual description of the missing properties (type and
     *         name) will be appended at the end.
     * @param missingProperties the properties that are missing
     * @param attributeNames all the allowed attribute names.
     */
    public MissingPropertyException(
            final String message, final Map<String, Class<?>> missingProperties,
            final Set<String> attributeNames) {
        super(createMessage(message, missingProperties, attributeNames));
        this.missingProperties = missingProperties;
        this.attributeNames = attributeNames;
    }

    private static String createMessage(
            final String message, final Map<String, Class<?>> missingProperties,
            final Set<String> attributeNames) {
        StringBuilder missingPropertyMessage =
                new StringBuilder(message).append("\n").append("Missing Properties: \n");
        for (Map.Entry<String, Class<?>> entry: missingProperties.entrySet()) {
            String type = entry.getValue().getName();
            if (entry.getValue().isArray()) {
                type = entry.getValue().getComponentType().getName() + "[]";
            }
            missingPropertyMessage.append("\n\t* ").append(type).append(" ").append(entry.getKey());
        }
        missingPropertyMessage.append("\n\nAll allowed properties are: \n");
        for (String attributeName: attributeNames) {
            missingPropertyMessage.append("\n\t* ").append(attributeName);
        }
        return missingPropertyMessage.toString();
    }

    public Map<String, Class<?>> getMissingProperties() {
        return this.missingProperties;
    }

    public Set<String> getAttributeNames() {
        return this.attributeNames;
    }
}
