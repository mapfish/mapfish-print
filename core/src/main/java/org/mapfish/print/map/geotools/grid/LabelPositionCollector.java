package org.mapfish.print.map.geotools.grid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class collects the position and angle of the labels that need to be rendered for the grid.
 */
final class LabelPositionCollector implements Iterable<GridLabel> {
    private final List<GridLabel> labels = new ArrayList<>();

    void add(final GridLabel label) {
        this.labels.add(label);
    }

    @Override
    public Iterator<GridLabel> iterator() {
        return this.labels.iterator();
    }
}
