package org.mapfish.print.attribute.map;

import com.google.common.collect.Ordering;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.config.ConfigurationObject;
import org.mapfish.print.map.DistanceUnit;
import org.mapfish.print.map.Scale;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import static org.mapfish.print.Constants.PDF_DPI;

/**
 * <p>Encapsulates a sorted set of scale denominators representing the allowed scales.</p>
 * <p>The scales are sorted from largest to smallest and the index starts at 0,
 * where 0 is the largest scale (most zoomed out).</p>
 * <pre><code>
 *   map: !map
 *     zoomLevels: !zoomLevels
 *        scales: [5000, 10000, 25000, 50000, 100000, 500000]</code></pre>
 * [[examples=datasource_many_dynamictables_legend]]
 */
public final class ZoomLevels implements ConfigurationObject {
    private double[] scaleDenominators;

    /**
     * Constructor.
     *
     * @param scaleDenominators do not need to be sorted or unique.
     */
    public ZoomLevels(final double... scaleDenominators) {
        setScales(scaleDenominators);
    }

    /**
     * default constructor for constructing by spring.
     */
    public ZoomLevels() {
        // intentionally empty
    }

    /**
     * Set the scales (sorts from largest to smallest).
     * @param newScaleDenominators The scales (may be unsorted).
     */
    public void setScales(final double[] newScaleDenominators) {
        TreeSet<Double> sortedSet = new TreeSet<Double>(Ordering.natural().reverse());
        for (int i = 0; i < newScaleDenominators.length; i++) {
            sortedSet.add(newScaleDenominators[i]);
        }
        this.scaleDenominators = new double[sortedSet.size()];
        int i = 0;
        for (Double scaleDenominator : sortedSet) {
            this.scaleDenominators[i] = scaleDenominator;
            i++;
        }
    }

    /**
     * The number of zoom levels.
     */
    public int size() {
        return this.scaleDenominators.length;
    }

    /**
     * Get the scale at the given index.
     * @param index the index of the zoom level to access.
     * @param unit the unit.
     */
    public Scale get(final int index, final DistanceUnit unit) {
        return new Scale(this.scaleDenominators[index], unit, PDF_DPI);
    }

    @Override
    public String toString() {
        return Arrays.toString(this.scaleDenominators);
    }

    // CHECKSTYLE:OFF
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ZoomLevels that = (ZoomLevels) o;

        return Arrays.equals(scaleDenominators, that.scaleDenominators);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(scaleDenominators);
    }


    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        if (scaleDenominators == null || scaleDenominators.length == 0) {
            validationErrors.add(new ConfigurationException("There are no scales defined in " + getClass().getName()));
        }
    }

    /**
     * Return a copy of the zoom level scale denominators. Scales are sorted greatest to least.
     */
    public double[] getScaleDenominators() {
        double[] dest = new double[this.scaleDenominators.length];
        System.arraycopy(this.scaleDenominators, 0, dest, 0, this.scaleDenominators.length);
        return dest;
    }

    // CHECKSTYLE:ON
}
