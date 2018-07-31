package org.mapfish.print.wrapper;

import org.mapfish.print.wrapper.json.PJsonArray;

/**
 * Common parent class for the Json and Yaml wrappers.
 */
public abstract class PElement {
    private final PElement parent;
    private final String contextName;

    /**
     * Constructor.
     *
     * @param parent the parent element
     * @param contextName the field name of this element in the parent.
     */
    protected PElement(final PElement parent, final String contextName) {
        this.parent = parent;
        this.contextName = contextName;
    }

    private static String getPathElement(final String val) {
        if (val == null) {
            return "";
        }
        if (val.contains(" ")) {
            return "'" + val + "'";
        } else {
            return val;
        }
    }

    /**
     * Gets the string representation of the path to the current JSON element.
     *
     * @param key the leaf key
     */
    public final String getPath(final String key) {
        StringBuilder result = new StringBuilder();
        addPathTo(result);
        result.append(".");
        result.append(getPathElement(key));
        return result.toString();
    }

    protected final String getContextName() {
        return this.contextName;
    }

    /**
     * Gets the string representation of the path to the current JSON element.
     */
    public final String getCurrentPath() {
        StringBuilder result = new StringBuilder();
        addPathTo(result);
        return result.toString();
    }

    /**
     * Append the path to the StringBuilder.
     *
     * @param result the string builder to add the path to.
     */
    protected final void addPathTo(final StringBuilder result) {
        if (this.parent != null) {
            this.parent.addPathTo(result);
            if (!(this.parent instanceof PJsonArray)) {
                result.append(".");
            }
        }
        result.append(getPathElement(this.contextName));
    }

    public final PElement getParent() {
        return this.parent;
    }
}
