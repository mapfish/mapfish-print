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

package org.mapfish.print.config;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mapfish.print.PrintTestCase;
import org.mapfish.print.scalebar.Type;

public class CustomEnumWrapperTest extends PrintTestCase {

    @Test
    public void testSetObject() {
        CustomEnumWrapper wrapper = new CustomEnumWrapper(Type.class);
        wrapper.setObject("bar");
        assertSame(Type.BAR, wrapper.getObject());

        wrapper.setObject("Line");
        assertSame(Type.LINE, wrapper.getObject());

        wrapper.setObject("bar sUB");
        assertSame(Type.BAR_SUB, wrapper.getObject());

        wrapper.setObject("LINE");
        assertSame(Type.LINE, wrapper.getObject());

        wrapper.setObject("BAR_SUB");
        assertSame(Type.BAR_SUB, wrapper.getObject());
    }
}
