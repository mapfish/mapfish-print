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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.TreeSet;

import org.mapfish.print.PrintTestCase;
import org.mapfish.print.ShellMapPrinter;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ConfigTest extends PrintTestCase {
    public ConfigTest(String name) {
        super(name);
    }

    public void testParse() throws FileNotFoundException {
    	new ClassPathXmlApplicationContext(ShellMapPrinter.DEFAULT_SPRING_CONTEXT).getBean(ConfigFactory.class).fromYaml(new File("samples/config.yaml"));
    }

    public void testBestScale() {
        Config config = new Config();
        try {
            TreeSet<Integer> scales = new TreeSet<Integer>(Arrays.asList(200000, 25000, 50000, 100000));
            config.setScales(scales);

            assertEquals("Too small scale => pick the smallest available", 25000, config.getBestScale(1));
            assertEquals("Exact match", 25000, config.getBestScale(25000.0));
            assertEquals("Just too big => should still take the previous one", 25000, config.getBestScale(25000.1));
            assertEquals("Normal behaviour", 200000, config.getBestScale(150000));
            assertEquals("Just a litle before", 200000, config.getBestScale(199999.9));
            assertEquals("When we want a scale that is too big, pick the highest available", 200000, config.getBestScale(99999999999.0));
        } finally {
            config.close();
        }
    }
}
