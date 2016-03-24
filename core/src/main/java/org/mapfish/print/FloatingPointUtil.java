package org.mapfish.print;

/**
 * Util class to Test equality of floating point
 * 
 * @author ayman on 2/22/16.
 */
 
public final class FloatingPointUtil {
    private static final float EPSILON =  0.00000001F;

    private FloatingPointUtil() {
    }

    /**
     * Check the equality of two floats taking into consideration the precision issue of floating point arithmetic in Java   
     * 
     * @param f1
     * @param f2
     * @return
     */
    public static boolean equals(final float f1, final float f2) {
        return Math.abs(f1 - f2) <= EPSILON;
    }

    /**
     * Check the equality of two doubles taking into consideration the precision issue of floating point arithmetic in Java
     * @param d1
     * @param d2
     * @return
     */
    public static boolean equals(final double d1, final double d2) {
        return Math.abs(d1 - d2) <= EPSILON;
    }
}