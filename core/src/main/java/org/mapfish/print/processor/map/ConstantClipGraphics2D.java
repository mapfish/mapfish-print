package org.mapfish.print.processor.map;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;

/**
 * A graphics 2d implementation that delegates all calls to the wrapped graphics2d except for the methods
 * related to setting the clip.  These calls are ignored.
 */
// CHECKSTYLE:OFF
final class ConstantClipGraphics2D extends Graphics2D {
    private final Graphics2D wrapped;

    ConstantClipGraphics2D(final Graphics2D wrapped, final Shape clipShape) {
        this.wrapped = wrapped;
        wrapped.setClip(clipShape);
    }

    @Override
    public void clip(Shape s) {
        // do nothing clip methods are ignored.
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        // do nothing clip methods are ignored.
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        // do nothing clip methods are ignored.
    }

    @Override
    public void draw3DRect(int x, int y, int width, int height, boolean raised) {
        wrapped.draw3DRect(x, y, width, height, raised);
    }

    @Override
    public void fill3DRect(int x, int y, int width, int height, boolean raised) {
        wrapped.fill3DRect(x, y, width, height, raised);
    }

    @Override
    public void draw(Shape s) {
        wrapped.draw(s);
    }

    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        return wrapped.drawImage(img, xform, obs);
    }

    @Override
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
        wrapped.drawImage(img, op, x, y);
    }

    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
        wrapped.drawRenderedImage(img, xform);
    }

    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
        wrapped.drawRenderableImage(img, xform);
    }

    @Override
    public void drawString(String str, int x, int y) {
        wrapped.drawString(str, x, y);
    }

    @Override
    public void drawString(String str, float x, float y) {
        wrapped.drawString(str, x, y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        wrapped.drawString(iterator, x, y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y) {
        wrapped.drawString(iterator, x, y);
    }

    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y) {
        wrapped.drawGlyphVector(g, x, y);
    }

    @Override
    public void fill(Shape s) {
        wrapped.fill(s);
    }

    @Override
    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        return wrapped.hit(rect, s, onStroke);
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        return wrapped.getDeviceConfiguration();
    }

    @Override
    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
        wrapped.setRenderingHint(hintKey, hintValue);
    }

    @Override
    public Object getRenderingHint(RenderingHints.Key hintKey) {
        return wrapped.getRenderingHint(hintKey);
    }

    @Override
    public void addRenderingHints(Map<?, ?> hints) {
        wrapped.addRenderingHints(hints);
    }

    @Override
    public RenderingHints getRenderingHints() {
        return wrapped.getRenderingHints();
    }

    @Override
    public void setRenderingHints(Map<?, ?> hints) {
        wrapped.setRenderingHints(hints);
    }

    @Override
    public void translate(int x, int y) {
        wrapped.translate(x, y);
    }

    @Override
    public void translate(double tx, double ty) {
        wrapped.translate(tx, ty);
    }

    @Override
    public void rotate(double theta) {
        wrapped.rotate(theta);
    }

    @Override
    public void rotate(double theta, double x, double y) {
        wrapped.rotate(theta, x, y);
    }

    @Override
    public void scale(double sx, double sy) {
        wrapped.scale(sx, sy);
    }

    @Override
    public void shear(double shx, double shy) {
        wrapped.shear(shx, shy);
    }

    @Override
    public void transform(AffineTransform Tx) {
        wrapped.transform(Tx);
    }

    @Override
    public AffineTransform getTransform() {
        return wrapped.getTransform();
    }

    @Override
    public void setTransform(AffineTransform Tx) {
        wrapped.setTransform(Tx);
    }

    @Override
    public Paint getPaint() {
        return wrapped.getPaint();
    }

    @Override
    public void setPaint(Paint paint) {
        wrapped.setPaint(paint);
    }

    @Override
    public Composite getComposite() {
        return wrapped.getComposite();
    }

    @Override
    public void setComposite(Composite comp) {
        wrapped.setComposite(comp);
    }

    @Override
    public Color getBackground() {
        return wrapped.getBackground();
    }

    @Override
    public void setBackground(Color color) {
        wrapped.setBackground(color);
    }

    @Override
    public Stroke getStroke() {
        return wrapped.getStroke();
    }

    @Override
    public void setStroke(Stroke s) {
        wrapped.setStroke(s);
    }

    @Override
    public FontRenderContext getFontRenderContext() {
        return wrapped.getFontRenderContext();
    }

    @Override
    public Graphics create() {
        return wrapped.create();
    }

    @Override
    public Graphics create(int x, int y, int width, int height) {
        return wrapped.create(x, y, width, height);
    }

    @Override
    public Color getColor() {
        return wrapped.getColor();
    }

    @Override
    public void setColor(Color c) {
        wrapped.setColor(c);
    }

    @Override
    public void setPaintMode() {
        wrapped.setPaintMode();
    }

    @Override
    public void setXORMode(Color c1) {
        wrapped.setXORMode(c1);
    }

    @Override
    public Font getFont() {
        return wrapped.getFont();
    }

    @Override
    public void setFont(Font font) {
        wrapped.setFont(font);
    }

    @Override
    public FontMetrics getFontMetrics() {
        return wrapped.getFontMetrics();
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        return wrapped.getFontMetrics(f);
    }

    @Override
    public Rectangle getClipBounds() {
        return wrapped.getClipBounds();
    }

    @Override
    public Shape getClip() {
        return wrapped.getClip();
    }

    @Override
    public void setClip(Shape clip) {
        // do nothing clip methods are ignored.
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        wrapped.copyArea(x, y, width, height, dx, dy);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        wrapped.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        wrapped.fillRect(x, y, width, height);
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        wrapped.drawRect(x, y, width, height);
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
        wrapped.clearRect(x, y, width, height);
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        wrapped.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        wrapped.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        wrapped.drawOval(x, y, width, height);
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        wrapped.fillOval(x, y, width, height);
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        wrapped.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        wrapped.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        wrapped.drawPolyline(xPoints, yPoints, nPoints);
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        wrapped.drawPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public void drawPolygon(Polygon p) {
        wrapped.drawPolygon(p);
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        wrapped.fillPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public void fillPolygon(Polygon p) {
        wrapped.fillPolygon(p);
    }

    @Override
    public void drawChars(char[] data, int offset, int length, int x, int y) {
        wrapped.drawChars(data, offset, length, x, y);
    }

    @Override
    public void drawBytes(byte[] data, int offset, int length, int x, int y) {
        wrapped.drawBytes(data, offset, length, x, y);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        return wrapped.drawImage(img, x, y, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        return wrapped.drawImage(img, x, y, width, height, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        return wrapped.drawImage(img, x, y, bgcolor, observer);
    }

    @Override
    public boolean drawImage(
            Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
        return wrapped.drawImage(img, x, y, width, height, bgcolor, observer);
    }

    @Override
    public boolean drawImage(
            Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
            ImageObserver observer) {
        return wrapped.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
    }

    @Override
    public boolean drawImage(
            Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor,
            ImageObserver observer) {
        return wrapped.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
    }

    @Override
    public void dispose() {
        wrapped.dispose();
    }

    @Override
    @Deprecated
    public Rectangle getClipRect() {
        return wrapped.getClipRect();
    }

    @Override
    public boolean hitClip(int x, int y, int width, int height) {
        return wrapped.hitClip(x, y, width, height);
    }

    @Override
    public Rectangle getClipBounds(Rectangle r) {
        return wrapped.getClipBounds(r);
    }
}
