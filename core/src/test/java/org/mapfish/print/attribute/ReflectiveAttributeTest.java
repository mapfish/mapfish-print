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

import com.vividsolutions.jts.util.AssertionFailedException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.wrapper.json.PJsonArray;
import org.mapfish.print.wrapper.json.PJsonObject;

import java.util.List;

public class ReflectiveAttributeTest {
    @Test(expected = AssertionFailedException.class)
    public void testPJsonObjIllegal() throws Exception {
        new TestReflectiveAtt(PJsonObjParamIllegal.class).init();
    }

    @Test(expected = AssertionFailedException.class)
    public void testPJsonArrayIllegal() throws Exception {
        new TestReflectiveAtt(PJsonArrayParamIllegal.class).init();
    }

    @Test(expected = AssertionFailedException.class)
    public void testJsonObjIllegal() throws Exception {
        new TestReflectiveAtt(JsonObjParamIllegal.class).init();
    }

    @Test(expected = AssertionFailedException.class)
    public void testJsonArrayIllegal() throws Exception {
        new TestReflectiveAtt(JsonArrayParamIllegal.class).init();
    }

    @Test(expected = AssertionFailedException.class)
    public void testEmpty() throws Exception {
        new TestReflectiveAtt(Empty.class).init();
    }

    private static class TestReflectiveAtt extends ReflectiveAttribute<Object> {

        private final Class<?> type;

        private TestReflectiveAtt(Class<?> type) {
            this.type = type;
        }

        @Override
        protected Class<?> getValueType() {
            return type;
        }

        @Override
        public Object createValue(Template template) {
            return null;
        }

        @Override
        public void validate(List<Throwable> validationErrors, final Configuration configuration) {

        }
    }

    static class PJsonObjParamIllegal {
        public PJsonObject p;
    }

    static class JsonObjParamIllegal {
        public JSONObject p;
    }

    static class PJsonArrayParamIllegal {
        public PJsonArray p;
    }

    static class JsonArrayParamIllegal {
        public JSONArray p;
    }
    static class Empty {
    }
}