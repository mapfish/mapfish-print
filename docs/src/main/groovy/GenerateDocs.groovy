import com.google.common.collect.HashMultimap
import com.google.common.io.Files
import java.io.FileReader;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template as MustacheTemplate;
import org.apache.commons.io.FileUtils;
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
import groovy.json.JsonSlurper

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
            handleMapLayerFactoryPlugin(entry.getValue(), "!" + entry.getKey())
        }
        springAppContext.getBeansOfType(Attribute.class, true, true).entrySet().each { entry ->
            handleAttribute(entry.getValue(), "!" + entry.getKey())
        }
        springAppContext.getBeansOfType(Processor.class, true, true).entrySet().each { entry ->
            handleProcessor(entry.getValue(), "!" + entry.getKey())
        }
        springAppContext.getBeansOfType(StyleParserPlugin.class).entrySet().each { entry ->
            handleSimplePlugin(entry.getValue(), 'styles', entry.getKey())
        }
        springAppContext.getBeansOfType(OutputFormat.class).entrySet().each { entry ->
            OutputFormat bean = entry.getValue()
            handleSimplePlugin(bean, 'outputformats', bean.contentType)
        }
        springAppContext.getBeansOfType(ConfigFileLoaderPlugin.class).entrySet().each { entry ->
            ConfigFileLoaderPlugin bean = entry.getValue()
            if (bean.class != ConfigFileLoaderManager.class) {
                handleSimplePlugin(bean, 'fileloaders', bean.uriScheme)
            }
        }
        springAppContext.getBeansWithAnnotation(Service.class).entrySet().each { entry ->
            handleApi(entry.getValue(), entry.getKey())
        }
        springAppContext.getBeansOfType(ConfigurationObject.class, true, true).entrySet().each { entry ->
            def bean = entry.getValue()
            if (!(bean instanceof Attribute || bean instanceof MapLayerFactoryPlugin || bean instanceof Processor)) {
                handleConfigurationObject(entry.getValue(),"!" + entry.getKey())
            }
        }

        springAppContext.stop()

        def gitRev =  'git rev-parse HEAD'.execute().text
        Map<String, String> version = new HashMap<String, String>();
        version.put("short", gitRev.substring(0, 8));
        version.put("long", gitRev);

        def siteConfigFile = new File(GenerateDocs.class.getResource("pages.json").toURI())
        def siteConfig = new JsonSlurper().parse(new FileReader(siteConfigFile))

        def siteDirectory = args[0]

        File mainTemplateFile = new File(GenerateDocs.class.getResource("/templates/_main.html").toURI());
        MustacheTemplate mainTemplate = Mustache.compiler().
            escapeHTML(false).
            defaultValue("").
            compile(new FileReader(mainTemplateFile));
        File subNavTemplateFile = new File(GenerateDocs.class.getResource("/templates/_sub_nav.html").toURI());
        MustacheTemplate subNavTemplate = Mustache.compiler().
            escapeHTML(false).
            defaultValue("").
            compile(new FileReader(subNavTemplateFile));
        File contentTemplateFile = new File(GenerateDocs.class.getResource("/templates/_content.html").toURI());
        MustacheTemplate contentTemplate = Mustache.compiler().
            escapeHTML(false).
            defaultValue("").
            compile(new FileReader(contentTemplateFile));

        plugins.asMap().each {key, value ->
            write(value as List, key, siteDirectory,
                mainTemplate, subNavTemplate, contentTemplate, siteConfig.generated,
                version);
        }

        siteConfig.pages.each { key, title ->
            createPage(key, title, siteDirectory,
                mainTemplate, subNavTemplate, contentTemplate,
                version)
        }
        System.exit(0)
    }
    static void write (Collection<Record> records, String varName, String siteDirectory,
            MustacheTemplate mainTemplate, MustacheTemplate subNavTemplate,
            MustacheTemplate contentTemplate,
            Object configGeneratedPages, Map<String, String> version ) {
        records.sort({ a, b -> a.title <=> b.title })
        new File(siteDirectory, varName + ".html").withPrintWriter "UTF-8", { pageWriter ->

              String description = FileUtils.readFileToString(
                  new File(GenerateDocs.class.getResource("/templates/" + varName + ".html").toURI())
              );

              // toc
              List<Map> entries = new ArrayList<Map>();
              records.eachWithIndex { record, idx ->
                  Map<String, String> entry = new HashMap<String, String>();
                  entry.put("key", record.title);
                  entry.put("title", record.title);
                  entries.add(entry);
              }
              Map<String, Object> dataSubNav = new HashMap<String, Object>();
              dataSubNav.put("entries", entries);
              String subNav = subNavTemplate.execute(dataSubNav);

              // content
              Map<String, String> contentData = new HashMap<String, String>();
              contentData.put("description", description);
              contentData.put("records", records);
              String content = contentTemplate.execute(contentData)

              Map<String, String> data = new HashMap<String, String>();
              data.put("content", content);
              data.put("pageTitle", configGeneratedPages.get(varName));
              data.put("current_" + varName, "current");
              data.put("sub_nav_" + varName, subNav);
              data.put("version", version);
              pageWriter.append(mainTemplate.execute(data));
        }
    }
    static void createPage (String key, String title, String siteDirectory,
            MustacheTemplate mainTemplate, MustacheTemplate subNavTemplate,
            MustacheTemplate contentTemplate, Map<String, String> version) {
        new File(siteDirectory, key + ".html").withPrintWriter "UTF-8", { pageWriter ->

              String description = FileUtils.readFileToString(
                  new File(GenerateDocs.class.getResource("/templates/" + key + ".html").toURI())
              );

              // toc
              Map<String, Object> dataSubNav = new HashMap<String, Object>();
              dataSubNav.put("entries", []);
              String subNav = subNavTemplate.execute(dataSubNav);

              // content
              Map<String, String> contentData = new HashMap<String, String>();
              contentData.put("description", description);
              contentData.put("records", []);
              String content = contentTemplate.execute(contentData)

              Map<String, String> data = new HashMap<String, String>();
              data.put("content", content);
              data.put("pageTitle", title);
              data.put("current_" + key, "current");
              data.put("sub_nav_" + key, subNav);
              data.put("version", version);
              pageWriter.append(mainTemplate.execute(data));
        }
    }
    static void handleConfigurationObject(ConfigurationObject bean, String beanName) {
        if (bean instanceof Attribute || bean instanceof MapLayerFactoryPlugin) {
            return;
        }
        List<Detail> details = findAllConfigurationDetails(bean, beanName)
        def desc = cleanUpCodeTags(javadocParser.findClassDescription(bean.getClass()))
        plugins.put('configuration', new Record([title:beanName, desc:desc, details: details]))
    }

    private static List<Detail> findAllConfigurationDetails(bean, String beanName) {
        def descriptors = BeanUtils.getPropertyDescriptors(bean.getClass())
        def details = descriptors.findAll { it.writeMethod != null && !"configName".equals(it.displayName)}.collect { desc ->
            def title = desc.displayName
            def detailDesc = cleanUpCodeTags(javadocParser.findMethodDescription(beanName, bean.getClass(), desc.writeMethod))
            return new Detail([title: title, desc: detailDesc])
        }
        details.sort({ a, b -> a.title <=> b.title })
        details
    }

    static void handleSimplePlugin(Object bean, String javascriptVarName, String title) {
        def desc = cleanUpCodeTags(javadocParser.findClassDescription(bean.getClass()))
        plugins.put(javascriptVarName, new Record([title:title, desc:desc]))
    }

    static void handleMapLayerFactoryPlugin(MapLayerFactoryPlugin<?> bean, String beanName) {
        def parseMethod = bean.class.methods.findAll { it.name == "parse" && it.returnType.simpleName != 'MapLayer' }[0]
        if (parseMethod == null) {
            throw new AssertionError("\nBean " + beanName + " needs to have the return type of the parse method be the specific type, not the generic MapLayer type.")
        }
        def layerType = parseMethod.returnType
        if (layerType.simpleName.equals("FeatureLayer")) {
            // for internal use only
            return;
        }
        List<Detail> details = findAllConfigurationDetails(bean, beanName)
        def desc = cleanUpCodeTags(javadocParser.findClassDescription(bean.getClass()))
        def input = findAllAttributes(bean.createParameter().class, beanName)
        plugins.put('layers', new Record([
                title:layerType.simpleName.replaceAll(/([A-Z][a-z])/, ' $1').trim(),
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
        def desc = cleanUpCodeTags(javadocParser.findClassDescription(bean.getClass()))
        plugins.put('attributes', new Record([title:beanName, desc: desc, details: details, input: input]))
    }

    private static Collection<Detail> findAllAttributes(Class cls, String beanName) {
        def details = []
        ParserUtils.getAllAttributes(cls).each { att ->
            def desc = cleanUpCodeTags(javadocParser.findFieldDescription(beanName, cls, att))
            def required = att.getAnnotation(HasDefaultValue.class) == null
            def annotations = att.getAnnotations().collect { it.toString() }
            def rec = new Detail([
                    title      : att.name,
                    desc       : desc,
                    required   : required,
                    annotations: annotations
            ])

            details << rec
        }
        details.sort({ a, b -> a.title <=> b.title })
        return details
    }

    static void handleProcessor(Processor bean, String beanName) {
        def desc = cleanUpCodeTags(javadocParser.findClassDescription(bean.getClass()))
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
                    desc: cleanUpCodeTags(javadocParser.findMethodDescription(beanName, bean.getClass(), apiMethod)),
            ])
        }

        details.sort({ a, b -> a.title <=> b.title })
        plugins.put('api', new Record([title: beanName.replaceAll(/API/, ' API'),
                            desc: cleanUpCodeTags(javadocParser.findClassDescription(bean.getClass())),
                            details: details,
                            translateTitle: true
        ]))
    }

    static def escape(String string) {
        return string.replaceAll("\\r|\\n|\"|\\\\") {it == "\n" || it == "\r" ? " " : "\\$it"}
    }
    static String escapeTranslationId(id) {
        return id.replace("\\", "")
    }
    static String cleanUpCodeTags(String desc) {
      return desc
        .replaceAll("<pre><code>", "<div class=\"highlight\"><pre>")
        .replaceAll("<div class=\"highlight\"><pre><br>", "<div class=\"highlight\"><pre>")
        .replaceAll("</code></pre>", "</pre></div>")
        .replaceAll("<br> </pre></div>", "</pre></div>")
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

        public boolean hasDetails() {
            return !this.details.isEmpty();
        }

        public boolean hasInputs() {
            return !this.input.isEmpty();
        }

        public boolean hasOutputs() {
            return !this.output.isEmpty();
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
