package org.mapfish.print.config;

import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointPlacement;
import org.geotools.styling.Style;
import org.geotools.styling.TextSymbolizer;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.Constants;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Jesse on 7/7/2015.
 */
public class ConfigurationIntegrationTest extends AbstractMapfishSpringTest {
    @Autowired
    ConfigurationFactory factory;

    @Test
    public void testGridStyle() throws Exception {
        final Configuration configuration = factory.getConfig(getFile("/org/mapfish/print/http/configuration/config.yaml"));
        final Style gridStyle = configuration.getDefaultStyle(Constants.Style.Grid.NAME_LINES);
        final AtomicInteger foundLineSymb = new AtomicInteger(0);
        final AtomicInteger foundTextSymb = new AtomicInteger(0);

        final AbstractStyleVisitor styleValidator = new AbstractStyleVisitor() {
            @Override
            public void visit(LineSymbolizer line) {
                foundLineSymb.incrementAndGet();
                super.visit(line);
            }

            @Override
            public void visit(TextSymbolizer text) {
                foundTextSymb.incrementAndGet();
                final PointPlacement labelPlacement = (PointPlacement) text.getLabelPlacement();
                assertNotNull(labelPlacement.getDisplacement());
                super.visit(text);
            }
        };

        styleValidator.visit(gridStyle);

        assertEquals(1, foundLineSymb.intValue());
        assertEquals(1, foundTextSymb.intValue());
    }
}
