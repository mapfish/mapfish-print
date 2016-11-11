package org.mapfish.print;

import org.mapfish.print.servlet.NoSuchAppException;

import java.util.Set;

/**
 * Interface for a class that creates MapPrinters.
 */
public interface MapPrinterFactory {
    /**
     * Creates the appropriate map printer.
     *
     * @param app an identifier that controls which configuration to use.
     */
    MapPrinter create(String app) throws NoSuchAppException;

    /**
     * Return the set of app ids that are available.
     */
    Set<String> getAppIds();
}
