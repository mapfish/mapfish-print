import com.google.common.collect.HashMultimap
import com.google.common.io.Files
import groovy.json.JsonBuilder
import org.mapfish.print.attribute.Attribute
import org.mapfish.print.attribute.ReflectiveAttribute
import org.mapfish.print.config.ConfigurationObject
import org.mapfish.print.config.Template
import org.mapfish.print.map.MapLayerFactoryPlugin
import org.mapfish.print.map.style.StyleParserPlugin
import org.mapfish.print.output.OutputFormat
import org.mapfish.print.parser.HasDefaultValue
import org.mapfish.print.parser.ParserUtils
import org.mapfish.print.processor.Processor
import org.mapfish.print.servlet.fileloader.ConfigFileLoaderManager
import org.mapfish.print.servlet.fileloader.ConfigFileLoaderPlugin
import org.mapfish.print.test.util.AttributeTesting
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
    static HashMultimap<String, Record> plugins = HashMultimap.create()

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
        springAppContext.getBeansOfType(StyleParserPlugin.class).entrySet().each { entry ->
            handleSimplePlugin(entry.getValue(), 'styles', entry.getKey())
        }
        springAppContext.getBeansOfType(OutputFormat.class).entrySet().each { entry ->
            OutputFormat bean = entry.getValue()
            handleSimplePlugin(bean, 'outputFormats', bean.contentType)
        }
        springAppContext.getBeansOfType(ConfigFileLoaderPlugin.class).entrySet().each { entry ->
            ConfigFileLoaderPlugin bean = entry.getValue()
            if (bean.class != ConfigFileLoaderManager.class) {
                handleSimplePlugin(bean, 'fileLoaders', bean.uriScheme)
            }
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

        def siteDirectory = args[0]
        new File(siteDirectory, "generated-data.js").withPrintWriter "UTF-8", { printWriter ->
            new File(siteDirectory, "strings-en.json").withPrintWriter "UTF-8", {strings ->

                strings.append(GenerateDocs.class.classLoader.getResource("strings-en.json").getText("UTF-8").trim())

                def file = new File(GenerateDocs.class.getResource("/index.html").toURI())
                file = new File(file.getParentFile(), "long-strings")
                Files.fileTreeTraverser().children(file).each {stringsFile ->
                    def name = Files.getNameWithoutExtension(stringsFile.getName())

                    strings.append(",\n  \"")
                    strings.append(name)
                    strings.append("\" : \"")
                    def text = stringsFile.getText("UTF-8").replaceAll(/\s?(\n|\r)\s?/, " ").replaceAll(/\\|"/) {"\\$it"}
                    strings.append(text)
                    strings.append("\"")
                }
                plugins.asMap().each {key, value ->
                    write(value as List, printWriter, strings, key)
                }

                strings.append("\n}")
            }
        }
    }
    static void write (Collection<Record> records, PrintWriter printWriter, PrintWriter strings, String varName) {
        printWriter.append("docs.")
        printWriter.append(varName)
        printWriter.append(" = [")
        records.eachWithIndex { record, idx ->
            if (idx > 0) {
                printWriter.append(",\n")
            }
            printWriter.append(record.json())

            record.translations().each { key, value ->
                strings.append(",\n  \"")
                strings.append(key)
                strings.append("\" : \"")
                strings.append(value)
                strings.append("\"")
            }
        }
        printWriter.append("\n];\n\n")


    }
    static void handleConfigurationObject(ConfigurationObject bean, String beanName) {
        if (bean instanceof Attribute || bean instanceof MapLayerFactoryPlugin) {
            return;
        }
        List<Detail> details = findAllConfigurationDetails(bean, beanName)
        def desc = javadocParser.findClassDescription(bean.getClass())
        plugins.put('config', new Record([title:beanName, desc:desc, details: details]))
    }

    private static List<Detail> findAllConfigurationDetails(bean, String beanName) {
        def descriptors = BeanUtils.getPropertyDescriptors(bean.getClass())
        def details = descriptors.findAll { it.writeMethod != null }.collect { desc ->
            def title = desc.displayName.replaceAll(/([A-Z][a-z])/, ' $1').capitalize()
            def detailDesc = javadocParser.findMethodDescription(beanName, bean.getClass(), desc.writeMethod)
            return new Detail([title: title, desc: detailDesc])
        }
        details
    }

    static void handleSimplePlugin(Object bean, String javascriptVarName, String title) {
        def desc = javadocParser.findClassDescription(bean.getClass())
        plugins.put(javascriptVarName, new Record([title:title, desc:desc]))
    }

    static void handleMapLayerFactoryPlugin(MapLayerFactoryPlugin<?> bean, String beanName) {
        def parseMethod = bean.class.methods.findAll { it.name == "parse" && it.returnType.simpleName != 'MapLayer' }[0]
        if (parseMethod == null) {
            throw new AssertionError("\nBean " + beanName + " needs to have the return type of the parse method be the specific type, not the generic MapLayer type.")
        }
        def layerType = parseMethod.returnType
        List<Detail> details = findAllConfigurationDetails(bean, beanName)
        def desc = javadocParser.findClassDescription(bean.getClass())
        def input = findAllAttributes(bean.createParameter().class, beanName)
        plugins.put('mapLayers', new Record([
                title:layerType.simpleName.replaceAll(/([A-Z][a-z])/, ' $1'),
                desc: desc,
                details: details,
                input: input,
                translateTitle: true
        ]))
    }
    static void handleAttribute(Attribute bean, String beanName) {
        def input = []
        AttributeTesting.configureAttributeForTesting(bean);
        if (bean instanceof ReflectiveAttribute) {
            input = findAllAttributes(bean.createValue(new Template()).class, beanName)
        }
        List<Detail> details = findAllConfigurationDetails(bean, beanName)
        def desc = javadocParser.findClassDescription(bean.getClass())
        plugins.put('attributes', new Record([title:beanName, desc: desc, details: details, input: input]))
    }

    private static Collection<Detail> findAllAttributes(Class cls, String beanName) {
        def details = []
        ParserUtils.getAllAttributes(cls).each { att ->
            def desc = javadocParser.findFieldDescription(beanName, cls, att)
            def required = att.getAnnotation(HasDefaultValue.class) != null
            def annotations = att.getAnnotations().collect { it.toString() }
            def rec = new Detail([
                    title      : att.name,
                    desc       : desc,
                    required   : required,
                    annotations: annotations
            ])

            details << rec
        }

        return details
    }

    static void handleProcessor(Processor bean, String beanName) {
        def desc = javadocParser.findClassDescription(bean.getClass())
        List<Detail> details = findAllConfigurationDetails(bean, beanName)
        def input = findAllAttributes(bean.createInputParameter().class, beanName)
        def output = findAllAttributes(bean.outputType, beanName)
        plugins.put('processors', new Record([title:beanName, desc: desc, details: details,  input: input, output: output]))
    }
    static void handleApi(Object bean, String beanName) {
        def details = bean.getClass().methods.findAll{it.getAnnotation(RequestMapping.class) != null}.collectAll {apiMethod ->
            def mapping = apiMethod.getAnnotation(RequestMapping.class)
            def method = mapping.method().length  > 0 ? mapping.method()[0] : RequestMethod.GET
            method = method != null ? method.name() : RequestMethod.GET.name()
            def title =  "${mapping.value()[0]} ($method)"
            return new Detail([
                    title: title,
                    desc: javadocParser.findMethodDescription(beanName, bean.getClass(), apiMethod),
            ])
        }

        plugins.put('api', new Record([title: beanName.replaceAll(/API/, ' API'),
                            desc: javadocParser.findClassDescription(bean.getClass()),
                            details: details,
                            translateTitle: true
        ]))
    }

    static def escape(String string) {
        return string.replaceAll("\\r|\\n|\"|\\\\") {it == "\n" || it == "\r" ? " <br/>" : "\\$it"}
    }
    static String escapeTranslationId(id) {
        return id.replace("\\", "")
    }
    static class Record {
        String title, desc
        boolean translateTitle = false
        List<Detail> details = []
        List<Detail> input = []
        List<Detail> output = []
        public String json() {
            def record = this
            def builder = new JsonBuilder()
            builder {
                title (translateTitle ? translationId("title") : title)
                desc (translationId("desc"))
                summaryDesc (translationId("summaryDesc"))
                details (details.collect{it.json(record, "detail")})
                input (input.collect{it.json(record, "input")})
                output (output.collect{it.json(record, "output")})
                translateTitle (translateTitle)
            }

            return builder.toPrettyString()
        }
        private String translationId(id) {
            escapeTranslationId("record/$title/$id")
        }
        public Map translations() {
            def record = this
            def translations = [:]
            if (translateTitle) {
                translations[translationId("title")] = escape(title)
            }

            translations[translationId("desc")] = escape(desc)
            translations[translationId("summaryDesc")] = escape(summary())
            details.each {it.translations(record, "detail", translations)}
            input.each {it.translations(record, "input", translations)}
            output.each {it.translations(record, "output", translations)}

            return translations
        }

        private String summary() {
            def xml = DocsXmlSupport.createHtmlSlurper().parseText(desc).body.div
            def text = xml.text()
            def indexOfPeriod = text.indexOf(".")

            if (indexOfPeriod > -1) {
                return text.substring(0, indexOfPeriod).replaceAll("\n", " ")
            } else {
                return text;
            }
        }
    }

    static class Detail {
        String title, desc
        boolean translateTitle = false
        boolean required = false
        List<String> annotations = []
        public Object json(record, type) {
            def jsonObj = [
                title : translateTitle ? translationId(record, type, "title") : title,
                desc : translationId(record, type, "desc"),
                required : required,
                translateTitle: translateTitle,
                annotations : annotations.collectAll {escape(it)}
            ]
            return jsonObj
        }

        private String translationId(record, type, id) {
            escapeTranslationId("record/$record.title/$type/$title/$id")
        }

        public void translations(record, type, translations) {
            if (translateTitle) {
                translations[translationId(record, type, "title")] = escape(title)
            }
            translations[translationId(record, type, "desc")] = escape(desc)
        }

    }

}