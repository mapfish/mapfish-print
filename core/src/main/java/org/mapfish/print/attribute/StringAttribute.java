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

package org.mapfish.print.attribute;


/**
 * <p>Attribute that reads a string from the request data.</p>
 * [[examples=verboseExample]]
 */
public class StringAttribute extends PrimitiveAttribute<String> {

    private int maxLength = -1;

    /**
     * Constructor.
     */
    public StringAttribute() {
        super(String.class);
    }

    /**
     * The maximum number of characters allowed for this field (default: unlimited).
     *
     * @param maxLength Maximum number of characters.
     */
    public final void setMaxLength(final int maxLength) {
        this.maxLength = maxLength;
    }

    public final int getMaxLength() {
        return this.maxLength;
    }

    @Override
    public final void validateValue(final Object value) {
        if (this.maxLength >= 0 && value instanceof String) {
            String text = (String) value;
            if (text.length() > this.maxLength) {
                throw new IllegalArgumentException("text contains more than " + this.maxLength + " characters");
            }
        }
    }
}
