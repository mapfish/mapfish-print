package org.mapfish.print.map.geotools;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Stroke;
import org.mapfish.print.map.geotools.function.MultiplicationFunction;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;

/**
 * Visits all elements in the style an multiplies the opacity of each element (where opacity applies) by
 * the opacity factory passed in.
 *
 * @author Jesse on 11/7/2014.
 */
public class OpacitySettingStyleVisitor extends AbstractStyleVisitor {
    private final Expression opacityFactor;
    private final FilterFactory2 filterFactory;

    /**
     * Constructor.
     *
     * @param opacityFactor a value between 0 and 1 to multiply against any existing opacity.
     */
    public OpacitySettingStyleVisitor(final double opacityFactor) {
        this.filterFactory = CommonFactoryFinder.getFilterFactory2();
        this.opacityFactor = this.filterFactory.literal(opacityFactor);
    }

    @Override
    public void visit(Fill fill) {
        final Expression opacity = fill.getOpacity();
        final Function newExpr = getOpacityAdjustingExpression(opacity);
        fill.setOpacity(newExpr);
    }

    @Override
    public void visit(Stroke stroke) {
        final Expression opacity = stroke.getOpacity();
        final Function newExpr = getOpacityAdjustingExpression(opacity);
        stroke.setOpacity(newExpr);
    }

    @Override
    public void visit(Graphic gr) {
        final Expression opacity = gr.getOpacity();
        final Function newExpr = getOpacityAdjustingExpression(opacity);
        gr.setOpacity(newExpr);
    }

    @Override
    public void visit(RasterSymbolizer raster) {
        final Expression opacity = raster.getOpacity();
        final Function newExpr = getOpacityAdjustingExpression(opacity);
        raster.setOpacity(newExpr);
    }

    private Function getOpacityAdjustingExpression(Expression opacity) {
        return this.filterFactory.function(MultiplicationFunction.NAME.getName(), opacity, opacityFactor);
    }

}

