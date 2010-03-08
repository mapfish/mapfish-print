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

import com.lowagie.text.DocumentException;
import org.mapfish.print.InvalidValueException;
import org.mapfish.print.PDFUtils;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.config.ColorWrapper;
import org.mapfish.print.utils.PJsonObject;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for blocks that can be found in "items" arrays.
 */
public abstract class Block {
    protected HorizontalAlign align = null;
    private VerticalAlign vertAlign = null;
    private String backgroundColor = null;
    private String condition = null;
    protected double spacingAfter = 0.0;

    public Block() {

    }

    /**
     * Called when the block is rendered.
     */
    public abstract void render(PJsonObject params, PdfElement target, RenderingContext context) throws DocumentException;

    public MapBlock getMap() {
        return null;
    }

    /**
     * Called just after the config has been loaded to check it is valid.
     *
     * @throws InvalidValueException When there is a problem
     */
    public void validate() {
    }

    public boolean isAbsolute() {
        return false;
    }

    public interface PdfElement {
        void add(com.lowagie.text.Element element) throws DocumentException;
    }

    public void setAlign(HorizontalAlign align) {
        this.align = align;
    }

    public void setVertAlign(VerticalAlign vertAlign) {
        this.vertAlign = vertAlign;
    }

    public Color getBackgroundColorVal(RenderingContext context, PJsonObject params) {
        return ColorWrapper.convertColor(PDFUtils.evalString(context, params, backgroundColor));
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public HorizontalAlign getAlign() {
        return align;
    }

    public VerticalAlign getVertAlign() {
        return vertAlign;
    }

    private static final Pattern CONDITION_REGEXP = Pattern.compile("^(!?)(.*)$");

    public boolean isVisible(RenderingContext context, PJsonObject params) {
        if (condition == null) {
            return true;
        }

        Matcher matcher = CONDITION_REGEXP.matcher(condition);
        if (!matcher.matches()) {
            throw new InvalidValueException("condition", condition);
        }

        String value = params.optString(condition);
        if (value == null) {
            value = context.getGlobalParams().optString(matcher.group(2));
        }
        boolean result = value != null && value.length() > 0 && !value.equals("0") && !value.equalsIgnoreCase("false");
        if (matcher.group(1).equals("!")) {
            result = !result;
        }
        return result;
    }

    public void setCondition(String condition) {
        this.condition = condition;
        if (condition != null && !CONDITION_REGEXP.matcher(condition).matches()) {
            throw new InvalidValueException("condition", condition);
        }
    }

    public void setSpacingAfter(double spacingAfter) {
        this.spacingAfter = spacingAfter;
        if (spacingAfter < 0.0) throw new InvalidValueException("spacingAfter", spacingAfter);
    }
}
