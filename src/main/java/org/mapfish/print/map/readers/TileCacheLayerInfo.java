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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mapfish.print.InvalidValueException;
import org.mapfish.print.utils.PJsonArray;

/**
 * Holds the information we need to manage a tilecache layer.
 */
public class TileCacheLayerInfo {
    /**
     * Tolerance we accept when trying to determine the nearest resolution.
     */
    protected float resolutionTolerance(){
        return 1.05f;
    }

    protected static final Pattern FORMAT_REGEXP = Pattern.compile("^[^/]+/([^/]+)$");
    protected static final Pattern RESOLUTIONS_REGEXP = Pattern.compile("\\s+");

    protected final int width;
    protected final int height;
    protected final float[] resolutions;
    protected final float minX;
    protected final float minY;
    protected final float maxX;
    protected final float maxY;
    protected final float originX;
    protected final float originY;
    protected String extension;

    public TileCacheLayerInfo(String resolutions, int width, int height, float minX, float minY, float maxX, float maxY, String format,
            float originX, float originY) {
        String[] resolutionsTxt = RESOLUTIONS_REGEXP.split(resolutions);
        this.resolutions = new float[resolutionsTxt.length];
        for (int i = 0; i < resolutionsTxt.length; ++i) {
            this.resolutions[i] = Float.parseFloat(resolutionsTxt[i]);
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
        this.resolutions = new float[resolutions.size()];
        for (int i = 0; i < resolutions.size(); ++i) {
            this.resolutions[i] = resolutions.getFloat(i);
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

    public ResolutionInfo getNearestResolution(float targetResolution) {
        int pos = resolutions.length - 1;
        float result = resolutions[pos];
        final float tolerance = resolutionTolerance();
        for (int i = resolutions.length - 1; i >= 0; --i) {
            float cur = resolutions[i];

            if (cur <= targetResolution * tolerance) {
                result = cur;
                pos = i;
                if(cur == targetResolution) {
                    break;
                }
            }
        }
        return new ResolutionInfo(pos, result);
    }

    public float[] getResolutions() {
        return resolutions;
    }

    public String getExtension() {
        return extension;
    }

    public static class ResolutionInfo {
        public final int index;
        public final float value;

        public ResolutionInfo(int index, float value) {
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
            return index == that.index && Float.compare(that.value, value) == 0;

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

    public float getMinX() {
        return minX;
    }

    public float getMinY() {
        return minY;
    }

    public float getMaxY() {
        return maxY;
    }

    public float getOriginX() {
        return originX;
    }

    public float getOriginY() {
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
    public boolean isVisible(float x1, float y1, float x2, float y2) {
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
            float temp = this.resolutions[left];
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
        if (Float.compare(that.maxX, maxX) != 0) return false;
        if (Float.compare(that.maxY, maxY) != 0) return false;
        if (Float.compare(that.minX, minX) != 0) return false;
        if (Float.compare(that.minY, minY) != 0) return false;
        if (width != that.width) return false;
        if (!extension.equals(that.extension)) return false;
        if (!Arrays.equals(resolutions, that.resolutions)) return false;

        return true;
    }
}
