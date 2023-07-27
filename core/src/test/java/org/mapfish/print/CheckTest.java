package org.mapfish.print;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class CheckTest {

  @Test
  public void testRegenerateFalse() throws Exception {
    assertFalse(
        "This flag should not be committed as true",
        org.mapfish.print.test.util.ImageSimilarity.REGENERATE_EXPECTED_IMAGES);
  }
}
