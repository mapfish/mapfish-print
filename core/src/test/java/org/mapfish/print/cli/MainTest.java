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

    @Before
    public void setUp() throws Exception {
        this.outputFile = File.createTempFile("main-test", ".png");
        this.configFile = getFile("config.yaml");
        this.v3ApiRequestFile = getFile("v3Request.json");
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
                "-output", this.outputFile.getAbsolutePath()
        };
        Main.runMain(args);

        new ImageSimilarity(getFile("expectedV3Image.png"))
                .assertSimilarity(this.outputFile, 10);
    }
}
