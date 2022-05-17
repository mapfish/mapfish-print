package org.mapfish.print.attribute;

import com.google.common.annotations.VisibleForTesting;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.locationtech.jts.util.Assert;
import org.locationtech.jts.util.AssertionFailedException;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.config.Template;
import org.mapfish.print.parser.HasDefaultValue;
import org.mapfish.print.parser.MapfishParser;
import org.mapfish.print.parser.OneOf;
import org.mapfish.print.parser.ParserUtils;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.PElement;
import org.mapfish.print.wrapper.PObject;
import org.mapfish.print.wrapper.json.PJsonArray;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.mapfish.print.wrapper.multi.PMultiObject;
import org.mapfish.print.wrapper.yaml.PYamlArray;
import org.mapfish.print.wrapper.yaml.PYamlObject;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

import static org.mapfish.print.parser.MapfishParser.stringRepresentation;

/**
 * Used for attribute that can have defaults specified in the YAML config file.
 *
 * @param <VALUE>
 */
public abstract class ReflectiveAttribute<VALUE> implements Attribute {
    /**
     * Name of attribute in the client config json.
     *
     * @see #printClientConfig(JSONWriter, Template)
     */
    public static final String JSON_NAME = "name";
    /**
     * Name of the required parameters object in the client config json.
     *
     * @see #printClientConfig(JSONWriter, Template)
     */
    public static final String JSON_CLIENT_PARAMS = "clientParams";
    /**
     * Name of the value suggestions object in the client config json.
     *
     * @see #printClientConfig(JSONWriter, Template)
     */
    public static final String JSON_CLIENT_INFO = "clientInfo";
    /**
     * A string describing the type of the attribute param in the clientConfig.
     */
    public static final String JSON_ATTRIBUTE_TYPE = "type";
    /**
     * If the parameter in the value object is another value object (and not a PObject or PArray) then this
     * will be a json object describing the embedded param in the same way as each object in clientParams.
     */
    public static final String JSON_ATTRIBUTE_EMBEDDED_TYPE = "embeddedType";
    /**
     * The default value of the attribute param in the optional params.
     */
    public static final String JSON_ATTRIBUTE_DEFAULT = "default";
    /**
     * Json field that declares if the param is an array.
     */
    public static final String JSON_ATTRIBUTE_IS_ARRAY = "isArray";
    private static final HashSet<Class<? extends Object>> VALUE_OBJ_FIELD_TYPE_THAT_SHOULD_BE_P_TYPE =
            createClassSet(PJsonArray.class, PJsonObject.class,
                           JSONObject.class, JSONArray.class);
    private static final HashSet<Class<? extends Object>> VALUE_OBJ_FIELD_NON_RECURSIVE_TYPE =
            createClassSet(PElement.class, PArray.class, PObject.class);
    private PYamlObject defaults;
    private String configName;

    private static HashSet<Class<? extends Object>> createClassSet(final Object... args) {
        final HashSet<Class<?>> classes = new HashSet<>();
        for (Object arg: args) {
            classes.add((Class<?>) arg);
        }
        return classes;
    }

    private void validateParamObject(final Class<?> typeToTest, final Set<Class<?>> tested) {
        if (!tested.contains(typeToTest)) {
            final Collection<Field> allAttributes = ParserUtils.getAllAttributes(typeToTest);
            Assert.isTrue(!allAttributes.isEmpty(),
                          "An attribute value object must have at least on public field.");
            for (Field attribute: allAttributes) {
                Class<?> type = attribute.getType();
                if (type.isArray()) {
                    type = type.getComponentType();
                }
                if (VALUE_OBJ_FIELD_NON_RECURSIVE_TYPE.contains(type) || isJavaType(type)) {
                    continue;
                }
                if (VALUE_OBJ_FIELD_TYPE_THAT_SHOULD_BE_P_TYPE.contains(type)) {
                    throw new AssertionFailedException(
                            typeToTest.getName() + "#" + attribute.getName() + " should not be a field in a" +
                                    " value object.  Instead use the more general " + PArray.class.getName() +
                                    " or " +
                                    PObject.class.getName());
                }
                tested.add(type);
                validateParamObject(type, tested);
            }
        }
    }

