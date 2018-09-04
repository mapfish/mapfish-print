package org.mapfish.print.processor;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.sf.jasperreports.engine.PrintPageFormat;
import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.attribute.map.MapfishMapContext;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 * Statisctics about the execution of a print job.
 */
public class ExecutionStats {
    private List<MapStats> mapStats = new ArrayList<>();
    private List<PageStats> pageStats = new ArrayList<>();

    /**
     * Add statistics about a created map.
     *
     * @param mapContext the
     * @param mapValues the
     */
    public synchronized void addMapStats(
            final MapfishMapContext mapContext, final MapAttribute.MapAttributeValues mapValues) {
        this.mapStats.add(new MapStats(mapContext, mapValues));
    }

    /**
     * Add statistics about a generated page.
     *
     * @param pageFormat Page format info from Jasper
     */
    public void addPageStats(final PrintPageFormat pageFormat) {
        this.pageStats.add(new PageStats(pageFormat));
    }

    /**
     * @return a JSON report about the collected statistics.
     */
    public ObjectNode toJson() {
        final JsonNodeFactory nc = JsonNodeFactory.instance;
        ObjectNode stats = new ObjectNode(nc);
        final ArrayNode maps = stats.putArray("maps");
        for (ExecutionStats.MapStats map: this.mapStats) {
            map.toJson(maps.addObject());
        }
        final ArrayNode pages = stats.putArray("pages");
        for (PageStats pageStat: this.pageStats) {
            pageStat.toJson(pages.addObject());
        }

        return stats;
    }

    private static final class MapStats {
        private final double dpi;
        private final Dimension size;
        private final int nbLayers;

        private MapStats(
                final MapfishMapContext mapContext, final MapAttribute.MapAttributeValues mapValues) {
            this.dpi = mapContext.getDPI();
            this.size = mapValues.getMapSize();
            this.nbLayers = mapValues.layers.size();
        }

        public void toJson(final ObjectNode target) {
            target.put("dpi", this.dpi)
                    .put("nbLayers", this.nbLayers)
                    .putObject("size")
                    .put("width", this.size.width)
                    .put("height", this.size.height);
        }
    }

    private static final class PageStats {
        private final PrintPageFormat format;

        private PageStats(final PrintPageFormat format) {
            this.format = format;
        }

        public void toJson(final ObjectNode target) {
            target.put("width", this.format.getPageWidth());
            target.put("height", this.format.getPageHeight());
        }
    }
}
