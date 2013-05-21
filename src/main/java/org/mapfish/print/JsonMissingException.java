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

package org.mapfish.print;

import org.mapfish.print.utils.PJsonElement;

/**
 * Thrown when an attribute is missing in the spec.
 */
public class JsonMissingException extends PrintException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public JsonMissingException(PJsonElement pJsonObject, String key) {
        super("attribute [" + pJsonObject.getPath(key) + "] missing");
    }
}
