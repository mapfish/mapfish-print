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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.test.util.ImageSimilarity;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class MainTest {

    private File outputFile;
    private File configFile;
    private File v3ApiRequestFile;
    private File v2ApiRequestFile;
    private String testSpringConfigFile;

    @Before
    public void setUp() throws Exception {
        this.outputFile = File.createTempFile("main-test", ".png");
        this.configFile = getFile("config.yaml");
        this.v3ApiRequestFile = getFile("v3Request.json");
        this.v2ApiRequestFile = getFile("v2Request.json");
        this.testSpringConfigFile = "/test-http-request-factory-application-context.xml";
        Main.setExceptionOnFailure(true);
        Main.setSpringContextCallback(new Function<ApplicationContext, Void>() {

            @Nullable
            @Override
            public Void apply(@Nonnull ApplicationContext input) {
                final TestHttpClientFactory bean = input.getBean(TestHttpClientFactory.class);
                bean.registerHandler(new Predicate<URI>() {
                    @Override
                    public boolean apply(@Nonnull URI input) {
                        return input.getHost().equals("main-test.json");
                    }
                }, new TestHttpClientFactory.Handler() {
                    @Override
                    public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws IOException, Exception {
                        try {
                            byte[] bytes = Files.toByteArray(getFile("/map-data" + uri.getPath()));
                            return ok(uri, bytes, httpMethod);
                        } catch (AssertionError e) {
                            return error404(uri, httpMethod);
                        }
                    }
                });
                return null;
            }
        });
    }

    private File getFile(String fileName) {
        return AbstractMapfishSpringTest.getFile(MainTest.class, fileName);
    }

    @Test
    public void testNewAPI() throws Exception {
        String[] args = {
                "-springConfig", this.testSpringConfigFile,
                "-config", this.configFile.getAbsolutePath(),
                "-spec", this.v3ApiRequestFile.getAbsolutePath(),
                "-output", this.outputFile.getAbsolutePath()};
        Main.main(args);

        new ImageSimilarity(getFile("expectedV3Image.png"), 2).assertSimilarity(this.outputFile, 10);
    }

    @Test
    public void testOldAPI() throws Exception {
        String[] args = {
                "-v2",
                "-springConfig", this.testSpringConfigFile,
                "-config", this.configFile.getAbsolutePath(),
                "-spec", this.v2ApiRequestFile.getAbsolutePath(),
                "-output", this.outputFile.getAbsolutePath()};
        Main.main(args);

        new ImageSimilarity(getFile("expectedV2Image.png"), 2).assertSimilarity(this.outputFile, 10);
    }

    @Test(expected = Exception.class)
    public void testV2SpecNoV2Param() throws Exception {
        String[] args = {
                "-springConfig", this.testSpringConfigFile,
                "-config", this.configFile.getAbsolutePath(),
                "-spec", this.v2ApiRequestFile.getAbsolutePath(),
                "-output", this.outputFile.getAbsolutePath()};
        Main.main(args);
    }

    @Test(expected = Exception.class)
    public void testV3SpecV2ApiParam() throws Exception {
        String[] args = {
                "-v2",
                "-springConfig", this.testSpringConfigFile,
                "-config", this.configFile.getAbsolutePath(),
                "-spec", this.v3ApiRequestFile.getAbsolutePath(),
                "-output", this.outputFile.getAbsolutePath()};
        Main.main(args);
    }
}