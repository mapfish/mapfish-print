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

package org.mapfish.print;

import static org.apache.log4j.Priority.DEBUG_INT;
import static org.apache.log4j.Priority.ERROR_INT;
import static org.apache.log4j.Priority.FATAL_INT;
import static org.apache.log4j.Priority.INFO_INT;
import static org.apache.log4j.Priority.WARN_INT;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.log4j.Logger;

/**
 * User: jeichar
 * Date: Oct 19, 2010
 * Time: 1:46:30 PM
 */
public class TimeLogger {
    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

    private final Logger logger;
    private final int level;
    private final String task;
    private long start;

  private static String now() {
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
    return sdf.format(cal.getTime());

  }
    private TimeLogger(Logger logger, int level, String task) {
        this.logger = logger;
        this.level = level;
        this.task = task;
        log("Starting '" + task + "' at "+now() );
        start = System.currentTimeMillis();
    }

    private void log(String s) {
        switch (level) {
            case DEBUG_INT:
                logger.debug(s);
                break;
            case ERROR_INT:
                logger.error(s);
                break;
            case INFO_INT:
                logger.info(s);
                break;
            case FATAL_INT:
                logger.fatal(s);
                break;
            case WARN_INT:
                logger.warn(s);
                break;
            default:
                logger.info(s);
        }

    }


    public void done() {
        long time = System.currentTimeMillis() - start;
        log("Finished '"+task + "' after " + time + " ms");
    }

    public static TimeLogger info(Logger logger, String task) {
        return new TimeLogger(logger, INFO_INT, task);
    }
    public static TimeLogger debug(Logger logger, String task) {
        return new TimeLogger(logger, DEBUG_INT, task);
    }
}
