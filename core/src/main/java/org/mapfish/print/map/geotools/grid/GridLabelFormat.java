package org.mapfish.print.map.geotools.grid;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Formats grid labels.
 */
public abstract class GridLabelFormat {

    /**
     * Create an instance from the given config.
     *
     * @param param Grid param from the request.
     */
    public static GridLabelFormat fromConfig(final GridParam param) {
        if (param.labelFormat != null) {
            return new GridLabelFormat.Simple(param.labelFormat);
        } else if (param.valueFormat != null) {
            return new GridLabelFormat.Detailed(
                    param.valueFormat, param.unitFormat,
                    param.formatDecimalSeparator, param.formatGroupingSeparator);
        }
        return null;
    }

    /**
     * Format a label using the given value and unit.
     *
     * @param value Value.
     * @param unit Unit.
     */
    public abstract String format(double value, String unit);

    /**
     * Label format where value and unit are formatted at once.
     */
    public static class Simple extends GridLabelFormat {
        private String labelFormat = null;

        /**
         * Constructor.
         *
         * @param labelFormat Label format.
         */
        public Simple(final String labelFormat) {
            this.labelFormat = labelFormat;
        }

        @Override
        public final String format(final double value, final String unit) {
            return String.format(this.labelFormat, value, unit);
        }
    }

    /**
     * Label format where value and unit are formatted with different patterns.
     */
    public static class Detailed extends GridLabelFormat {
        private String valueFormat;
        private String unitFormat;
        private String formatDecimalSeparator;
        private String formatGroupingSeparator;

        /**
         * Constructor.
         *
         * @param valueFormat Value format.
         * @param unitFormat Unit format.
         * @param formatDecimalSeparator Decimal separator.
         * @param formatGroupingSeparator Grouping separator.
         */
        public Detailed(
                final String valueFormat, final String unitFormat,
                final String formatDecimalSeparator, final String formatGroupingSeparator) {
            this.valueFormat = valueFormat;
            this.unitFormat = (unitFormat == null) ? GridParam.DEFAULT_UNIT_FORMAT : unitFormat;
            this.formatDecimalSeparator = formatDecimalSeparator;
            this.formatGroupingSeparator = formatGroupingSeparator;
        }

        @Override
        public final String format(final double value, final String unit) {
            DecimalFormat decimalFormat = null;

            if (this.formatDecimalSeparator != null || this.formatGroupingSeparator != null) {
                // if custom separator characters are given, use them to create the format
                DecimalFormatSymbols symbols = new DecimalFormatSymbols();
                if (this.formatDecimalSeparator != null) {
                    symbols.setDecimalSeparator(this.formatDecimalSeparator.charAt(0));
                }
                if (this.formatGroupingSeparator != null) {
                    symbols.setGroupingSeparator(this.formatGroupingSeparator.charAt(0));
                }
                decimalFormat = new DecimalFormat(this.valueFormat, symbols);
            } else {
                decimalFormat = new DecimalFormat(this.valueFormat);
            }

            return decimalFormat.format(value) + String.format(this.unitFormat, unit);
        }
    }
}
