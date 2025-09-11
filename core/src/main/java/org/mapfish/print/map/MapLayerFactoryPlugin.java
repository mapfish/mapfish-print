package org.mapfish.print.map;

import java.io.IOException;
import java.util.Set;
import jakarta.annotation.Nonnull;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.config.Template;

/**
 * Parses layer request data and creates a MapLayer from it.
 *
 * @param <PARAM> the type of object that will be populated from the JSON and passed to the factory
 *     to create the layer.
 */
public interface MapLayerFactoryPlugin<PARAM> {

  /**
   * Return a set of all the values the json 'type' property should have for this plugin to apply
   * typenames <em>MUST</em> be lowercase.
   */
  Set<String> getTypeNames();

  /**
   * Create an instance of a param object. Each instance must be new and unique. Instances must
   * <em>NOT</em> be shared.
   *
   * <p>The object will be populated from the json. Each public field will be populated by looking
   * up the value in the json.
   *
   * <p>The same mechanism used for reading from the JSON into the param object is also used for
   * parsing the JSON into {@link org.mapfish.print.attribute.Attribute} value objects. See {@link
   * org.mapfish.print.attribute.ReflectiveAttribute#createValue(org.mapfish.print.config.Template)}()}
   * for details on how the parsing mechanism works.
   */
  PARAM createParameter();

  /**
   * Inspect the json data and return Optional&lt;MapLayer&gt; or Optional.absent().
   *
   * @param template the configuration related to the current request.
   * @param layerData an object populated from the json for the layer
   */
  @Nonnull
  MapLayer parse(@Nonnull Template template, @Nonnull PARAM layerData) throws IOException;
}
