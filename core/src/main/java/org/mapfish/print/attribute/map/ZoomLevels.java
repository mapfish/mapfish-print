package org.mapfish.print.attribute.map;

import com.google.common.collect.Ordering;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.config.ConfigurationObject;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/**
 * <p>Encapsulates a sorted set of scale denominators representing the allowed scales.</p>
 * <p>The scales are sorted from largest to smallest and the index starts at 0,
 * where 0 is the largest scale (most zoomed out).</p>
 * <pre><code>
 *   map: !map
 *     zoomLevels: !zoomLevels
 *        scales: [5000, 10000, 25000, 50000, 100000, 500000]</code></pre>
 * [[examples=datasource_many_dynamictables_legend]]
 *
 * @author Jesse on 4/1/14.
 */
public final class ZoomLevels implements ConfigurationObject {
    private double[] scales;

    /**
     * Constructor.
     *
     * @param scales do not need to be sorted or unique.
     */
    public ZoomLevels(final double... scales) {
        setScales(scales);
    }

    /**
     * default constructor for constructing by spring.
     */
    public ZoomLevels() {
        // intentionally empty
    }

    /**
     * Set the scales (sorts from largest to smallest).
     * @param scales The scales (may be unsorted).
     */
    public void setScales(final double[] scales) {
        TreeSet<Double> sortedSet = new TreeSet<Double>(Ordering.natural().reverse());
        for (int i = 0; i < scales.length; i++) {
            sortedSet.add(scales[i]);
        }
        this.scales = new double[sortedSet.size()];
        int i = 0;
        for (Double aDouble : sortedSet) {
            this.scales[i] = aDouble;
            i++;
        }
    }

    /**
     * The number of zoom levels.
     */
    public int size() {
        return this.scales.length;
    }

    /**
     * Get the zoom level at the given index.
     * @param index the index of the zoom level to access.
     */
    public double get(final int index) {
        return this.scales[index];
    }

    @Override
    public String toString() {
        return Arrays.toString(this.scales);
    }

    // CHECKSTYLE:OFF
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ZoomLevels that = (ZoomLevels) o;

        if (!Arrays.equals(scales, that.scales)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(scales);
    }


    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        if (scales == null || scales.length == 0) {
            validationErrors.add(new ConfigurationException("There are no scales defined in " + getClass().getName()));
        }
    }

    /**
     * Return a copy of the zoom level scale denominators. Scales are sorted greatest to least.
     */
    public double[] getScales() {
        double[] dest = new double[this.scales.length];
        System.arraycopy(this.scales, 0, dest, 0, this.scales.length);
        return dest;
    }

    // CHECKSTYLE:ON
}
