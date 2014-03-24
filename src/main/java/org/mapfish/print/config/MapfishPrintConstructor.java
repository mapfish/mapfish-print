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

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeId;

import java.util.Map;

/**
 * Created by Jesse on 3/24/14.
 */
public final class MapfishPrintConstructor extends Constructor {
    private static final String CONFIGURATION_TAG = "configuration";
    private final MapfishPrintConstruct construct;

    /**
     * Constructor.
     *
     * @param context the application context object for creating
     */
    public MapfishPrintConstructor(final ApplicationContext context) {
        super(new TypeDescription(Configuration.class, CONFIGURATION_TAG));
        Map<String, ConfigurationObject> yamlObjects = context.getBeansOfType(ConfigurationObject.class);
        for (Map.Entry<String, ConfigurationObject> entry : yamlObjects.entrySet()) {
            addTypeDescription(new TypeDescription(entry.getValue().getClass(), entry.getKey()));
        }

        this.construct = new MapfishPrintConstruct(context);
        super.yamlClassConstructors.put(NodeId.mapping, this.construct);
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
            try {
                return this.applicationContext.getBean(node.getTag().getValue());
            } catch (NoSuchBeanDefinitionException e) {
                return this.applicationContext.getBean(node.getType());
            }
        }
    }
}
