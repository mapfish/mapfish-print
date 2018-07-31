import groovy.util.slurpersupport.NoChildren
import org.ccil.cowan.tagsoup.Parser

import java.lang.reflect.Field


/**
 * Helpful xml related methods.
 */
class DocsXmlSupport {
    static String toXmlString(node) {
        def builder = new StringBuilder()
        appendXmlToBuilder(node, builder)
        return builder.toString()
    }

    static String appendXmlToBuilder(node, StringBuilder stringBuilder) {
        stringBuilder.append("<").append(node.name())

        if (node instanceof NoChildren) {
            stringBuilder.append(">")
        } else {
            node.attributes().each { name, value ->


                def hrefPrefixIndicatingJavadocLink = 'org/mapfish/print'
                if (node.name() == 'a' && name == 'href' && value.contains(hrefPrefixIndicatingJavadocLink)) {
                    def pos = value.indexOf(hrefPrefixIndicatingJavadocLink) + hrefPrefixIndicatingJavadocLink.length()
                    value = "javadoc/org/mapfish/print" + value.substring(pos)
                    stringBuilder.append(' target="javadocsTab"')
                }

                stringBuilder.append(" ")
                stringBuilder.append(name)
                stringBuilder.append('="')
                stringBuilder.append(value)
                stringBuilder.append('"')
            }

            stringBuilder.append(">")

            def children
            // need to access inner node because children on this node object only returns children that are nodes.
            // We also want the text elements.  So we get inner node via reflection and execute children on that.
            // if for some reason we can't get the inner node then we will use "children()".
            try {
                Field nodeField = node.getClass().getDeclaredField("node")
                nodeField.setAccessible(true)
                def innerNode = nodeField.get(node)
                children = innerNode.children()
            } catch (Throwable) {
                children = node.children()
            }
            children.each { child ->
                if (child instanceof String) {
                    if (node.name() == 'code') {
                        stringBuilder.append(child.replaceAll("\\r|\\n") { "<br>" })
                    } else {
                        stringBuilder.append(child)
                    }
                } else {
                    appendXmlToBuilder(child, stringBuilder)
                }
            }
        }

        stringBuilder.append("</").append(node.name()).append(">")
    }

    static XmlSlurper createHtmlSlurper() {
        def tagsoupParser = new Parser()
        new XmlSlurper(tagsoupParser)
    }
}
