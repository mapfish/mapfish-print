/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.json.parser;

import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.mapfish.print.ExtraPropertyException;
import org.mapfish.print.MissingPropertyException;
import org.mapfish.print.json.JsonMissingException;
import org.mapfish.print.json.PJsonArray;
import org.mapfish.print.json.PJsonObject;
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

import static org.mapfish.print.json.parser.JsonParserUtils.FILTER_NON_FINAL_FIELDS;
import static org.mapfish.print.json.parser.JsonParserUtils.getAllAttributeNames;
import static org.mapfish.print.json.parser.JsonParserUtils.getAttributeNames;

/**
 * This class parses json parameter objects into the parameter object taken by {@link org.mapfish.print.map.MapLayerFactoryPlugin}
 * instances and into {@link org.mapfish.print.attribute.ReflectiveAttribute} value objects
 * <p/>
 * Essentially it maps the keys in the json object to public fields in the object obtained from the
 * {@link org.mapfish.print.map.MapLayerFactoryPlugin#createParameter()} method.
 * <p/>
 * There is a more explicit explanation in
 * {@link org.mapfish.print.attribute.ReflectiveAttribute#createValue(org.mapfish.print.config.Template)}
 *
 * @author Jesse on 4/3/14.
 * @see org.mapfish.print.attribute.ReflectiveAttribute
 * @see org.mapfish.print.map.MapLayerFactoryPlugin
 */
