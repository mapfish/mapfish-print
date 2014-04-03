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
import org.mapfish.print.json.PJsonObject;

import java.util.Iterator;


/**
 * The attributes for {@link org.mapfish.print.processor.jasper.TableListProcessor}.
 */
public class TableListAttribute extends AbstractAttribute<TableListAttribute.TableListAttributeValue> {

    @Override
    public final TableListAttributeValue getValue(final Template template, final PJsonObject values, final String name) {
        return new TableListAttributeValue(values.getJSONObject(name));
    }

    @Override
    protected final String getType() {
        return "tablelist";
    }
    /**
     * The value of {@link org.mapfish.print.attribute.TableAttribute}.
     */
    public static class TableListAttributeValue implements Iterable<Object> {

        private final PJsonObject jsonObject;

        TableListAttributeValue(final PJsonObject jsonObject) {
            this.jsonObject = jsonObject;
        }

        public final PJsonObject getJsonObject() {
            return this.jsonObject;
        }

        @Override
        public final Iterator<Object> iterator() {
            return null;
        }
    }
}
