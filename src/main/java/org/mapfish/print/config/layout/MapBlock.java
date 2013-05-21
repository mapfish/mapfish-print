/*
 * Copyright (C) 2013  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.config.layout;

import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.print.InvalidJsonValueException;
import org.mapfish.print.InvalidValueException;
import org.mapfish.print.PDFCustomBlocks;
import org.mapfish.print.PDFUtils;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.Transformer;
import org.mapfish.print.map.MapChunkDrawer;
import org.mapfish.print.utils.DistanceUnit;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;

/**
 * Bean to configure the !map blocks.
 * <p/>
 * See http://trac.mapfish.org/trac/mapfish/wiki/PrintModuleServer#Mapblock
 */
public class MapBlock extends Block {
    private String height = null;
    private String width = null;
    private String absoluteX = null;
    private String absoluteY = null;
    private double overviewMap = Double.NaN;

    /**
     * Name given in the PDF layer.
     */
    private String name = null;

    public void render(PJsonObject params, PdfElement target, RenderingContext context) throws DocumentException {
        final Transformer transformer = createTransformer(context, params);

        final MapChunkDrawer drawer = new MapChunkDrawer(context.getCustomBlocks(), transformer, overviewMap, params, context, getBackgroundColorVal(context, params), name);

        if (isAbsolute()) {
            final float absX = getAbsoluteX(context, params);
            final float absY = getAbsoluteY(context, params);
            context.getCustomBlocks().addAbsoluteDrawer(new PDFCustomBlocks.AbsoluteDrawer() {
                public void render(PdfContentByte dc) {
                    final Rectangle rectangle = new Rectangle(absX, absY - transformer.getPaperH(),
                            absX + transformer.getPaperW(), absY);
                    drawer.render(rectangle, dc);
                }
            });
        } else {
            target.add(PDFUtils.createPlaceholderTable(transformer.getPaperW(), transformer.getPaperH(), spacingAfter, drawer, align, context.getCustomBlocks()));
        }
    }

    /**
     * Creates the transformer in function of the JSON parameters and the block's config
     */
    public Transformer createTransformer(RenderingContext context, PJsonObject params) {
        Integer dpi = params.optInt("dpi");
        if (dpi == null) {
            dpi = context.getGlobalParams().getInt("dpi");
        }
        if (!context.getConfig().getDpis().contains(dpi)) {
            throw new InvalidJsonValueException(params, "dpi", dpi);
        }

        String units = context.getGlobalParams().getString("units");
        final DistanceUnit unitEnum = DistanceUnit.fromString(units);
        if (unitEnum == null) {
            throw new RuntimeException("Unknown unit: '" + units + "'");
        }

        final int scale;
        final float centerX;
        final float centerY;

        final float width = getWidth(context, params);
        final float height = getHeight(context, params);
        final PJsonArray center = params.optJSONArray("center");
        if (center != null) {
            //normal mode
            scale = params.getInt("scale");
            centerX = center.getFloat(0);
            centerY = center.getFloat(1);
        } else {
            //bbox mode
            PJsonArray bbox = params.getJSONArray("bbox");
            float minX = bbox.getFloat(0);
            float minY = bbox.getFloat(1);
            float maxX = bbox.getFloat(2);
            float maxY = bbox.getFloat(3);

            if (minX >= maxX) {
                throw new InvalidValueException("maxX", maxX);
            }
            if (minY >= maxY) {
                throw new InvalidValueException("maxY", maxY);
            }

            centerX = (minX + maxX) / 2.0F;
            centerY = (minY + maxY) / 2.0F;
            
            double rotation = params.optDouble("rotation", 0.0);
            rotation *= Math.PI / 180;
            float projWidth  = (maxX - minX) * (float)Math.abs(Math.cos(rotation)) +
                               (maxY - minY) * (float)Math.abs(Math.sin(rotation));
            float projHeight = (maxY - minY) * (float)Math.abs(Math.cos(rotation)) +
                               (maxX - minX) * (float)Math.abs(Math.sin(rotation));
            scale = context.getConfig().getBestScale(Math.max(
                    projWidth  / (DistanceUnit.PT.convertTo(width, unitEnum)),
                    projHeight / (DistanceUnit.PT.convertTo(height, unitEnum))));
            // if the rotation is 0:
            // scale = context.getConfig().getBestScale(Math.max(
            //         (maxX - minX) / (DistanceUnit.PT.convertTo(width, unitEnum)),
            //         (maxY - minY) / (DistanceUnit.PT.convertTo(height, unitEnum))));
        }

        if (!context.getConfig().isDisableScaleLocking() && !context.getConfig().isScalePresent(scale)) {
            throw new InvalidJsonValueException(params, "scale", scale);
        }

        String srs = null;
        if (params.optBool("geodetic", false)
                || context.getGlobalParams().optBool("geodetic", false)) {
            srs = params.optString("srs");
            if (srs == null) {
                srs = context.getGlobalParams().optString("srs");
            }
            if (srs == null) {
                throw new RuntimeException(
                        "When geodetic is true the srs is value is required");
            }
        }
        double rotation = params.optFloat("rotation", 0.0F) * Math.PI / 180.0;
        return new Transformer(centerX, centerY, width, height, scale, dpi,
                unitEnum, rotation, srs, context.getConfig().getIntegerSvg());
    }

