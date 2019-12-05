package org.mapfish.print.config;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;

import java.util.Map;

/**
 * The interface to SnakeYaml that is responsible for creating the different objects during parsing the config
 * yaml files.
 *
 * The objects are created using spring dependency injection so that the methods are correctly wired using
 * spring.
 *
 * If an object has the interface HashConfiguration then this class will inject the configuration object after
 * creating the object.
 *
 */
public final class MapfishPrintConstructor extends Constructor {
    private static final String CONFIGURATION_TAG = "configuration";
    private static final ThreadLocal<Configuration> CONFIGURATION_UNDER_CONSTRUCTION =
            new InheritableThreadLocal<>();
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MapfishPrintConstructor.class);
    private final ConfigurableApplicationContext context;

    /**
     * Constructor.
     *
     * @param context the application context object for creating
     */
    public MapfishPrintConstructor(final ConfigurableApplicationContext context) {
        super(new TypeDescription(Configuration.class, CONFIGURATION_TAG));
        this.context = context;
        Map<String, ConfigurationObject> yamlObjects = context.getBeansOfType(ConfigurationObject.class);
        for (Map.Entry<String, ConfigurationObject> entry: yamlObjects.entrySet()) {
            final BeanDefinition beanDefinition = context.getBeanFactory().getBeanDefinition(entry.getKey());
            if (!beanDefinition.isPrototype()) {
                final String message =
                        "Error: Spring bean: " + entry.getKey() + " is not defined as scope = prototype";
                LOGGER.error(message);
                throw new AssertionError(message);
            }
            addTypeDescription(new TypeDescription(entry.getValue().getClass(), "!" + entry.getKey()));
        }
    }

    static void setConfigurationUnderConstruction(final Configuration config) {
        CONFIGURATION_UNDER_CONSTRUCTION.set(config);
    }

    @Override
    protected Object newInstance(final Node node) {
        if (node.getType() == Configuration.class) {
            return CONFIGURATION_UNDER_CONSTRUCTION.get();
        }
        Object bean;
        try {
            bean = this.context.getBean(node.getTag().getValue().substring(1));
        } catch (NoSuchBeanDefinitionException e) {
            bean = this.context.getBean(node.getType());
        }

        if (bean instanceof HasConfiguration) {
            ((HasConfiguration) bean).setConfiguration(CONFIGURATION_UNDER_CONSTRUCTION.get());
        }

        return bean;
    }
}