    private boolean isJavaType(final Class<?> type) {
        return type.getPackage() == null || type.getPackage().getName().startsWith("java.");
    }

    @VisibleForTesting
    @PostConstruct
    final void init() {
        if (this.defaults == null) {
            this.defaults = new PYamlObject(Collections.emptyMap(), getAttributeName());
        }
        validateParamObject(getValueType(), new HashSet<>());
    }

    /**
     * Return the type created by {@link #createValue(Template)}.
     */
    public abstract Class<? extends VALUE> getValueType();

    /**
     * The YAML config default values.
     *
     * @return the default values
     */
    public final PObject getDefaultValue() {
        return this.defaults;
    }

    /**
     * <p>Default values for this attribute. Example:</p>
     * <pre><code>
     * attributes:
     *   legend: !legend
     *     default:
     *       name: "Legend"</code></pre>
     *
     * @param defaultValue The default values.
     */
    public final void setDefault(final Map<String, Object> defaultValue) {
        this.defaults = new PYamlObject(defaultValue, getAttributeName());
    }

    @Override
    public final void setConfigName(final String configName) {
        this.configName = configName;
    }

    /**
     * Return a descriptive name of this attribute.
     */
    protected final String getAttributeName() {
        return getClass().getSimpleName().substring(0, 1).toLowerCase() +
                getClass().getSimpleName().substring(1);
    }

    /**
     * Create an instance of a attribute value object.  Each instance must be new and unique. Instances must
     * <em>NOT</em> be shared.
     *
     * The object will be populated from the json.  Each public field will be populated by looking up the
     * value in the json.
     *
     * If a field in the object has the {@link HasDefaultValue} annotation then no
     * exception will be thrown if the json does not contain a value.
     *
     * Fields in the object with the {@link OneOf} annotation must have one of the
     * fields in the request data.
     *
     * <ul>
     * <li>{@link String}</li>
     * <li>{@link Integer}</li>
     * <li>{@link Float}</li>
     * <li>{@link Double}</li>
     * <li>{@link Short}</li>
     * <li>{@link Boolean}</li>
     * <li>{@link Character}</li>
     * <li>{@link Byte}</li>
     * <li>{@link Enum}</li>
     * <li>PJsonObject</li>
     * <li>URL</li>
     * <li>Any enum</li>
     * <li>PJsonArray</li>
     * <li>any type with a 0 argument constructor</li>
     * <li>array of any of the above (String[], boolean[], PJsonObject[], ...)</li>
     * </ul>
     *
     * If there is a public
     * <code>{@value org.mapfish.print.parser.MapfishParser#POST_CONSTRUCT_METHOD_NAME}()</code>
     * method then it will be called after the fields are all set.
     *
     * In the case where the a parameter type is a normal POJO (not a special case like PJsonObject, URL,
     * enum, double, etc...) then it will be assumed that the json data is a json object and the parameters
     * will be recursively parsed into the new object as if it is also MapLayer parameter object.
     *
     * It is important to put values in the value object as public fields because reflection is used when
     * printing client config as well as generating documentation.  If a field is intended for the client
     * software as information but is not intended to be set (or sent as part of the request data), the field
     * can be a final field.
     *
     * @param template the template that this attribute is part of.
     */
    public abstract VALUE createValue(Template template);

