/*
 * Copyright (C) 2008 Patrick Valsecchi
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  U
 */
package org.pvalsecc.misc;

/**
 * Some functions to convert an elapsed time, file size, ... into a human readable format.
 */
public abstract class UnitUtilities {
    private static final String[] COMPUTER_UNITS = {"", "K", "M", "G", "T", "P", "E"};

    /**
     * Converts a number of bytes into K, M, G or ... (in increment of 1024)
     */
    public static String toComputerSize(double value) {
        int curUnit = 0;
        while (value >= 10000.0) {
            //noinspection AssignmentToMethodParameter
            value /= 1024.0;
            curUnit++;
        }

        if (curUnit > COMPUTER_UNITS.length) {
            throw new RuntimeException("Cannot convert (unit=2^3*" + curUnit + ")");
        }

        if (value < 10.0) {
            return String.format("%.2f%s", value, COMPUTER_UNITS[curUnit]);
        } else if (value + 0.05 < 100.0) {
            return String.format("%.1f%s", value, COMPUTER_UNITS[curUnit]);
        } else {
            return String.format("%d%s", Math.round(value), COMPUTER_UNITS[curUnit]);
        }
    }

    /**
     * Converts a time in milli-seconds into ms, s, m, h or d.
     */
    public static String toElapsedTime(long millis) {
        double secs = millis / 1000.0;

        if (secs < 10.0) {
            return String.format("%dms", millis);
        } else if (secs < 60 - 0.005) {
            return String.format("%.2fs", secs);
        } else if (secs < 3600 - 0.5) {
            return String.format("%dm%02ds", (int) Math.floor((secs + 0.005) / 60.0), Math.round(secs) % 60);
        } else if (secs < 24 * 3600 - 0.5) {
            return String.format("%dh%02dm%02ds", (int) Math.floor((secs + 0.5) / 3600.0), (int) Math.floor((secs + 0.5) / 60.0) % 60, Math.round(secs) % 60);
        } else {
            return String.format("%dd%02dh%02dm%02ds", (int) Math.floor((secs + 0.5) / 3600.0 / 24.0), (int) Math.floor((secs + 0.5) / 3600.0) % 24, (int) Math.floor((secs + 0.5) / 60.0) % 60, Math.round(secs) % 60);
        }
    }

    /**
     * Converts a time in nano-seconds into ms, s, m, h or d.
     */
    public static String toElapsedNanoTime(long nanos) {
        if (nanos < 10 * 1000) {
            return String.format("%dns", nanos);
        }
        if (nanos < 10 * 1000 * 1000 - 500) {
            return String.format("%dus", (nanos + 500) / 1000);
        } else {
            return toElapsedTime((nanos + 500000) / (1000 * 1000));
        }
    }
}
