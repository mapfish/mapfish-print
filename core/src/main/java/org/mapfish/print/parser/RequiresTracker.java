package org.mapfish.print.parser;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.locationtech.jts.util.Assert;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Keeps track of the attributes that require other dependencies and verifies they are all satisfied at the
 * end of the json parsing.
 */
class RequiresTracker {
    private Multimap<Field, String> dependantToRequirementsMap = HashMultimap.create();
    private Map<String, Field> requirementToDependantMap = new HashMap<>();
    private Collection<Field> dependantsInJson = new ArrayList<>();

    /**
     * Check if field has the {@link org.mapfish.print.parser.Requires} annotation and registers it and its
     * requirement.
     *
     * @param field the field to inspect
     */
    public void register(final Field field) {
        Requires requires = field.getAnnotation(Requires.class);
        if (requires != null) {
            final String[] requirements = requires.value();
            for (String requirement: requirements) {
                this.dependantToRequirementsMap.put(field, requirement);
                this.requirementToDependantMap.put(requirement, field);
            }

        }
    }


    /**
     * Check if a field is part of a {@link org.mapfish.print.parser.Requires} relationship and mark the
     * requirement as satisfied for the given field.
     *
     * @param field the field that is done.
     */
    public void markAsVisited(final Field field) {
        if (field.getAnnotation(Requires.class) != null) {
            this.dependantsInJson.add(field);
        }

        final Field dependant = this.requirementToDependantMap.get(field.getName());
        if (dependant != null) {
            this.dependantToRequirementsMap.remove(dependant, field.getName());
        }
    }


    /**
     * Check that each requirement is satisfied.
     *
     * @param currentPath the json path to the element being checked
     */
    public void checkAllRequirementsSatisfied(final String currentPath) {
        StringBuilder errors = new StringBuilder();

        for (Field field: this.dependantsInJson) {
            final Collection<String> requirements = this.dependantToRequirementsMap.get(field);
            if (!requirements.isEmpty()) {
                errors.append("\n");
                String type = field.getType().getName();
                if (field.getType().isArray()) {
                    type = field.getType().getComponentType().getName() + "[]";
                }
                errors.append("\t* ").append(type).append(' ').append(field.getName()).append(" depends on ")
                        .append(requirements);
            }
        }
        Assert.equals(0, errors.length(),
                      "\nErrors were detected when analysing the @Requires dependencies of '" +
                              currentPath + "': " + errors);
    }
}
