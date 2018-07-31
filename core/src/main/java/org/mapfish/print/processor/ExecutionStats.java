package org.mapfish.print.processor;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    /**
     * Add statistics about a created map.
     *
     * @param mapContext the
     * @param mapValues the
     */
    public synchronized void addMapStats(final MapfishMapContext mapContext, final MapAttribute.MapAttributeValues mapValues) {
        this.mapStats.add(new MapStats(mapContext, mapValues));
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
        return stats;
    }

    private static final class MapStats {
        private final double dpi;
        private final Dimension size;
        private final int nbLayers;

        private MapStats(final MapfishMapContext mapContext, final MapAttribute.MapAttributeValues mapValues) {
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
}
