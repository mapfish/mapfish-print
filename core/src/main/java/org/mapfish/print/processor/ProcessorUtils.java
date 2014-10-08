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

package org.mapfish.print.processor;

import com.google.common.base.Strings;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.output.Values;
import org.mapfish.print.parser.HasDefaultValue;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.mapfish.print.parser.ParserUtils.getAllAttributes;

/**
 * Shared methods for working with processor.
 *
 * @author Jesse on 6/26/2014.
 */
public final class ProcessorUtils {
    private ProcessorUtils() {
        // do nothing
    }

    /**
     * Create the input object required by the processor and populate all the fields from the values object.
     * <p/>
     * If {@link Processor#createInputParameter()} returns an instance of values then the values object will be returned.
     *
     * @param processor the processor that the input object will be for.
     * @param values    the object containing the values to put into the input object
     * @param <In>      type of the processor input object
     * @param <Out>     type of the processor output object
     */
    public static <In, Out> In populateInputParameter(final Processor<In, Out> processor, final Values values) {
        In inputObject = processor.createInputParameter();
        if (inputObject instanceof Values) {
            @SuppressWarnings("unchecked")
            final In castValues = (In) values;
            return castValues;
        }
        if (inputObject != null) {
            Map<String, String> inputMapper = processor.getInputMapperBiMap();
            if (inputMapper == null) {
                inputMapper = Collections.emptyMap();
            } else {
                inputMapper = processor.getInputMapperBiMap().inverse();
            }

            Collection<Field> fields = getAllAttributes(inputObject.getClass());
            for (Field field : fields) {
                String name = inputMapper.get(field.getName());
                if (name == null) {
                    name = field.getName();
                }
                Object value;
                if (field.getType() == Values.class) {
                    value = values;
                } else {
                    value = values.getObject(name, Object.class);
                }
                if (value != null) {
                    try {
                        field.set(inputObject, value);
                    } catch (IllegalAccessException e) {
                        throw ExceptionUtils.getRuntimeException(e);
                    }
                } else {
                    if (field.getAnnotation(HasDefaultValue.class) == null) {
                        throw new NoSuchElementException(name + " is a required property for " + processor +
                                                         " and therefore must be defined in the Request Data or be an output of one" +
                                                         " of the other processors.");
                    }
                }
            }
        }
        return inputObject;
    }

    /**
     * Read the values from the output object and write them to the values object.
     * @param output    the output object from a processor
     * @param processor the processor the output if from
     * @param values    the object for sharing values between processors
     */
    public static void writeProcessorOutputToValues(final Object output,
                                                    final Processor<?, ?> processor,
                                                    final Values values) {
        Map<String, String> mapper = processor.getOutputMapperBiMap();
        if (mapper == null) {
            mapper = Collections.emptyMap();
        }

        final Collection<Field> fields = getAllAttributes(output.getClass());
        for (Field field : fields) {
            String name = getOutputValueName(processor.getOutputPrefix(), mapper, field);
            try {
                final Object value = field.get(output);
                if (value != null) {
                    values.put(name, value);
                } else {
                    values.remove(name);
                }
            } catch (IllegalAccessException e) {
                throw ExceptionUtils.getRuntimeException(e);
            }
        }
    }

    /**
     * Calculate the name of the output value.
     *
     * @param outputPrefix a nullable prefix to prepend to the name if non-null and non-empty
     * @param outputMapper the name mapper
     * @param field        the field containing the value
     */
    public static String getOutputValueName(@Nullable final String outputPrefix,
                                            @Nonnull final Map<String, String> outputMapper,
                                            @Nonnull final Field field) {
        String name = outputMapper.get(field.getName());
        if (name == null) {
            name = field.getName();
            if (!Strings.isNullOrEmpty(outputPrefix) && !outputPrefix.trim().isEmpty()) {
                name = outputPrefix.trim() + Character.toUpperCase(name.charAt(0)) + name.substring(1);
            }
        }

        return name;
    }
}
