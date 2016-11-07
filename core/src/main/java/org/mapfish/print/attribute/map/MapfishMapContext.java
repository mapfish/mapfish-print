package org.mapfish.print.attribute.map;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.mapfish.print.FloatingPointUtil;
import org.mapfish.print.map.Scale;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Utility class that adjusts the bounds and the map size in case a rotation
 * is set. Also it provides an {@link AffineTransform} to render the layer graphics.
 */
public final class MapfishMapContext {

    private final MapBounds bounds;
    private final Dimension mapSize;
    private final double rotation;
    private final double dpi;
    private final double requestorDpi;
    private final boolean forceLongitudeFirst;
    private final boolean dpiSensitiveStyle;
    private final MapfishMapContext parent;

    /**
     * Constructor.
     * @param bounds the map bounds
     * @param mapSize the map size
     * @param rotationInDegree the rotation in degree
     * @param dpi the dpi of the printed map
     * @param requestorDpi the dpi of the client map
     * @param forceLongitudeFirst If true then force longitude coordinates as the first coordinate.
     * @param dpiSensitiveStyle Scale the vector styles?
     */
    public MapfishMapContext(final MapBounds bounds, final Dimension mapSize, final double rotationInDegree, final double dpi,
                             final double requestorDpi, final Boolean forceLongitudeFirst, final boolean dpiSensitiveStyle) {
        this(null, bounds, mapSize, rotationInDegree, dpi, requestorDpi, forceLongitudeFirst, dpiSensitiveStyle);
    }

    /**
     * Constructor.
     * @param parent the context that this context is derived from
     * @param bounds the map bounds
     * @param mapSize the map size
     * @param rotationInDegree the rotation in degree
     * @param dpi the dpi of the printed map
     * @param requestorDpi the dpi of the client map
     * @param forceLongitudeFirst If true then force longitude coordinates as the first coordinate.
     * @param dpiSensitiveStyle Scale the vector styles?
     */
    // CSOFF: ParameterNumber
    public MapfishMapContext(final MapfishMapContext parent, final MapBounds bounds, final Dimension mapSize,
                             final double rotationInDegree, final double dpi, final double requestorDpi,
                             final Boolean forceLongitudeFirst, final boolean dpiSensitiveStyle) {
        this(parent, bounds, mapSize, rotationInDegree, true, dpi, requestorDpi, forceLongitudeFirst, dpiSensitiveStyle);
    }

    /**
     * Constructor.
     * @param parent the context that this context is derived from
     * @param bounds the map bounds
     * @param mapSize the map size
     * @param rotation the rotation
     * @param rotationInDegree the rotation in degree?
     * @param dpi the dpi of the printed map
     * @param requestorDpi the dpi of the client map
     * @param forceLongitudeFirst If true then force longitude coordinates as the first coordinate.
     * @param dpiSensitiveStyle Scale the vector styles?
     */
    // CSOFF: ParameterNumber
    public MapfishMapContext(final MapfishMapContext parent, final MapBounds bounds, final Dimension mapSize,
                             final double rotation, final boolean rotationInDegree, final double dpi, final double requestorDpi,
                             final Boolean forceLongitudeFirst, final boolean dpiSensitiveStyle) {
        // CSON: ParameterNumber
        this.parent = parent;
        this.bounds = bounds;
        this.mapSize = mapSize;
        this.rotation = rotationInDegree ? Math.toRadians(rotation) : rotation;
        this.dpi = dpi;
        this.requestorDpi = requestorDpi;
        this.forceLongitudeFirst = forceLongitudeFirst == null ? false : forceLongitudeFirst;
        this.dpiSensitiveStyle = dpiSensitiveStyle;
    }

    /**
     * @return The rotation in radians.
     */
    public double getRotation() {
        return this.rotation;
    }

    public MapBounds getBounds() {
        return this.bounds;
    }

