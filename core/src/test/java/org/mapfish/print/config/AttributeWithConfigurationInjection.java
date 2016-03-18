package org.mapfish.print.config;

import org.mapfish.print.attribute.PrimitiveAttribute;

import static org.junit.Assert.assertNotNull;

/**
 * Attribute that needs the configuration object injected.
 *
 * @author jesseeichar on 3/25/14.
 */
public class AttributeWithConfigurationInjection extends PrimitiveAttribute<Integer> implements HasConfiguration {

    private Configuration configuration;

    /**
     * Constructor.
     */
    public AttributeWithConfigurationInjection() {
        super(Integer.class);
    }

    public void assertInjected() {
        assertNotNull(configuration);
    }

    @Override
    public void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }

}
