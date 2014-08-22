package org.mapfish.print.servlet;

import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mapfish.print.servlet.MapPrinterServlet.TempFile.cleanUpName;

public class MapPrinterServletTest {

    @Test
    public void TempFileFormatFileNameTest() {
        Date date = new Date();

        String dateString = cleanUpName(DateFormat.getDateInstance().format(date));
        String dateTimeString = cleanUpName(DateFormat.getDateTimeInstance().format(date));
        String timeString = cleanUpName(DateFormat.getTimeInstance().format(date));
        String customPattern = "yy-MM-dd";
        String customPattern2 = "yy:MM:dd:mm:ss";
        String custom = new SimpleDateFormat(customPattern).format(date);
        String custom2 = new SimpleDateFormat(customPattern2).format(date);

        assertExpectedFormat(date, "|${else}|", "|${else}|", "");
        assertExpectedFormat(date, "|"+dateString+"|", "|${date}|", "");
        assertExpectedFormat(date, "|"+dateTimeString+"|", "|${dateTime}|", "");
        assertExpectedFormat(date, "|"+timeString+"|", "|${time}|", "");
        assertExpectedFormat(date, "|"+custom+"|", "|${"+customPattern+"}|", "");
        assertExpectedFormat(date, "|"+custom2+"|", "|${"+customPattern2+"}|", "");
        assertExpectedFormat(date, "|"+timeString+"|"+dateString, "|${time}|${date}", "");
        assertExpectedFormat(date, "|"+timeString+"|"+dateTimeString, "|${time}|${dateTime}", "");
        assertExpectedFormat(date, "|"+timeString+"|"+dateTimeString+"|"+timeString, "|${time}|${dateTime}|${time}", "");
        assertExpectedFormat(date, "|"+custom+"|"+custom2+"|"+timeString, "|${"+customPattern+"}|${"+customPattern2+"}|${time}", "");
    }

    private void assertExpectedFormat(Date date, String expected, String fileName, String suffix) {
        String actual = MapPrinterServlet.TempFile.formatFileName(fileName, date);
        if (!suffix.isEmpty()) {
            actual += "." + suffix;
        }
        assertEquals(expected, actual);
    }

}
