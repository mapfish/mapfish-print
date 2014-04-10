/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.json.parser;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.vividsolutions.jts.util.Assert;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

/**
 * Keeps track of the attributes that require other dependencies and verifies they are all satisfied at the end
 * of the json parsing.
 *
 * @author Jesse on 4/10/2014.
 */
class RequiresTracker {
    private Multimap<Field, String> dependantToRequirementsMap = HashMultimap.create();
    private Map<String, Field> requirementToDependantMap = Maps.newHashMap();
    private Collection<Field> dependantsInJson = Lists.newArrayList();
    /**
     * Check if field has the {@link org.mapfish.print.json.parser.Requires} annotation and registers it and its requirement.
     * @param field the field to inspect
     */
    public void register(final Field field) {
        Requires requires = field.getAnnotation(Requires.class);
        if (requires != null) {
            final String[] requirements = requires.value();
            for (String requirement : requirements) {
                this.dependantToRequirementsMap.put(field, requirement);
                this.requirementToDependantMap.put(requirement, field);
            }

        }
    }


    /**
     * Check if a field is part of a {@link org.mapfish.print.json.parser.Requires} relationship and mark the requirement as satisfied
     * for the given field.
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
     */
    public void checkAllRequirementsSatisfied() {
        StringBuilder errors = new StringBuilder();

        for (Field field : this.dependantsInJson) {
            final Collection<String> requirements = this.dependantToRequirementsMap.get(field);
            if (!requirements.isEmpty()) {
                errors.append("\n");
                String type = field.getType().getName();
                if (field.getType().isArray()) {
                    type = field.getType().getComponentType().getName() + "[]";
                }
                errors.append("\t* ").append(type).append(' ').append(field.getName()).append(" depends on ").append(requirements);
            }
        }
        Assert.equals(0, errors.length(), "\nErrors were detected when analysing the @Requires dependencies: " + errors);
    }
}
