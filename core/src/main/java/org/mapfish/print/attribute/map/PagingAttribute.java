package org.mapfish.print.attribute.map;

import java.util.List;
import org.mapfish.print.attribute.ReflectiveAttribute;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.parser.HasDefaultValue;

/**
 * Attribute that defines how a map is displayed across many pages.
 *
 * <p>This is used by the <a href="processors.html#!paging">paging processor</a>.
 * [[examples=paging]]
 */
public final class PagingAttribute
    extends ReflectiveAttribute<PagingAttribute.PagingProcessorValues> {

  @Override
  public Class<? extends PagingProcessorValues> getValueType() {
    return PagingProcessorValues.class;
  }

  @Override
  public PagingProcessorValues createValue(final Template template) {
    return new PagingProcessorValues();
  }

  @Override
  public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
    // nothing to be done
  }

  /** Values object for this attribute type. */
  public static class PagingProcessorValues {
    /** The scale denominator for each page/sub-map. */
    public double scale;

    /**
     * The amount that each page/sub-map should overlap each other.
     *
     * <p>For example if the value is 1 and the projection of the map is degrees then the overlap
     * will be 1 degree.
     *
     * <p>The default is to not have any overlap.
     */
    @HasDefaultValue public double overlap = 0;

    /**
     * Indicates how to render the area of interest on this sub-map. This makes it easier to how the
     * all the sub-maps fit together to for the complete map. Also if the map is rendered as a whole
     * in one part of the report one can easily see where in the complete map the sub-map fits, even
     * without looking at the labeling.
     *
     * <p>For options see: {@link org.mapfish.print.attribute.map.AreaOfInterest.AoiDisplay}
     *
     * <p>By default the rendering in the <a href="index.html#/attributes?location=!map">map
     * attribute's</a> area of interest will be used
     */
    @HasDefaultValue public AreaOfInterest.AoiDisplay aoiDisplay;

    /**
     * If this is defined it will override the style used for rendering the Area Of Interest in the
     * main <a href="index.html#/attributes?location=!map">map attribute's</a> Area of Interest
     * definition.
     */
    @HasDefaultValue public String aoiStyle = null;

    /**
     * If set to true will add an overview layer to the main map with all the pages and their label.
     */
    @HasDefaultValue public Boolean renderPagingOverview = false;

    /**
     * The style of the paging overview layer. This parameter is usefull only if
     * renderPagingOverview is true.
     */
    @HasDefaultValue public String pagingOverviewStyle = null;
  }
}
