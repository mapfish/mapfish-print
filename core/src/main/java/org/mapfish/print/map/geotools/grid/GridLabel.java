package org.mapfish.print.map.geotools.grid;

/**
 * Represents text, position and rotation of a label.
 */
class GridLabel {
    final String text;
    final int x, y;
    final Side side;

    GridLabel(final String text, final int x, final int y, final Side side) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.side = side;
    }

    @Override
    public String toString() {
        return "GridLabel{" +
                "text='" + this.text + '\'' +
                ", x=" + this.x +
                ", y=" + this.y +
                ", side=" + this.side +
                '}';
    }

    enum Side {
        TOP, BOTTOM, LEFT, RIGHT
    }
}
