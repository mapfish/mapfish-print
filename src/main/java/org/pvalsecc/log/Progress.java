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
package org.pvalsecc.log;

import org.apache.commons.logging.Log;
import org.pvalsecc.misc.UnitUtilities;

/**
 * Display progress reports using the provided logger. The period between two
 * reports is configurable.
 * <p/>
 * The time remaining is determined from the time it took to process the N
 * things from the previous report. It's averaged from the previous reports
 * with a faster adaptation if it's growing (processing becomming slower).
 * That's being pessimistic.
 */
public class Progress {
    private static final double GROW_FACTOR = 0.7;
    private static final double SHRINK_FACTOR = 0.2;

    private long reportFrequency;
    private int target;
    private String description;
    private Log logger;
    private int cur = 0;

    private long nextReporting;
    private int prevReportValue;
    private double prevPeriodForOne;

    /**
     * @param reportFrequency How often to report in milliseconds
     * @param target          The target number of "things" to do
     * @param description     The message to append
     * @param logger          The logger to use
     */
    public Progress(long reportFrequency, int target, String description, Log logger) {
        this.reportFrequency = reportFrequency;
        this.target = target;
        this.description = description;
        this.logger = logger;

        nextReporting = System.currentTimeMillis() + reportFrequency;
        prevReportValue = 0;
        prevPeriodForOne = 0.0;
    }

    /**
     * To be called each time a "thing" is done.
     */
    public void update(int cur) {
        this.cur = cur;
        if (logger.isInfoEnabled()) {
            long now = System.currentTimeMillis();
            if (now >= nextReporting) {
                doReporting(now, cur);
            }
        }
    }

    private void doReporting(long now, int cur) {
        logger.info(description + " " + (cur * 100L / target) + "% done, approx. " +
                UnitUtilities.toElapsedTime(computeRemaingTime(now, cur)) + " remaining.");
        nextReporting = now + reportFrequency;
        prevReportValue = cur;
    }

    private long computeRemaingTime(long now, int cur) {
        final long timeSinceLastReport = now - (nextReporting - reportFrequency);
        final int nbSinceLastReport = cur - prevReportValue;
        final long nbRemaining = target - cur;
        if (nbSinceLastReport > 0) {
            final double curPeriodForOne = timeSinceLastReport / (double) nbSinceLastReport;
            if (prevPeriodForOne == 0.0) {
                prevPeriodForOne = curPeriodForOne;
            } else if (prevPeriodForOne > curPeriodForOne) {
                prevPeriodForOne = prevPeriodForOne * (1 - SHRINK_FACTOR) + curPeriodForOne * SHRINK_FACTOR;
            } else {
                prevPeriodForOne = prevPeriodForOne * (1 - GROW_FACTOR) + curPeriodForOne * GROW_FACTOR;
            }
        }
        return Math.round(nbRemaining * prevPeriodForOne);
    }

    public int getCur() {
        return cur;
    }
}
