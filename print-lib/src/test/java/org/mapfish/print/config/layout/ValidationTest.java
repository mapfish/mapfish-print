/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Server
 *
 * MapFish Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MapFish Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.config.layout;

import org.mapfish.print.InvalidValueException;
import org.mapfish.print.PrintTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A few test around the {@link org.mapfish.print.config.layout.Block#validate()} method.
 */
public class ValidationTest extends PrintTestCase {
    public ValidationTest(String name) {
        super(name);
    }

    public void testAbsoluteInnerColumns() {
        Page page = new Page();
        ColumnsBlock rootColumns = new ColumnsBlock();
        page.setItems(Arrays.asList((Block) rootColumns));
        TextBlock textBlock = new TextBlock();
        final List<Block> cols = new ArrayList<Block>();
        cols.add(textBlock);
        rootColumns.setItems(cols);

        page.validate();

        final ColumnsBlock innerColumns = new ColumnsBlock();
        innerColumns.setItems(Arrays.asList((Block) textBlock));
        cols.add(innerColumns);

        page.validate();

        innerColumns.setWidth(100);
        innerColumns.setAbsoluteX(10);
        innerColumns.setAbsoluteY(100);

        try {
            page.validate();
            fail("must throw an InvalidValueException");
        } catch (InvalidValueException ex) {
            //expected
        }
    }

    public void testColumnsPartialAbsolute() {
        ColumnsBlock columns = new ColumnsBlock();
        TextBlock textBlock = new TextBlock();
        columns.setItems(Arrays.asList((Block) textBlock));
        columns.validate();

        columns.setWidth(100);
        try {
            columns.validate();
            fail("must throw an InvalidValueException");
        } catch (InvalidValueException ex) {
            //expected
        }

        columns.setAbsoluteX(100);
        try {
            columns.validate();
            fail("must throw an InvalidValueException");
        } catch (InvalidValueException ex) {
            //expected
        }

        columns.setAbsoluteY(100);
        columns.validate();
    }

    public void testMapPartialAbsolute() {
        MapBlock map = new MapBlock();

        try {
            map.validate();
            fail("must throw an InvalidValueException for width and height");
        } catch (InvalidValueException ex) {
            //expected
            map.setWidth("100");
            map.setHeight("100");
        }

        map.validate();

        map.setAbsoluteX("100");
        try {
            map.validate();
            fail("must throw an InvalidValueException");
        } catch (InvalidValueException ex) {
            //expected
            map.setAbsoluteY("100");
        }


        map.validate();
    }
}