    public void setHeight(String height) {
        //this.height = Integer.toString(height);a
        this.height = height;
    }

    public float getHeight(RenderingContext context, PJsonObject params) {
        return Float.parseFloat(PDFUtils.evalString(context, params, height));
    }

    public void setWidth(String width) {
        //this.width = Integer.toString(width);
        this.width = width;
    }

    public float getWidth(RenderingContext context, PJsonObject params) {
        return Float.parseFloat(PDFUtils.evalString(context, params, width));
    }

    public boolean isAbsolute() {
        return absoluteX != null &&
                absoluteY != null;
    }

    public void setAbsoluteX(String absoluteX) {
        this.absoluteX = absoluteX;
    }

    public float getAbsoluteX(RenderingContext context, PJsonObject params) {
        //return Integer.parseInt(PDFUtils.evalString(context, params, absoluteX));
      return Float.parseFloat(PDFUtils.evalString(context, params, absoluteX));
    }

    public void setAbsoluteY(String absoluteY) {
        this.absoluteY = absoluteY;
    }

    public float getAbsoluteY(RenderingContext context, PJsonObject params) {
        return Float.parseFloat(PDFUtils.evalString(context, params, absoluteY));
    }

    public MapBlock getMap() {
        return Double.isNaN(overviewMap) ? this : null;
    }

    public void printClientConfig(JSONWriter json) throws JSONException {
        /**
         * Changed width and height to be double from int to accomodate double values in yaml config
         */
        json.object();
        double w; //int w;
        try {
        	w = Math.round(Double.parseDouble(width)); //w = Integer.parseInt(width);
        } catch (NumberFormatException e) {
            w = 0;
        }
        json.key("width").value(w);

        double h; //int h;
        try {
        	h = Math.round(Double.parseDouble(height)); //h = Integer.parseInt(height);
        } catch (NumberFormatException e) {
            h = 0;
        }
        json.key("height").value(h);
        json.endObject();
    }

    public void setOverviewMap(double overviewMap) {
        this.overviewMap = overviewMap;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void validate() {
        super.validate();
        if (absoluteX != null ^ absoluteY != null) {
            if (absoluteX == null) {
                throw new InvalidValueException("absoluteX", "null");
            } else {
                throw new InvalidValueException("absoluteY", "null");
            }
        }

        if (width == null) {
            throw new InvalidValueException("width", null);
        }

        if (height == null) {
            throw new InvalidValueException("width", null);
        }
    }
}
