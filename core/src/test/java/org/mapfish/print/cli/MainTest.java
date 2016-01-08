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

package org.mapfish.print.cli;

import org.junit.Before;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.test.util.ImageSimilarity;

import java.io.File;


public class MainTest {

    private File outputFile;
    private File configFile;
    private File v3ApiRequestFile;
    private File v2ApiRequestFile;

    @Before
    public void setUp() throws Exception {
        this.outputFile = File.createTempFile("main-test", ".png");
        this.configFile = getFile("config.yaml");
        this.v3ApiRequestFile = getFile("v3Request.json");
        this.v2ApiRequestFile = getFile("v2Request.json");
        Main.setExceptionOnFailure(true);
    }

    private File getFile(String fileName) {
        return AbstractMapfishSpringTest.getFile(MainTest.class, fileName);
    }

    @Test
    public void testNewAPI() throws Exception {
        String[] args = {
                "-config", this.configFile.getAbsolutePath(),
                "-spec", this.v3ApiRequestFile.getAbsolutePath(),
                "-output", this.outputFile.getAbsolutePath()};
        Main.runMain(args);

        new ImageSimilarity(getFile("expectedV3Image.png"), 1).assertSimilarity(this.outputFile, 70);
    }

    @Test
    public void testOldAPI() throws Exception {
        String[] args = {
                "-v2",
                "-config", this.configFile.getAbsolutePath(),
                "-spec", this.v2ApiRequestFile.getAbsolutePath(),
                "-output", this.outputFile.getAbsolutePath()};
        Main.runMain(args);

        new ImageSimilarity(getFile("expectedV2Image.png"), 1).assertSimilarity(this.outputFile, 70);
    }

    @Test(expected = Exception.class)
    public void testV2SpecNoV2Param() throws Exception {
        String[] args = {
                "-config", this.configFile.getAbsolutePath(),
                "-spec", this.v2ApiRequestFile.getAbsolutePath(),
                "-output", this.outputFile.getAbsolutePath()};
        Main.runMain(args);
    }

    @Test(expected = Exception.class)
    public void testV3SpecV2ApiParam() throws Exception {
        String[] args = {
                "-v2",
                "-config", this.configFile.getAbsolutePath(),
                "-spec", this.v3ApiRequestFile.getAbsolutePath(),
                "-output", this.outputFile.getAbsolutePath()};
        Main.runMain(args);
    }
}