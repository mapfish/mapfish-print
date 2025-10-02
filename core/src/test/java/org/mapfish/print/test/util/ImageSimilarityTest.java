package org.mapfish.print.test.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.imageio.ImageIO;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;

public class ImageSimilarityTest extends AbstractMapfishSpringTest {

  /** Ensure that we ar not regenerating the expected images in the CI. */
  @Test
  public void testNotRegenerateImage() {
    assertFalse(
        "This flag should not be committed as true", ImageSimilarity.REGENERATE_EXPECTED_IMAGES);
  }

  /** Test that we get a distance in images with small differences. */
  @Test
  public void testSmallDist() throws Exception {
    assertTrue(
        new ImageSimilarity(getFile("imageSimilarity/with.png"))
                .calcDistance(ImageIO.read(getFile("imageSimilarity/without.png")))
            > 0);
  }
}
