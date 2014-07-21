import java.lang.reflect.Field
import java.lang.reflect.Method
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
 * Strategy for parsing out the field, class and method descriptions from Javadoc 7 compatible javadocs.
 *
 * @author Jesse on 7/13/2014.
 */
class Javadoc7Parser {
    File javadocDir;

    String findFieldDescription(String objectName, Class cls, Field field) {
        if (field == null || field.name == null) {
            throw new Error("field is null");
        }

        findJavadocDescription(objectName, cls, field.name, Javadoc7Parser.&unableToFindFieldError)
    }

    static String unableToFindFieldError(String field, String objectName) {
        return "Unable to find javadoc for field '$field' in '$objectName'"
    }

    String findMethodDescription(String objectName, Class cls, Method method) {
        if (method == null || method.name == null) {
            throw new Error("method is null");
        }
        findJavadocDescription(objectName, cls, method.name, Javadoc7Parser.&unableToFindMethodError)
    }
    String findJavadocDescription(String objectName, Class cls, String obj, Closure errorHandler) {
        try {
            def html = loadJavadocFile(cls)

            def contentContainer = html.depthFirst().find { it.name() == 'div' && it.@class == 'contentContainer' }
            def detailsEl = contentContainer.depthFirst().find { it.name() == 'div' && it.@class == 'details' }

            def descDiv = detailsEl.depthFirst().find {

                if (it.name() == "li") {
                    def h4 = it.h4
                    return h4.size() > 0 && !h4.text().isEmpty() && obj == h4.text()
                } else {
                    return false
                }
            }


            def firstDescriptionDiv = descDiv.div[0]
            return DocsXmlSupport.toXmlString(firstDescriptionDiv)
        } catch (Exception e) {
            if (cls.getSuperclass() != null) {
                try {
                    return findJavadocDescription(objectName, cls.getSuperclass(), obj, errorHandler)
                } catch (Exception iae) {
                    throw new IllegalArgumentException(errorHandler(obj, objectName))
                }
            } else {
                throw new IllegalArgumentException(errorHandler(obj, objectName))
            }
        }
    }

    static String unableToFindMethodError(String method, String objectName) {
        return "Unable to find javadoc for method '$method' in '$objectName'"
    }


    String findClassDescription(Class cls) {
        def html = loadJavadocFile(cls)

        def contentContainer = html.depthFirst().find{it.name() == 'div' && it.@class == 'contentContainer'}
        def descriptionEl = contentContainer.depthFirst().find{it.name() == 'div' && it.@class == 'description'}
        return DocsXmlSupport.toXmlString(descriptionEl.ul.li.div[0])
    }
    def xmlCache = [:]
    def loadJavadocFile(Class cls) {
        if (xmlCache.containsKey(cls)) {
            return xmlCache.get(cls)
        }
        XmlSlurper slurper = DocsXmlSupport.createHtmlSlurper()
        def javadocFile = new File(javadocDir, cls.name.replace(".", File.separator).replace("\$", ".") + ".html")

        def xml;
        javadocFile.withReader "UTF-8", {
            xml = slurper.parse(it)
        }
        xmlCache.put(cls, xml)
        return xml
    }


}
