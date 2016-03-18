package org.mapfish.print.cli;

import com.sampullara.cli.Argument;

/**
 * The Cli definition for when the user wants to print the cli usage/options.
 */
public class CliHelpDefinition {
    CliHelpDefinition() {
        // this is intentionally empty
    }

    // CHECKSTYLE:OFF
    @Argument(description = "Print all the commandline options.", alias = "?")
    public boolean help = false;
}
