/*
 * Copyright (C) 2013  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.map.readers;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mapfish.print.InvalidValueException;
import org.mapfish.print.utils.PJsonArray;

/**
 * Holds the information we need to manage a tilecache layer.
 */
public class TileCacheLayerInfo {
    private List<String> versions;

    /**
     * Tolerance we accept when trying to determine the nearest resolution.
     */
    public float getResolutionTolerance(){
        return 1.05f;
    }

    protected static final Pattern FORMAT_REGEXP = Pattern.compile("^[^/]+/([^/]+)$");
    protected static final Pattern RESOLUTIONS_REGEXP = Pattern.compile("(\\s+)|,");

    protected final int width;
    protected final int height;
    protected final double[] resolutions;
    protected final double minX;
    protected final double minY;
    protected final double maxX;
    protected final double maxY;
    protected final double originX;
    protected final double originY;
    protected String extension;

    public TileCacheLayerInfo(String resolutions, int width, int height, double minX, double minY, double maxX, double maxY, String format,
                              double originX, double originY) {
        String[] resolutionsTxt = RESOLUTIONS_REGEXP.split(resolutions);
        this.resolutions = new double[resolutionsTxt.length];
        for (int i = 0; i < resolutionsTxt.length; ++i) {
            this.resolutions[i] = Double.parseDouble(resolutionsTxt[i]);
        }
        sortResolutions();

        this.width = width;
        this.height = height;
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.originX = originX;
        this.originY = originY;

        if (format != null) {
            Matcher formatMatcher = FORMAT_REGEXP.matcher(format);
            if (formatMatcher.matches()) {
                extension = formatMatcher.group(1).toLowerCase();
                if (extension.equals("jpg")) {
                    extension = "jpeg";
                }
            } else {
                throw new InvalidValueException("format", format);
            }
        }
    }

    public TileCacheLayerInfo(String resolutions, int width, int height, float minX, float minY, float maxX, float maxY, String format) {
        this(resolutions, width, height, minX, minY, maxX, maxY, format, minX, minY);
    }

    public TileCacheLayerInfo(PJsonArray resolutions, int width, int height, float minX, float minY, float maxX, float maxY, String extension,
            float originX, float originY) {
        this.resolutions = new double[resolutions.size()];
        for (int i = 0; i < resolutions.size(); ++i) {
            this.resolutions[i] = resolutions.getDouble(i);
        }
        sortResolutions();

        this.width = width;
        this.height = height;
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.originX = originX;
        this.originY = originY;
        this.extension = extension;
    }

    public TileCacheLayerInfo(PJsonArray resolutions, int width, int height, float minX, float minY, float maxX, float maxY, String extension) {
        this(resolutions, width, height, minX, minY, maxX, maxY, extension, minX, minY);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ResolutionInfo getNearestResolution(double targetResolution) {
        int pos = resolutions.length - 1;
        double result = resolutions[pos];
        final float tolerance = getResolutionTolerance();
        for (int i = resolutions.length - 1; i >= 0; --i) {
            double cur = resolutions[i];

            double distance = Math.abs(targetResolution - cur);
            if (cur <= targetResolution * tolerance) {
                if (distance <= Math.abs(targetResolution - result)) {
                    result = cur;
                    pos = i;
                    if(distance < 0.0000001f) {
                        break;
                    }
                }
            } else {
                break;
            }
        }
        return new ResolutionInfo(pos, result);
    }

    public double[] getResolutions() {
        return resolutions;
    }

    public String getExtension() {
        return extension;
    }

    public static class ResolutionInfo {
        public final int index;
        public final double value;

        public ResolutionInfo(int index, double value) {
            this.index = index;
            this.value = value;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ResolutionInfo that = (ResolutionInfo) o;
            return index == that.index && Double.compare(that.value, value) == 0;

        }

        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("ResolutionInfo");
            sb.append("{index=").append(index);
            sb.append(", result=").append(value);
            sb.append('}');
            return sb.toString();
        }
    }

    public double getMinX() {
        return minX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMaxY() {
        return maxY;
    }

    public double getOriginX() {
        return originX;
    }

    public double getOriginY() {
        return originY;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("TileCacheLayerInfo");
        sb.append("{width=").append(width);
        sb.append(", height=").append(height);
        sb.append(", minX=").append(minX);
        sb.append(", minY=").append(minY);
        sb.append(", maxX=").append(maxX);
        sb.append(", maxY=").append(maxY);
        sb.append(", originX=").append(originX);
        sb.append(", originY=").append(originY);
        sb.append(", extension='").append(extension).append('\'');
        sb.append(", resolutions=").append(resolutions == null ? "null" : "");
        for (int i = 0; resolutions != null && i < resolutions.length; ++i) {
            sb.append(i == 0 ? "" : ", ").append(resolutions[i]);
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * Receives the extent of a tile and checks that tilecache has it.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public boolean isVisible(double x1, double y1, double x2, double y2) {
        return x1 >= minX && x1 <= maxX && y1 >= minY && y1 <= maxY /*&&
                x2 >= minX && x2 <= maxX && y2 >= minY && y2 <= maxY*/;
        //we don't use x2 and y2 since tilecache doesn't seems to care about those...
    }

    /**
     * The resolutions must be sorted in descending order.
     */
    private void sortResolutions() {
        Arrays.sort(this.resolutions);
        int right = this.resolutions.length - 1;
        for (int left = 0; left < right; left++, right--) {
            double temp = this.resolutions[left];
            this.resolutions[left] = this.resolutions[right];
            this.resolutions[right] = temp;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TileCacheLayerInfo that = (TileCacheLayerInfo) o;

        if (height != that.height) return false;
        if (Double.compare(that.maxX, maxX) != 0) return false;
        if (Double.compare(that.maxY, maxY) != 0) return false;
        if (Double.compare(that.minX, minX) != 0) return false;
        if (Double.compare(that.minY, minY) != 0) return false;
        if (width != that.width) return false;
        if (!extension.equals(that.extension)) return false;
        if (!Arrays.equals(resolutions, that.resolutions)) return false;

        return true;
    }
}
