package org.mapfish.print;

import javax.imageio.ImageIO;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/** Scans for ImageIO plugins when the application context is loaded. */
public final class ImageIOPluginScanner implements ApplicationListener<ContextRefreshedEvent> {
  /**
   * Ensure that extensions for ImageIO (like the reader and writer for TIFF) are registered. This
   * is required for certain Windows systems.
   *
   * @param event the startup event. Not needed here but required for API.
   */
  @Override
  public void onApplicationEvent(final ContextRefreshedEvent event) {
    ImageIO.scanForPlugins();
  }
}
