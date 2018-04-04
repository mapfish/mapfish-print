package org.mapfish.print.parser;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.ExtraPropertyException;
import org.mapfish.print.MissingPropertyException;
import org.mapfish.print.attribute.PrimitiveAttribute;
import org.mapfish.print.wrapper.ObjectMissingException;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.PObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import static org.mapfish.print.parser.ParserUtils.FILTER_NON_FINAL_FIELDS;
import static org.mapfish.print.parser.ParserUtils.getAllAttributeNames;
import static org.mapfish.print.parser.ParserUtils.getAttributeNames;

/**
 * This class parses json parameter objects into the parameter object taken by
 * {@link org.mapfish.print.map.MapLayerFactoryPlugin}
 * instances and into {@link org.mapfish.print.attribute.ReflectiveAttribute} value objects
 * <p></p>
 * Essentially it maps the keys in the json object to public fields in the object obtained from the
 * {@link org.mapfish.print.map.MapLayerFactoryPlugin#createParameter()} method.
 * <p></p>
 * There is a more explicit explanation in
 * {@link org.mapfish.print.attribute.ReflectiveAttribute#createValue(org.mapfish.print.config.Template)}
 *
 * @see org.mapfish.print.attribute.ReflectiveAttribute
 * @see org.mapfish.print.map.MapLayerFactoryPlugin
 */
