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
 * A graphics 2d implementation that delegates all calls to the wrapped graphics2d except for the
 * methods related to setting the clip. These calls are ignored.
 */
final class ConstantClipGraphics2D extends Graphics2D {
  private final Graphics2D wrapped;

  ConstantClipGraphics2D(final Graphics2D wrapped, final Shape clipShape) {
    this.wrapped = wrapped;
    wrapped.setClip(clipShape);
  }

  @Override
  public void clip(final Shape s) {
    // do nothing clip methods are ignored.
  }

  @Override
  public void clipRect(final int x, final int y, final int width, final int height) {
    // do nothing clip methods are ignored.
  }

  @Override
  public void setClip(final int x, final int y, final int width, final int height) {
    // do nothing clip methods are ignored.
  }

  @Override
  public void draw3DRect(
      final int x, final int y, final int width, final int height, final boolean raised) {
    wrapped.draw3DRect(x, y, width, height, raised);
  }

  @Override
  public void fill3DRect(
      final int x, final int y, final int width, final int height, final boolean raised) {
    wrapped.fill3DRect(x, y, width, height, raised);
  }

  @Override
  public void draw(final Shape s) {
    wrapped.draw(s);
  }

  @Override
  public boolean drawImage(final Image img, final AffineTransform xform, final ImageObserver obs) {
    return wrapped.drawImage(img, xform, obs);
  }

  @Override
  public void drawImage(
      final BufferedImage img, final BufferedImageOp op, final int x, final int y) {
    wrapped.drawImage(img, op, x, y);
  }

  @Override
  public void drawRenderedImage(final RenderedImage img, final AffineTransform xform) {
    wrapped.drawRenderedImage(img, xform);
  }

  @Override
  public void drawRenderableImage(final RenderableImage img, final AffineTransform xform) {
    wrapped.drawRenderableImage(img, xform);
  }

  @Override
  public void drawString(final String str, final int x, final int y) {
    wrapped.drawString(str, x, y);
  }

  @Override
  public void drawString(final String str, final float x, final float y) {
    wrapped.drawString(str, x, y);
  }

  @Override
  public void drawString(final AttributedCharacterIterator iterator, final int x, final int y) {
    wrapped.drawString(iterator, x, y);
  }

  @Override
  public void drawString(final AttributedCharacterIterator iterator, final float x, final float y) {
    wrapped.drawString(iterator, x, y);
  }

  @Override
  public void drawGlyphVector(final GlyphVector g, final float x, final float y) {
    wrapped.drawGlyphVector(g, x, y);
  }

  @Override
  public void fill(final Shape s) {
    wrapped.fill(s);
  }

  @Override
  public boolean hit(final Rectangle rect, final Shape s, final boolean onStroke) {
    return wrapped.hit(rect, s, onStroke);
  }

  @Override
  public GraphicsConfiguration getDeviceConfiguration() {
    return wrapped.getDeviceConfiguration();
  }

  @Override
  public void setRenderingHint(final RenderingHints.Key hintKey, final Object hintValue) {
    wrapped.setRenderingHint(hintKey, hintValue);
  }

  @Override
  public Object getRenderingHint(final RenderingHints.Key hintKey) {
    return wrapped.getRenderingHint(hintKey);
  }

  @Override
  public void addRenderingHints(final Map<?, ?> hints) {
    wrapped.addRenderingHints(hints);
  }

  @Override
  public RenderingHints getRenderingHints() {
    return wrapped.getRenderingHints();
  }

  @Override
  public void setRenderingHints(final Map<?, ?> hints) {
    wrapped.setRenderingHints(hints);
  }

  @Override
  public void translate(final int x, final int y) {
    wrapped.translate(x, y);
  }

  @Override
  public void translate(final double tx, final double ty) {
    wrapped.translate(tx, ty);
  }

  @Override
  public void rotate(final double theta) {
    wrapped.rotate(theta);
  }

  @Override
  public void rotate(final double theta, final double x, final double y) {
    wrapped.rotate(theta, x, y);
  }

  @Override
  public void scale(final double sx, final double sy) {
    wrapped.scale(sx, sy);
  }

  @Override
  public void shear(final double shx, final double shy) {
    wrapped.shear(shx, shy);
  }

  @Override
  public void transform(final AffineTransform tx) {
    wrapped.transform(tx);
  }

  @Override
  public AffineTransform getTransform() {
    return wrapped.getTransform();
  }

  @Override
  public void setTransform(final AffineTransform tx) {
    wrapped.setTransform(tx);
  }

  @Override
  public Paint getPaint() {
    return wrapped.getPaint();
  }

  @Override
  public void setPaint(final Paint paint) {
    wrapped.setPaint(paint);
  }

  @Override
  public Composite getComposite() {
    return wrapped.getComposite();
  }

  @Override
  public void setComposite(final Composite comp) {
    wrapped.setComposite(comp);
  }

  @Override
  public Color getBackground() {
    return wrapped.getBackground();
  }

  @Override
  public void setBackground(final Color color) {
    wrapped.setBackground(color);
  }

  @Override
  public Stroke getStroke() {
    return wrapped.getStroke();
  }

