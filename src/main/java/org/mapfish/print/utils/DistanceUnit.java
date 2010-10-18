/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Server
 *
 * MapFish Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MapFish Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.utils;

import org.ho.yaml.wrapper.EnumWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An enum for expressing distance units. Contains everything needed for
 * convertions and others.
 */
public enum DistanceUnit {

    M(null, 1.0, 1.0, new String[]{"m", "meter", "meters"}),
    MM(DistanceUnit.M, 0.001, 0.001, new String[]{"mm", "millimeter", "millimeters"}),
    CM(DistanceUnit.M, 0.01, 0.01, new String[]{"cm", "centimeter", "centimeters"}),
    KM(DistanceUnit.M, 1000.0, 1000.0, new String[]{"km", "kilometer", "kilometers"}),

    FT(null, 1.0, 25.4 / 1000.0 * 12.0, new String[]{"ft", "foot", "feet"}),
    PT(DistanceUnit.FT, 1 / 12.0 / 72.0, 25.4 / 1000.0 / 72.0, new String[]{"pt", "point"}),
    IN(DistanceUnit.FT, 1 / 12.0, 25.4 / 1000.0, new String[]{"in", "inch"}),
    YD(DistanceUnit.FT, 3.0, 25.4 / 1000.0 * 12.0 * 3.0, new String[]{"yd", "yard", "yards"}),
    MI(DistanceUnit.FT, 5280.0, 25.4 / 1000.0 * 12.0 * 5280.0, new String[]{"mi", "mile", "miles"}),

    DEGREES(null, 1.0, 40041470.0 / 360.0, new String[]{"\u00B0", "dd", "degree", "degrees"}),
    MINUTE(DistanceUnit.DEGREES, 1.0 / 60.0, 40041470.0 / 360.0, new String[]{"min", "minute", "minutes"}),
    SECOND(DistanceUnit.DEGREES, 1.0 / 3600.0, 40041470.0 / 360.0, new String[]{"sec", "second", "seconds"});

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
     * Global dictionnary of every textual representations of every units.
     */
    private static Map<String, DistanceUnit> translations = null;

    DistanceUnit(DistanceUnit baseUnit, double baseFactor, double metersFactor, String[] texts) {
        this.baseUnit = baseUnit;
        this.baseFactor = baseFactor;
        this.texts = texts;
        this.metersFactor = metersFactor;
    }

    public boolean isBase() {
        return baseUnit == this;
    }

    /**
     * How much is "value" in unit "this" exprimed in the unit "target"). For example:
     * <pre>
     * DistanceUnit.M.convertTo(1.0, DistanceUnit.MM)==1000.0
     * </pre>
     */
    public double convertTo(double value, DistanceUnit targetUnit) {
        if (isSameBaseUnit(targetUnit)) {
            return value * baseFactor / targetUnit.baseFactor;
        } else {
            return value * metersFactor / targetUnit.metersFactor;
        }
    }

    public boolean isSameBaseUnit(DistanceUnit target) {
        return target.baseUnit == baseUnit || target == baseUnit || target.baseUnit == this;
    }

    public String toString() {
        return texts[0];
    }

    /**
     * @return null if this unit is unknown
     */
    public static DistanceUnit fromString(String val) {
        return getTranslations().get(val.toLowerCase());
    }

    /**
     * @return the sorted list (from smallest to biggest) of units sharing the
     *         same base unit.
     */
    public synchronized DistanceUnit[] getAllUnits() {
        if (allUnits != null) {
            return allUnits;
        } else if (baseUnit != null) {
            return allUnits = baseUnit.getAllUnits();
        } else {
            final DistanceUnit[] values = DistanceUnit.values();
            final List<DistanceUnit> list = new ArrayList<DistanceUnit>(values.length);
            list.add(this);
            for (int i = 0; i < values.length; ++i) {
                DistanceUnit value = values[i];
                if (value.baseUnit == this) {
                    list.add(value);
                }
            }
            final DistanceUnit[] result = new DistanceUnit[list.size()];
            list.toArray(result);
            Arrays.sort(result, new Comparator<DistanceUnit>() {
                public int compare(DistanceUnit o1, DistanceUnit o2) {
                    return Double.compare(o1.baseFactor, o2.baseFactor);
                }
            });
            allUnits = result;
            return result;
        }
    }

    /**
     * Return the first unit that would give a value >=1
     */
    public static DistanceUnit getBestUnit(double value, DistanceUnit unit) {
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
            translations = new HashMap<String, DistanceUnit>();
            final DistanceUnit[] values = DistanceUnit.values();
            for (int i = 0; i < values.length; ++i) {
                DistanceUnit cur = values[i];
                for (int j = 0; j < cur.texts.length; ++j) {
                    translations.put(cur.texts[j], cur);
                }
            }
        }
        return translations;
    }

    public static class Wrapper extends EnumWrapper {
        public Wrapper(Class type) {
            super(type);
        }

        public void setObject(Object obj) {
            if (obj instanceof String) {
                super.setObject(DistanceUnit.fromString((String) obj));
            } else {
                super.setObject(obj);
            }
        }
    }
}
