package org.mapfish.print.output;

import java.util.List;

public interface OutputFormatFactory {
    List<String> formats();
    OutputFormat create(String format);
    String enablementStatus();
}