  @Override
  public void setStroke(final Stroke s) {
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
  public Graphics create(final int x, final int y, final int width, final int height) {
    return wrapped.create(x, y, width, height);
  }

  @Override
  public Color getColor() {
    return wrapped.getColor();
  }

  @Override
  public void setColor(final Color c) {
    wrapped.setColor(c);
  }

  @Override
  public void setPaintMode() {
    wrapped.setPaintMode();
  }

  @Override
  public void setXORMode(final Color c1) {
    wrapped.setXORMode(c1);
  }

  @Override
  public Font getFont() {
    return wrapped.getFont();
  }

  @Override
  public void setFont(final Font font) {
    wrapped.setFont(font);
  }

  @Override
  public FontMetrics getFontMetrics() {
    return wrapped.getFontMetrics();
  }

  @Override
  public FontMetrics getFontMetrics(final Font f) {
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
  public void setClip(final Shape clip) {
    // do nothing clip methods are ignored.
  }

  @Override
  public void copyArea(
      final int x, final int y, final int width, final int height, final int dx, final int dy) {
    wrapped.copyArea(x, y, width, height, dx, dy);
  }

  @Override
  public void drawLine(final int x1, final int y1, final int x2, final int y2) {
    wrapped.drawLine(x1, y1, x2, y2);
  }

  @Override
  public void fillRect(final int x, final int y, final int width, final int height) {
    wrapped.fillRect(x, y, width, height);
  }

  @Override
  public void drawRect(final int x, final int y, final int width, final int height) {
    wrapped.drawRect(x, y, width, height);
  }

  @Override
  public void clearRect(final int x, final int y, final int width, final int height) {
    wrapped.clearRect(x, y, width, height);
  }

  @Override
  public void drawRoundRect(
      final int x,
      final int y,
      final int width,
      final int height,
      final int arcWidth,
      final int arcHeight) {
    wrapped.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
  }

  @Override
  public void fillRoundRect(
      final int x,
      final int y,
      final int width,
      final int height,
      final int arcWidth,
      final int arcHeight) {
    wrapped.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
  }

  @Override
  public void drawOval(final int x, final int y, final int width, final int height) {
    wrapped.drawOval(x, y, width, height);
  }

  @Override
  public void fillOval(final int x, final int y, final int width, final int height) {
    wrapped.fillOval(x, y, width, height);
  }

  @Override
  public void drawArc(
      final int x,
      final int y,
      final int width,
      final int height,
      final int startAngle,
      final int arcAngle) {
    wrapped.drawArc(x, y, width, height, startAngle, arcAngle);
  }

  @Override
  public void fillArc(
      final int x,
      final int y,
      final int width,
      final int height,
      final int startAngle,
      final int arcAngle) {
    wrapped.fillArc(x, y, width, height, startAngle, arcAngle);
  }

  @Override
  public void drawPolyline(final int[] xPoints, final int[] yPoints, final int nPoints) {
    wrapped.drawPolyline(xPoints, yPoints, nPoints);
  }

  @Override
  public void drawPolygon(final int[] xPoints, final int[] yPoints, final int nPoints) {
    wrapped.drawPolygon(xPoints, yPoints, nPoints);
  }

  @Override
  public void drawPolygon(final Polygon p) {
    wrapped.drawPolygon(p);
  }

  @Override
  public void fillPolygon(final int[] xPoints, final int[] yPoints, final int nPoints) {
    wrapped.fillPolygon(xPoints, yPoints, nPoints);
  }

  @Override
  public void fillPolygon(final Polygon p) {
    wrapped.fillPolygon(p);
  }

  @Override
  public void drawChars(
      final char[] data, final int offset, final int length, final int x, final int y) {
    wrapped.drawChars(data, offset, length, x, y);
  }

  @Override
  public void drawBytes(
      final byte[] data, final int offset, final int length, final int x, final int y) {
    wrapped.drawBytes(data, offset, length, x, y);
  }

  @Override
  public boolean drawImage(
      final Image img, final int x, final int y, final ImageObserver observer) {
    return wrapped.drawImage(img, x, y, observer);
  }

  @Override
  public boolean drawImage(
      final Image img,
      final int x,
      final int y,
      final int width,
      final int height,
      final ImageObserver observer) {
    return wrapped.drawImage(img, x, y, width, height, observer);
  }

  @Override
  public boolean drawImage(
      final Image img,
      final int x,
      final int y,
      final Color bgcolor,
      final ImageObserver observer) {
    return wrapped.drawImage(img, x, y, bgcolor, observer);
  }

  @Override
  public boolean drawImage(
      final Image img,
      final int x,
      final int y,
      final int width,
      final int height,
      final Color bgcolor,
      final ImageObserver observer) {
    return wrapped.drawImage(img, x, y, width, height, bgcolor, observer);
  }

  // CSOFF: ParameterNumber

  @Override
  public boolean drawImage(
      final Image img,
      final int dx1,
      final int dy1,
      final int dx2,
      final int dy2,
      final int sx1,
      final int sy1,
      final int sx2,
      final int sy2,
      final ImageObserver observer) {
    // CSON: ParameterNumber

    return wrapped.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
  }

  // CSOFF: ParameterNumber
  @Override
  public boolean drawImage(
      final Image img,
      final int dx1,
      final int dy1,
      final int dx2,
      final int dy2,
      final int sx1,
      final int sy1,
      final int sx2,
      final int sy2,
      final Color bgcolor,
      final ImageObserver observer) {
    // CSON: ParameterNumber
    return wrapped.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
  }

  @Override
  public void dispose() {
    wrapped.dispose();
  }

  /**
   * @deprecated As the underlying super-method is deprecated as well.
   */
  @Override
  @Deprecated
  public Rectangle getClipRect() {
    return wrapped.getClipRect();
  }

  @Override
  public boolean hitClip(final int x, final int y, final int width, final int height) {
    return wrapped.hitClip(x, y, width, height);
  }

  @Override
  public Rectangle getClipBounds(final Rectangle r) {
    return wrapped.getClipBounds(r);
  }
}
