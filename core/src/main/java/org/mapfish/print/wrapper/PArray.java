package org.mapfish.print.wrapper;


/**
 * Array wrapper interface for Json and Yaml parsing.
 */
public interface PArray {
    /**
     * Return the size of the array.
     */
    int size();

    /**
     * Get the element at the index as a object.
     * @param i the index of the object to access
     */
    PObject getObject(final int i);

    /**
     * Get the element at the index as a json array.
     * @param i the index of the element to access
     */
    PArray getArray(final int i);

    /**
     * Get the element at the index as an integer.
     * @param i the index of the element to access
     */
    int getInt(final int i);

    /**
     * Get the element at the index as a long.
     * @param i the index of the element to access
     */
    long getLong(final int i);

    /**
     * Get the element at the index as a float.
     * @param i the index of the element to access
     */
    float getFloat(final int i);

    /**
     * Get the element at the index as a double.
     * @param i the index of the element to access
     */
    double getDouble(final int i);

    /**
     * Get the element at the index as a string.
     * @param i the index of the element to access
     */
    String getString(final int i);

    /**
     * Get the element as a boolean.
     * @param i the index of the element to access
     */
    boolean getBool(final int i);

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

    /**
     * Get the object at the given index.
     *
     * @param i the index of the element to access
     */
    Object get(int i);
}
