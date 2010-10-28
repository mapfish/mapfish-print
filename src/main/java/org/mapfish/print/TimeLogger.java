package org.mapfish.print;

import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import static org.apache.log4j.Priority.*;

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
