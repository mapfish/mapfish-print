package org.mapfish.print.attribute;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.parser.MapfishParser;
import org.mapfish.print.wrapper.PObject;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.mapfish.print.wrapper.multi.PMultiObject;

import java.util.List;
import javax.annotation.Nonnull;

/**
 * A type of attribute whose value is a primitive type.
 * <ul>
 * <li>{@link java.lang.String}</li>
 * <li>{@link java.lang.String[]}</li>
 * <li>{@link java.lang.Integer}</li>
 * <li>{@link java.lang.Double}</li>
 * <li>{@link java.lang.Boolean}</li>
 * </ul>
 *
 * @param <Value> The value type of the attribute
 */
public abstract class PrimitiveAttribute<Value> implements Attribute {
    private Class<Value> valueClass;
    /** The default value. */
    protected Value defaultValue;

    private String configName;

    /**
     * Constructor.
     *
     * @param valueClass the type of the value of this attribute
     */
    public PrimitiveAttribute(final Class<Value> valueClass) {
        this.valueClass = valueClass;
    }

    public final Class<Value> getValueClass() {
        return this.valueClass;
    }

    /**
     * <p>A default value for this attribute.</p>
     * @param value The default value.
     */
    public abstract void setDefault(final Value value);

    public final Value getDefault() {
        return this.defaultValue;
    }

    @Override
    public final void setConfigName(final String configName) {
        this.configName = configName;
    }

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        // no checks required
    }

    /**
     * Validation of the value from a request.
     * @param value The value from a request.
     */
    public void validateValue(final Object value) {
    }

    @Override
    public final void printClientConfig(final JSONWriter json, final Template template) throws JSONException {
        json.key(ReflectiveAttribute.JSON_NAME).value(this.configName);
        json.key(ReflectiveAttribute.JSON_ATTRIBUTE_TYPE).value(clientConfigTypeDescription());
        if (getDefault() != null) {
            json.key(ReflectiveAttribute.JSON_ATTRIBUTE_DEFAULT).value(getDefault());
        }
    }

    /**
     * Returns a string that is a technical description of the type.  In other words, a string that the client software
     * (user of the capabilities response) can use to create a request or UI.
     */
    protected String clientConfigTypeDescription() {
        return this.valueClass.getSimpleName();
    }

    @Override
    public Object getValue(@Nonnull final Template template,
                           @Nonnull final String attributeName,
                           @Nonnull final PObject requestJsonAttributes) {
        final Object defaultVal = getDefault();
        PObject jsonToUse = requestJsonAttributes;
        if (defaultVal != null) {
            final JSONObject obj = new JSONObject();
            obj.put(attributeName, defaultVal);
            final PObject[] pValues = new PObject[]{requestJsonAttributes, new PJsonObject(obj, "default_" + attributeName)};
            jsonToUse = new PMultiObject(pValues);
        }
        return MapfishParser.parsePrimitive(attributeName, this, jsonToUse);
    }
}
