package org.mapfish.print;

import java.util.Collection;
import java.util.Set;

/**
 * Indicates one or more properties are not used either in a config.yaml configuration file or in the request
 * json.
 */
public final class ExtraPropertyException extends RuntimeException {
    private final Collection<String> extraProperties;
    private final Set<String> attributeNames;

    /**
     * Constructor.
     *
     * @param message the error message. A textual description of the extra properties. List of names
     *         will be appended at the end
     * @param extraProperties the properties that are extra
     * @param attributeNames all the allowed attribute names.
     */
    public ExtraPropertyException(
            final String message, final Collection<String> extraProperties,
            final Set<String> attributeNames) {
        super(createMessage(message, extraProperties, attributeNames));
        this.extraProperties = extraProperties;
        this.attributeNames = attributeNames;
    }

    private static String createMessage(
            final String message, final Collection<String> extraProperties,
            final Set<String> attributeNames) {
        StringBuilder missingPropertyMessage =
                new StringBuilder(message).append("\n").append("Extra Properties: \n");
        for (String extraProperty: extraProperties) {
            missingPropertyMessage.append("\n\t* ").append(extraProperty);
        }
        missingPropertyMessage.append("\n\nAll allowed properties are: \n");
        for (String attributeName: attributeNames) {
            missingPropertyMessage.append("\n\t* ").append(attributeName);
        }
        return missingPropertyMessage.toString();
    }

    public Collection<String> getExtraProperties() {
        return this.extraProperties;
    }

    public Set<String> getAttributeNames() {
        return this.attributeNames;
    }
}
