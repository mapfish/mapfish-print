package org.mapfish.print.processor.map;

import java.io.File;

class SvgFileGenerator {
  private final File printDirectory;
  private final String mapKey;
  private int currentFileNumber;

  SvgFileGenerator(final File printDirectory, final String mapKey, final int fileNumber) {
    this.printDirectory = printDirectory;
    this.mapKey = mapKey;
    this.currentFileNumber = fileNumber;
  }

  public File generate() {
    return new File(printDirectory, mapKey + "_layer_" + currentFileNumber++ + ".svg");
  }

  public int getCurrentFileNumber() {
    return currentFileNumber;
  }
}
