package org.mapfish.print.servlet;

import static org.junit.Assert.*;

import java.text.DateFormat;
import java.util.Date;

import org.junit.Test;

public class MapPrinterServletTest {

    @Test
    public void TempFileFormatFileNameTest() {
        Date date = new Date();

        String dateString = DateFormat.getDateInstance().format(date);
        String dateTimeString = DateFormat.getDateTimeInstance().format(date);
        String timeString = DateFormat.getTimeInstance().format(date);
        
        assertExpectedFormat(date, "|${else}|", "|${else}|");
        assertExpectedFormat(date, "|"+dateString+"|", "|${date}|");
        assertExpectedFormat(date, "|"+dateTimeString+"|", "|${dateTime}|");
        assertExpectedFormat(date, "|"+timeString+"|", "|${time}|");
        assertExpectedFormat(date, "|"+timeString+"|"+dateString, "|${time}|${date}");
        assertExpectedFormat(date, "|"+timeString+"|"+dateTimeString, "|${time}|${dateTime}");
        assertExpectedFormat(date, "|"+timeString+"|"+dateTimeString+"|"+timeString, "|${time}|${dateTime}|${time}");
    }

    private void assertExpectedFormat(Date date, String expected, String fileName) {
        assertEquals(expected, MapPrinterServlet.TempFile.formatFileName(fileName,date));
    }

}
