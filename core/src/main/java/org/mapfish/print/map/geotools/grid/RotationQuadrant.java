package org.mapfish.print.map.geotools.grid;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * The strategies for rotating and translating the  when the rotation is in a specific rotation.
 *
 * For example if the rotation is between 0 and 90 then the top and bottom text will be upside down.  In this case the
 * Quadrant 1 strategy will be used to ensure the  will be correctly oriented.
 *
 * @author Jesse on 8/6/2015.
 */
enum RotationQuadrant {
    /**
     * The quadrant when the rotation is > 0 and <= 90.
     */
    QUADRANT_1 {
        @Override
        void updateTransform(final AffineTransform baseTransform, final int indent, final GridLabel.Side side,
                             final int charHeight, final Rectangle2D textBounds) {
            switch (side) {
                case TOP:
                    baseTransform.rotate(Constants.MINUS_NINETY_RADIANS);
                    baseTransform.translate(-textBounds.getWidth() - indent, charHeight);
                    break;
                case BOTTOM:
                    baseTransform.rotate(Constants.MINUS_NINETY_RADIANS);
                    baseTransform.translate(indent, charHeight);
                    break;
                case RIGHT:
                    baseTransform.translate(-textBounds.getWidth() - indent, charHeight);
                    break;
                default:
                    baseTransform.translate(indent, charHeight);
            }

        }
    },
    /**
     * The quadrant when the rotation is > 90 and <= 180.
     */
    QUADRANT_2 {
        @Override
        void updateTransform(final AffineTransform baseTransform, final int indent, final GridLabel.Side side,
                             final int charHeight, final Rectangle2D textBounds) {
            switch (side) {
                case TOP:
                    baseTransform.rotate(Constants.MINUS_NINETY_RADIANS);
                    baseTransform.translate(-textBounds.getWidth() - indent, charHeight);
                    break;
                case BOTTOM:
                    baseTransform.rotate(Constants.MINUS_NINETY_RADIANS);
                    baseTransform.translate(indent, charHeight);
                    break;
                case RIGHT:
                    baseTransform.rotate(Constants.ONE_EIGHTY_RADIANS);
                    baseTransform.translate(indent, charHeight);
                    break;
                default:
                    baseTransform.rotate(Constants.ONE_EIGHTY_RADIANS);
                    baseTransform.translate(-textBounds.getWidth() - indent, charHeight);
            }

        }
    },
    /**
     * The quadrant when the rotation is > 180 and <= 270.
     */
    QUADRANT_3 {
        @Override
        void updateTransform(final AffineTransform baseTransform, final int indent, final GridLabel.Side side,
                             final int charHeight, final Rectangle2D textBounds) {
            switch (side) {
                case TOP:
                    baseTransform.rotate(Constants.NINETY_RADIANS);
                    baseTransform.translate(indent, charHeight);
                    break;
                case BOTTOM:
                    baseTransform.rotate(Constants.NINETY_RADIANS);
                    baseTransform.translate(-textBounds.getWidth() - indent, charHeight);
                    break;
                case RIGHT:
                    baseTransform.rotate(Constants.ONE_EIGHTY_RADIANS);
                    baseTransform.translate(indent, charHeight);
                    break;
                default:
                    baseTransform.rotate(Constants.ONE_EIGHTY_RADIANS);
                    baseTransform.translate(-textBounds.getWidth() - indent, charHeight);
            }

        }
    },
    /**
     * The quadrant when the rotation is > 270 and <= 360.
     */
    QUADRANT_4 {
        @Override
        void updateTransform(final AffineTransform baseTransform, final int indent, final GridLabel.Side side,
                             final int charHeight, final Rectangle2D textBounds) {
            switch (side) {
                case TOP:
                    baseTransform.rotate(Constants.NINETY_RADIANS);
                    baseTransform.translate(indent, charHeight);
                    break;
                case BOTTOM:
                    baseTransform.rotate(Constants.NINETY_RADIANS);
                    baseTransform.translate(-textBounds.getWidth() - indent, charHeight);
                    break;
                case RIGHT:
                    baseTransform.translate(-textBounds.getWidth() - indent, charHeight);
                    break;
                default:
                    baseTransform.translate(indent, charHeight);
            }

        }
    };

    public static final int THREE_SIXTY_DEGREES = 360;

    abstract void updateTransform(AffineTransform baseTransform, int indent, GridLabel.Side side,
                                  int charHeight, Rectangle2D textBounds);

    static RotationQuadrant getQuadrant(final double rotationDegrees) {
        double rotation = rotationDegrees;
        while (rotation > THREE_SIXTY_DEGREES) {
            rotation -= THREE_SIXTY_DEGREES;
        }

        while (rotation < 0) {
            rotation += THREE_SIXTY_DEGREES;
        }

        // CSOFF: MagicNumber
        if (rotation > 0 && rotation <= 90) {
            return QUADRANT_1;
        }
        if (rotation > 90 && rotation <= 180) {
            return QUADRANT_2;
        }
        if (rotation > 180 && rotation <= 270) {
            return QUADRANT_3;
        }
        // CSOFF: MagicNumber
        return QUADRANT_4;
    }

    private static class Constants {
        public static final double MINUS_NINETY_RADIANS = Math.toRadians(-90);
        public static final double ONE_EIGHTY_RADIANS = Math.toRadians(180);
        public static final double NINETY_RADIANS = Math.toRadians(90);
    }
}
