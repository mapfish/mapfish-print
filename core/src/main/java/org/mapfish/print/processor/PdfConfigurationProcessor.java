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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.config.ConfigurationObject;
import org.mapfish.print.config.PDFConfig;
import org.mapfish.print.output.Values;
import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * This processor allows the dynamic configuration of the {@link org.mapfish.print.config.PDFConfig} object by obtaining data
 * from attributes.  For example the title and author could be string attributes posted from the client, this processor would update
 * the {@link org.mapfish.print.config.PDFConfig} object with the attribute data allowing per report PDF metadata.
 * <p/>
 * Note: The {@link org.mapfish.print.config.PDFConfig} can also be configured in the config.yaml either at the config or template level.
 *
 * @author Jesse on 9/13/2014.
 */
public final class PdfConfigurationProcessor extends AbstractProcessor<PdfConfigurationProcessor.In, Void> {

    private Map<String, Update> updates;

    /**
     * Constructor.
     */
    public PdfConfigurationProcessor() {
        super(Void.class);
    }

    /**
     * The pdf metadata property -> attribute name map.  The keys must be one of the values in
     * {@link org.mapfish.print.config.PDFConfig} and the values must be the name of the attribute to obtain the
     * the data from.  Example Configuration:
     * <p/>
     * <pre><code>
     * processors:
     *   - !updatePdfConfig
     *     updates:
     *       title: "titleAttribute"
     *       subject: "subjectAttribute"
     * </code></pre>
     * <p/>
     * The type of the attribute must be of the correct type, for example title mus be a string, keywords must be an array of strings,
     * compress must be a boolean.
     * <p/>
     * If the value is within the attribute output object then you can use dot separators for each level. For example suppose
     * there is a custom attribute: myconfig, if and it has a property title then the configuration would be:
     * <pre><code>
     * processors:
     *   - updatePdfConfig
     *     updates: {title: :myconfig.title"}
     * </code></pre>
     * <p/>
     * For more power a "format" can be defined.  The format is a printf style format string which will be called with a single
     * value that is identified by the value key/path.  In this case the short hand key: value can't be used instead it is as follows:
     * <pre><code>
     *   - updatePdfConfig
     *     updates:
     *       title: !updatePdfConfigUpdate
     *          valueKey: "myconfig.title"
     *          format: "Print Report %s"
     * </code></pre>
     *
     * @param updates the attribute map
     */
    public void setUpdates(final Map<String, Object> updates) {
        Map<String, Update> finalUpdatesMap = Maps.newHashMap();
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String property = entry.getKey();
            Update update;
            if (entry.getValue() instanceof Update) {
                update = (Update) entry.getValue();
                update.property = property;
            } else if (entry.getValue() instanceof String) {
                String value = (String) entry.getValue();
                update = new Update();
                update.property = property;
                update.setValueKey(value);
            } else {
                throw new IllegalArgumentException("Update property " + property + " has a non-string and non-!updatePdfConfigUpdate " +
                                                   "value: " + entry.getValue() + "(" + entry.getValue().getClass() + ")");
            }

            finalUpdatesMap.put(property, update);
        }
        this.updates = finalUpdatesMap;
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors, final Configuration configuration) {
        if (this.updates == null) {
            validationErrors.add(new ConfigurationException(
                    "The property 'attributeMap' in the !updatePdfConfig processor is required"));
        } else {
            if (this.updates.isEmpty()) {
                validationErrors.add(new ConfigurationException(
                        "At least one value for 'attributeMap' in !updatePdfConfig should be declared."));
            }
            for (Map.Entry<String, Update> entry : this.updates.entrySet()) {
                entry.getValue().validate(validationErrors, configuration);
            }
        }
    }

    @Nullable
    @Override
    public In createInputParameter() {
        return new In();
    }

    @Nullable
    @Override
    public Void execute(final In in, final ExecutionContext context) throws Exception {
        for (Map.Entry<String, Update> entry : this.updates.entrySet()) {
            Object value = getAttributeValue(entry.getValue().valueKey, in.values);
            final PropertyDescriptor propertyDescriptor = BeanUtils.getPropertyDescriptor(PDFConfig.class, entry.getValue().property);
            final String format = entry.getValue().format;
            if (format != null) {
                value = String.format(format, value);
            }

            final MethodParameter writeMethodParameter = BeanUtils.getWriteMethodParameter(propertyDescriptor);
            try {
                if (propertyDescriptor.getName().equals("keywords")) {
                    value = convertKeywords(value);
                }
                writeMethodParameter.getMethod().invoke(in.pdfConfig, value);
            } catch (Throwable e) {
                if (writeMethodParameter == null) {
                    throw new RuntimeException(
                            "An error occurred while executing !updatePdfConfig.  Unable to set configuration property '" +
                            entry.getKey() + " with value " + value + ". ");
                }
                throw new RuntimeException(
                        "An error occurred while executing !updatePdfConfig.  Unable to set configuration property '" +
                        entry.getKey() + " with value " + value + ". The expected type is " + writeMethodParameter.getParameterType() +
                        " but the type of the value being set was: " + (value != null ? value.getClass() : "null"));
            }
        }

        return null;
    }

    private List<String> convertKeywords(@Nullable final Object keywordsObj) {
        if (keywordsObj == null) {
            return Collections.emptyList();
        }
        if (keywordsObj instanceof Iterable) {
            Iterable obj = (Iterable) keywordsObj;
            final ArrayList<String> list = Lists.newArrayList();
            for (Object keyword : obj) {
                list.add(keyword.toString());
            }
            return list;
        }
        if (keywordsObj.getClass().isArray()) {
            Object[] arr = (Object[]) keywordsObj;
            final ArrayList<String> list = Lists.newArrayList();
            for (int i = 0; i < arr.length; i++) {
                Object keyword = arr[i];
                list.add(keyword.toString());

            }
            return list;
        }
        final String s = keywordsObj.toString();
        if (s.contains(",")) {
            return Lists.newArrayList(Arrays.asList(s.split(",")));
        }
        return Lists.newArrayList(s);
    }

    private Object getAttributeValue(final String attributeName, final Values values) {
        String[] parts = attributeName.split("\\.");
        Object value = values.getObject(parts[0], Object.class);
        for (int i = 1; i < parts.length; i++) {
            assertNonnullValue(attributeName, values, value);
            String part = parts[i];
            final Field field;
            try {
                field = value.getClass().getField(part);
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                value = field.get(value);
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException(
                        "No field " + part + " in object: " + value.getClass() + ". This error is part of the processor " +
                        "!updatePdfConfig for the value: " + attributeName);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(
                        "Not permitted to access" + part + " in object: " + value.getClass() + ".  This is likely caused by the " +
                        "Java security manager. This error is part of the processor " +
                        "!updatePdfConfig for the value: " + attributeName);
            }
        }

        assertNonnullValue(attributeName, values, value);
        return value;
    }

    private void assertNonnullValue(final String attributeName, final Values values, final Object value) {
        if (value == null) {
            throw new IllegalArgumentException(attributeName + " does not identify a value that is currently in the values object.  " +
                                               "Values object is: \n" + values);
        }
    }

    /**
     * The input parameters object.
     */
    public static class In {
        /**
         * The values object used to retrieve the required attributes.
         */
        @InternalValue
        public Values values;

        /**
         * The pdf configuration object.
         */
        @InternalValue
        public PDFConfig pdfConfig;
    }

    /**
     * The object that defines how to update the {@link org.mapfish.print.config.PDFConfig}.
     */
    public static final class Update implements ConfigurationObject {
        private String property;
        private String valueKey;
        private String format;

        /**
         * Default constructor.
         */
        public Update() {
            // do nothing
        }

        Update(final String valueKey, final String format) {
            this.valueKey = valueKey;
            this.format = format;
        }

        @Override
        public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
            if (this.valueKey.isEmpty()) {
                validationErrors.add(new ConfigurationException(
                        "The value of '" + this.property + "' should not be empty. Error in !updatePdfConfig"));
                return;
            }
            if (this.valueKey.charAt(0) == '.') {
                validationErrors.add(new ConfigurationException(
                        "The value of '" + this.property + "' should start with a '.', it was " +
                        this.valueKey + ". Error in !updatePdfConfig"));
                return;
            }

            String[] attributeAccessorDefinition = this.valueKey.split("\\.");
            if (attributeAccessorDefinition.length == 0) {
                validationErrors.add(new ConfigurationException(
                        this.property + ": " + this.valueKey + " is not a valid mapping in !updatePdfConfig"));
                return;
            }

            final PropertyDescriptor propertyDescriptor = BeanUtils.getPropertyDescriptor(PDFConfig.class, this.property);
            if (propertyDescriptor == null || BeanUtils.getWriteMethodParameter(propertyDescriptor) == null) {
                PropertyDescriptor[] descriptors = BeanUtils.getPropertyDescriptors(PDFConfig.class);
                StringBuilder options = new StringBuilder();
                for (PropertyDescriptor descriptor : descriptors) {
                    options.append("\n\t* ").append(descriptor.getName());
                }
                validationErrors.add(new ConfigurationException(
                        "There is no pdf config property called '" + this.property + "'. Options include: " + options));
            }

        }

        /**
         * The key to use to look up the value in the values object.  It can be a path that can reach into nested objects.
         * <p/>
         * Examples 1 a simple lookup key: "key"
         * Example 2 a path.  First part (before .) is the lookup key, the second part is the field name to load: "key.fieldName"
         *
         * @param valueKey the path or key for retrieving the value
         */
        public void setValueKey(final String valueKey) {
            this.valueKey = valueKey;
        }

        /**
         * The replacement format.  It is a printf style format.  The documentation is in the Formatter class
         * (just google/bing java.util.Formatter).
         * <p/>
         * Example: "Report for %s"
         *
         * @param format the update format.  There can only be a single value.
         */
        public void setFormat(final String format) {
            this.format = format;
        }
    }

}
