package org.mapfish.print.wrapper;

import java.util.Iterator;


/**
 * Object wrapper interface for Json and Yaml parsing.
 *
 * @author Stéphane Brunner on 11/04/14.
 */
public interface PObject {

    /**
     * Get the value for the key.
     *
     * @param key the key identifying the value to obtain.
     */
    Object opt(final String key);

    /**
     * Get a property as a string or throw an exception.
     * @param key the property name
     */
    String getString(final String key);

    /**
     * Get a property as a string or null.
     * @param key the property name
     */
    String optString(final String key);

    /**
     * Get a property as a string or defaultValue.
     * @param key the property name
     * @param defaultValue the default value
     */
    String optString(final String key, final String defaultValue);

    /**
     * Get a property as a int or throw an exception.
     * @param key the property name
     */
    int getInt(final String key);

    /**
     * Get a property as a int or MIN_VALUE.
     * @param key the property name
     */
    Integer optInt(final String key);

    /**
     * Get a property as a int or default value.
     * @param key the property name
     * @param defaultValue the default value
     */
    Integer optInt(final String key, final Integer defaultValue);
    /**
     * Get a property as a long or throw an exception.
     * @param key the property name
     */
     long getLong(String key);
    /**
     * Get a property as a long or MIN_VALUE.
     * @param key the property name
     */
    Long optLong(final String key);

    /**
     * Get a property as a long or default value.
     * @param key the property name
     * @param defaultValue the default value
     */
    long optLong(final String key, final long defaultValue);
    /**
     * Get a property as a double or throw an exception.
     * @param key the property name
     */
    double getDouble(final String key);

    /**
     * Get a property as a double or defaultValue.
     * @param key the property name
     */
    Double optDouble(final String key);

    /**
     * Get a property as a double or defaultValue.
     * @param key the property name
     * @param defaultValue the default value
     */
    Double optDouble(final String key, final Double defaultValue);

    /**
     * Get a property as a float or throw an exception.
     * @param key the property name
     */
    float getFloat(final String key);

    /**
     * Get a property as a float or null.
     * @param key the property name
     */
    Float optFloat(final String key);

    /**
     * Get a property as a float or Default vaule.
     * @param key the property name
     * @param defaultValue default value
     */
    Float optFloat(final String key, final Float defaultValue);

    /**
     * Get a property as a boolean or throw exception.
     * @param key the property name
     */
    boolean getBool(final String key);

    /**
     * Get a property as a boolean or null.
     * @param key the property name
     */
    Boolean optBool(final String key);

    /**
     * Get a property as a boolean or default value.
     * @param key the property name
     * @param defaultValue the default
     */
    Boolean optBool(final String key, final Boolean defaultValue);

    /**
     * Get a property as a object or throw exception.
     * @param key the property name
     */
    PObject getObject(final String key);

    /**
     * Get a property as a object or null.
     * @param key the property name
     */
    PObject optObject(final String key);

    /**
     * Get a property as a object or null.
     * @param key the property name
     * @param defaultValue default
     */
    PObject optObject(final String key, final PObject defaultValue);

    /**
     * Get a property as a array or throw exception.
     * @param key the property name
     */
    PArray getArray(final String key);

    /**
     * Get a property as a array or null.
     * @param key the property name
     */
    PArray optArray(final String key);

    /**
     * Is the property an array.
     * @param key the property name
     */
    boolean isArray(final String key);

    /**
     * Get a property as a array or default.
     * @param key the property name
     * @param defaultValue default
     */
    PArray optArray(final String key, final PArray defaultValue);

    /**
     * Get an iterator of all keys in this objects.
     * @return The keys iterator
     */
    Iterator<String> keys();

    /**
     * Get the number of properties in this object.
     */
    int size();

    /**
     * Check if the object has a property with the key.
     * @param key key to check for.
     */
    boolean has(final String key);

    /**
     * Gets the string representation of the path to the current element.
     *
     * @param key the leaf key
     */
    String getPath(final String key);

    /**
     * Gets the string representation of the path to the current element.
     */
    String getCurrentPath();
}
