import com.google.common.collect.HashMultimap
import com.samskivert.mustache.Mustache
import com.samskivert.mustache.Template as MustacheTemplate
import groovy.io.FileType
import groovy.json.JsonSlurper
import org.apache.commons.io.FileUtils
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
import org.springframework.web.context.support.XmlWebApplicationContext

import java.lang.reflect.Modifier

class GenerateDocs {
    static def javadocParser;
    static HashMultimap<String, Record> plugins = HashMultimap.create()
    static def examplePattern = ~/\[\[examples=(.*?)]]/
    static def Set<String> availableExamples = null

    public static void main(String[] args) {
        GenerateDocs.availableExamples = initExamples()
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
        springAppContext.getBeansOfType(MapLayerFactoryPlugin.class, true, true).entrySet().each { entry ->
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
        springAppContext.getBeansOfType(ConfigurationObject.class, true, true).entrySet().each { entry ->
            def bean = entry.getValue()
            if (!(bean instanceof Attribute || bean instanceof MapLayerFactoryPlugin || bean instanceof Processor)) {
                handleConfigurationObject(entry.getValue(), "!" + entry.getKey())
            }
        }

        springAppContext.stop()

        def gitRev = 'git rev-parse HEAD'.execute().text
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

        plugins.asMap().each { key, value ->
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

    static void write(Collection<Record> records, String varName, String siteDirectory,
                      MustacheTemplate mainTemplate, MustacheTemplate subNavTemplate,
                      MustacheTemplate contentTemplate,
                      Object configGeneratedPages, Map<String, String> version) {
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

    static void createPage(String key, String title, String siteDirectory,
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
        def examples = getExamples(desc)
        desc = desc.replaceAll(examplePattern, "")
        plugins.put('configuration', new Record([
                title   : beanName,
                desc    : desc,
                details : details,
                examples: examples]))
    }

    private static List<Detail> findAllConfigurationDetails(bean, String beanName) {
        def descriptors = BeanUtils.getPropertyDescriptors(bean.getClass())
        def details = descriptors.findAll {
            it.writeMethod != null && !"configName".equals(it.displayName)
        }.collect { desc ->
            def title = desc.displayName
            def detailDesc = cleanUpCodeTags(javadocParser.findMethodDescription(beanName, bean.getClass(), desc.writeMethod))
            return new Detail([title: title, desc: detailDesc])
        }
        details.sort({ a, b -> a.title <=> b.title })
        details
    }

    static void handleSimplePlugin(Object bean, String javascriptVarName, String title) {
        def desc = cleanUpCodeTags(javadocParser.findClassDescription(bean.getClass()))
        def examples = getExamples(desc)
        desc = desc.replaceAll(examplePattern, "")
        plugins.put(javascriptVarName, new Record([
                title   : title,
                desc    : desc,
                examples: examples]))
    }

    static void handleMapLayerFactoryPlugin(MapLayerFactoryPlugin<?> bean, String beanName) {
        def parseMethod = bean.class.methods.findAll {
            it.name == "parse" && it.returnType.simpleName != 'MapLayer'
        }[0]
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
        def examples = getExamples(desc)
        desc = desc.replaceAll(examplePattern, "")
        plugins.put('layers', new Record([
                title   : layerType.simpleName.replaceAll(/([A-Z][a-z])/, ' $1').trim(),
                desc    : desc,
                details : details,
                input   : input,
                examples: examples
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
        def examples = getExamples(desc)
        desc = desc.replaceAll(examplePattern, "")
        plugins.put('attributes', new Record([
                title   : beanName,
                desc    : desc,
                examples: examples,
                details : details,
                input   : input]))
    }

    private static Collection<Detail> findAllAttributes(Class cls, String beanName) {
        def details = []
        ParserUtils.getAllAttributes(cls).each { att ->
            if (!Modifier.isStatic(att.getModifiers())) {
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
        }
        details.sort({ a, b -> a.title <=> b.title })
        return details
    }

    static void handleProcessor(Processor bean, String beanName) {
        def desc = cleanUpCodeTags(javadocParser.findClassDescription(bean.getClass()))
        List<Detail> details = findAllConfigurationDetails(bean, beanName)
        def input = findAllAttributes(bean.createInputParameter().class, beanName)
        def output = findAllAttributes(bean.outputType, beanName)
        def examples = getExamples(desc)
        desc = desc.replaceAll(examplePattern, "")
        plugins.put('processors', new Record([
                title   : beanName,
                desc    : desc,
                details : details,
                input   : input,
                output  : output,
                examples: examples]))
    }

    private static List<String> getExamples(String desc) {
        def examples = new ArrayList<String>();
        desc.find(examplePattern) { match, rawExamples ->
            examples.addAll(rawExamples.split(","));
        }
        examples = examples.collect { it.trim() }
        examples.each { example ->
            if (!(example in GenerateDocs.availableExamples)) {
                throw new Exception(
                        "Example " + example + " does not exist in " +
                                System.getProperty("path_to_examples"));
            }
        }
        return examples;
    }

    static String cleanUpCodeTags(String desc) {
        return desc
                .replaceAll("<pre><code>", "<div class=\"highlight\"><pre>")
                .replaceAll("<div class=\"highlight\"><pre><br>", "<div class=\"highlight\"><pre>")
                .replaceAll("</code></pre>", "</pre></div>")
                .replaceAll("<br> </pre></div>", "</pre></div>")
    }

    static Set<String> initExamples() {
        final File examplesDir = new File(System.getProperty("path_to_examples"))
        def examples = []

        examplesDir.eachFileRecurse(FileType.DIRECTORIES) { dir ->
            examples << dir.getName()
        }

        return examples.toSet()
    }

    static class Record {
        String title, desc
        List<String> examples = []
        List<Detail> details = []
        List<Detail> input = []
        List<Detail> output = []

        public boolean hasDetails() {
            return !this.details.isEmpty();
        }

        public boolean hasInputs() {
            return !this.input.isEmpty();
        }

        public boolean hasOutputs() {
            return !this.output.isEmpty();
        }

        public boolean hasExamples() {
            return this.examples != null && !this.examples.isEmpty();
        }
    }

    static class Detail {
        String title, desc
        boolean required = false
        List<String> annotations = []

    }

}
