/*
 * Copyright (C) 2008 Patrick Valsecchi
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  U
 */
package org.pvalsecc.misc;

public class StringUtils {
    /**
     * @return every items of the given array, separated by the given
     *         separator.
     */
    public static <T> String join(T[] list, String separator) {
        StringBuilder result = new StringBuilder();
        join(result, list, separator);
        return result.toString();
    }

    /**
     * Append to target every items of the given array, separated by the given
     * separator.
     */
    public static <T> void join(StringBuilder target, T[] list, String separator) {
        int j = 0;
        for (int i = 0; i < list.length; ++i) {
            T cur = list[i];
            if (cur != null) {
                if (j++ > 0) {
                    target.append(separator);
                }
                target.append(cur.toString());
            }
        }
    }

    /**
     * @return every items of the given collection, separated by the given
     *         separator.
     */
    public static <T> String join(Iterable<T> list, String separator) {
        StringBuilder result = new StringBuilder();
        join(result, list, separator);
        return result.toString();
    }

    /**
     * Append to target every items of the given collection, separated by the given
     * separator.
     */
    public static <T> void join(StringBuilder target, Iterable<T> list, String separator) {
        int i = 0;
        for (T cur : list) {
            if (cur != null) {
                if (i++ > 0) {
                    target.append(separator);
                }
                target.append(cur);
            }
        }
    }

    /**
     * Append the given value nb times.
     */
    public static void repeat(StringBuilder target, String value, int nb) {
        for (int i = 0; i < nb; ++i) {
            target.append(value);
        }
    }
}