    /**
     * Return the map bounds rotated with the set rotation.
     * @return Rotated bounds.
     */
    public MapBounds getRotatedBounds() {
        return this.bounds.adjustBoundsToRotation(this.rotation);
    }

    /**
     * Return the map bounds rotated with the set rotation. The bounds are adapted
     * to rounding changes of the size of the paint area.
     * @param paintAreaPrecise The exact size of the paint area.
     * @param paintArea The rounded size of the paint area.
     * @return Rotated bounds.
     */
    public MapBounds getRotatedBounds(final Rectangle2D.Double paintAreaPrecise, final Rectangle paintArea) {
        final MapBounds rotatedBounds = this.getRotatedBounds();

        if (rotatedBounds instanceof CenterScaleMapBounds) {
            return rotatedBounds;
        }

        final ReferencedEnvelope envelope = ((BBoxMapBounds) rotatedBounds).toReferencedEnvelope(null, -1);
        // the paint area size and the map bounds are rotated independently. because
        // the paint area size is rounded to integers, the map bounds have to be adjusted
        // to these rounding changes.
        final double widthRatio = paintArea.getWidth() / paintAreaPrecise.getWidth();
        final double heightRatio = paintArea.getHeight() / paintAreaPrecise.getHeight();

        final double adaptedWidth = envelope.getWidth() * widthRatio;
        final double adaptedHeight = envelope.getHeight() * heightRatio;

        final double widthDiff = adaptedWidth - envelope.getWidth();
        final double heigthDiff = adaptedHeight - envelope.getHeight();
        envelope.expandBy(widthDiff / 2.0, heigthDiff / 2.0);

        return new BBoxMapBounds(envelope);
    }

    public Dimension getMapSize() {
        return this.mapSize;
    }

    public Scale getScale() {
        return this.bounds.getScaleDenominator(getPaintArea(), this.dpi);
    }
    public Scale getGeodeticScale() {
        return this.bounds.getGeodeticScaleDenominator(getPaintArea(), this.dpi);
    }

    /**
     * Get a nicely rounded scale for to use for displaying the map scale.
     * <p>
     *     One of the output parameters of the {@link org.mapfish.print.processor.map.CreateMapProcessor} is 'mapContext' which can
     *     be accessed in a template.  If the scale is required in the template then it can be accessed via:
     *     <code>$P{mapContext}.getRoundedScale()</code>
     * </p>
     */
    public double getRoundedScale() {
        return getRoundedScale(false);
    }

    /**
     * Get a nicely rounded scale for to use for displaying the map scale.
     * <p>
     *     One of the output parameters of the {@link org.mapfish.print.processor.map.CreateMapProcessor} is 'mapContext' which can
     *     be accessed in a template.  If the scale is required in the template then it can be accessed via:
     *     <code>$P{mapContext}.getRoundedScale()</code>
     * </p>
     *
     * @param geodetic Get geodetic scale
     */
    public double getRoundedScale(final boolean geodetic) {
        double scale;
        if (geodetic) {
            scale = this.bounds.getGeodeticScaleDenominator(getPaintArea(), this.dpi).getDenominator();
        } else {
            scale = this.bounds.getScaleDenominator(getPaintArea(), this.dpi).getDenominator();
        }

        final int numChars = String.format("%d", Math.round(scale)).length();
        if (numChars > 2) {
            // CSOFF: MagicNumber
            double factor = Math.pow(10, (numChars - 2));
            // CSON: MagicNumber
            scale = Math.round(scale / factor) * factor;
        } else if (scale > 1) {
            scale = Math.round(scale);
        }

        return scale;
    }

    /**
     * @return The new map size taking the rotation into account.
     */
    public Dimension getRotatedMapSize() {
        return MapfishMapContext.rectangleDoubleToDimension(getRotatedMapSizePrecise());
    }

