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

package org.mapfish.print.processor.jasper;

import com.google.common.annotations.Beta;
import com.google.common.collect.BiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.util.Assert;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignField;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.config.ConfigurationObject;
import org.mapfish.print.output.Values;
import org.mapfish.print.parser.ParserUtils;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.CustomDependencies;
import org.mapfish.print.processor.InternalValue;
import org.mapfish.print.processor.Processor;
import org.mapfish.print.processor.ProcessorDependency;
import org.mapfish.print.processor.ProcessorGraphNode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This processor combines DataSources and individual processor outputs (or attribute values) into a single DataSource which
 * can be used in a jasper report's detail section.
 * <p>
 * An example use case is where we might have zero or many of tables and zero or many legends.  You can configure the
 * template with a detail section that contains a subreport, the name of which is a field in the DataSources and the DataSources
 * for the sub-template another field.  Then you can merge the legend and the tables into a single DataSources.  This way the
 * report will nicely expand depending on if you have a legend and how many tables you have in your report.
 * </p>
 *
 * @author Jesse on 9/6/2014.
 */
@Beta
public final class MergeDataSourceProcessor extends AbstractProcessor<MergeDataSourceProcessor.In, MergeDataSourceProcessor.Out>
        implements CustomDependencies {
    private List<Source> sources = Lists.newArrayList();

    /**
     * Constructor.
     */
    protected MergeDataSourceProcessor() {
        super(Out.class);
    }

    /**
     * The <em>source</em> to add to the merged DataSource.  Each <em>source</em> indicates if it should be treated
     * as a datasource or as a single item to add to the merged DataSource.  If the source indicates that it is a
     * {@link org.mapfish.print.processor.jasper.MergeDataSourceProcessor.SourceType#DATASOURCE} the object
     * each row in the datasource will be used to form a row in the merged DataSource.  If the source type is
     * {@link org.mapfish.print.processor.jasper.MergeDataSourceProcessor.SourceType#SINGLE} the object will be a single row
     * even if it is in fact a DataSource.
     *
     * @param sources the source objects to merge
     */
    public void setSources(final List<Source> sources) {
        this.sources = sources;
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors, final Configuration config) {
        if (this.sources == null || this.sources.isEmpty()) {
            validationErrors.add(new ConfigurationException(getClass().getSimpleName() + " needs to have at minimum a single source. " +
                                                            "Although logically it should have more"));
            return;
        }

        for (int i = 0; i < this.sources.size(); i++) {
            Source source = this.sources.get(i);

            if (source.type == null) {
                validationErrors.add(new ConfigurationException(
                        "The " + indexString(i) + " source in " + getClass().getSimpleName() + " needs to " +
                        "have a 'type' parameter defined."));
            } else {
                source.type.validate(i, validationErrors, source);
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
    public Out execute(final In values, final ExecutionContext context) throws Exception {
        List<Map<String, ?>> rows = Lists.newArrayList();

        for (Source source : this.sources) {
            source.type.add(rows, values.values, source);
        }

        JRDataSource mergedDataSource = new JRMapCollectionDataSource(rows);
        return new Out(mergedDataSource);
    }

    @Nonnull
    @Override
    public List<ProcessorDependency> createDependencies(@Nonnull final List<ProcessorGraphNode<Object, Object>> nodes) {
        HashSet<String> sourceKeys = Sets.newHashSet();
        for (Source source : this.sources) {
            source.type.addValuesKeys(source, sourceKeys);
        }

        final ArrayList<ProcessorDependency> dependencies = Lists.newArrayList();
        for (ProcessorGraphNode<Object, Object> node : nodes) {
            if (node.getProcessor() == this) {
                continue;
            }
            final Processor<?, ?> processor = node.getProcessor();
            final Collection<Field> allAttributes = ParserUtils.getAllAttributes(processor.getOutputType());
            final BiMap<String, String> outputMapper = node.getOutputMapper();
            ProcessorDependency customDependency = null;
            for (Field allAttribute : allAttributes) {
                String attributeName = allAttribute.getName();
                final String mappedName = outputMapper.get(attributeName);
                if (mappedName != null) {
                    attributeName = mappedName;
                }
                if (sourceKeys.contains(attributeName)) {
                    if (customDependency == null) {
                        final Class<? extends Processor<?, ?>> processorClass = (Class<? extends Processor<?, ?>>) processor.getClass();
                        customDependency = new ProcessorDependency(processorClass, getClass(),
                                Collections.singleton(attributeName));
                        dependencies.add(customDependency);
                    } else {
                        customDependency.addCommonInput(attributeName);
                    }
                }
            }
        }
        return dependencies;
    }

    private static String indexString(final int i) {
        switch (i + 1) {
            case 1:
                return "1st";
            case 2:
                return "2nd";
            default:
                return (i + 1) + "th";
        }
    }

    /**
     * The input object for {@link org.mapfish.print.processor.jasper.MergeDataSourceProcessor}.
     */
    public static class In {
        /**
         * The values used to look up the values to merge together.
         */
        @InternalValue
        public Values values;
    }

    /**
     * The output object for {@link org.mapfish.print.processor.jasper.MergeDataSourceProcessor}.
     */
    public static class Out {
        /**
         * The resulting datasource.
         */
        public final JRDataSource mergedDataSource;

        /**
         * Constructor.
         *
         * @param mergedDataSource the merged datasource
         */
        public Out(final JRDataSource mergedDataSource) {
            this.mergedDataSource = mergedDataSource;
        }
    }

    /**
     * Describes the objects to used as sources for the merged DataSource.
     */
    public static final class Source implements ConfigurationObject {
        String key;
        SourceType type;
        Map<String, String> fields = Maps.newHashMap();

        /**
         * The key to use when looking for the object among the attributes and the processor output values.
         *
         * @param key the look up key
         */
        public void setKey(final String key) {
            this.key = key;
        }

        /**
         * The type of source.  See {@link org.mapfish.print.processor.jasper.MergeDataSourceProcessor.SourceType} for the options.
         *
         * @param type the type of source
         */
        public void setType(final SourceType type) {
            this.type = type;
        }

        /**
         * The names of each field in the DataSource.  See {@link org.mapfish.print.processor.jasper.MergeDataSourceProcessor.SourceType}
         * for instructions on how to declare the fields
         *
         * @param fields the field names
         */
        public void setFields(final Map<String, String> fields) {
            this.fields = fields;
        }

        static Source createSource(final String key, final SourceType type) {
            Source source = new Source();
            source.key = key;
            source.type = type;
            return source;
        }

        static Source createSource(final String key, final SourceType type, final Map<String, String> fields) {
            Source source = new Source();
            source.key = key;
            source.type = type;
            source.fields = fields;
            return source;
        }

        @Override
        public void validate(final List<Throwable> validationErrors, final Configuration config) {
            // validation is done in MergeDataSourceProcessor
        }
    }

    /**
     * An enumeration of the different <em>types</em> of source objects.  Essentially this describes how the source should be merged
     * into the final merged DataSource.
     */
    public enum SourceType {
        /**
         * Creates a single row from a set of values from the output and attribute objects.
         * <p>
         * In this case the key is not required, only the fields.  Each field key will be the
         * look up key to find the object from the set of processor output and attributes.  The field
         * value will be the column name for that value in the created row
         * </p>
         */
        SINGLE {
            @Override
            void add(final List<Map<String, ?>> rows, final Values values, final Source source) {

                Map<String, Object> row = Maps.newHashMap();
                for (Map.Entry<String, String> entry : source.fields.entrySet()) {
                    final Object object = values.getObject(entry.getKey(), Object.class);
                    row.put(entry.getValue(), object);
                }

                rows.add(row);
            }

            @Override
            void validate(final int rowIndex, final List<Throwable> validationErrors, final Source source) {
                if (source.key != null) {
                    validationErrors.add(new ConfigurationException(
                            "The 'key' property is not required for source with the type " + name() + ". The " + indexString(rowIndex) +
                            " source has a key property configured when it should not"));
                }
                if (source.fields.isEmpty()) {
                    validationErrors.add(new ConfigurationException(
                            "The " + indexString(rowIndex) + " source in " + getClass().getSimpleName() + " has an invalid 'fields' " +
                            "parameter defined. There should be at least most one field defined"));
                }
            }

            @Override
            public void addValuesKeys(final Source source, final HashSet<String> sourceKeys) {
                sourceKeys.addAll(source.fields.keySet());
            }
        },
        /**
         * Indicates that the object is a DataSource and each row in it should be expanded to be a row in the output table.
         * <p>
         * If the datasource does not exist or is null then this source will be skipped
         * </p>
         * <p>
         * The fields parameter of the source should contain all the fields to pull from the source DataSource.  Not all
         * Fields need to be declared.  For example if the source has 5 fields not all of them need to be in the resulting
         * merged datasource.
         * </p>
         */
        DATASOURCE {
            @Override
            void add(final List<Map<String, ?>> rows, final Values values, final Source source) throws JRException {
                JRDataSource dataSource = values.getObject(source.key, JRDataSource.class);
                Assert.isTrue(dataSource != null, "The Datasource object referenced by key: " + source.key + " does not exist.  Check" +
                                                  " that the key is correctly spelled in the config.yaml file.\n\t This is one of the" +
                                                  " sources for the !mergeDataSources.");

                JRDesignField jrField = new JRDesignField();

                while (dataSource.next()) {
                    Map<String, Object> row = Maps.newHashMap();
                    for (Map.Entry<String, String> field : source.fields.entrySet()) {
                        jrField.setName(field.getKey());
                        row.put(field.getValue(), dataSource.getFieldValue(jrField));
                    }
                    rows.add(row);
                }
            }

            @Override
            void validate(final int rowIndex, final List<Throwable> validationErrors, final Source source) {
                if (source.key.isEmpty()) {
                    validationErrors.add(new ConfigurationException(
                            "The " + indexString(rowIndex) + " source in " + MergeDataSourceProcessor.class.getSimpleName() +
                            " needs to have a 'key' parameter defined."));
                }
                if (source.fields.isEmpty()) {
                    validationErrors.add(new ConfigurationException(
                            "The " + indexString(rowIndex) + " source in " + MergeDataSourceProcessor.class.getSimpleName() +
                            " needs to have a 'fields' parameter defined."));
                }

            }

            @Override
            public void addValuesKeys(final Source source, final HashSet<String> sourceKeys) {
                sourceKeys.add(source.key);
            }
        };

        abstract void add(List<Map<String, ?>> rows, Values values, Source source) throws JRException;

        abstract void validate(int rowIndex, List<Throwable> validationErrors, Source source);

        abstract void addValuesKeys(Source source, HashSet<String> sourceKeys);
    }
}
