/*
 * Copyright (C) 2013  Camptocamp
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

package org.mapfish.print.config.layout;

import java.util.ArrayList;

import org.ho.yaml.wrapper.DefaultCollectionWrapper;

/**
 * Just to make sure the values of the hash have the good type.
 */
@SuppressWarnings({"RawUseOfParameterizedType"})
public class Exceptions extends ArrayList<CellException> {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static class Wrapper extends DefaultCollectionWrapper {
        public Wrapper(Class<?> type) {
            super(type);
        }

        public Class<CellException> componentType() {
            return CellException.class;
        }

        public boolean isTyped() {
            return true;
        }
    }
}