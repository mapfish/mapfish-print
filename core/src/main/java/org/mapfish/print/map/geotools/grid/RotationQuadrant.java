package org.mapfish.print.map.geotools.grid;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * The strategies for rotating and translating the  when the rotation is in a specific rotation.
 *
 * For example if the rotation is between 0 and 90 then the top and bottom text will be upside down.  In this case the
 * Quadrant 1 strategy will be used to ensure the  will be correctly oriented.
 */
enum RotationQuadrant {
    /**
     * The quadrant when the rotation is > 0 and <= 90.
     */
    QUADRANT_1 {
        @Override
        void updateTransform(final AffineTransform baseTransform, final int indent, final GridLabel.Side side,
                             final int halfCharHeight, final Rectangle2D textBounds) {
            switch (side) {
                case TOP:
                    baseTransform.rotate(Constants.MINUS_NINETY_RADIANS);
                    baseTransform.translate(-textBounds.getWidth() - indent, halfCharHeight);
                    break;
                case BOTTOM:
                    baseTransform.rotate(Constants.MINUS_NINETY_RADIANS);
                    baseTransform.translate(indent, halfCharHeight);
                    break;
                case RIGHT:
                    baseTransform.translate(-textBounds.getWidth() - indent, halfCharHeight);
                    break;
                default:
                    baseTransform.translate(indent, halfCharHeight);
            }

        }
    },
    /**
     * The quadrant when the rotation is > 90 and <= 180.
     */
    QUADRANT_2 {
        @Override
        void updateTransform(final AffineTransform baseTransform, final int indent, final GridLabel.Side side,
                             final int halfCharHeight, final Rectangle2D textBounds) {
            switch (side) {
                case TOP:
                    baseTransform.rotate(Constants.MINUS_NINETY_RADIANS);
                    baseTransform.translate(-textBounds.getWidth() - indent, halfCharHeight);
                    break;
                case BOTTOM:
                    baseTransform.rotate(Constants.MINUS_NINETY_RADIANS);
                    baseTransform.translate(indent, halfCharHeight);
                    break;
                case RIGHT:
                    baseTransform.rotate(Constants.ONE_EIGHTY_RADIANS);
                    baseTransform.translate(indent, halfCharHeight);
                    break;
                default:
                    baseTransform.rotate(Constants.ONE_EIGHTY_RADIANS);
                    baseTransform.translate(-textBounds.getWidth() - indent, halfCharHeight);
            }

        }
    },
    /**
     * The quadrant when the rotation is > 180 and <= 270.
     */
    QUADRANT_3 {
        @Override
        void updateTransform(final AffineTransform baseTransform, final int indent, final GridLabel.Side side,
                             final int halfCharHeight, final Rectangle2D textBounds) {
            switch (side) {
                case TOP:
                    baseTransform.rotate(Constants.NINETY_RADIANS);
                    baseTransform.translate(indent, halfCharHeight);
                    break;
                case BOTTOM:
                    baseTransform.rotate(Constants.NINETY_RADIANS);
                    baseTransform.translate(-textBounds.getWidth() - indent, halfCharHeight);
                    break;
                case RIGHT:
                    baseTransform.rotate(Constants.ONE_EIGHTY_RADIANS);
                    baseTransform.translate(indent, halfCharHeight);
                    break;
                default:
                    baseTransform.rotate(Constants.ONE_EIGHTY_RADIANS);
                    baseTransform.translate(-textBounds.getWidth() - indent, halfCharHeight);
            }

        }
    },
    /**
     * The quadrant when the rotation is > 270 and <= 360.
     */
    QUADRANT_4 {
        @Override
        void updateTransform(final AffineTransform baseTransform, final int indent, final GridLabel.Side side,
                             final int halfCharHeight, final Rectangle2D textBounds) {
            switch (side) {
                case TOP:
                    baseTransform.rotate(Constants.NINETY_RADIANS);
                    baseTransform.translate(indent, halfCharHeight);
                    break;
                case BOTTOM:
                    baseTransform.rotate(Constants.NINETY_RADIANS);
                    baseTransform.translate(-textBounds.getWidth() - indent, halfCharHeight);
                    break;
                case RIGHT:
                    baseTransform.translate(-textBounds.getWidth() - indent, halfCharHeight);
                    break;
                default:
                    baseTransform.translate(indent, halfCharHeight);
            }

        }
    },

    NO_ROTATION {
      @Override
        void updateTransform(final AffineTransform baseTransform, final int indent, final GridLabel.Side side,
                             final int halfCharHeight, final Rectangle2D textBounds) {
            switch (side) {
                case TOP:
                    baseTransform.translate(-textBounds.getWidth() / 2.0, indent + halfCharHeight * 2.0);
                    break;
                case BOTTOM:
                    baseTransform.translate(-textBounds.getWidth() / 2.0, -halfCharHeight - indent);
                    break;
                case RIGHT:
                    baseTransform.translate(-textBounds.getWidth() - indent, halfCharHeight);
                    break;
                default:
                    baseTransform.translate(indent, halfCharHeight);
            }

        }
    };

    public static final double THREE_SIXTY_RADIANS = Math.PI * 2;

    abstract void updateTransform(
            AffineTransform baseTransform, int indent, GridLabel.Side side,
            int halfCharHeight, Rectangle2D textBounds);

    static RotationQuadrant getQuadrant(final double rotation, final boolean rotate) {
        if (!rotate) {
            return NO_ROTATION;
        }

        double rot = rotation;
        while (rot > THREE_SIXTY_RADIANS) {
            rot -= THREE_SIXTY_RADIANS;
        }

        while (rot < 0) {
            rot += THREE_SIXTY_RADIANS;
        }

        if (rotation > 0 && rotation <= Math.PI / 2) {
            return QUADRANT_1;
        }
        if (rot > Math.PI / 2 && rot <= Math.PI) {
            return QUADRANT_2;
        }
        if (rot > Math.PI && rot <= Math.PI * 3 / 2) {
            return QUADRANT_3;
        }
        return QUADRANT_4;
    }

    private static class Constants {
        public static final double MINUS_NINETY_RADIANS = -Math.PI / 2;
        public static final double ONE_EIGHTY_RADIANS = Math.PI;
        public static final double NINETY_RADIANS = Math.PI / 2;
    }
}
