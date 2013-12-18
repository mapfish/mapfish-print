package org.mapfish.print.map.readers;

import java.util.List;

import org.mapfish.print.RenderingContext;
import org.mapfish.print.utils.PJsonObject;

public interface MapReaderFactory {
    List<? extends MapReader> create(String type, RenderingContext context, PJsonObject params);
}
