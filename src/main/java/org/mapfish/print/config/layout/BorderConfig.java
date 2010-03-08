/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Server
 *
 * MapFish Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MapFish Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.config.layout;

import org.mapfish.print.PDFUtils;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.InvalidValueException;
import org.mapfish.print.config.ColorWrapper;
import org.mapfish.print.utils.PJsonObject;

import java.awt.*;

/**
 * Bean for configuring a cell's borders.
 */
public class BorderConfig {
    protected Double borderWidthLeft = null;
    protected Double borderWidthRight = null;
    protected Double borderWidthTop = null;
    protected Double borderWidthBottom = null;
    private String borderColorLeft = null;
    private String borderColorRight = null;
    private String borderColorTop = null;
    private String borderColorBottom = null;

    public void setBorderColor(String color) {
        setBorderColorLeft(color);
        setBorderColorRight(color);
        setBorderColorTop(color);
        setBorderColorBottom(color);
    }

    public void setBorderWidth(double border) {
        setBorderWidthLeft(border);
        setBorderWidthRight(border);
        setBorderWidthTop(border);
        setBorderWidthBottom(border);
    }

    public void setBorderWidthLeft(double borderWidthLeft) {
        this.borderWidthLeft = borderWidthLeft;
        if (borderWidthLeft < 0.0) throw new InvalidValueException("borderWidthLeft", borderWidthLeft);
    }

    public void setBorderWidthRight(double borderWidthRight) {
        this.borderWidthRight = borderWidthRight;
        if (borderWidthRight < 0.0) throw new InvalidValueException("borderWidthRight", borderWidthRight);
    }

    public void setBorderWidthTop(double borderWidthTop) {
        this.borderWidthTop = borderWidthTop;
        if (borderWidthTop < 0.0) throw new InvalidValueException("borderWidthTop", borderWidthTop);
    }

    public void setBorderWidthBottom(double borderWidthBottom) {
        this.borderWidthBottom = borderWidthBottom;
        if (borderWidthBottom < 0.0) throw new InvalidValueException("borderWidthBottom", borderWidthBottom);
    }

    public void setBorderColorLeft(String borderColorLeft) {
        this.borderColorLeft = borderColorLeft;
    }

    public void setBorderColorRight(String borderColorRight) {
        this.borderColorRight = borderColorRight;
    }

    public void setBorderColorTop(String borderColorTop) {
        this.borderColorTop = borderColorTop;
    }

    public void setBorderColorBottom(String borderColorBottom) {
        this.borderColorBottom = borderColorBottom;
    }

    public Color getBorderColorLeftVal(RenderingContext context, PJsonObject params) {
        return ColorWrapper.convertColor(PDFUtils.evalString(context, params, borderColorLeft));
    }

    public Color getBorderColorTopVal(RenderingContext context, PJsonObject params) {
        return ColorWrapper.convertColor(PDFUtils.evalString(context, params, borderColorTop));
    }

    public Color getBorderColorRightVal(RenderingContext context, PJsonObject params) {
        return ColorWrapper.convertColor(PDFUtils.evalString(context, params, borderColorRight));
    }

    public Color getBorderColorBottomVal(RenderingContext context, PJsonObject params) {
        return ColorWrapper.convertColor(PDFUtils.evalString(context, params, borderColorBottom));
    }

    public void validate() {
    }
}
