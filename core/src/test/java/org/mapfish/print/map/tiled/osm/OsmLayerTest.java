package org.mapfish.print.map.tiled.osm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import java.awt.Rectangle;
import java.util.concurrent.ForkJoinPool;
import org.geotools.coverage.grid.GridCoverage2D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.map.Scale;
import org.mapfish.print.map.geotools.StyleSupplier;
import org.mapfish.print.map.tiled.TileInformation;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class OsmLayerTest {
  @Mock private StyleSupplier<GridCoverage2D> styleSupplier;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testCreateTileInformation_CreatesNewTileInformation() {
    // GIVEN
    ForkJoinPool forkJoinPool = new ForkJoinPool();
    OsmLayerParam osmLayerParam = new OsmLayerParam();
    MetricRegistry metricRegistry = mock(MetricRegistry.class);
    Configuration configuration = mock(Configuration.class);

    OsmLayer osmLayer =
        new OsmLayer(forkJoinPool, styleSupplier, osmLayerParam, metricRegistry, configuration);

    MapBounds mockBounds = mock(MapBounds.class);
    Rectangle mockPaintArea = new Rectangle(0, 0, 256, 256);
    double dpi = 96.0;
    osmLayerParam.dpi = 97.0;

    Scale mockScale = mock(Scale.class);
    when(mockScale.getResolution()).thenReturn(0.5);
    when(mockBounds.getScale(mockPaintArea, dpi)).thenReturn(mockScale);

    // WHEN
    TileInformation<OsmLayerParam> tileInfo =
        osmLayer.createTileInformation(mockBounds, mockPaintArea, dpi);

    // THEN
    assertNotNull(tileInfo);
    assertEquals(osmLayerParam.dpi, tileInfo.getLayerDpi());
  }

  @Test
  public void testCreateTileInformation_DifferentScaling() {
    // GIVEN
    ForkJoinPool forkJoinPool = new ForkJoinPool();
    OsmLayerParam osmLayerParam = new OsmLayerParam();
    MetricRegistry metricRegistry = mock(MetricRegistry.class);
    Configuration configuration = mock(Configuration.class);

    OsmLayer osmLayer =
        new OsmLayer(forkJoinPool, styleSupplier, osmLayerParam, metricRegistry, configuration);

    MapBounds mockBounds1 = mock(MapBounds.class);
    Rectangle mockPaintArea1 = new Rectangle(0, 0, 256, 256);
    double dpi1 = 96.0;

    Scale mockScale1 = mock(Scale.class);
    when(mockScale1.getResolution()).thenReturn(0.5);
    when(mockBounds1.getScale(mockPaintArea1, dpi1)).thenReturn(mockScale1);

    // WHEN
    TileInformation<OsmLayerParam> tileInformation1 =
        osmLayer.createTileInformation(mockBounds1, mockPaintArea1, dpi1);

    // GIVEN
    MapBounds mockBounds2 = mock(MapBounds.class);
    Rectangle mockPaintArea2 = new Rectangle(0, 0, 128, 128);
    double dpi2 = 72.0;

    Scale mockScale2 = mock(Scale.class);
    when(mockScale2.getResolution()).thenReturn(0.5);
    when(mockBounds2.getScale(mockPaintArea2, dpi2)).thenReturn(mockScale2);

    // WHEN
    TileInformation<OsmLayerParam> tileInformation2 =
        osmLayer.createTileInformation(mockBounds2, mockPaintArea2, dpi2);

    // THEN
    assertNotEquals(tileInformation1, tileInformation2);
  }
}
