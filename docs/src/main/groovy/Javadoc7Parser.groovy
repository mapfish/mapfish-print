import org.ccil.cowan.tagsoup.Parser

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

    String findFieldDescription(Class cls, Field field) {
        return "";
    }

    String findMethodDescription(String objectName, Class cls, Method method) {
        if (method == null || method.name == null) {
            throw new Error("method is null");
        }
        try {
            def html = loadJavadocFile(cls)

            def contentContainer = html.depthFirst().find { it.name() == 'div' && it.@class == 'contentContainer' }
            def detailsEl = contentContainer.depthFirst().find { it.name() == 'div' && it.@class == 'details' }

            def methodDesc = detailsEl.depthFirst().find {

                if (it.name() == "li") {
                    def h4 = it.h4
                    return h4.size() > 0 && !h4.text().isEmpty() && method.name == h4.text()
                } else {
                    return false
                }
            }

            return methodDesc.div[0].toString()
        } catch (Exception e) {
            if (cls.getSuperclass() != null) {
                try {
                    return findMethodDescription(objectName, cls.getSuperclass(), method)
                } catch (IllegalArgumentException iae) {
                    throw new IllegalArgumentException(unableToFindMethodError(method, objectName))
                }
            } else {
                throw new IllegalArgumentException(unableToFindMethodError(method, objectName))
            }
        }
    }

    static String unableToFindMethodError(Method method, String objectName) {
        return "Unable to find method '$method.name' in '$objectName'"
    }

    String findClassDescription(Class cls) {
        def html = loadJavadocFile(cls)

        def contentContainer = html.depthFirst().find{it.name() == 'div' && it.@class == 'contentContainer'}
        def descriptionEl = contentContainer.depthFirst().find{it.name() == 'div' && it.@class == 'description'}
        return descriptionEl.ul.li.div
    }
    def loadJavadocFile(Class cls) {
        def tagsoupParser = new Parser()
        def slurper = new XmlSlurper(tagsoupParser)
        def javadocFile = new File(javadocDir, cls.name.replace(".", File.separator).replace("\$", ".") + ".html")

        def xml;
        javadocFile.withReader "UTF-8", {
            xml = slurper.parse(it)
        }

        return xml
    }

}
