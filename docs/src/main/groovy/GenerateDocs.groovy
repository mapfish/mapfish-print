import org.ccil.cowan.tagsoup.Parser
import org.mapfish.print.attribute.Attribute
import org.mapfish.print.config.ConfigurationObject
import org.mapfish.print.map.MapLayerFactoryPlugin
import org.springframework.beans.BeanUtils
import org.springframework.mock.web.MockServletContext
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
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
    static def api = []
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
        springAppContext.getBeansOfType(MapLayerFactoryPlugin.class, true, true).entrySet().each {entry ->
            handleMapLayerFactoryPlugin(entry.getValue(), javadocDir, entry.getKey())
        }
        springAppContext.getBeansOfType(Attribute.class, true, true).entrySet().each { entry ->
            handleAttribute(entry.getValue(), javadocDir, entry.getKey())
        }
        springAppContext.getBeansWithAnnotation(Service.class).entrySet().each { entry ->
            handleService(entry.getValue(), javadocDir, entry.getKey())
        }
        springAppContext.getBeansOfType(ConfigurationObject.class, true, true).entrySet().each { entry ->
            handleConfigurationObject(entry.getValue(), javadocDir, entry.getKey())
        }

        springAppContext.stop()

        write(configuration, args[0], 'config')
        write(attributes, args[0], 'attributes')
        write(api, args[0], 'api')
        write(mapLayer, args[0], 'mapLayers')
    }
    static void write (Collection<Record> records, String siteDir, String varName) {
        new File(siteDir, varName + ".js").withPrintWriter "UTF-8", { printWriter ->
            printWriter.append("docs.")
            printWriter.append(varName)
            printWriter.append(" = [")
            printWriter.append(records.join(", "))
            printWriter.append("\n]")
        }
    }
    static void handleConfigurationObject(ConfigurationObject bean, File javadocDir, String beanName) {
        def descriptors = BeanUtils.getPropertyDescriptors(bean.getClass())
        def details = descriptors.findAll{it.writeMethod != null}.collect{desc ->
            return new Detail([title : desc.displayName, desc: ''])
        }
        def desc = findClassDescription(javadocDir, bean.getClass())
        configuration.add(new Record([title:beanName, desc:desc, details: details]))
    }
    static void handleMapLayerFactoryPlugin(MapLayerFactoryPlugin<?> bean, File javadocDir, String beanName) {
        def layerType = bean.class.methods.findAll { it.name == "parse" && it.returnType.simpleName != 'MapLayer'}[0].returnType
        def desc = findClassDescription(javadocDir, bean.getClass())
        mapLayer.add(new Record([title:layerType.simpleName.replaceAll(/([A-Z][a-z])/, ' $1'), desc: desc]))
    }
    static void handleAttribute(Attribute bean, File javadocDir, String beanName) {
        def desc = findClassDescription(javadocDir, bean.getClass())
        attributes.add(new Record([title:beanName, desc: desc]))
    }
    static void handleService(Object bean, File javadocDir, String beanName) {
        bean.getClass().methods.findAll{it.getAnnotation(RequestMapping.class) != null}.each {apiMethod ->
            def mapping = apiMethod.getAnnotation(RequestMapping.class)
            def method = mapping.method().length  > 0 ? mapping.method()[0] : RequestMethod.GET
            method = method != null ? method.name() : RequestMethod.GET.name()
            def title =  "${mapping.value()[0]} ($method)"
            api.add(new Record([title: title, desc: apiMethod.getName()]))
        }
    }

    static String findClassDescription(File javadocDir, Class cls) {
        def html = loadJavadocFile(javadocDir, cls)

        def contentContainer = html.depthFirst().findAll{it.name() == 'div' && it.@class == 'contentContainer'}
        def descriptionEl = contentContainer[0].depthFirst().findAll{it.name() == 'div' && it.@class == 'description'}
        return descriptionEl[0].ul.li.div
    }
    static def loadJavadocFile(File javadocDir, Class cls) {
        def tagsoupParser = new Parser()
        def slurper = new XmlSlurper(tagsoupParser)
        def javadocFile = new File(javadocDir, cls.name.replace(".", File.separator).replace("\$", ".") + ".html")
        return slurper.parse(javadocFile)
    }

    static class Record {
        String title, desc
        List<Detail> details = []

        public String toString() {
            def finalDesc = desc.replace("\n", "\\\n");
            return "{\n  \"title\":\"$title\",\n  \"desc\":\"$finalDesc\",\n  \"details\":[" + details.join(", ") + "]\n  }"
        }
    }

    static class Detail {
        String title, desc

        public String toString() {
            def finalDesc = desc.replace("\n", "\\\n");
            return "{\n    \"title\":\"$title\",\n    \"desc\":\"$finalDesc\"\n    }"
        }
    }
}