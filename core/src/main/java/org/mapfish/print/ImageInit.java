package org.mapfish.print;

import com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi;
import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes image plugins (initially for WebP support) at application startup.
 *
 * <p>This class registers the WebP image reader plugin with the Java ImageIO framework, enabling
 * support for WebP images. Typically used in a servlet context.
 */
public class ImageInit {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImageInit.class);

  /**
   * Initializes image plugins if the {@code mapfish.image.plugins} system property is set to {@code
   * true}. Registers additional image readers such as WebP.
   */
  @PostConstruct
  public final void init() {
    if (System.getProperty("mapfish.image.plugins", "false").equals("true")) {
      LOGGER.info("Scanning for image plugins");
      ImageIO.scanForPlugins();
      IIORegistry.getDefaultInstance().registerServiceProvider(new WebPImageReaderSpi());
    }
  }
}
