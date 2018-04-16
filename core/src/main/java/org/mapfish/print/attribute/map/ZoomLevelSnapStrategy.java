package org.mapfish.print.attribute.map;

import org.mapfish.print.map.DistanceUnit;
import org.mapfish.print.map.Scale;

/**
 * Enumerates the different strategies for finding the closest zoom-level/scale.
 */
public enum ZoomLevelSnapStrategy {
    /**
     * Find the closest zoom level.  If the targetScale is directly between two zoomLevels then the
     * smaller/higher resolution scale will be chosen.
     */
    CLOSEST_LOWER_SCALE_ON_TIE {
        @Override
        protected SearchResult search(
                final Scale scale, final double tolerance,
                final ZoomLevels zoomLevels) {
            double resolution = scale.getResolution();
            int pos = -1;
            double distance = Double.POSITIVE_INFINITY;

            for (int i = zoomLevels.size() - 1; i >= 0; --i) {
                Scale cur = zoomLevels.get(i, scale.getUnit());

                double newDistance = Math.abs(resolution - cur.getResolution());
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
     * Find the closest zoom level.  If the targetScale is directly between two zoomLevels then the
     * larger/lower resolution scale will be chosen.
     */
    CLOSEST_HIGHER_SCALE_ON_TIE {
        @Override
        protected SearchResult search(
                final Scale scale, final double tolerance,
                final ZoomLevels zoomLevels) {
            double resolution = scale.getResolution();
            int pos = -1;
            double distance = Double.POSITIVE_INFINITY;

            for (int i = 0; i < zoomLevels.size(); i++) {
                Scale cur = zoomLevels.get(i, scale.getUnit());

                double newDistance = Math.abs(resolution - cur.getResolution());
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
        protected SearchResult search(
                final Scale scale, final double tolerance,
                final ZoomLevels zoomLevels) {
            final double cutOff = scale.getResolution() * (1 - tolerance);

            int pos = zoomLevels.size() - 1;
            for (int i = zoomLevels.size() - 1; i >= 0; --i) {
                Scale cur = zoomLevels.get(i, scale.getUnit());

                if (cur.getResolution() >= cutOff) {
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
        protected SearchResult search(
                final Scale scale, final double tolerance,
                final ZoomLevels zoomLevels) {
            final double cutOff = scale.getResolution() * (1 + tolerance);

            int pos = -1;
            for (int i = 0; i < zoomLevels.size(); i++) {
                Scale cur = zoomLevels.get(i, scale.getUnit());

                if (cur.getResolution() <= cutOff) {
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
     * @param scale the reference scale
     * @param tolerance the amount from one of the zoomLevels to still be considered <em>at</em> the scale.
     *      This is important for all strategies other than CLOSEST in order to prevent the scale from jumping
     *      to a different version even when it is very close to one of the zoomLevels.
     * @param zoomLevels the allowed zoomLevels.
     */
    protected abstract SearchResult search(
            Scale scale, double tolerance, ZoomLevels zoomLevels);

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

        /**
         * Gets the zoom level.
         * @return the zoom level
         */
        public int getZoomLevel() {
            return this.zoomLevel;
        }

        /**
         * Gets the zoom levels.
         * @return the zoom levels
         */
        public ZoomLevels getZoomLevels() {
            return this.zoomLevels;
        }

        /**
         * Gets the scale.
         * @param unit the unit
         * @return the scale
         */
        public Scale getScale(final DistanceUnit unit) {
            return this.zoomLevels.get(this.zoomLevel, unit);
        }

        // CHECKSTYLE:OFF

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SearchResult that = (SearchResult) o;

            return zoomLevel == that.zoomLevel && zoomLevels.equals(that.zoomLevels);
        }

        @Override
        public int hashCode() {
            int result = zoomLevel;
            result = 31 * result + zoomLevels.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return String.format("SearchResult{zoomLevel=%s, scale=%s, zoomLevels=%s}",
                    zoomLevel, zoomLevels.get(zoomLevel, DistanceUnit.M), zoomLevels);
        }

// CHECKSTYLE:ON

    }

    private static class Constants {
        private static final double DISTANCE_TREATED_AS_EQUAL = 0.00000000001;
    }
}