public final class MapfishJsonParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapfishJsonParser.class);
    private static final String POST_CONSTRUCT_METHOD_NAME = "postConstruct";

    /**
     * Populate the param object by obtaining the values from the like names values in the request data object.
     *
     * @param errorOnExtraProperties if true then throw an error when there are properties in the request data that are not in the
     *                               param.  Otherwise log them as a warning.
     * @param requestData            the layer configuration json.
     * @param objectToPopulate       the parameter object that will be passed to the layer factory or is the attribute value.
     * @param extraPropertyToIgnore  An array of properties to ignore in request data.  For example Layers do not need "type" but
     *                               the property has to be there for {@link org.mapfish.print.attribute.map.MapAttribute} to
     *                               be able to choose the correct plugin.
     */
    public void parse(final boolean errorOnExtraProperties, final PJsonObject requestData, final Object objectToPopulate,
                      final String... extraPropertyToIgnore) {
        checkForExtraProperties(errorOnExtraProperties, objectToPopulate.getClass(), requestData, extraPropertyToIgnore);

        final Collection<Field> allAttributes = JsonParserUtils.getAttributes(objectToPopulate.getClass(), FILTER_NON_FINAL_FIELDS);
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
                    throw new RuntimeException(e);
                }
            } catch (JsonMissingException e) {
                final HasDefaultValue hasDefaultValue = property.getAnnotation(HasDefaultValue.class);
                final OneOf oneOf = property.getAnnotation(OneOf.class);
                if (hasDefaultValue == null && oneOf == null) {
                    missingProperties.put(property.getName(), property.getType());
                }
            }
        }

        oneOfTracker.checkAllGroupsSatisfied();
        requiresTracker.checkAllRequirementsSatisfied();

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

    private void checkForExtraProperties(final boolean errorOnExtraProperties, final Class<?> paramClass,
                                         final PJsonObject layerJson, final String[] extraPropertyToIgnore) {
        final Collection<String> acceptableKeyValues = Sets.newHashSet();
        for (String name : getAttributeNames(paramClass, FILTER_NON_FINAL_FIELDS)) {
            acceptableKeyValues.add(name.toLowerCase());
        }
        if (extraPropertyToIgnore != null) {
            for (String propName : extraPropertyToIgnore) {
                acceptableKeyValues.add(propName.toLowerCase());
            }
        }

        Collection<String> extraProperties = Sets.newHashSet();
        @SuppressWarnings("unchecked")
        final Iterator<String> keys = layerJson.getInternalObj().keys();
        while (keys.hasNext()) {
            String next = keys.next();
            if (!acceptableKeyValues.contains(next.toLowerCase())) {
                extraProperties.add(next);
            }
        }
        if (!extraProperties.isEmpty()) {
            String msg = "Extra properties were found in the request data at: " + layerJson.getCurrentPath() + ": ";
            ExtraPropertyException exception = new ExtraPropertyException(msg, extraProperties, getAttributeNames(paramClass,
                    Predicates.<Field>alwaysTrue()));
            if (errorOnExtraProperties) {
                throw exception;
            } else {
                LOGGER.warn(exception.getMessage(), exception);
            }
        }
    }


    private Object parseValue(final boolean errorOnExtraProperties, final String[] extraPropertyToIgnore, final Class<?> type,
                              final String fieldName, final PJsonObject layerJson) throws
            UnsupportedTypeException {
        String name = fieldName;
        if (!layerJson.has(name) && layerJson.has(name.toLowerCase())) {
            name = name.toLowerCase();
        }

        Object value;
        if (type == String.class) {
            value = layerJson.getString(name);
        } else if (type == Integer.class || type == int.class) {
            value = layerJson.getInt(name);
        } else if (type == Double.class || type == double.class) {
            value = layerJson.getDouble(name);
        } else if (type == Float.class || type == float.class) {
            value = layerJson.getFloat(name);
        } else if (type == Boolean.class || type == boolean.class) {
            value = layerJson.getBool(name);
        } else if (type == PJsonObject.class) {
            value = layerJson.getJSONObject(name);
        } else if (type == PJsonArray.class) {
            value = layerJson.getJSONArray(name);
        } else if (type == URL.class) {
            try {
                value = new URL(layerJson.getString(name));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        } else if (type.isArray()) {
            final PJsonArray jsonArray = layerJson.getJSONArray(name);
            value = Array.newInstance(type.getComponentType(), jsonArray.size());

            for (int i = 0; i < jsonArray.size(); i++) {
                Object arrayValue = parseArrayValue(errorOnExtraProperties, extraPropertyToIgnore, type.getComponentType(), i, jsonArray);
                if (arrayValue == null) {
                    throw new IllegalArgumentException("Arrays cannot have null values in them.  Error found with: " + jsonArray +
                                                       " when being converted to a " + type.getComponentType());
                }
                Array.set(value, i, arrayValue);
            }
        } else if (type.isEnum()) {
            value = parseEnum(type, layerJson.getPath(fieldName), layerJson.getString(name));
        } else {
            try {
                value = type.newInstance();
                PJsonObject jsonObject = layerJson.getJSONObject(name);
                parse(errorOnExtraProperties, jsonObject, value, extraPropertyToIgnore);
            } catch (InstantiationException e) {
                throw new UnsupportedTypeException(type);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private Object parseEnum(final Class<?> type, final String path, final String enumString) {
        Object value;
        try {
            value = Enum.valueOf((Class<Enum>) type, enumString);
        } catch (IllegalArgumentException e) {
            // not the name, maybe the ordinal;

            try {
                int ordinal = Integer.parseInt(enumString);
                final Object[] enumConstants = type.getEnumConstants();
                if (ordinal < enumConstants.length) {
                    value = enumConstants[ordinal];
                } else {
                    throw enumError(enumConstants, path, enumString);
                }
            } catch (NumberFormatException ne) {
                throw enumError(type.getEnumConstants(), path, enumString);
            }
        }
        return value;
    }

    private IllegalArgumentException enumError(final Object[] enumConstants, final String path, final String enumString) {
        return new IllegalArgumentException(path + " should be an enumeration value or ordinal " +
                                            "but was: " + enumString + "\nEnum constants are: " + Arrays.toString(enumConstants));
    }

    private Object parseArrayValue(final boolean errorOnExtraProperties, final String[] extraPropertyToIgnore, final Class<?> type,
                                   final int i, final PJsonArray jsonArray) throws UnsupportedTypeException {

        Object value;
        if (type == String.class) {
            value = jsonArray.getString(i);
        } else if (type == Integer.class || type == int.class) {
            value = jsonArray.getInt(i);
        } else if (type == Double.class || type == double.class) {
            value = jsonArray.getDouble(i);
        } else if (type == Float.class || type == float.class) {
            value = jsonArray.getFloat(i);
        } else if (type == Boolean.class || type == boolean.class) {
            value = jsonArray.getBool(i);
        } else if (type == PJsonObject.class) {
            value = jsonArray.getJSONObject(i);
        } else if (type == PJsonArray.class) {
            value = jsonArray.getJSONArray(i);
        } else if (type == URL.class) {
            try {
                value = new URL(jsonArray.getString(i));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        } else if (type.isEnum()) {
            value = parseEnum(type, jsonArray.getPath("" + i), jsonArray.getString(i));
        } else {
            try {
                value = type.newInstance();
                PJsonObject jsonObject = jsonArray.getJSONObject(i);
                parse(errorOnExtraProperties, jsonObject, value, extraPropertyToIgnore);
            } catch (InstantiationException e) {
                throw new UnsupportedTypeException(type);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return value;
    }

    /**
     * Get the value of a primitive type from the request data.
     *
     * @param fieldName   the name of th attribute to get from the request data.
     * @param valueClass  the type to get
     * @param requestData the data to retrieve the value from
     */
    public Object parsePrimitive(final String fieldName, final Class valueClass, final PJsonObject requestData) {
        try {
            return parseValue(false, new String[0], valueClass, fieldName, requestData);
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
    }

    /**
     * Return a friendly representation of the class for printing the configuration options to a client.
     *
     * @param aClass the class to inspect
     */
    public static String stringRepresentation(final Class<? extends Object> aClass) {
        return aClass.getSimpleName();
    }

    private static final class UnsupportedTypeException extends Exception {
        private final Class<?> type;

        private UnsupportedTypeException(final Class<?> type) {
            this.type = type;
        }
    }
}
