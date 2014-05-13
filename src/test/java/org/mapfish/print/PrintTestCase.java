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

package org.mapfish.print;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

public abstract class PrintTestCase {

    @Rule
    public TestName name = new TestName();

    @Before
    public void setUp() throws Exception {
        BasicConfigurator.configure(new ConsoleAppender(
                new PatternLayout("%d{HH:mm:ss.SSS} [%t] %-5p %30.30c - %m%n")));
        Logger.getRootLogger().setLevel(Level.DEBUG);

    }

    @After
    public void tearDown() throws Exception {
        BasicConfigurator.resetConfiguration();
    }
}
