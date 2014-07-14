import org.mapfish.print.attribute.Attribute
import org.mapfish.print.attribute.ReflectiveAttribute
import org.mapfish.print.config.ConfigurationObject
import org.mapfish.print.config.Template
import org.mapfish.print.map.MapLayerFactoryPlugin
import org.mapfish.print.parser.HasDefaultValue
import org.mapfish.print.parser.ParserUtils
import org.mapfish.print.processor.Processor
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
    static def javadocParser;
    static def configuration = []
    static def mapLayers = []
    static def attributes = []
    static def api = []
    static def processors = []
    public static void main(String[] args) {
        javadocParser = new Javadoc7Parser(javadocDir: new File(args[1]))

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
            handleMapLayerFactoryPlugin(entry.getValue(), entry.getKey())
        }
        springAppContext.getBeansOfType(Attribute.class, true, true).entrySet().each { entry ->
            handleAttribute(entry.getValue(), entry.getKey())
        }
        springAppContext.getBeansOfType(Processor.class, true, true).entrySet().each { entry ->
            handleProcessor(entry.getValue(), entry.getKey())
        }
        springAppContext.getBeansWithAnnotation(Service.class).entrySet().each { entry ->
            handleApi(entry.getValue(), entry.getKey())
        }
        springAppContext.getBeansOfType(ConfigurationObject.class, true, true).entrySet().each { entry ->
            def bean = entry.getValue()
            if (!(bean instanceof Attribute || bean instanceof MapLayerFactoryPlugin || bean instanceof Processor)) {
                handleConfigurationObject(entry.getValue(), entry.getKey())
            }
        }

        springAppContext.stop()

        new File(args[0], "generated-data.js").withPrintWriter "UTF-8", { printWriter ->
            write(configuration, printWriter, 'config')
            write(attributes, printWriter, 'attributes')
            write(api, printWriter, 'api')
            write(mapLayers, printWriter, 'mapLayers')
            write(processors, printWriter, 'processors')
        }
    }
    static void write (Collection<Record> records, PrintWriter printWriter, String varName) {
        printWriter.append("docs.")
        printWriter.append(varName)
        printWriter.append(" = [")
        printWriter.append(records.join(", "))
        printWriter.append("\n];\n\n")
    }
    static void handleConfigurationObject(ConfigurationObject bean, String beanName) {
        if (bean instanceof Attribute || bean instanceof MapLayerFactoryPlugin) {
            return;
        }
        def descriptors = BeanUtils.getPropertyDescriptors(bean.getClass())
        def details = descriptors.findAll{it.writeMethod != null}.collect{desc ->
            def title = desc.displayName.replaceAll(/([A-Z][a-z])/, ' $1').capitalize()
            def detailDesc = javadocParser.findMethodDescription(beanName, bean.getClass(), desc.writeMethod)
            return new Detail([title : title, desc: detailDesc])
        }
        def desc = javadocParser.findClassDescription(bean.getClass())
        configuration.add(new Record([title:beanName, desc:desc, details: details]))
    }
    static void handleMapLayerFactoryPlugin(MapLayerFactoryPlugin<?> bean, String beanName) {
        def layerType = bean.class.methods.findAll { it.name == "parse" && it.returnType.simpleName != 'MapLayer'}[0].returnType
        def desc = javadocParser.findClassDescription(bean.getClass())
        mapLayers.add(new Record([title:layerType.simpleName.replaceAll(/([A-Z][a-z])/, ' $1'), desc: desc]))
    }
    static void handleAttribute(Attribute bean, String beanName) {
        def details = []
        if (bean instanceof ReflectiveAttribute) {
            def value = bean.createValue(new Template())
            ParserUtils.getAllAttributes(value.class).each {att ->
                def desc = javadocParser.findFieldDescription(value.class, att)
                def required = att.getAnnotation(HasDefaultValue.class) != null
                def annotations = att.getAnnotations().collect {it.toString()}
                def rec = new Detail([
                        title: att.name,
                        desc: desc,
                        required: required,
                        annotations: annotations
                ])

                details << rec
            }
        }
        def desc = javadocParser.findClassDescription(bean.getClass())
        attributes.add(new Record([title:beanName, desc: desc, details: details]))
    }
    static void handleProcessor(Processor bean, String beanName) {
        def desc = javadocParser.findClassDescription(bean.getClass())
        processors.add(new Record([title:beanName, desc: desc]))
    }
    static void handleApi(Object bean, String beanName) {
        def details = bean.getClass().methods.findAll{it.getAnnotation(RequestMapping.class) != null}.collectAll {apiMethod ->
            def mapping = apiMethod.getAnnotation(RequestMapping.class)
            def method = mapping.method().length  > 0 ? mapping.method()[0] : RequestMethod.GET
            method = method != null ? method.name() : RequestMethod.GET.name()
            def title =  "${mapping.value()[0]} ($method)"
            return new Detail([title: title, desc: javadocParser.findMethodDescription(beanName, bean.getClass(), apiMethod)])
        }

        api.add(new Record([title: beanName.replaceAll(/API/, ' API'), desc: javadocParser.findClassDescription(bean.getClass()), details: details]))
    }

    static def escape(String string) {
        return string.replace("\n", "\\\n<br/>").replace("\"", "\\\"");
    }
    static class Record {
        String title, desc
        List<Detail> details = []

        public String toString() {
            def finalDesc = escape(desc);
            return "{\n  \"title\":\"$title\",\n  \"desc\":\"$finalDesc\",\n  \"details\":[" + details.join(", ") + "]\n  }"
        }
    }

    static class Detail {
        String title, desc
        boolean required = false
        List<String> annotations = []

        public String toString() {
            def finalDesc = escape(desc);
            def annotationList = annotations.collectAll {'"' + escape(it) + '"'}.join(',')
            return "{\n    \"title\":\"$title\",\n    \"desc\":\"$finalDesc\",\n    \"required\":$required,\n    \"annotations\":[$annotationList]\n    }"
        }
    }
}