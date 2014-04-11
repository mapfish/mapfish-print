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

import org.mapfish.print.config.Template;
import org.mapfish.print.parser.HasDefaultValue;
import org.mapfish.print.wrapper.json.PJsonArray;


/**
 * The attributes for {@link org.mapfish.print.processor.jasper.TableListProcessor}.
 */
public final class TableListAttribute extends ArrayReflectiveAttribute<TableListAttribute.TableListAttributeValue> {

    @Override
    public TableListAttributeValue createValue(final Template template) {
        return new TableListAttributeValue();
    }

    /**
     * The value of {@link org.mapfish.print.attribute.TableAttribute}.
     */
    public static final class TableListAttributeValue {
        /**
         * The id of the table.
         */
        public String id;
        /**
         * A more human-friendly name for the table.
         */
        @HasDefaultValue
        public String displayName;
        /**
         * The column names.
         */
        public String[] columns;
        /**
         * The row data for each table.
         */
        public PJsonArray[] data;

        public String getDisplayName() {
            return this.displayName == null ? this.id : this.displayName;
        }
    }
}
