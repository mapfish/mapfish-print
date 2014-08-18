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

package org.mapfish.print.attribute.map;

import org.mapfish.print.config.Template;
import org.mapfish.print.parser.HasDefaultValue;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.PObject;
public final class TestMapAttribute extends GenericMapAttribute<TestMapAttribute.TestMapAttributeValues> {

    @Override
    protected Class<TestMapAttributeValues> getValueType() {
        return TestMapAttributeValues.class;
    }

    @Override
    public TestMapAttributeValues createValue(final Template template) {
        return new TestMapAttributeValues(template);
    }

    public final class TestMapAttributeValues extends GenericMapAttribute<?>.GenericMapAttributeValues {
        public int requiredElem;
        public int[] requiredArray;
        public EmbeddedTestAttribute embedded;
        public PArray pArray;
        public PObject pObject;

        @HasDefaultValue
        public EmbeddedTestAttribute optionalEmbedded;
        @HasDefaultValue
        public int[] optionalArray;

        public TestMapAttributeValues(Template template) {
            super(template, null);
            optionalArray = new int[]{1, 2};
        }

        @Override
        public Double getDpi() {
            return null;
        }

        @Override
        protected PArray getRawLayers() {
            return null;
        }
    }

    public static final class EmbeddedTestAttribute {
        public boolean embeddedElem;
    }

}
