import org.mapfish.print.attribute.Attribute
import org.mapfish.print.config.ConfigurationObject
import org.mapfish.print.map.MapLayerFactoryPlugin
import org.springframework.beans.BeanUtils
import org.springframework.mock.web.MockServletContext
import org.springframework.stereotype.Service
import org.springframework.web.context.support.XmlWebApplicationContext
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

class GenerateDocs {
    static def configuration = []
    static def mapLayer = []
    static def attributes = []
    static def services = []
    public static void main(String[] args) {
        def javadocDir = new File(args[1])

        XmlWebApplicationContext springAppContext = new XmlWebApplicationContext()
        String[] appContextLocations = new String[args.length - 2]
        for (int i = 2; i < args.length; i++) {
            appContextLocations[i - 2] = args[i]
        }
        springAppContext.setConfigLocations(appContextLocations)

        springAppContext.setServletContext(new MockServletContext())
        springAppContext.refresh()
        springAppContext.start()
        springAppContext.getBeansOfType(MapLayerFactoryPlugin.class).entrySet().each {entry ->
            handleMapLayerFactoryPlugin(entry.getValue(), javadocDir, entry.getKey())
        }
        springAppContext.getBeansOfType(Attribute.class).entrySet().each { entry ->
            handleAttribute(entry.getValue(), javadocDir, entry.getKey())
        }
        springAppContext.getBeansWithAnnotation(Service.class).entrySet().each { entry ->
            handleService(entry.getValue(), javadocDir, entry.getKey())
        }
        springAppContext.getBeansOfType(ConfigurationObject.class).entrySet().each { entry ->
            handleConfigurationObject(entry.getValue(), javadocDir, entry.getKey())
        }

        springAppContext.stop()

        write(configuration, new File(args[0], 'config.json'))
        write(attributes, new File(args[0], 'attributes.json'))
        write(services, new File(args[0], 'services.json'))
        write(mapLayer, new File(args[0], 'mapLayer.json'))
    }
    static void write (Collection<Record> records, File file) {
        file.withPrintWriter "UTF-8", { printWriter ->
            printWriter.append("[")
            printWriter.append(records.join(", "))
            printWriter.append("\n]")
        }
    }
    static void handleConfigurationObject(Object bean, File javadocDir, String beanName) {
        def descriptors = BeanUtils.getPropertyDescriptors(bean.getClass())
        def details = descriptors.findAll{it.writeMethod != null}.collect{desc ->
            return new Detail([title : desc.displayName, desc: ''])
        }
        configuration.add(new Record([title:beanName, details: details]))
    }
    static void handleMapLayerFactoryPlugin(Object bean, File javadocDir, String beanName) {
        mapLayer.add(new Record([title:beanName]))
    }
    static void handleAttribute(Object bean, File javadocDir, String beanName) {
        attributes.add(new Record([title:beanName]))
    }
    static void handleService(Object bean, File javadocDir, String beanName) {
        services.add(new Record([title:beanName]))
    }

    static class Record {
        String title, desc
        List<Detail> details = []

        public String toString() {
            return "{\n  \"title\":\"$title\",\n  \"desc\":\"$desc\",\n  \"details\":[" + details.join(", ") + "]\n  }"
        }
    }

    static class Detail {
        String title, desc

        public String toString() {
            return "{\n    \"title\":\"$title\",\n    \"desc\":\"$desc\"\n    }"
        }
    }
}