    /**
     * Uses reflection on the object created by {@link #createValue(Template)} to
     * create the options.
     *
     * The public final fields are written as the field name as the key and the value as the value.
     *
     * The public (non-final) mandatory fields are written as part of clientParams and are written with the
     * field name as the key and the field type as the value.
     *
     * The public (non-final) {@link HasDefaultValue} fields are written as part of
     * clientOptions and are written with the field name as the key and an object as a value with a type
     * property with the type and a default property containing the default value.
     *
     * @param json the json writer to write to
     * @param template the template that this attribute is part of
     * @throws JSONException
     */
    @Override
    public final void printClientConfig(final JSONWriter json, final Template template) throws JSONException {
        try {
            Set<Class<?>> printed = new HashSet<>();
            final VALUE exampleValue = createValue(template);
            json.key(JSON_NAME).value(this.configName);
            json.key(JSON_ATTRIBUTE_TYPE).value(getValueType().getSimpleName());
            final Class<?> valueType = exampleValue.getClass();

            json.key(JSON_CLIENT_PARAMS);
            json.object();
            printClientConfigForType(json, exampleValue, valueType, this.defaults, printed);
            json.endObject();

            Optional<JSONObject> clientOptions = getClientInfo();
            clientOptions.ifPresent(jsonObject -> json.key(JSON_CLIENT_INFO).value(jsonObject));
        } catch (Throwable e) {
            // Note: If this test fails and you just added a new attribute, make
            // sure to set defaults in AbstractMapfishSpringTest.configureAttributeForTesting
            throw new Error("Error printing the clientConfig of: " + getValueType().getName(), e);
        }
    }

    /**
     * Return an object that will be added to the client config with the key <em>clientInfo</em>.
     */
    protected Optional<JSONObject> getClientInfo() throws JSONException {
        return Optional.empty();
    }

    private void printClientConfigForType(
            final JSONWriter json,
            final Object exampleValue,
            final Class<?> valueType,
            final PObject defaultValue,
            final Set<Class<?>> printed) throws JSONException, IllegalAccessException {

        final Collection<Field> mutableFields = ParserUtils.getAttributes(
                valueType, ParserUtils.FILTER_ONLY_REQUIRED_ATTRIBUTES::test);
        if (!mutableFields.isEmpty()) {
            for (Field attribute: mutableFields) {
                encodeAttributeValue(true, json, exampleValue, getDefaultValue(defaultValue, attribute),
                                     attribute, printed);
            }
        }
        final Collection<Field> hasDefaultFields = ParserUtils.getAttributes(
                valueType, ParserUtils.FILTER_HAS_DEFAULT_ATTRIBUTES::test);
        if (!hasDefaultFields.isEmpty()) {
            for (Field attribute: hasDefaultFields) {
                encodeAttributeValue(false, json, exampleValue, getDefaultValue(defaultValue, attribute),
                                     attribute, printed);
            }
        }
    }

    private Object getDefaultValue(final PObject defaultValue, final Field attribute) {
        if (defaultValue == null) {
            return null;
        }
        return defaultValue.opt(attribute.getName());
    }

