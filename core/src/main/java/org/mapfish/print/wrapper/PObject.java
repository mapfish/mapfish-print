package org.mapfish.print.wrapper;

import java.util.Iterator;


/**
 * Object wrapper interface for Json and Yaml parsing.
 */
public interface PObject {

    /**
     * Get the value for the key.
     *
     * @param key the key identifying the value to obtain.
     */
    Object opt(String key);

    /**
     * Get a property as a string or throw an exception.
     *
     * @param key the property name
     */
    String getString(String key);

    /**
     * Get a property as a string or null.
     *
     * @param key the property name
     */
    String optString(String key);

    /**
     * Get a property as a string or defaultValue.
     *
     * @param key the property name
     * @param defaultValue the default value
     */
    String optString(String key, String defaultValue);

    /**
     * Get a property as a int or throw an exception.
     *
     * @param key the property name
     */
    int getInt(String key);

    /**
     * Get a property as a int or MIN_VALUE.
     *
     * @param key the property name
     */
    Integer optInt(String key);

    /**
     * Get a property as a int or default value.
     *
     * @param key the property name
     * @param defaultValue the default value
     */
    Integer optInt(String key, Integer defaultValue);

    /**
     * Get a property as a long or throw an exception.
     *
     * @param key the property name
     */
    long getLong(String key);

    /**
     * Get a property as a long or MIN_VALUE.
     *
     * @param key the property name
     */
    Long optLong(String key);

    /**
     * Get a property as a long or default value.
     *
     * @param key the property name
     * @param defaultValue the default value
     */
    long optLong(String key, long defaultValue);

    /**
     * Get a property as a double or throw an exception.
     *
     * @param key the property name
     */
    double getDouble(String key);

    /**
     * Get a property as a double or defaultValue.
     *
     * @param key the property name
     */
    Double optDouble(String key);

    /**
     * Get a property as a double or defaultValue.
     *
     * @param key the property name
     * @param defaultValue the default value
     */
    Double optDouble(String key, Double defaultValue);

    /**
     * Get a property as a float or throw an exception.
     *
     * @param key the property name
     */
    float getFloat(String key);

    /**
     * Get a property as a float or null.
     *
     * @param key the property name
     */
    Float optFloat(String key);

    /**
     * Get a property as a float or Default vaule.
     *
     * @param key the property name
     * @param defaultValue default value
     */
    Float optFloat(String key, Float defaultValue);

    /**
     * Get a property as a boolean or throw exception.
     *
     * @param key the property name
     */
    boolean getBool(String key);

    /**
     * Get a property as a boolean or null.
     *
     * @param key the property name
     */
    Boolean optBool(String key);

    /**
     * Get a property as a boolean or default value.
     *
     * @param key the property name
     * @param defaultValue the default
     */
    Boolean optBool(String key, Boolean defaultValue);

    /**
     * Get a property as a object or throw exception.
     *
     * @param key the property name
     */
    PObject getObject(String key);

    /**
     * Get a property as a object or null.
     *
     * @param key the property name
     */
    PObject optObject(String key);

    /**
     * Get a property as a object or null.
     *
     * @param key the property name
     * @param defaultValue default
     */
    PObject optObject(String key, PObject defaultValue);

    /**
     * Get a property as a array or throw exception.
     *
     * @param key the property name
     */
    PArray getArray(String key);

    /**
     * Get a property as a array or null.
     *
     * @param key the property name
     */
    PArray optArray(String key);

    /**
     * Is the property an array.
     *
     * @param key the property name
     */
    boolean isArray(String key);

    /**
     * Get a property as a array or default.
     *
     * @param key the property name
     * @param defaultValue default
     */
    PArray optArray(String key, PArray defaultValue);

    /**
     * Get an iterator of all keys in this objects.
     *
     * @return The keys iterator
     */
    Iterator<String> keys();

    /**
     * Get the number of properties in this object.
     */
    int size();

    /**
     * Check if the object has a property with the key.
     *
     * @param key key to check for.
     */
    boolean has(String key);

    /**
     * Gets the string representation of the path to the current element.
     *
     * @param key the leaf key
     */
    String getPath(String key);

    /**
     * Gets the string representation of the path to the current element.
     */
    String getCurrentPath();
}
