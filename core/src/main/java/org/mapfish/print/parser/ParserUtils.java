package org.mapfish.print.parser;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import jakarta.annotation.Nullable;

/** Utility method for getting and setting parameters on Processor Input and Output objects. */
public final class ParserUtils {
  /**
   * A filter (for the get attribute methods) that selects only the attributes that are required and
   * excludes all of those with defaults, and therefore are considered optional.
   */
  public static final Predicate<Field> FILTER_ONLY_REQUIRED_ATTRIBUTES =
      input ->
          input != null
              && input.getAnnotation(HasDefaultValue.class) == null
              && !Modifier.isFinal(input.getModifiers());

  /**
   * A filter (for the get attribute methods) that selects only the attributes that are NOT required
   * and excludes all of those that are considered required.
   */
  public static final Predicate<Field> FILTER_HAS_DEFAULT_ATTRIBUTES =
      input -> input != null && input.getAnnotation(HasDefaultValue.class) != null;

  /**
   * A filter (for the get attribute methods) that selects only the attributes that are non final.
   * (Can be modified)
   */
  public static final Predicate<Field> FILTER_NON_FINAL_FIELDS =
      input -> input != null && !Modifier.isFinal(input.getModifiers());

  /**
   * A filter (for the get attribute methods) that selects only the attributes that are final. (Can
   * NOT be modified)
   */
  public static final Predicate<Field> FILTER_FINAL_FIELDS =
      (@Nullable final Field input) -> input != null && Modifier.isFinal(input.getModifiers());

  private static final Function<Field, String> FIELD_TO_NAME = Field::getName;

  private ParserUtils() {
    // intentionally empty.
  }

  /**
   * Inspects the object and all superclasses for public, non-final, accessible methods and returns
   * a collection containing all the attributes found.
   *
   * @param classToInspect the class under inspection.
   */
  public static Collection<Field> getAllAttributes(final Class<?> classToInspect) {
    Set<Field> allFields = new HashSet<>();
    getAllAttributes(classToInspect, allFields, Function.identity(), field -> true);
    return allFields;
  }

  /**
   * Get a subset of the attributes of the provided class. An attribute is each public field in the
   * class or super class.
   *
   * @param classToInspect the class to inspect
   * @param filter a predicate that returns true when a attribute should be kept in resulting
   *     collection.
   */
  public static Collection<Field> getAttributes(
      final Class<?> classToInspect, final Predicate<Field> filter) {
    Set<Field> allFields = new HashSet<>();
    getAllAttributes(classToInspect, allFields, Function.identity(), filter);
    return allFields;
  }

  private static <V> void getAllAttributes(
      final Class<?> classToInspect,
      final Set<V> results,
      final Function<Field, V> map,
      final Predicate<Field> filter) {

    if (classToInspect != null && classToInspect != Void.class) {
      Collection<? extends V> resultsForClass =
          Arrays.stream(classToInspect.getFields())
              .filter(filter)
              .map(map)
              .collect(Collectors.toList());
      results.addAll(resultsForClass);
      if (classToInspect.getSuperclass() != null) {
        getAllAttributes(classToInspect.getSuperclass(), results, map, filter);
      }
    }
  }

  /**
   * Converts all non-final properties in {@link #getAllAttributes(Class)} to a set of the attribute
   * names.
   *
   * @param classToInspect the class to inspect
   */
  public static Set<String> getAllAttributeNames(final Class<?> classToInspect) {
    Set<String> allFields = new HashSet<>();
    getAllAttributes(classToInspect, allFields, FIELD_TO_NAME, field -> true);
    return allFields;
  }

  /**
   * Converts all properties in {@link #getAllAttributes(Class)} to a set of the attribute names.
   *
   * @param classToInspect the class to inspect
   * @param filter a predicate that returns true when a attribute should be kept in resulting
   *     collection.
   */
  public static Set<String> getAttributeNames(
      final Class<?> classToInspect, final Predicate<Field> filter) {
    Set<String> allFields = new HashSet<>();
    getAllAttributes(classToInspect, allFields, FIELD_TO_NAME, filter);
    return allFields;
  }
}