    private void encodeAttributeValue(
            final boolean required,
            final JSONWriter json,
            final Object exampleValue,
            final Object defaultValue,
            final Field attribute,
            final Set<Class<?>> printed) throws JSONException, IllegalAccessException {
        json.key(attribute.getName());
        json.object();
        final Class<?> type = attribute.getType();
        final Class<?> typeOrComponentType = type.isArray() ? type.getComponentType() : type;

        if (!VALUE_OBJ_FIELD_NON_RECURSIVE_TYPE.contains(typeOrComponentType) &&
                !isJavaType(typeOrComponentType)) {
            if (printed.contains(typeOrComponentType)) {
                json.key(JSON_ATTRIBUTE_TYPE).value("recursiveDefinition");
            } else {
                Set<Class<?>> printedForSubTree = new HashSet<>(printed);
                printedForSubTree.add(typeOrComponentType);

                json.key(JSON_ATTRIBUTE_TYPE).value(stringRepresentation(type));
                json.key(JSON_ATTRIBUTE_EMBEDDED_TYPE);
                json.object();
                Object value = attribute.get(exampleValue);
                if (value == null) {
                    if (typeOrComponentType.isEnum()) {
                        if (typeOrComponentType.getEnumConstants().length > 0) {
                            value = typeOrComponentType.getEnumConstants()[0];
                        }
                    } else {
                        try {
                            value = typeOrComponentType.newInstance();
                        } catch (InstantiationException e) {
                            throw ExceptionUtils.getRuntimeException(e);
                        }
                    }
                }
                final Object childDefaultValue = getDefaultValue(((PObject) defaultValue), attribute);
                printClientConfigForType(json, value, typeOrComponentType, (PObject) childDefaultValue,
                                         printedForSubTree);
                json.endObject();
            }
        } else {
            final String typeDescription = getTypeDescription(typeOrComponentType);
            json.key(JSON_ATTRIBUTE_TYPE).value(typeDescription);
        }
        if (!required || defaultValue != null) {
            json.key(JSON_ATTRIBUTE_DEFAULT);
            Object valueToAdd = defaultValue;
            if (defaultValue == null) {
                valueToAdd = attribute.get(exampleValue);
            }

            if (valueToAdd instanceof PJsonArray) {
                valueToAdd = ((PJsonArray) valueToAdd).getInternalArray();
            } else if (valueToAdd instanceof PJsonObject) {
                valueToAdd = ((PJsonObject) valueToAdd).getInternalObj();
            } else if (valueToAdd instanceof PYamlObject) {
                valueToAdd = ((PYamlObject) valueToAdd).toJSON().getInternalObj();
            } else if (valueToAdd instanceof PYamlArray) {
                valueToAdd = ((PYamlArray) valueToAdd).toJSON().getInternalArray();
            }

            json.value(valueToAdd);
        }
        if (type.isArray()) {
            json.key(JSON_ATTRIBUTE_IS_ARRAY).value(type.isArray());
        }

        json.endObject();
    }

    private String getTypeDescription(final Class<?> type) {
        final String typeDescription;
        if (PArray.class.isAssignableFrom(type)) {
            typeDescription = "array";
        } else if (PObject.class.isAssignableFrom(type)) {
            typeDescription = "object";
        } else if (Double.class.isAssignableFrom(type)) {
            typeDescription = "double";
        } else if (Integer.class.isAssignableFrom(type)) {
            typeDescription = "int";
        } else if (Boolean.class.isAssignableFrom(type)) {
            typeDescription = "boolean";
        } else if (Byte.class.isAssignableFrom(type)) {
            typeDescription = "byte";
        } else if (Long.class.isAssignableFrom(type)) {
            typeDescription = "long";
        } else if (Float.class.isAssignableFrom(type)) {
            typeDescription = "float";
        } else {
            typeDescription = stringRepresentation(type);
        }
        return typeDescription;
    }

    @Override
    public Object getValue(
            @Nonnull final Template template,
            @Nonnull final String attributeName, @Nonnull final PObject requestJsonAttributes) {
        boolean errorOnExtraParameters = template.getConfiguration().isThrowErrorOnExtraParameters();
        final Object value = this.createValue(template);
        PObject pValue = requestJsonAttributes.optObject(attributeName);

        if (pValue != null) {
            PObject[] pValues = new PObject[]{pValue, this.getDefaultValue()};
            pValue = new PMultiObject(pValues);
        } else {
            final Object valueOpt = requestJsonAttributes.opt(attributeName);
            if (valueOpt != null) {
                String valueAsString;
                if (valueOpt instanceof PJsonArray) {
                    valueAsString = ((PJsonArray) valueOpt).getInternalArray().toString(2);
                } else if (valueOpt instanceof JSONArray) {
                    valueAsString = ((JSONArray) valueOpt).toString(2);
                } else {
                    valueAsString = valueOpt.toString();
                }
                final String message =
                        "Expected a JSON Object as the value for the element with the path: '" +
                                requestJsonAttributes.getPath(attributeName) + "' but instead "
                                + "got a '" + valueOpt.getClass().toString() +
                                "'.\nThe value is: \n" + valueAsString;
                throw new IllegalArgumentException(message);
            }
            pValue = this.getDefaultValue();
        }
        MapfishParser.parse(errorOnExtraParameters, pValue, value);
        return value;
    }
}
