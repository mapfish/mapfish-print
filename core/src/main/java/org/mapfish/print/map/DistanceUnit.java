package org.mapfish.print.map;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.measure.Unit;

import static org.mapfish.print.Constants.INCH_TO_MM;


/**
 * An enum for expressing distance units. Contains everything needed for conversions and others.
 */
public enum DistanceUnit {
    /**
     * Represents the meter unit.
     */
    M(1.0, new String[]{"m", "meter", "meters"}),
    /**
     * Represents the millimeter unit.
     */
    MM(DistanceUnit.M, 0.001, new String[]{"mm", "millimeter", "millimeters"}),
    /**
     * Represents the centimeter unit.
     */
    CM(DistanceUnit.M, 0.01, new String[]{"cm", "centimeter", "centimeters"}),
    /**
     * Represents the kilometer unit.
     */
    KM(DistanceUnit.M, 1000.0, new String[]{"km", "kilometer", "kilometers"}),

    /**
     * Represents the american foot unit.
     */
    FT(INCH_TO_MM / 1000.0 * 12.0, new String[]{"ft", "foot", "feet"}),
    /**
     * Represents the american inch unit.
     */
    IN(DistanceUnit.FT, 1 / 12.0, new String[]{"in", "inch"}),
    /**
     * Represents the american yard unit.
     */
    YD(DistanceUnit.FT, 3.0, new String[]{"yd", "yard", "yards"}),
    /**
     * Represents the american mile unit.
     */
    MI(DistanceUnit.FT, 5280.0, new String[]{"mi", "mile", "miles"}),

    /**
     * Represents the lat long degree unit.
     */
    DEGREES(40041470.0 / 360.0, new String[]{"\u00B0", "dd", "deg", "degree", "degrees"}),
    /**
     * Represents the lat long minute unit.
     */
    MINUTE(DistanceUnit.DEGREES, 1.0 / 60.0, new String[]{"min", "minute", "minutes"}),
    /**
     * Represents the lat long second unit.
     */
    SECOND(DistanceUnit.MINUTE, 1.0 / 60.0, new String[]{"sec", "second", "seconds"}),

    /**
     * Represents the pixel unit. The conversion factor is the one used by JasperReports (1 inch = 72 pixel).
     */
    PX(1 / 72.0 * (INCH_TO_MM / 1000.0), new String[]{"px", "pixel"}),

    /**
     * Represents the point unit.
     */
    PT(DistanceUnit.IN, 1.0 / 72.0, new String[]{"pt", "point"}),
    /**
     * Represents the pica unit.
     */
    PC(DistanceUnit.PT, 12.0, new String[]{"pc", "pica"});

    /**
     * Global dictionary of every textual representations of every units.
     */
    private static Map<String, DistanceUnit> translations = null;
    /**
     * If null means that this is a base unit. Otherwise, point to the base unit.
     */
    private final DistanceUnit baseUnit;
    /**
     * Conversion factor to the base unit.
     */
    private final double baseFactor;
    /**
     * Conversion factor to meters.
     */
    private final double metersFactor;
    /**
     * All the ways to represent this unit as text.
     */
    private final String[] texts;
    /**
     * Cache all the units that share the same base unit.
     */
    private DistanceUnit[] allUnits = null;

    /**
     * Constructor.
     *
     * @param factor the factor to meter.
     * @param texts unit representations.
     */
    DistanceUnit(final double factor, final String[] texts) {
        this.baseUnit = this;
        this.baseFactor = 1.0;
        this.metersFactor = factor;
        this.texts = texts;
    }

    /**
     * Constructor.
     *
     * @param baseUnit the base unit.
     * @param factor the factor to the base unit.
     * @param texts unit representations.
     */
    DistanceUnit(final DistanceUnit baseUnit, final double factor, final String[] texts) {
        // Get the real base
        this.baseUnit = baseUnit.baseUnit;
        this.baseFactor = factor * baseUnit.baseFactor;
        this.metersFactor = baseUnit.metersFactor * factor;
        this.texts = texts;
    }

    /**
     * Parse the value and return the identified unit object.
     *
     * @param val the string to parse.
     * @return null if this unit is unknown
     */
    public static DistanceUnit fromString(final String val) {
        return getTranslations().get(val.toLowerCase());
    }

    /**
     * Return the first unit that would give a value &gt;=1.
     *
     * @param value the value
     * @param unit the unit of the value
     */
    public static DistanceUnit getBestUnit(final double value, final DistanceUnit unit) {
        DistanceUnit[] units = unit.getAllUnits();
        for (int i = units.length - 1; i >= 0; --i) {
            DistanceUnit cur = units[i];
            final double converted = Math.abs(unit.convertTo(1.0, cur) * value);
            if (converted >= 1.0) {
                return cur;
            }
        }
        return units[0];
    }

    private static synchronized Map<String, DistanceUnit> getTranslations() {
        if (translations == null) {
            translations = new HashMap<>();
            final DistanceUnit[] values = DistanceUnit.values();
            for (DistanceUnit cur: values) {
                for (int j = 0; j < cur.texts.length; ++j) {
                    translations.put(cur.texts[j], cur);
                }
            }
        }
        return translations;
    }

    /**
     * Determine the unit of the given projection.
     *
     * @param projection the projection to determine
     */
    public static DistanceUnit fromProjection(final CoordinateReferenceSystem projection) {
        final Unit<?> projectionUnit = projection.getCoordinateSystem().getAxis(0).getUnit();
        return DistanceUnit.fromString(projectionUnit.toString());
    }

    public boolean isBase() {
        return this.baseUnit == this;
    }

    /**
     * Convert values in this unit to the equivalent value in another unit.
     * <pre>
     * DistanceUnit.M.convertTo(1.0, DistanceUnit.MM)==1000.0
     * </pre>
     *
     * @param value a value in the same unit as this {@link org.mapfish.print.map.DistanceUnit}
     * @param targetUnit the unit to convert value to (from this unit)
     */
    public double convertTo(final double value, final DistanceUnit targetUnit) {
        if (targetUnit == this) {
            return value;
        }
        if (isSameBaseUnit(targetUnit)) {
            return value * this.baseFactor / targetUnit.baseFactor;
        } else {
            return value * this.metersFactor / targetUnit.metersFactor;
        }
    }

    /**
     * Check if this unit and the target unit have the same "base" unit  IE inches and feet have same base
     * unit.
     *
     * @param target the unit to compare to this unit.
     */
    public boolean isSameBaseUnit(final DistanceUnit target) {
        return target.baseUnit == this.baseUnit || target == this.baseUnit || target.baseUnit == this;
    }

    @Override
    public final String toString() {
        return this.texts[0];
    }

    /**
     * Return the sorted list (from smallest to biggest) of units sharing the same base unit.
     *
     * @return the sorted list (from smallest to biggest) of units sharing the same base unit.
     */
    public final synchronized DistanceUnit[] getAllUnits() {
        if (this.allUnits == null) {
            if (this.baseUnit != this) {
                this.allUnits = this.baseUnit.getAllUnits();
            } else {
                final DistanceUnit[] values = DistanceUnit.values();
                final List<DistanceUnit> list = new ArrayList<>(values.length);
                for (DistanceUnit value: values) {
                    if (value.baseUnit == this) {
                        list.add(value);
                    }
                }
                final DistanceUnit[] result = new DistanceUnit[list.size()];
                list.toArray(result);
                Arrays.sort(result, Comparator.comparingDouble(o -> o.baseFactor));
                this.allUnits = result;
            }
        }

        return this.allUnits;
    }
}
