package org.mapfish.print.servlet.fileloader;

import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClasspathConfigFileLoaderTest extends AbstractMapfishSpringTest {

    final String configFileUriString =
            "classpath://" + FileConfigFileLoaderTest.class.getPackage().getName().replace('.',
                                                                                           '/') +
                    "/config.yaml";
    @Autowired
    private ClasspathConfigFileLoader loader;

    @Test
    public void testToFile() throws Exception {
        assertFalse(loader.toFile(new URI("file://blahblahblah")).isPresent());
        assertTrue(loader.toFile(new URI(configFileUriString)).isPresent());
        final URI fileUri = new URI("classpath://" + FileConfigFileLoaderTest.class.getPackage().getName()
                .replace('.', '/'));
        assertTrue(loader.toFile(fileUri).isPresent());
    }

    @Test
    public void testLastModified() throws Exception {
        final File file = getFile(FileConfigFileLoaderTest.class, "config.yaml");

        Optional<Long> lastModified = this.loader.lastModified(new URI(configFileUriString));
        assertTrue(lastModified.isPresent());
        assertEquals(file.lastModified(), lastModified.get().longValue());
    }

    @Test
    public void testAccessible() throws Exception {
        assertTrue(loader.isAccessible(new URI(configFileUriString)));
        assertFalse(loader.isAccessible(new URI(configFileUriString + "xzy")));
    }

    @Test
    public void testLoadFile() throws Exception {
        final File file = getFile(FileConfigFileLoaderTest.class, "config.yaml");

        byte[] loaded = this.loader.loadFile(new URI(configFileUriString));
        assertArrayEquals(Files.readAllBytes(file.toPath()), loaded);
    }


    @Test
    public void testAccessibleChildResource() throws Exception {
        final URI configFileUri = new URI(configFileUriString);
        final String resourceFileName = "resourceFile.txt";
        assertTrue(this.loader.isAccessible(configFileUri,
                                            "classpath://org/mapfish/print/servlet/fileloader/" +
                                                    resourceFileName));
        assertTrue(this.loader.isAccessible(configFileUri, resourceFileName));
        assertFalse(this.loader.isAccessible(configFileUri,
                                             getFile(FileConfigFileLoader.class, resourceFileName).toURI()
                                                     .toString()));
        assertFalse(this.loader.isAccessible(configFileUri,
                                             getFile(FileConfigFileLoader.class, resourceFileName)
                                                     .getAbsolutePath()));
        assertFalse(this.loader.isAccessible(configFileUri,
                                             getFile(FileConfigFileLoader.class, resourceFileName)
                                                     .getPath()));

        assertFalse(this.loader.isAccessible(configFileUri, getFile(FileConfigFileLoader.class,
                                                                    "/test-http-request-factory-application" +
                                                                            "-context.xml")
                .getAbsolutePath()));
        assertFalse(this.loader.isAccessible(configFileUri,
                                             "classpath://test-http-request-factory-application-context" +
                                                     ".xml"));
    }

    @Test
    public void testLoadFileChildResource() throws Exception {
        final URI configFileUri = new URI(configFileUriString);
        final String resourceFileName = "resourceFile.txt";
        final byte[] bytes = getFileBytes(FileConfigFileLoader.class, resourceFileName);

        assertArrayEquals(bytes, this.loader.loadFile(configFileUri, resourceFileName));
        assertArrayEquals(bytes, this.loader
                .loadFile(configFileUri, "classpath://org/mapfish/print/servlet/fileloader/" +
                        resourceFileName));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadFileChildResource_NotInConfigDir() throws Exception {
        final URI configFileUri = new URI(configFileUriString);

        this.loader.loadFile(configFileUri, "classpath://test-http-request-factory-application-context.xml");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadFileChildResource_NotClasspathURI() throws Exception {
        final URI configFileUri = new URI(configFileUriString);
        final String resourceFileName = "resourceFile.txt";

        final String uri = getFile(FileConfigFileLoader.class, resourceFileName).toURI().toString();
        this.loader.loadFile(configFileUri, uri);
    }

    @Test(expected = NoSuchElementException.class)
    public void testLoadFileChildResource_DoesNotExist() throws Exception {
        final URI configFileUri = new URI(configFileUriString);
        final String resourceFileName = "resourceFile.txt";

        final File file = new File(getFile(FileConfigFileLoader.class, resourceFileName).getParentFile(),
                                   "doesNotExist");
        this.loader.loadFile(configFileUri, file.getPath());
    }

    @Test(expected = NoSuchElementException.class)
    public void testLoadFileChildResource_DoesNotExist2() throws Exception {
        final URI configFileUri = new URI(configFileUriString);

        this.loader.loadFile(configFileUri, "doesNotExist");
    }

    @Test(expected = NoSuchElementException.class)
    public void testLoadFileChildResource_ConfigFileDoesNotExist() throws Exception {
        final URI configFileUri = new URI("classpath://xyz.yaml");
        final String resourceFileName = "resourceFile.txt";

        this.loader.loadFile(configFileUri,
                             getFile(FileConfigFileLoader.class, resourceFileName).toURI().toString());
    }

}
