package org.mapfish.print.parser;

import com.google.common.collect.Collections2;
import org.locationtech.jts.util.Assert;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Keeps track of which OneOf groups there are and which ones are satisfied.
 */
final class OneOfTracker {
    private Map<String, OneOfGroup> mapping = new HashMap<>();

    /**
     * Check if a field is part of a  {@link org.mapfish.print.parser.OneOf} relationship and add if
     * necessary.
     *
     * @param field the field to register.
     */
    public void register(final Field field) {
        final OneOf oneOfAnnotation = field.getAnnotation(OneOf.class);

        String groupName = null;
        if (oneOfAnnotation != null) {
            groupName = oneOfAnnotation.value();
        } else {
            final CanSatisfyOneOf canSatisfyOneOf = field.getAnnotation(CanSatisfyOneOf.class);
            if (canSatisfyOneOf != null) {
                groupName = canSatisfyOneOf.value();
            }
        }

        if (groupName != null) {
            OneOfGroup oneOfGroup = this.mapping.get(groupName);
            if (oneOfGroup == null) {
                oneOfGroup = new OneOfGroup(groupName);
                this.mapping.put(groupName, oneOfGroup);
            }
            oneOfGroup.choices.add(field);
        }
    }

    /**
     * Check if a field is part of a {@link org.mapfish.print.parser.OneOf} relationship and mark the group as
     * satisfied.
     *
     * @param field the field that is done.
     */
    public void markAsVisited(final Field field) {
        final OneOf oneOfAnnotation = field.getAnnotation(OneOf.class);
        if (oneOfAnnotation != null) {
            final OneOfGroup oneOfGroup = this.mapping.get(oneOfAnnotation.value());
            oneOfGroup.satisfiedBy.add(new OneOfSatisfier(field, false));
        }
        final CanSatisfyOneOf canSatisfyOneOf = field.getAnnotation(CanSatisfyOneOf.class);
        if (canSatisfyOneOf != null) {
            final OneOfGroup oneOfGroup = this.mapping.get(canSatisfyOneOf.value());
            oneOfGroup.satisfiedBy.add(new OneOfSatisfier(field, true));
        }
    }

    /**
     * Check that each group is satisfied by one and only one field.
     *
     * @param currentPath the json path to the element being checked
     */
    public void checkAllGroupsSatisfied(final String currentPath) {
        StringBuilder errors = new StringBuilder();

        for (OneOfGroup group: this.mapping.values()) {

            if (group.satisfiedBy.isEmpty()) {
                errors.append("\n");
                errors.append("\t* The OneOf choice: ").append(group.name)
                        .append(" was not satisfied.  One (and only one) of the ");
                errors.append("following fields is required in the request data: ")
                        .append(toNames(group.choices));
            }

            Collection<OneOfSatisfier> oneOfSatisfiers =
                    Collections2.filter(group.satisfiedBy, input -> !input.isCanSatisfy);
            if (oneOfSatisfiers.size() > 1) {
                errors.append("\n");
                errors.append("\t* The OneOf choice: ").append(group.name)
                        .append(" was satisfied by too many fields.  Only one choice ");
                errors.append("may be in the request data.  The fields found were: ")
                        .append(toNames(toFields(group.satisfiedBy)));
            }
        }

        Assert.equals(0, errors.length(),
                      "\nErrors were detected when analysing the @OneOf dependencies of '" + currentPath +
                              "': \n" + errors);
    }

    private Collection<Field> toFields(final Set<OneOfSatisfier> satisfiedBy) {
        return Collections2.transform(satisfiedBy, (@Nonnull final OneOfSatisfier input) -> input.field);
    }

    private String toNames(final Collection<Field> choices) {
        StringBuilder names = new StringBuilder();
        for (Field choice: choices) {
            if (names.length() > 0) {
                names.append(", ");
            }
            String type = choice.getType().getName();
            if (choice.getType().isArray()) {
                type = choice.getType().getComponentType().getName() + "[]";
            }
            names.append(type).append(' ').append(choice.getName());
        }
        return names.toString();
    }

    private static final class OneOfGroup {
        private String name;
        private Collection<Field> choices = new ArrayList<>();
        private Set<OneOfSatisfier> satisfiedBy = new HashSet<>();

        private OneOfGroup(final String name) {
            this.name = name;
        }
    }

    private static final class OneOfSatisfier {
        private final Field field;
        private final boolean isCanSatisfy;

        private OneOfSatisfier(
                @Nonnull final Field field,
                final boolean isCanSatisfy) {
            this.field = field;
            this.isCanSatisfy = isCanSatisfy;
        }

        // CHECKSTYLE:OFF
        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            OneOfSatisfier that = (OneOfSatisfier) o;

            if (!field.equals(that.field)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return field.hashCode();
        }
        // CHECKSTYLE:ON
    }
}
