package org.mapfish.print.servlet.fileloader;

import org.junit.Test;
import org.mapfish.print.IllegalFileAccessException;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.nio.file.Files;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileConfigFileLoaderTest extends AbstractConfigLoaderTest {

    @Autowired
    private FileConfigFileLoader loader;

    @Override
    public FileConfigFileLoader getLoader() {
        return loader;
    }

    @Test
    public void testToFile() throws Exception {
        assertFalse(loader.toFile(new URI("servlet:///blahblahblah")).isPresent());
        assertTrue(loader.toFile(CONFIG_FILE.toURI()).isPresent());
        assertTrue(loader.toFile(CONFIG_FILE.getParentFile().toURI()).isPresent());
    }

    @Test
    public void testLastModified() {

        Optional<Long> lastModified = this.loader.lastModified(CONFIG_FILE.toURI());
        assertTrue(lastModified.isPresent());
        assertEquals(CONFIG_FILE.lastModified(), lastModified.get().longValue());
    }

    @Test
    public void testAccessible() throws Exception {
        assertTrue(CONFIG_FILE.toURI().toString(), loader.isAccessible(CONFIG_FILE.toURI()));
        assertFalse(loader.isAccessible(new URI(CONFIG_FILE.toURI() + "xzy")));
    }

    @Test
    public void testAccessible_RelativePath() throws Exception {
        final URI fileURI = new URI("file://relativePath/config.yaml");
        assertFalse(loader.isAccessible(fileURI));
    }

    @Test
    public void testLoadFile() throws Exception {
        byte[] loaded = this.loader.loadFile(CONFIG_FILE.toURI());
        assertArrayEquals(Files.readAllBytes(CONFIG_FILE.toPath()), loaded);
    }

    @Test(expected = NoSuchElementException.class)
    public void testLoadFileMissingFile() throws Exception {
        this.loader.loadFile(new URI("file:/c:/doesnotexist"));
    }

    @Test(expected = NoSuchElementException.class)
    public void testLastModifiedMissingFile() throws Exception {
        this.loader.lastModified(new URI("file:/c:/doesnotexist"));
    }

    @Test
    public void testAccessibleChildResource() throws Exception {
        final URI configFileUri = CONFIG_FILE.toURI();
        final String resourceFileName = "resourceFile.txt";
        assertTrue(this.loader.isAccessible(configFileUri, resourceFileName));
        assertTrue(this.loader.isAccessible(configFileUri,
                                            getFile(FileConfigFileLoader.class, resourceFileName).toURI()
                                                    .toString()));
        assertTrue(this.loader.isAccessible(configFileUri,
                                            getFile(FileConfigFileLoader.class, resourceFileName)
                                                    .getAbsolutePath()));
        assertTrue(this.loader.isAccessible(configFileUri,
                                            getFile(FileConfigFileLoader.class, resourceFileName).getPath()));

        assertFileAccessException(configFileUri, getFile(FileConfigFileLoader.class,
                                                         "/test-http-request-factory-application-context.xml")
                .getAbsolutePath());
        assertFileAccessException(configFileUri, getFile(FileConfigFileLoader.class,
                                                         "../../../../../test-http-request-factory" +
                                                                 "-application-context.xml")
                .getAbsolutePath());
    }

    @Test
    public void testLoadFileChildResource() throws Exception {
        final URI configFileUri = CONFIG_FILE.toURI();
        final String resourceFileName = "resourceFile.txt";
        final byte[] bytes = getFileBytes(FileConfigFileLoader.class, resourceFileName);

        assertArrayEquals(bytes, this.loader.loadFile(configFileUri, resourceFileName));
        assertArrayEquals(bytes, this.loader.loadFile(configFileUri, getFile(FileConfigFileLoader.class,
                                                                             resourceFileName)
                .getAbsolutePath()));
        assertArrayEquals(bytes, this.loader
                .loadFile(configFileUri, getFile(FileConfigFileLoader.class, resourceFileName).getPath()));
    }

    @Test(expected = IllegalFileAccessException.class)
    public void testLoadFileChildResource_NotInConfigDir() throws Exception {
        final URI configFileUri = CONFIG_FILE.toURI();

        this.loader.loadFile(configFileUri, getFile(FileConfigFileLoader.class,
                                                    "/test-http-request-factory-application-context.xml")
                .getAbsolutePath());
    }

    @Test(expected = NoSuchElementException.class)
    public void testLoadFileChildResource_DoesNotExist() throws Exception {
        final URI configFileUri = CONFIG_FILE.toURI();

        this.loader.loadFile(configFileUri, "doesNotExist");
    }

}
