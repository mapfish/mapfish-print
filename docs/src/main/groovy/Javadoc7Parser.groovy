import java.lang.reflect.Field
import java.lang.reflect.Method
import groovy.xml.XmlSlurper
/**
 * Strategy for parsing out the field, class and method descriptions from Javadoc 7 compatible javadocs.
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
            def contentContainer = html.depthFirst().find {
                it.name() == 'main' && it.@role == 'main'
            }
            def detailsEl = contentContainer.depthFirst().find {
                it.name() == 'section' && it.@class == 'details'
            }
            def descDiv = detailsEl.depthFirst().find {

                if (it.name() == "li") {
                    def h3 = it.section.h3
                    return h3.size() > 0 && !h3.text().isEmpty() && obj == h3.text()
                } else {
                    return false
                }
            }

            def description = descDiv.breadthFirst().find {
                return  it.name() == 'div' && it.@class == 'block'
            }

            return DocsXmlSupport.toXmlString(description)
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

        def contentContainer = html.depthFirst().find {
            it.name() == 'main' && it.@role == 'main'
        }
        def descriptionEl = contentContainer.depthFirst().find {
            it.name() == 'section' && it.@class == 'class-description'
        }
        def result = contentContainer.depthFirst().find {
            it.name() == 'div' && it.@class == 'block'
        }
        return DocsXmlSupport.toXmlString(result)
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