public final class MapfishParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapfishParser.class);
    private static final String POST_CONSTRUCT_METHOD_NAME = "postConstruct";

    private MapfishParser() { }

    /**
     * Populate the param object by obtaining the values from the like names values in the request data object.
     *
     * @param errorOnExtraProperties if true then throw an error when there are properties in the request data that are not in the
     *                               param.  Otherwise log them as a warning.
     * @param requestData the layer configuration json.
     * @param objectToPopulate the parameter object that will be passed to the layer factory or is the attribute value.
     * @param extraPropertyToIgnore An array of properties to ignore in request data.  For example Layers do not need "type" but
     *                               the property has to be there for {@link org.mapfish.print.attribute.map.MapAttribute} to
     *                               be able to choose the correct plugin.
     */
    public static void parse(final boolean errorOnExtraProperties, final PObject requestData, final Object objectToPopulate,
                      final String... extraPropertyToIgnore) {
        checkForExtraProperties(errorOnExtraProperties, objectToPopulate.getClass(), requestData, extraPropertyToIgnore);

        final Collection<Field> allAttributes = ParserUtils.getAttributes(objectToPopulate.getClass(), FILTER_NON_FINAL_FIELDS);
        Map<String, Class<?>> missingProperties = Maps.newHashMap();

        final OneOfTracker oneOfTracker = new OneOfTracker();
        final RequiresTracker requiresTracker = new RequiresTracker();
        for (Field attribute : allAttributes) {
            oneOfTracker.register(attribute);
            requiresTracker.register(attribute);
        }

        for (Field property : allAttributes) {
            try {
                Object value;
                try {
                    value = parseValue(errorOnExtraProperties, extraPropertyToIgnore, property.getType(), property.getName(),
                            requestData);
                } catch (UnsupportedTypeException e) {
                    final String paramClassName = objectToPopulate.getClass().getName();
                    String type = e.type.getName();
                    if (e.type.isArray()) {
                        type = e.type.getComponentType().getName() + "[]";
                    }
                    throw new RuntimeException("The type '" + type + "' is not a supported type when parsing json.  " +
                                               "See documentation for supported types.\n\nUnsupported type found in " + paramClassName
                                               + " " +
                                               "under the property: " + property.getName() + "\n\nTo support more types add the type to" +
                                               " " +
                                               "parseValue and parseArrayValue in this class and add a test to the test class", e);
                }
                try {
                    oneOfTracker.markAsVisited(property);
                    requiresTracker.markAsVisited(property);
                    property.set(objectToPopulate, value);
                } catch (IllegalAccessException e) {
                    throw ExceptionUtils.getRuntimeException(e);
                }
            } catch (ObjectMissingException e) {
                final HasDefaultValue hasDefaultValue = property.getAnnotation(HasDefaultValue.class);
                final OneOf oneOf = property.getAnnotation(OneOf.class);
                final CanSatisfyOneOf canSatisfyOneOf = property.getAnnotation(CanSatisfyOneOf.class);
                if (hasDefaultValue == null && oneOf == null && canSatisfyOneOf == null) {
                    missingProperties.put(property.getName(), property.getType());
                }
            }
        }

        oneOfTracker.checkAllGroupsSatisfied(requestData.getCurrentPath());
        requiresTracker.checkAllRequirementsSatisfied(requestData.getCurrentPath());

        if (!missingProperties.isEmpty()) {
            String message = "Request Json is missing some required attributes at: '" + requestData.getCurrentPath() + "': ";
            throw new MissingPropertyException(message, missingProperties, getAllAttributeNames(objectToPopulate.getClass()));
        }

        try {
            final Method method = objectToPopulate.getClass().getMethod(POST_CONSTRUCT_METHOD_NAME);
            LOGGER.debug("Executing " + POST_CONSTRUCT_METHOD_NAME + " method on parameter object.");
            method.invoke(objectToPopulate);
        } catch (NoSuchMethodException e) {
            LOGGER.debug("No " + POST_CONSTRUCT_METHOD_NAME + " method on parameter object.");
        } catch (InvocationTargetException e) {
            final Throwable targetException = e.getTargetException();
            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            } else if (targetException instanceof Error) {
                throw (Error) targetException;
            }
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void checkForExtraProperties(final boolean errorOnExtraProperties, final Class<?> paramClass,
                                         final PObject layer, final String[] extraPropertyToIgnore) {
        final Collection<String> acceptableKeyValues = Sets.newHashSet();
        for (String name : getAttributeNames(paramClass, FILTER_NON_FINAL_FIELDS::test)) {
            acceptableKeyValues.add(name.toLowerCase());
        }
        if (extraPropertyToIgnore != null) {
            for (String propName : extraPropertyToIgnore) {
                acceptableKeyValues.add(propName.toLowerCase());
            }
        }

        Collection<String> extraProperties = Sets.newHashSet();
        @SuppressWarnings("unchecked")
        final Iterator<String> keys = layer.keys();
        while (keys.hasNext()) {
            String next = keys.next();
            if (!acceptableKeyValues.contains(next.toLowerCase())) {
                extraProperties.add(next);
            }
        }
        if (!extraProperties.isEmpty()) {
            String msg = "Extra properties were found in the request data at: " + layer.getCurrentPath() + ": ";
            ExtraPropertyException exception = new ExtraPropertyException(msg, extraProperties, getAttributeNames(paramClass,
                    field -> true));
            if (errorOnExtraProperties) {
                throw exception;
            } else {
                LOGGER.warn(exception.getMessage(), exception);
            }
        }
    }


    private static Object parseValue(final boolean errorOnExtraProperties, final String[] extraPropertyToIgnore, final Class<?> type,
                              final String fieldName, final PObject layer) throws
            UnsupportedTypeException {
        String name = fieldName;
        if (!layer.has(name) && layer.has(name.toLowerCase())) {
            name = name.toLowerCase();
        }

        Object value;
        if (type == String.class) {
            value = layer.getString(name);
        } else if (type == Integer.class || type == int.class) {
            value = layer.getInt(name);
        } else if (type == Long.class || type == long.class) {
            value = layer.getLong(name);
        } else if (type == Double.class || type == double.class) {
            value = layer.getDouble(name);
        } else if (type == Float.class || type == float.class) {
            value = layer.getFloat(name);
        } else if (type == Boolean.class || type == boolean.class) {
            value = layer.getBool(name);
        } else if (PObject.class.isAssignableFrom(type)) {
            value = layer.getObject(name);
        } else if (PArray.class.isAssignableFrom(type)) {
            value = layer.getArray(name);
        } else if (type == URL.class) {
            try {
                value = new URL(layer.getString(name));
            } catch (MalformedURLException e) {
                throw ExceptionUtils.getRuntimeException(e);
            }
        } else if (type.isArray()) {
            final PArray array = layer.getArray(name);
            value = Array.newInstance(type.getComponentType(), array.size());

            for (int i = 0; i < array.size(); i++) {
                Object arrayValue = parseArrayValue(errorOnExtraProperties, extraPropertyToIgnore, type.getComponentType(), i, array);
                if (arrayValue == null) {
                    throw new IllegalArgumentException("Arrays cannot have null values in them.  Error found with: " + array +
                                                       " when being converted to a " + type.getComponentType());
                }
                Array.set(value, i, arrayValue);
            }
        } else if (type.isEnum()) {
            value = parseEnum(type, layer.getPath(fieldName), layer.getString(name));
        } else {
            try {
                value = type.newInstance();
                PObject object = layer.getObject(name);
                parse(errorOnExtraProperties, object, value, extraPropertyToIgnore);
            } catch (InstantiationException e) {
                throw new UnsupportedTypeException(type, e);
            } catch (IllegalAccessException e) {
                throw ExceptionUtils.getRuntimeException(e);
            }
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static Object parseEnum(final Class<?> type, final String path, final String enumString) {
        // not the name, maybe the ordinal
        try {
            int ordinal = Integer.parseInt(enumString);
            final Object[] enumConstants = type.getEnumConstants();
            if (ordinal < enumConstants.length) {
                return enumConstants[ordinal];
            } else {
                throw enumError(enumConstants, path, enumString);
            }
        } catch (NumberFormatException ne) {
            final Object[] enumConstants = type.getEnumConstants();

            for (Object enumConstant : enumConstants) {
                if (enumConstant.toString().equalsIgnoreCase(enumString) ||
                    ((Enum) enumConstant).name().equalsIgnoreCase(enumString)) {
                    return enumConstant;
                }
            }
            throw enumError(type.getEnumConstants(), path, enumString);
        }
    }

    private static IllegalArgumentException enumError(final Object[] enumConstants, final String path, final String enumString) {
        return new IllegalArgumentException(path + " should be an enumeration value or ordinal " +
                                            "but was: " + enumString + "\nEnum constants are: " + Arrays.toString(enumConstants));
    }

    private static Object parseArrayValue(final boolean errorOnExtraProperties, final String[] extraPropertyToIgnore, final Class<?> type,
                                   final int i, final PArray array) throws UnsupportedTypeException {

        Object value;
        if (type == String.class) {
            value = array.getString(i);
        } else if (type == Integer.class || type == int.class) {
            value = array.getInt(i);
        } else if (type == Long.class || type == long.class) {
            value = array.getLong(i);
        } else if (type == Double.class || type == double.class) {
            value = array.getDouble(i);
        } else if (type == Float.class || type == float.class) {
            value = array.getFloat(i);
        } else if (type == Boolean.class || type == boolean.class) {
            value = array.getBool(i);
        } else if (PObject.class.isAssignableFrom(type)) {
            value = array.getObject(i);
        } else if (PArray.class.isAssignableFrom(type)) {
            value = array.getArray(i);
        } else if (type == URL.class) {
            try {
                value = new URL(array.getString(i));
            } catch (MalformedURLException e) {
                throw ExceptionUtils.getRuntimeException(e);
            }
        } else if (type.isEnum()) {
            value = parseEnum(type, array.getPath("" + i), array.getString(i));
        } else {
            try {
                value = type.newInstance();
                PObject object = array.getObject(i);
                parse(errorOnExtraProperties, object, value, extraPropertyToIgnore);
            } catch (InstantiationException e) {
                throw new UnsupportedTypeException(type, e);
            } catch (IllegalAccessException e) {
                throw ExceptionUtils.getRuntimeException(e);
            }
        }

        return value;
    }

    /**
     * Get the value of a primitive type from the request data.
     *
     * @param fieldName the name of the attribute to get from the request data.
     * @param pAtt the primitive attribute.
     * @param requestData the data to retrieve the value from.
     */
    public static Object parsePrimitive(final String fieldName, final PrimitiveAttribute<?> pAtt, final PObject requestData) {
        Class<?> valueClass = pAtt.getValueClass();
        Object value;
        try {
            value = parseValue(false, new String[0], valueClass, fieldName, requestData);
        } catch (UnsupportedTypeException e) {
            String type = e.type.getName();
            if (e.type.isArray()) {
                type = e.type.getComponentType().getName() + "[]";
            }
            throw new RuntimeException("The type '" + type + "' is not a supported type when parsing json.  " +
                                       "See documentation for supported types.\n\nUnsupported type found in attribute " + fieldName
                                       + "\n\nTo support more types add the type to " +
                                       "parseValue and parseArrayValue in this class and add a test to the test class", e);
        }
        pAtt.validateValue(value);

        return value;
    }

    /**
     * Return a friendly representation of the class for printing the configuration options to a client.
     *
     * @param aClass the class to inspect
     */
    public static String stringRepresentation(final Class<?> aClass) {
        return aClass.getSimpleName();
    }

    private static final class UnsupportedTypeException extends Exception {
        private final Class<?> type;

        private UnsupportedTypeException(final Class<?> type, final Exception cause) {
            super(cause);
            this.type = type;
        }
    }
}
