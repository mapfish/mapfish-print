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

import java.util.*;

public abstract class CollectionUtils {
    /**
     * @deprecated use {@link java.util.Arrays#asList} instead.
     */
    public static <T> List<T> createList(T[] values) {
        return Arrays.asList(values);
    }

    public static <T> Set<T> createSet(T[] values) {
        Set<T> result=new HashSet<T>(values.length);
        for (int i = 0; i < values.length; ++i) {
            result.add(values[i]);
        }
        return result;
    }
}
