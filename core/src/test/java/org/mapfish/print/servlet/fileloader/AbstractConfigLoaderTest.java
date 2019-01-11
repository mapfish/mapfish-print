package org.mapfish.print.servlet.fileloader;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.IllegalFileAccessException;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.WorkingDirectories;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class AbstractConfigLoaderTest extends AbstractMapfishSpringTest {
    protected static final File CONFIG_FILE = getFile(AbstractConfigLoaderTest.class, "config.yaml");

    @Autowired
    protected WorkingDirectories workingDirectories;

    @After
    public void tearDown() {
        final File[] children = this.workingDirectories.getWorking().listFiles();
        assertNotNull(children);
        for (File child: children) {
            this.workingDirectories.removeDirectory(child);
        }
    }

    protected abstract ConfigFileLoaderPlugin getLoader();

    @Test
    public void testAccessibleChildResource_InWorkingDir() throws Exception {

        final Configuration configuration = getConfiguration();

        assertAccessible(this.workingDirectories.getReports());
        assertAccessible(this.workingDirectories.getWorking(configuration));
        assertAccessible(this.workingDirectories.getJasperCompilation(configuration));
        assertAccessible(this.workingDirectories.getTaskDirectory());
    }

    private Configuration getConfiguration() {
        final Configuration configuration = new Configuration();
        configuration.setConfigurationFile(CONFIG_FILE);
        return configuration;
    }

    @Test
    public void testLoadFileChildResource_InWorkingDir() throws Exception {
        final Configuration configuration = getConfiguration();

        final String resourceFileName = "resourceFile.txt";
        final byte[] bytes = getFileBytes(FileConfigFileLoader.class, resourceFileName);

        assertLoadable(bytes, this.workingDirectories.getReports());
        assertLoadable(bytes, this.workingDirectories.getWorking(configuration));
        assertLoadable(bytes, this.workingDirectories.getJasperCompilation(configuration));
        assertLoadable(bytes, this.workingDirectories.getTaskDirectory());
    }

    protected void assertAccessible(File baseWorkingDir) throws IOException {
        final File testFile = new File(baseWorkingDir, "testFile");
        try {
            testFile.getParentFile().mkdirs();
            new FileOutputStream(testFile).close();
            assertTrue(testFile.getAbsolutePath() + " is not accessible",
                       getLoader().isAccessible(CONFIG_FILE.toURI(),
                                                testFile.getAbsolutePath()));
            assertTrue(testFile.getAbsoluteFile().toURI().toString() + " is not accessible",
                       getLoader().isAccessible(CONFIG_FILE.toURI
                               (), testFile.getAbsoluteFile().toURI().toString()));
            assertTrue(testFile.toURI().toString() + " is not accessible",
                       getLoader().isAccessible(CONFIG_FILE.toURI(), testFile.toURI().toString()));
        } finally {
            testFile.delete();
        }
    }

    protected void assertLoadable(byte[] bytes, File baseWorkingDir) throws IOException {
        final File testFile = new File(baseWorkingDir, "testFile");
        try {
            testFile.getParentFile().mkdirs();
            FileUtils.writeByteArrayToFile(testFile, bytes);
            assertArrayEquals(bytes, getLoader().loadFile(CONFIG_FILE.toURI(), testFile.getAbsolutePath()));
            assertArrayEquals(bytes, getLoader()
                    .loadFile(CONFIG_FILE.toURI(), testFile.getAbsoluteFile().toURI().toString()));
            assertArrayEquals(bytes, getLoader().loadFile(CONFIG_FILE.toURI(), testFile.toURI().toString()));
        } finally {
            testFile.delete();
        }
    }

    protected void assertFileAccessException(URI configFileUri, String resource) throws IOException {
        try {
            getLoader().isAccessible(configFileUri, resource);
            fail("Expected " + IllegalFileAccessException.class.getSimpleName());
        } catch (IllegalFileAccessException e) {
            // good
        }
    }
}
