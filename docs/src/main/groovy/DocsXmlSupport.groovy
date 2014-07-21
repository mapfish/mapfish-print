import groovy.util.slurpersupport.NoChildren
import org.ccil.cowan.tagsoup.Parser

import java.lang.reflect.Field

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

/**
 * Helpful xml related methods.
 * @author Jesse on 7/20/2014.
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


                def hrefPrefixIndicatingJavadocLink = '../../../../../org/mapfish/'
                if (node.name() == 'a' && name == 'href' && value.startsWith(hrefPrefixIndicatingJavadocLink)) {

                    value = "javadoc/org/mapfish/" + value.substring(hrefPrefixIndicatingJavadocLink.length())
                    stringBuilder.append(' target="javadocsTab"')
                }

                stringBuilder.append(" ")
                stringBuilder.append(name)
                stringBuilder.append('="')
                stringBuilder.append(value)
                stringBuilder.append('"')
            }

            stringBuilder.append(">")

            def children;
            // need to access inner node because children on this node object only returns children that are nodes.
            // We also want the text elements.  So we get inner node via reflection and execute children on that.
            // if for some reason we can't get the inner node then we will just get the local strings and add then before the
            // nodes.  All the data will be there but possibly in a different order.
            try {
                Field nodeField = node.getClass().getDeclaredField("node")
                nodeField.setAccessible(true)
                def innerNode = nodeField.get(node)
                children = innerNode.children()
            } catch (Throwable) {
                children = node.childNodes()
                node.localText().each {
                    stringBuilder.append("\n").append(it)
                }
            }
            children.each { child ->
                if (child instanceof String) {
                    stringBuilder.append(child)
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
