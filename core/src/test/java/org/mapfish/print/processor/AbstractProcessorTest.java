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

package org.mapfish.print.processor;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.mapfish.print.config.Configuration;

import java.util.List;
import javax.annotation.Nullable;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Jesse on 3/29/14.
 */
public class AbstractProcessorTest {

    /**
     * This test checks that all the inputMapper mappings have an associated property in the params object.
     *
     * @throws Exception
     */
    @Test
    public void testExtraInputMapperMapping() throws Exception {
        final TestProcessor testProcessor = new TestProcessor();
        testProcessor.getInputMapperBiMap().put("pqr", "prop");


        Configuration configuration = new Configuration();
        List<Throwable> errors = Lists.newArrayList();
        testProcessor.validate(errors, configuration);


        assertTrue(errors.isEmpty());
        testProcessor.getInputMapperBiMap().put("ml", "proc");
        testProcessor.validate(errors, configuration);
        assertFalse(errors.isEmpty());
    }



    /**
     * This test checks that all the outputMapper mappings have an associated property in the output object.
     *
     * @throws Exception
     */
    @Test
    public void testExtraOutputMapperMapping() throws Exception {
        final TestProcessor testProcessor = new TestProcessor();
        testProcessor.getOutputMapperBiMap().put("prop", "oq");


        Configuration configuration = new Configuration();
        List<Throwable> errors = Lists.newArrayList();
        testProcessor.validate(errors, configuration);


        assertTrue(errors.isEmpty());
        testProcessor.getInputMapperBiMap().put("proc", "mk");
        testProcessor.validate(errors, configuration);
        assertFalse(errors.isEmpty());
    }

    class TestIn {
        public String prop;
    }
    class TestOut {
        public String prop;
    }
    class TestProcessor extends AbstractProcessor<TestIn, TestOut> {

        protected TestProcessor() {
            super(TestOut.class);
        }

        @Override
        public TestIn createInputParameter() {
            return new TestIn();
        }

        @Nullable
        @Override
        public TestOut execute(TestIn values, ExecutionContext context) throws Exception {
            return new TestOut();
        }

        @Override
        protected void extraValidation(List<Throwable> validationErrors, final Configuration configuration) {
            // no checks
        }
    }
}
