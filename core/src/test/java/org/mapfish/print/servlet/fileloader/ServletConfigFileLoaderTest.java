package org.mapfish.print.servlet.fileloader;

import org.junit.Test;
import org.mapfish.print.IllegalFileAccessException;
import org.mapfish.print.servlet.MapPrinterServletTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@ContextConfiguration(locations = {
        MapPrinterServletTest.PRINT_CONTEXT
})
public class ServletConfigFileLoaderTest extends AbstractConfigLoaderTest {

    public static final String CONFIG_FILE_URI_STRING =
            "servlet:///org/mapfish/print/servlet/fileloader/config.yaml";

    @Autowired
    private ServletConfigFileLoader loader;

    @Override
    protected ConfigFileLoaderPlugin getLoader() {
        return loader;
    }

    @Test
    public void testToFile() throws Exception {
        assertFalse(loader.toFile(new URI("file://blahblahblah")).isPresent());
        assertTrue(loader.toFile(new URI(CONFIG_FILE_URI_STRING)).isPresent());
        assertTrue(loader.toFile(new URI("servlet:///org/mapfish/print/servlet/fileloader/")).isPresent());
    }

    @Test
    public void testLastModified() throws Exception {
        final File file = CONFIG_FILE;

        Optional<Long> lastModified = this.loader.lastModified(new URI(CONFIG_FILE_URI_STRING));
        assertTrue(lastModified.isPresent());
        assertEquals(file.lastModified(), lastModified.get().longValue());
    }

    @Test
    public void testAccessible() throws Exception {
        assertTrue(loader.isAccessible(new URI(CONFIG_FILE_URI_STRING)));
        assertFalse(loader.isAccessible(new URI(CONFIG_FILE_URI_STRING + "xzy")));
    }

    @Test
    public void testLoadFile() throws Exception {
        final File file = CONFIG_FILE;

        byte[] loaded = this.loader.loadFile(new URI(CONFIG_FILE_URI_STRING));
        assertArrayEquals(Files.readAllBytes(file.toPath()), loaded);
    }

    @Test
    public void testAccessibleChildResource() throws Exception {
        final URI configFileUri = new URI(CONFIG_FILE_URI_STRING);
        final String resourceFileName = "resourceFile.txt";
        assertTrue(this.loader.isAccessible(configFileUri,
                                            "servlet:///org/mapfish/print/servlet/fileloader/" +
                                                    resourceFileName));
        assertTrue(this.loader.isAccessible(configFileUri, resourceFileName));
        assertTrue(this.loader.isAccessible(configFileUri,
                                            getFile(FileConfigFileLoader.class, resourceFileName).toURI()
                                                    .toString()));
        assertTrue(this.loader.isAccessible(configFileUri,
                                            getFile(FileConfigFileLoader.class, resourceFileName)
                                                    .getAbsolutePath()));
        assertTrue(this.loader.isAccessible(configFileUri,
                                            getFile(FileConfigFileLoader.class, resourceFileName).getPath()));

        File file = getFile(FileConfigFileLoader.class,
                            "/test-http-request-factory-application-context.xml");
        assertFileAccessException(configFileUri, file.getAbsolutePath());
        file = getFile(FileConfigFileLoader.class,
                       "../../../../../test-http-request-factory-application-context.xml");
        assertFileAccessException(configFileUri, file.getAbsolutePath());
        assertFileAccessException(configFileUri,
                                  "servlet:///test-http-request-factory-application-context.xml");
    }

    @Test
    public void testLoadFileChildResource() throws Exception {
        final URI configFileUri = new URI(CONFIG_FILE_URI_STRING);
        final String resourceFileName = "resourceFile.txt";
        final byte[] bytes = getFileBytes(FileConfigFileLoader.class, resourceFileName);

        assertArrayEquals(bytes, this.loader.loadFile(configFileUri, resourceFileName));
        assertArrayEquals(bytes, this.loader.loadFile(configFileUri, getFile(FileConfigFileLoader.class,
                                                                             resourceFileName)
                .getAbsolutePath()));
        assertArrayEquals(bytes, this.loader
                .loadFile(configFileUri, getFile(FileConfigFileLoader.class, resourceFileName).getPath()));
        assertArrayEquals(bytes, this.loader
                .loadFile(configFileUri, "servlet:///org/mapfish/print/servlet/fileloader/" +
                        resourceFileName));
    }

    @Test(expected = IllegalFileAccessException.class)
    public void testLoadFileChildResource_NotInConfigDir() throws Exception {
        final URI configFileUri = new URI(CONFIG_FILE_URI_STRING);

        this.loader.loadFile(configFileUri, getFile(FileConfigFileLoader.class,
                                                    "/test-http-request-factory-application-context.xml")
                .getAbsolutePath());
    }

    @Test(expected = IllegalFileAccessException.class)
    public void testLoadFileChildResource_NotInConfigDir_ServletURI() throws Exception {
        final URI configFileUri = new URI(CONFIG_FILE_URI_STRING);

        this.loader.loadFile(configFileUri, "servlet:///test-http-request-factory-application-context.xml");
    }

    @Test(expected = NoSuchElementException.class)
    public void testLoadFileChildResource_DoesNotExist() throws Exception {
        final URI configFileUri = new URI(CONFIG_FILE_URI_STRING);

        this.loader.loadFile(configFileUri, "doesNotExist");
    }

}