    /**
     * @return The new map size taking the rotation into account.
     */
    public Rectangle2D.Double getRotatedMapSizePrecise() {
        if (FloatingPointUtil.equals(this.rotation, 0.0)) {
            return new Rectangle2D.Double(0, 0, this.mapSize.getWidth(), this.mapSize.getHeight());
        }

        final double rotatedWidth = getRotatedMapWidth();
        final double rotatedHeight = getRotatedMapHeight();
        return new Rectangle2D.Double(0, 0, rotatedWidth, rotatedHeight);
    }


    /**
     * Round the size of a rectangle with double values.
     * @param rectangle The rectangle.
     * @return
     */
    public static Dimension rectangleDoubleToDimension(final Rectangle2D.Double rectangle) {
        return new Dimension(
                (int) Math.round(rectangle.width),
                (int) Math.round(rectangle.height));
    }

    /**
     * Returns an {@link AffineTransform} taking the rotation into account.
     * 
     * @return an affine transformation
     */
    public AffineTransform getTransform() {
        if (FloatingPointUtil.equals(this.rotation, 0.0)) {
            return null;
        }
        
        final Dimension rotatedMapSize = getRotatedMapSize();
        
        final AffineTransform transform = AffineTransform.getTranslateInstance(0.0, 0.0);
        // move to the center of the original map rectangle (this is the actual 
        // size of the graphic)
        transform.translate(this.mapSize.width / 2, this.mapSize.height / 2);
        
        // then rotate around this center
        transform.rotate(this.rotation);
        
        // then move to an artificial origin (0,0) which might be outside of the actual
        // painting area. this origin still keeps the center of the original map area
        // at the center of the rotated map area.
        transform.translate(-rotatedMapSize.width / 2, -rotatedMapSize.height / 2);

        return transform;
    }

    public double getDPI() {
        return this.dpi;
    }

    public double getRequestorDPI() {
        return this.requestorDpi;
    }

    private double getRotatedMapWidth() {
        double width = this.mapSize.getWidth();
        if (!FloatingPointUtil.equals(this.rotation, 0.0)) {
            double height = this.mapSize.getHeight();
            width = Math.abs(width * Math.cos(this.rotation))
                    + Math.abs(height * Math.sin(this.rotation));
        }
        return width;
    }

    private double getRotatedMapHeight() {
        double height = this.mapSize.getHeight();
        if (!FloatingPointUtil.equals(this.rotation, 0.0)) {
            double width = this.mapSize.getWidth();
            height = Math.abs(height * Math.cos(this.rotation))
                     + Math.abs(width * Math.sin(this.rotation));
        }
        return height;
    }

    public Rectangle getPaintArea() {
        return new Rectangle(this.mapSize);
    }

    @Nullable
    public Boolean isForceLongitudeFirst() {
        return this.forceLongitudeFirst;
    }

    public Boolean isDpiSensitiveStyle() {
        return this.dpiSensitiveStyle;
    }

    /**
     * Get the bounds as a referenced envelope.
     *
     * @return bounds as a referenced envelope.
     */
    public ReferencedEnvelope toReferencedEnvelope() {
        return this.bounds.toReferencedEnvelope(getPaintArea(), this.dpi);
    }

    /**
     * Get the parent context if there is one.  A parent context is the context that this context is derived from.
     * Normally there are some parameters that have been changed for this context from the parent.  An example of when
     * there might be a parent is when the child has been rotated and has a bounds to envelope the original bounds.  It can be
     * useful in some cases to be able to access the parent (and original bounds).
     *
     * @return the parent context or null if there is no parent.
     */
    @Nullable
    public MapfishMapContext getParentContext() {
        return this.parent;
    }

    /**
     * Return the root context which is this context or the context found by recursively calling parent.getRootContext().
     * @return
     */
    @Nonnull
    public MapfishMapContext getRootContext() {
        if (this.parent != null) {
            return this.parent.getRootContext();
        }
        return this;
    }
}
