package org.mapfish.print.map.geotools.grid;

/** Represents text, position and rotation of a label. */
record GridLabel(String text, int x, int y, Side side) {

  @Override
  public String toString() {
    return "GridLabel{"
        + "text='"
        + this.text
        + '\''
        + ", x="
        + this.x
        + ", y="
        + this.y
        + ", side="
        + this.side
        + '}';
  }

  enum Side {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT
  }
}
