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

package org.mapfish.print.config;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeId;

import java.util.Map;

/**
 * The interface to SnakeYaml that is responsible for creating the different objects during parsing the config yaml files.
 * <p/>
 * The objects are created using spring dependency injection so that the methods are correctly wired using spring.
 * <p/>
 * If an object has the interface HashConfiguration then this class will inject the configuration object after creating the object.
 * <p/>
 * @author jesseeichar on 3/24/14.
 */
public final class MapfishPrintConstructor extends Constructor {
    private static final String CONFIGURATION_TAG = "configuration";
    private static final ThreadLocal<Configuration> CONFIGURATION_UNDER_CONSTRUCTION = new InheritableThreadLocal<Configuration>();
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MapfishPrintConstructor.class);

    /**
     * Constructor.
     *
     * @param context the application context object for creating
     */
    public MapfishPrintConstructor(final ConfigurableApplicationContext context) {
        super(new TypeDescription(Configuration.class, CONFIGURATION_TAG));
        Map<String, ConfigurationObject> yamlObjects = context.getBeansOfType(ConfigurationObject.class);
        for (Map.Entry<String, ConfigurationObject> entry : yamlObjects.entrySet()) {
            final BeanDefinition beanDefinition = context.getBeanFactory().getBeanDefinition(entry.getKey());
            final String message = "Error: Spring bean: " + entry.getKey() + " is not defined as scope = prototype";
            if (!beanDefinition.isPrototype()) {
                LOGGER.error(message);
                throw new AssertionError(message);
            }
            addTypeDescription(new TypeDescription(entry.getValue().getClass(), entry.getKey()));
        }

        MapfishPrintConstruct construct = new MapfishPrintConstruct(context);
        super.yamlClassConstructors.put(NodeId.mapping, construct);
    }


    /**
     * The object that will create the yaml object using the spring application context.
     */
    private final class MapfishPrintConstruct extends ConstructMapping {
        private final ApplicationContext applicationContext;

        private MapfishPrintConstruct(final ApplicationContext context) {
            this.applicationContext = context;
        }

        @Override
        protected Object constructJavaBean2ndStep(final MappingNode node, final Object object) {
            return super.constructJavaBean2ndStep(node, object);
        }

        @Override
        protected Object createEmptyJavaBean(final MappingNode node) {
            if (node.getType() == Configuration.class) {
                return getConfiguration();
            }
            Object bean;
            try {
                bean = this.applicationContext.getBean(node.getTag().getValue());
            } catch (NoSuchBeanDefinitionException e) {
                bean = this.applicationContext.getBean(node.getType());
            }

            if (bean instanceof HasConfiguration) {
                ((HasConfiguration) bean).setConfiguration(getConfiguration());
            }

            return bean;
        }

        private Configuration getConfiguration() {
            return MapfishPrintConstructor.this.CONFIGURATION_UNDER_CONSTRUCTION.get();
        }
    }

    static void setConfigurationUnderConstruction(final Configuration config) {
        CONFIGURATION_UNDER_CONSTRUCTION.set(config);
    }
}
