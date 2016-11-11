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

    public void assertInjected() {
        assertNotNull(registry);
    }
    /**
     * Constructor.
     */
    public AttributeWithSpringInjection() {
        super(Integer.class);
    }
}
