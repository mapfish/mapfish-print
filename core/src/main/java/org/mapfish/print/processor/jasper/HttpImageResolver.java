package org.mapfish.print.processor.jasper;

import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * <p>Interprets text in a table cell as an image URL.</p>
 * <p>See also: <a href="tableimages.html">Configuration of tables with HTML images</a></p>
 * [[examples=datasource_many_dynamictables_legend]]
 */
public final class HttpImageResolver implements TableColumnConverter<BufferedImage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpImageResolver.class);
    private static final int IMAGE_SIZE = 48;
    private Pattern urlExtractor = Pattern.compile("(.*)");
    private int urlGroup = 1;
    private BufferedImage defaultImage;

    /**
     * Constructor.
     */
    public HttpImageResolver() {
        this.defaultImage = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_4BYTE_ABGR);
    }

    /**
     * Sets the RegExp pattern to use for extracting the url from the text.  By default the whole string is
     * used.
     * <p>
     * For example: <code>.*&amp;img src="([^"]+)".*</code>
     *
     * @param pattern The url extractor regular expression.  Default is <code>"(.*)"</code>
     */
    public void setUrlExtractor(final String pattern) {
        this.urlExtractor = Pattern.compile(pattern);
    }

    /**
     * Select the group in the regular expression that contains the url.
     *
     * @param urlGroup the index of the group (starting at 1) that contains the url.
     */
    public void setUrlGroup(final int urlGroup) {
        this.urlGroup = urlGroup;
    }

    @Override
    public BufferedImage resolve(
            final MfClientHttpRequestFactory requestFactory,
            final String text) {
        Matcher urlMatcher = this.urlExtractor.matcher(text);

        if (urlMatcher.matches() && urlMatcher.group(this.urlGroup) != null) {
            final String uriString = urlMatcher.group(this.urlGroup);
            try {
                URI url = new URI(uriString);
                final ClientHttpRequest request = requestFactory.createRequest(url, HttpMethod.GET);
                final ClientHttpResponse response = request.execute();
                if (response.getStatusCode() == HttpStatus.OK) {
                    try {
                        final BufferedImage image = ImageIO.read(response.getBody());
                        if (image == null) {
                            LOGGER.warn("The URL: {} is NOT an image format that can be decoded", url);
                            return this.defaultImage;
                        }
                        return image;
                    } catch (IOException e) {
                        LOGGER.warn("Image loaded from '{}'is not valid", url, e);
                    }
                } else {
                    LOGGER.warn("Error loading the table row image: {}.\nStatus Code: {}\nStatus Text: {}",
                                url, response.getStatusCode(), response.getStatusText());
                }
            } catch (Throwable e) {
                LOGGER.warn("Error loading table row image: {}", uriString, e);
            }
        }

        return this.defaultImage;
    }

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        if (this.urlExtractor == null) {
            validationErrors.add(new ConfigurationException("No urlExtractor defined"));
        }
    }

    @Override
    public boolean canConvert(final String text) {
        Matcher urlMatcher = this.urlExtractor.matcher(text);
        return (urlMatcher.matches() && urlMatcher.group(this.urlGroup) != null);
    }
}
