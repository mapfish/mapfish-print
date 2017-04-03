package org.mapfish.print.processor;

import com.google.common.base.Strings;
import com.google.common.collect.BiMap;

import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.output.Values;
import org.mapfish.print.parser.HasDefaultValue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.mapfish.print.parser.ParserUtils.getAllAttributes;

/**
 * Shared methods for working with processor.
 */
public final class ProcessorUtils {
    private ProcessorUtils() {
        // do nothing
    }

    /**
     * Create the input object required by the processor and populate all the fields from the values object.
     * <p></p>
     * If {@link Processor#createInputParameter()} returns an instance of values then the values object will be returned.
     *
     * @param processor the processor that the input object will be for.
     * @param values the object containing the values to put into the input object
     * @param <In> type of the processor input object
     * @param <Out> type of the processor output object
     */
    public static <In, Out> In populateInputParameter(
            final Processor<In, Out> processor,
            @Nonnull final Values values) {
        In inputObject = processor.createInputParameter();
        if (inputObject != null) {
            Collection<Field> fields = getAllAttributes(inputObject.getClass());
            for (Field field : fields) {
                String name = getInputValueName(processor.getOutputPrefix(),
                        processor.getInputMapperBiMap(), field.getName());
                Object value = values.getObject(name, Object.class);
                if (value != null) {
                    try {
                        field.set(inputObject, value);
                    } catch (IllegalAccessException e) {
                        throw ExceptionUtils.getRuntimeException(e);
                    }
                } else {
                    if (field.getAnnotation(HasDefaultValue.class) == null) {
                        throw new NoSuchElementException(name + " is a required property for " + processor
                                + " and therefore must be defined in the Request Data or be an output of " +
                                "one of the other processors. Available values: " +
                                values.asMap().keySet() + ".");
                    }
                }
            }
        }
        return inputObject;
    }

    /**
     * Read the values from the output object and write them to the values object.
     * @param output the output object from a processor
     * @param processor the processor the output if from
     * @param values the object for sharing values between processors
     */
    public static void writeProcessorOutputToValues(
            final Object output,
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
     * Calculate the name of the input value.
     *
     * @param inputPrefix a nullable prefix to prepend to the name if non-null and non-empty
     * @param inputMapper the name mapper
     * @param field the field containing the value
     */
    public static String getInputValueName(@Nullable final String inputPrefix,
                                            @Nonnull final BiMap<String, String> inputMapper,
                                            @Nonnull final String field) {
        String name = inputMapper == null ? null : inputMapper.inverse().get(field);
        if (name == null) {
            if (inputMapper != null && inputMapper.containsKey(field)) {
                throw new RuntimeException("field in keys");
            }
            final String[] defaultValues = {
                Values.TASK_DIRECTORY_KEY, Values.CLIENT_HTTP_REQUEST_FACTORY_KEY,
                Values.TEMPLATE_KEY, Values.PDF_CONFIG_KEY, Values.SUBREPORT_DIR_KEY,
                Values.OUTPUT_FORMAT_KEY, Values.JOB_ID_KEY
            };
            if (inputPrefix == null || Arrays.asList(defaultValues).contains(field)) {
                name = field;
            } else {
                name = inputPrefix.trim() +
                        Character.toUpperCase(field.charAt(0)) +
                        field.substring(1);
            }
        }
        return name;
    }

    /**
     * Calculate the name of the output value.
     *
     * @param outputPrefix a nullable prefix to prepend to the name if non-null and non-empty
     * @param outputMapper the name mapper
     * @param field the field containing the value
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
