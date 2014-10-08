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

import com.google.common.collect.Lists;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.junit.Test;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mockito.Mockito;

import java.io.StringWriter;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mapfish.print.attribute.ReflectiveAttribute.JSON_ATTRIBUTE_TYPE;
import static org.mapfish.print.attribute.ReflectiveAttribute.JSON_NAME;

/**
 * Common base class for testing attributes.
 *
 * @author Jesse on 5/8/2014.
 */
public abstract class AbstractAttributeTest {

    @Test
    public void testPrintClientConfig() throws Exception {
        final StringWriter jsonOutput = new StringWriter();
        JSONWriter json = new JSONWriter(jsonOutput);
        Template template = Mockito.mock(Template.class);
        // verify there is no error
        json.object();
        final Attribute attribute = createAttribute();
        attribute.printClientConfig(json, template);
        json.endObject();
        JSONObject capabilities = new JSONObject(jsonOutput.toString());
        assertTrue("Missing " + JSON_NAME + " in: \n" + capabilities.toString(2), capabilities.has(JSON_NAME));
        assertTrue("Missing " + JSON_ATTRIBUTE_TYPE + " in: \n" + capabilities.toString(2), capabilities.has(JSON_ATTRIBUTE_TYPE));
    }

    @Test
    public void testValidate() throws Exception {
        List<Throwable> errors = Lists.newArrayList();
        Configuration configuration = new Configuration();
        createAttribute().validate(errors, configuration);

        assertTrue(errors.toString(), errors.isEmpty());
    }

    protected abstract Attribute createAttribute();
}
