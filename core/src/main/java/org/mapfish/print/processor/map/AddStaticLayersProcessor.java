package org.mapfish.print.processor.map;

import java.util.Arrays;
import java.util.List;
import jakarta.annotation.Nullable;
import org.mapfish.print.attribute.map.GenericMapAttribute.GenericMapAttributeValues;
import org.mapfish.print.attribute.map.StaticLayersAttribute;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.InputOutputValue;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.PJoinedArray;

/**
 * This processor adds the configured set of layers to the map.
 *
 * <p>This is useful when all maps should have a default set of background layers or overlays added
 * to those that the client sends for printing.
 *
 * <p>This can simplify the client so the client only needs to be concerned with the data layers.
 *
 * <p>See also: <a href="attributes.html#!staticLayers">!staticLayers</a> attribute
 * [[examples=add_overlay_layer,report]]
 */
public final class AddStaticLayersProcessor
    extends AbstractProcessor<AddStaticLayersProcessor.Input, Void> {

  private StaticLayerPosition position;

  /** Constructor. */
  protected AddStaticLayersProcessor() {
    super(Void.class);
  }

  /**
   * Set the position enumeration which indicates where the layers should be added to the map:
   * {@link org.mapfish.print.processor.map.AddStaticLayersProcessor.StaticLayerPosition}.
   *
   * @param position the position.
   */
  public void setPosition(final StaticLayerPosition position) {
    this.position = position;
  }

  @Override
  protected void extraValidation(
      final List<Throwable> validationErrors, final Configuration configuration) {
    if (this.position == null) {
      validationErrors.add(
          new ConfigurationException(
              "The addPosition field needs to be set to one of the allowed options: "
                  + Arrays.toString(StaticLayerPosition.values())));
    }
  }

  @Nullable
  @Override
  public Input createInputParameter() {
    return new Input();
  }

  @Nullable
  @Override
  public Void execute(final Input values, final ExecutionContext context) throws Exception {
    switch (this.position) {
      case BOTTOM:
        values.map.setRawLayers(
            new PJoinedArray(new PArray[] {values.map.getRawLayers(), values.staticLayers.layers}));
        break;
      case TOP:
        values.map.setRawLayers(
            new PJoinedArray(new PArray[] {values.staticLayers.layers, values.map.getRawLayers()}));
        break;
      default:
        throw new Error(
            "An enumeration value was added that does not have an implementation.  A Programmer"
                + " must add "
                + "this implementation to "
                + getClass().getName());
    }
    values.map.postConstruct();
    return null;
  }

  /** Indications where in the layer list to add the static layers. */
  public enum StaticLayerPosition {
    /** Add Layers to the top of the map. Essentially overlays. */
    TOP,
    /** Add Layers to the bottom of the map, background layers. */
    BOTTOM
  }

  /** The object containing the values required for this processor. */
  public static class Input {

    /** The map to update with the static layers. */
    @InputOutputValue public GenericMapAttributeValues map;

    /** The attribute containing the static layers to add to the map. */
    public StaticLayersAttribute.StaticLayersAttributeValue staticLayers;
  }
}
