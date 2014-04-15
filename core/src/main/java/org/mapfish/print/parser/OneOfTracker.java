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

package org.mapfish.print.parser;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.util.Assert;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Keeps track of which OneOf groups there are and which ones are satisfied.
 *
 * @author Jesse on 4/10/2014.
 */
final class OneOfTracker {
    private Map<String, OneOfGroup> mapping = Maps.newHashMap();

    /**
     * Check if a field is part of a  {@link org.mapfish.print.parser.OneOf} relationship and add if necessary.
     *
     * @param field the field to register.
     */
    public void register(final Field field) {
        final OneOf annotation = field.getAnnotation(OneOf.class);
        if (annotation != null) {
            OneOfGroup oneOfGroup = this.mapping.get(annotation.value());
            if (oneOfGroup == null) {
                oneOfGroup = new OneOfGroup(annotation.value());
                this.mapping.put(annotation.value(), oneOfGroup);
            }
            oneOfGroup.choices.add(field);
        }
    }

    /**
     * Check if a field is part of a {@link org.mapfish.print.parser.OneOf} relationship and mark the group as satisfied.
     *
     * @param field the field that is done.
     */
    public void markAsVisited(final Field field) {
        final OneOf annotation = field.getAnnotation(OneOf.class);
        if (annotation != null) {
            final OneOfGroup oneOfGroup = this.mapping.get(annotation.value());
            oneOfGroup.satisfiedBy.add(field);
        }
    }

    /**
     * Check that each group is satisfied by one and only one field.
     */
    public void checkAllGroupsSatisfied() {
        StringBuilder errors = new StringBuilder();

        for (OneOfGroup group : this.mapping.values()) {

            if (group.satisfiedBy.size() == 0) {
                errors.append("\n");
                errors.append("\t* The OneOf choice: ").append(group.name).append(" was not satisfied.  One (and only one) of the ");
                errors.append("following fields is required in the request data: ").append(toNames(group.choices));
            }
            if (group.satisfiedBy.size() > 1) {
                errors.append("\n");
                errors.append("\t* The OneOf choice: ").append(group.name).append(" was satisfied by too many fields.  Only one choice");
                errors.append("may be in the request data.  The fields found were: ").append(toNames(group.satisfiedBy));
            }
        }

        Assert.equals(0, errors.length(), "\nErrors were detected when analysing the @OneOf dependencies: \n" + errors);
    }

    private String toNames(final Collection<Field> choices) {
        StringBuilder names = new StringBuilder();
        for (Field choice : choices) {
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
        private Collection<Field> choices = Lists.newArrayList();
        private Set<Field> satisfiedBy = Sets.newHashSet();

        public OneOfGroup(final String name) {
            this.name = name;
        }
    }
}
