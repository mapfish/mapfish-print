package org.mapfish.print.processor.map;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.mapfish.print.ImageUtils;
import org.mapfish.print.attribute.map.MapLayer;

class ImageWriter {
  private final File printDirectory;
  private final String mapKey;
  private final String formatName;
  private final BufferedImage bufferedImage;
  private final List<URI> graphics;
  private int currentFileNumber;

  ImageWriter(
      final File printDirectory,
      final String mapKey,
      final boolean isLayerGroupOpaque,
      final MapLayer.RenderType renderType,
      final int fileNumber,
      final BufferedImage bufferedImage,
      final List<URI> graphics) {
    this.printDirectory = printDirectory;
    this.mapKey = mapKey;
    this.formatName = isLayerGroupOpaque && renderType == MapLayer.RenderType.JPEG ? "JPEG" : "PNG";
    this.currentFileNumber = fileNumber;
    this.bufferedImage = bufferedImage;
    this.graphics = graphics;
  }

  public void writeImage() throws IOException {
    // Try to respect the original format of the layer. But if it needs to be transparent,
    // no choice, we need PNG.
    final File path =
        new File(
            printDirectory,
            String.format("%s_layer_%d.%s", mapKey, currentFileNumber++, formatName.toLowerCase()));
    ImageUtils.writeImage(bufferedImage, formatName, path);
    graphics.add(path.toURI());
  }

  public int getCurrentFileNumber() {
    return currentFileNumber;
  }
}
