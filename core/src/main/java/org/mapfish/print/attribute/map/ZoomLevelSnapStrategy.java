/*
 * Copyright (C) 2014  Camptocamp
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

package org.mapfish.print.attribute.map;

import org.mapfish.print.map.Scale;

/**
 * Enumerates the different strategies for finding the closest zoom-level/scale.
 *
 * @author Jesse on 3/31/14.
 */
public enum ZoomLevelSnapStrategy {
    /**
     * Find the closest zoom level.  If the targetScale is directly between two zoomLevels then the smaller/higher resolution scale
     * will be chosen.
     */
    CLOSEST_LOWER_SCALE_ON_TIE {
        @Override
        protected SearchResult search(final Scale scale, final double tolerance, final ZoomLevels zoomLevels) {
            double targetScale = scale.getDenominator();

            int pos = zoomLevels.size() - 1;
            double distance = Math.abs(zoomLevels.get(pos) - targetScale);

            for (int i = zoomLevels.size() - 2; i >= 0; --i) {
                double cur = zoomLevels.get(i);

                double newDistance = Math.abs(targetScale - cur);
                if (newDistance < distance) {
                    distance = newDistance;
                    pos = i;
                    if (distance < Constants.DISTANCE_TREATED_AS_EQUAL) {
                        break;
                    }
                }
            }
            return new SearchResult(pos, zoomLevels);
        }
    },
    /**
     * Find the closest zoom level.  If the targetScale is directly between two zoomLevels then the larger/lower resolution scale
     * will be chosen.
     */
    CLOSEST_HIGHER_SCALE_ON_TIE {
        @Override
        protected SearchResult search(final Scale scale, final double tolerance, final ZoomLevels zoomLevels) {
            double targetScale = scale.getDenominator();
            int pos = zoomLevels.size() - 1;
            double distance = Math.abs(zoomLevels.get(pos) - targetScale);

            for (int i = 1; i < zoomLevels.size(); i++) {
                double cur = zoomLevels.get(i);

                double newDistance = Math.abs(targetScale - cur);
                if (newDistance < distance) {
                    distance = newDistance;
                    pos = i;
                    if (distance < Constants.DISTANCE_TREATED_AS_EQUAL) {
                        break;
                    }
                }
            }
            return new SearchResult(pos, zoomLevels);
        }
    },
    /**
     * Always choose the zoom-level that is just higher than the target value.
     */
    HIGHER_SCALE {
        @Override
        protected SearchResult search(final Scale scale, final double tolerance, final ZoomLevels zoomLevels) {
            double targetScale = scale.getDenominator();
            final double cutOff = targetScale * (1 - tolerance);

            int pos = zoomLevels.size() - 1;
            for (int i = zoomLevels.size() - 1; i >= 0; --i) {
                double cur = zoomLevels.get(i);

                if (cur >= cutOff) {
                    pos = i;
                    break;
                }
            }

            return new SearchResult(pos, zoomLevels);
        }
    },
    /**
     * Always choose the zoom-level that is just lower than the target value.
     */
    LOWER_SCALE {
        @Override
        protected SearchResult search(final Scale scale, final double tolerance, final ZoomLevels zoomLevels) {
            double targetScale = scale.getDenominator();
            final double cutOff = targetScale * (1 + tolerance);

            int pos = 0;
            for (int i = 1; i < zoomLevels.size(); i++) {
                double cur = zoomLevels.get(i);

                if (cur <= cutOff) {
                    pos = i;
                    break;
                }
            }

            return new SearchResult(pos, zoomLevels);
        }
    };

    /**
     * Search the provided zoomLevels for the scale that is the closest according to the current strategy.
     *
     * @param targetScale the reference scale
     * @param tolerance    the amount from one of the zoomLevels to still be considered <em>at</em> the scale.
     *                     This is important for all strategies other than CLOSEST in order to prevent the scale from jumping
     *                     to a different version even when it is very close to one of the zoomLevels.
     * @param zoomLevels   the allowed zoomLevels
     */
    protected abstract SearchResult search(Scale targetScale, double tolerance, ZoomLevels zoomLevels);

    /**
     * The results of a search.
     */
    public static final class SearchResult {
        private final int zoomLevel;
        private final ZoomLevels zoomLevels;

        SearchResult(final int zoomLevel, final ZoomLevels zoomLevels) {
            this.zoomLevel = zoomLevel;
            this.zoomLevels = zoomLevels;
        }

        public int getZoomLevel() {
            return this.zoomLevel;
        }

        public ZoomLevels getZoomLevels() {
            return this.zoomLevels;
        }

        public Scale getScale() {
            return new Scale(this.zoomLevels.get(this.zoomLevel));
        }

        // CHECKSTYLE:OFF

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SearchResult that = (SearchResult) o;

            if (zoomLevel != that.zoomLevel) return false;
            if (!zoomLevels.equals(that.zoomLevels)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = zoomLevel;
            result = 31 * result + zoomLevels.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "SearchResult{" +
                   "zoomLevel=" + zoomLevel +
                   ", scale=" + zoomLevels.get(zoomLevel) +
                   ", zoomLevels=" + zoomLevels +
                   '}';
        }

// CHECKSTYLE:ON

    }

    private static class Constants {
        private static final double DISTANCE_TREATED_AS_EQUAL = 0.00000000001;
    }
}
