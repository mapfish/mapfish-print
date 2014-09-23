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

package org.mapfish.print;


/**
 * Util class for exception handling.
 */
public final class ExceptionUtils {

    private ExceptionUtils() { }

    /**
     * Returns a {@link RuntimeException} for the given exception.
     *
     * @param exc An exception.
     * @return A {@link RuntimeException}
     */
    public static RuntimeException getRuntimeException(final Throwable exc) {
        Throwable e = exc;
        while (e.getCause() instanceof RuntimeException) {
            e = e.getCause();
        }
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        } else {
            return new RuntimeException(exc);
        }
    }
}
