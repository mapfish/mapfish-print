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

import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.wrapper.PArray;

import java.util.List;

/**
 * The attributes for {@link org.mapfish.print.processor.jasper.TableProcessor}.
 */
public final class TableAttribute extends ReflectiveAttribute<TableAttribute.TableAttributeValue> {
    @Override
    protected Class<TableAttributeValue> getValueType() {
        return TableAttributeValue.class;
    }
    @Override
    public TableAttributeValue createValue(final Template template) {
        return new TableAttributeValue();
    }
    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        // no checks required
    }

    /**
     * The value of {@link org.mapfish.print.attribute.TableAttribute}.
     */
    public static final class TableAttributeValue {

        /**
         * The column configuration names for the table.
         */
        public String[] columns;
        /**
         * An array for each table row.
         */
        public PArray[] data;
    }

}
