package org.mapfish.print.map.geotools.grid;

import java.awt.Color;
import java.util.List;
import org.geotools.api.style.Graphic;
import org.geotools.api.style.Mark;
import org.geotools.api.style.Style;
import org.geotools.api.style.Symbolizer;
import org.geotools.styling.StyleBuilder;
import org.mapfish.print.map.style.json.ColorParser;

/** Creates the Named LineGridStyle. */
public final class PointGridStyle {

  private static final double CROSS_SIZE = 10.0;

  private PointGridStyle() {
    // do nothing
  }

  /** Create the Grid Point style. */
  static Style get(final GridParam params) {
    final StyleBuilder builder = new StyleBuilder();

    final Symbolizer pointSymbolizer =
        crossSymbolizer("shape://plus", builder, CROSS_SIZE, params.gridColor);
    final Style style = builder.createStyle(pointSymbolizer);
    final List<Symbolizer> symbolizers =
        style.featureTypeStyles().getFirst().rules().getFirst().symbolizers();

    if (params.haloRadius > 0.0) {
      Symbolizer halo =
          crossSymbolizer("cross", builder, CROSS_SIZE + params.haloRadius * 2.0, params.haloColor);
      symbolizers.addFirst(halo);
    }

    return style;
  }

  private static Symbolizer crossSymbolizer(
      final String name,
      final StyleBuilder builder,
      final double crossSize,
      final String pointColorTxt) {
    final Color pointColor = ColorParser.toColor(pointColorTxt);
    final Mark cross = builder.createMark(name, pointColor, pointColor, 1);
    final Graphic graphic = builder.createGraphic(null, cross, null);
    graphic.setSize(builder.literalExpression(crossSize));

    return builder.createPointSymbolizer(graphic);
  }
}
