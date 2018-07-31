package org.mapfish.print.cli;

import com.sampullara.cli.Argument;

/**
 * The Cli definition for when the user wants to print the cli usage/options.
 */
public class CliHelpDefinition {
    /**
     * Print all the commandline options.
     */
    @Argument(description = "Print all the commandline options.", alias = "?")
    public boolean help = false;

    CliHelpDefinition() {
        // this is intentionally empty
    }
}
