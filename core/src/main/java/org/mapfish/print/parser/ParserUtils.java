package org.mapfish.print.parser;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Utility method for getting and setting parameters on Processor Input and Output objects.
 */
public final class ParserUtils {
    /**
     * A filter (for the get attribute methods) that selects only the attributes that are required and excludes all of
     * those with defaults, and therefore are considered optional.
     */
    public static final Predicate<Field> FILTER_ONLY_REQUIRED_ATTRIBUTES = new Predicate<Field>() {
        @Override
        public boolean apply(@Nullable final Field input) {
            return input != null && input.getAnnotation(HasDefaultValue.class) == null && !Modifier.isFinal(input.getModifiers());
        }
    };
    /**
     *  A filter (for the get attribute methods) that selects only the attributes that are NOT required and excludes all of
     * those that are considered required.
     */
    public static final Predicate<Field> FILTER_HAS_DEFAULT_ATTRIBUTES = new Predicate<Field>() {
        @Override
        public boolean apply(@Nullable final Field input) {
            return input != null && input.getAnnotation(HasDefaultValue.class) != null;
        }
    };
    /**
     * A filter (for the get attribute methods) that selects only the attributes that are non final.
     * (Can be modified)
     */
    public static final Predicate<Field> FILTER_NON_FINAL_FIELDS = new Predicate<Field>() {
        @Override
        public boolean apply(@Nullable final Field input) {
            return input != null && !Modifier.isFinal(input.getModifiers());
        }
    };
    /**
     * A filter (for the get attribute methods) that selects only the attributes that are final.
     * (Can NOT be modified)
     */
    public static final Predicate<Field> FILTER_FINAL_FIELDS = new Predicate<Field>() {
        @Override
        public boolean apply(@Nullable final Field input) {
            return input != null && Modifier.isFinal(input.getModifiers());
        }
    };

    private static final Function<Field, String> FIELD_TO_NAME = new Function<Field, String>() {
        @Nullable
        @Override
        public String apply(final Field input) {
            return input.getName();
        }
    };
    private ParserUtils() {
        // intentionally empty.
    }

    /**
     * Inspects the object and all superclasses for public, non-final, accessible methods and returns a collection containing all the
     * attributes found.
     *
     * @param classToInspect the class under inspection.
     */
    public static Collection<Field> getAllAttributes(final Class<?> classToInspect) {
        Set<Field> allFields = Sets.newHashSet();
        getAllAttributes(classToInspect, allFields, Functions.<Field>identity(), field -> true);
        return allFields;
    }

    /**
     * Get a subset of the attributes of the provided class.  An attribute is each public field in the class or super class.
     *
     * @param classToInspect the class to inspect
     * @param filter a predicate that returns true when a attribute should be kept in resulting collection.
     */
    public static Collection<Field> getAttributes(final Class<?> classToInspect, final Predicate<Field> filter) {
        Set<Field> allFields = Sets.newHashSet();
        getAllAttributes(classToInspect, allFields, Functions.<Field>identity(), filter::test);
        return allFields;
    }

    private static<V> void getAllAttributes(final Class<?> classToInspect, final Set<V> results,
                                            final Function<Field, V> map, final java.util.function.Predicate<Field> filter) {

        if (classToInspect != null && classToInspect != Void.class) {
            Collection<Field> filteredResults = Collections2.filter(Arrays.asList(classToInspect.getFields()), filter::test);
            Collection<? extends V> resultsForClass = Collections2.transform(filteredResults, map);
            results.addAll(resultsForClass);
            if (classToInspect.getSuperclass() != null) {
                getAllAttributes(classToInspect.getSuperclass(), results, map, filter);
            }
        }
    }

    /**
     * Converts all non-final properties in {@link #getAllAttributes(Class)} to a set of the attribute names.
     *
     * @param classToInspect the class to inspect
     */
    public static Set<String> getAllAttributeNames(final Class<?> classToInspect) {
        Set<String> allFields = Sets.newHashSet();
        getAllAttributes(classToInspect, allFields, FIELD_TO_NAME, field -> true);
        return allFields;
    }
    /**
     * Converts all properties in {@link #getAllAttributes(Class)} to a set of the attribute names.
     *
     * @param classToInspect the class to inspect
     * @param filter a predicate that returns true when a attribute should be kept in resulting collection.
     */
    public static Set<String> getAttributeNames(final Class<?> classToInspect, final java.util.function.Predicate<Field> filter) {
        Set<String> allFields = Sets.newHashSet();
        getAllAttributes(classToInspect, allFields, FIELD_TO_NAME, filter);
        return allFields;
    }
}
