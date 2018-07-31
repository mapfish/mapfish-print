package org.mapfish.print.config;

import com.codahale.metrics.MetricRegistry;
import org.mapfish.print.attribute.PrimitiveAttribute;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertNotNull;

/**
 * Test Attribute.
 */
public class AttributeWithSpringInjection extends PrimitiveAttribute<Integer> {

    @Autowired
    private MetricRegistry registry;

    /**
     * Constructor.
     */
    public AttributeWithSpringInjection() {
        super(Integer.class);
    }

    public void assertInjected() {
        assertNotNull(registry);
    }

    /**
     * <p>A default value for this attribute.</p>
     *
     * @param value The default value.
     */
    public final void setDefault(final Integer value) {
        this.defaultValue = value;
    }

    @Override
    public Class getValueType() {
        return Integer.class;
    }
}
