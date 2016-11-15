package org.mapfish.print;

import com.google.common.base.Optional;
import com.google.common.io.Files;
import org.geotools.referencing.CRS;
import org.junit.runner.RunWith;
import org.mapfish.print.attribute.map.CenterScaleMapBounds;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.WorkingDirectories;
import org.mapfish.print.map.Scale;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that loads the normal spring application context from the spring config file.
 * Subclasses can use Autowired to get dependencies from the application context.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        AbstractMapfishSpringTest.DEFAULT_SPRING_XML,
        AbstractMapfishSpringTest.TEST_SPRING_XML,
        AbstractMapfishSpringTest.TEST_SPRING_FONT_XML
})
public abstract class AbstractMapfishSpringTest {
    public static final String DEFAULT_SPRING_XML = "classpath:mapfish-spring-application-context.xml";
    public static final String TEST_SPRING_XML = "classpath:test-http-request-factory-application-context.xml";
    public static final String TEST_SPRING_FONT_XML = "classpath:test-mapfish-spring-custom-fonts.xml";
    static final Pattern IMPORT_PATTERN = Pattern.compile("@@importFile\\((\\S+)\\)@@");
    public static final String TMP = System.getProperty("java.io.tmpdir");

    @Autowired
    private WorkingDirectories workingDirectories;

    /**
     * Look on the classpath for the named file.  Will look at the root package and in the same package as testClass.
     *
     * @param testClass class to look relative to.
     * @param fileName name of file to find.  Can be a path.
     */
    public static File getFile(Class<?> testClass, String fileName) {
        final URL resource = testClass.getResource(fileName);
        if (resource == null) {
            throw new AssertionError("Unable to find test resource: "+fileName);
        }

        return new File(resource.getFile());
    }

    /**
     * Parse the json string.
     *
     * @param jsonString the json string to parse.
     */
    public static PJsonObject parseJSONObjectFromString(String jsonString) {
        return MapPrinter.parseSpec(jsonString);
    }

    public static PJsonObject parseJSONObjectFromFile(Class<?> testClass, String fileName) throws IOException {
        final File file = getFile(testClass, fileName);
        final Charset charset = Charset.forName(Constants.DEFAULT_ENCODING);
        String jsonString = Files.asCharSource(file, charset).read();
        Matcher matcher = IMPORT_PATTERN.matcher(jsonString);
        while (matcher.find()) {
            final String importFileName = matcher.group(1);
            File importFile = new File(file.getParentFile(), importFileName);
            final String tagToReplace = matcher.group();
            final String importJson = Files.asCharSource(importFile, charset).read();
            jsonString = jsonString.replace(tagToReplace, importJson);
            matcher = IMPORT_PATTERN.matcher(jsonString);
        }
        return parseJSONObjectFromString(jsonString);
    }

    /**
     * Get a file from the classpath relative to this test class.
     * @param fileName the name of the file to load.
     */
    protected File getFile(String fileName) {
        return getFile(getClass(), fileName);
    }

    protected File getTaskDirectory() {
        return workingDirectories.getTaskDirectory();
    }

    public static MapfishMapContext createTestMapContext() {
        try {
            final CenterScaleMapBounds bounds = new CenterScaleMapBounds(CRS.decode("CRS:84"), 0, 0, 30000);
            return new MapfishMapContext(bounds, new Dimension(500,500), 0, 72, Constants.PDF_DPI, null, true);
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    public static String normalizedOSName() {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return "win";
        } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            return "mac";
        } else {
            return "linux";
        }
    }
    protected String getExpectedImageName(String classifier, BufferedImage actualImage, String baseDir) throws IOException {
        int javaVersion;

        String fullVersion = System.getProperty("java.specification.version");

        if (fullVersion.startsWith("1.6")) {
            javaVersion = 6;
        } else if (fullVersion.startsWith("1.7")) {
            javaVersion = 7;
        } else if (fullVersion.startsWith("1.8")) {
            javaVersion = 8;
        } else {
            throw new RuntimeException(fullVersion + " is not yet supported in the tests.  Update this switch");
        }

        String platformVersionName = "expectedSimpleImage" + classifier + "-" + normalizedOSName() +
                "-jdk" + javaVersion + ".png";
        String platformName = "expectedSimpleImage" + classifier + "-" + normalizedOSName() + ".png";
        String defaultName = "expectedSimpleImage" + classifier + ".png";

//        new File(TMP, baseDir).mkdirs();
//        ImageIO.write(actualImage, "png", new File(TMP, baseDir + "/" + platformVersionName));
//        ImageIO.write(actualImage, "png", new File(TMP, baseDir + "/" + platformName));
//        ImageIO.write(actualImage, "png", new File(TMP, baseDir + "/" + defaultName));


        return findImage(baseDir, platformVersionName).or(findImage(baseDir, platformName)).or(defaultName);
    }

    private Optional<String> findImage(final String baseDir, final String fileName) {
        try {
            getFile(baseDir + fileName);
            return Optional.of(fileName);
        } catch (AssertionError e) {
            return Optional.absent();
        }
    }

}
