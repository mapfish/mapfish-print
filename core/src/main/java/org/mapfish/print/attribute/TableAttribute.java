package org.mapfish.print.attribute;

import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.wrapper.PArray;

import java.util.List;

/**
 * <p>The attributes for {@link org.mapfish.print.processor.jasper.TableProcessor} (see
 * <a href="processors.html#!prepareTable">!prepareTable</a> processor).</p>
 * [[examples=verboseExample,datasource_dynamic_tables,customDynamicReport]]
 */
public final class TableAttribute extends ReflectiveAttribute<TableAttribute.TableAttributeValue> {
    @Override
    public Class<TableAttributeValue> getValueType() {
        return TableAttributeValue.class;
    }

    @Override
    public TableAttributeValue createValue(final Template template) {
        return new TableAttributeValue();
    }

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        // no checks required
    }

    /**
     * The value of {@link org.mapfish.print.attribute.TableAttribute}.
     */
    public static final class TableAttributeValue {

        /**
         * The column configuration names for the table.
         */
        public String[] columns;
        /**
         * An array for each table row.
         */
        public PArray[] data;
    }
}
