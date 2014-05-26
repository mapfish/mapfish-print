package org.mapfish.print.map.readers;

import com.codahale.metrics.MetricRegistry;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.mapfish.print.RenderingContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Contains shared code for loading information from a server and caching it for later use.
 *
 * Created by Jesse on 1/17/14.
 */
public class ServerInfoCache<T extends ServiceInfo> {
    private final Map<URI, T> cache = Collections.synchronizedMap(new HashMap<URI, T>());

    private final ServiceInfoLoader<T> loader;

    public ServerInfoCache(ServiceInfoLoader loader) {
        this.loader = loader;
    }

    public synchronized void clearCache() {
        cache.clear();
    }

    public synchronized final T getInfo(URI uri, RenderingContext context) {
        T result = cache.get(uri);
        if (result == null) {
            try {
                result = requestInfo(uri, context);
            } catch (Exception e) {
                loader.logger().info("Error while getting capabilities for "+uri+". The print module will assume it's a standard WMS.");
                String stackTrace = "";
                for (StackTraceElement el : e.getStackTrace()) {
                    stackTrace += el.toString() +"\n";
                }
                loader.logger().info(stackTrace);
                result = loader.createNewErrorResult();
            }
            if (loader.logger().isDebugEnabled()) {
                loader.logger().debug("GetCapabilities " + uri + ": " + result);
            }
            cache.put(uri, result);
        }
        return result;

    }


    private T requestInfo(URI baseUrl, RenderingContext context) throws IOException, URISyntaxException, ParserConfigurationException, SAXException {
        URL url = loader.createURL(baseUrl, context);

        GetMethod method = null;

        MetricRegistry registry = context.getConfig().getMetricRegistry();
        final com.codahale.metrics.Timer.Context timer = registry.timer("http_" + url.getAuthority()).time();
        try {
            final InputStream stream;

            if ((url.getProtocol().equals("http") || url.getProtocol().equals("https")) &&
                context.getConfig().localHostForwardIsFrom(url.getHost())) {
                String scheme = url.getProtocol();
                final String host = url.getHost();
                if (url.getProtocol().equals("https") &&
                    context.getConfig().localHostForwardIsHttps2http()) {
                    scheme = "http";
                }
                URL localUrl = new URL(scheme, "localhost", url.getPort(),
                        url.getFile());
                HttpURLConnection connexion = (HttpURLConnection)localUrl.openConnection();
                connexion.setRequestProperty("Host", host);
                for (Map.Entry<String, String> entry : context.getHeaders().entrySet()) {
                    connexion.setRequestProperty(entry.getKey(), entry.getValue());
                }
                stream = connexion.getInputStream();
            }
            else {
                method = new GetMethod(url.toString());
                for (Map.Entry<String, String> entry : context.getHeaders().entrySet()) {
                    method.setRequestHeader(entry.getKey(), entry.getValue());
                }
                context.getConfig().getHttpClient(baseUrl).executeMethod(method);
                int code = method.getStatusCode();
                if (code < 200 || code >= 300) {
                    throw new IOException("Error " + code + " while reading the Capabilities from " + url + ": " + method.getStatusText());
                }
                stream = method.getResponseBodyAsStream();
            }
            final T result;
            try {
                result = loader.parseInfo(stream);
            } finally {
                stream.close();
            }
            return result;
        } finally {
            timer.stop();
            if (method != null) {
                method.releaseConnection();
            }
        }
    }
    public static abstract class ServiceInfoLoader<T extends ServiceInfo> {

        public abstract Log logger();

        public abstract T createNewErrorResult();

        public abstract URL createURL(URI baseUrl, RenderingContext context) throws UnsupportedEncodingException, URISyntaxException, MalformedURLException;

        public abstract T parseInfo(InputStream stream) throws ParserConfigurationException, IOException, SAXException;


        /**
         * Get the text content of the _child_ element or return default value if element does not exist. An exception is thrown
         * if the child is not found.
         *
         * @param element
         * @param tagName
         */
        public static String getTextContentOfChild(Element element, String tagName) {
            String result = getTextContextFromPath(element, tagName, null);
            if (result == null) {
                throw new NoSuchElementException("No child "+tagName+" was found in element "+element.getNodeName());
            }
            return result;
        }
        /**
         * Get the text content of the _child_ element or return default value if element does not exist. The elements are used for selection but
         * the namespace is ignored.
         *
         * @param element
         * @param tagName
         * @param defaultValue
         */
        public static String getTextContentOfChild(Element element, String tagName, String defaultValue) {
            Element child = getFirstChildElement(element, tagName);
            if (child == null || child.getTextContent().trim().isEmpty()) {
                return defaultValue;
            }
            return child.getTextContent();
        }

        /**
         * Find all the children with the provided tagName and return an array of their non-empty text.
         *
         *
         * @param element parent element of elements to get
         * @param tagName child names
         * @return
         */
        public static ArrayList<String> getTextContentOfChildren(Element element, String tagName) {

            ArrayList<String> text = new ArrayList<String>();
            final NodeList nodeList = element.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    Element child = (Element) node;
                    if (getLocalName(child).equals(tagName)) {
                        final String textContent = node.getTextContent();
                        if (!textContent.trim().isEmpty()) {
                            text.add(textContent);
                        }
                    }
                }
            }
            return text;
        }

        /**
         * Find the text at the indicated path (not an XPath just a / separated path of tagnames) or return default value.
         *
         * Assumptions:
         * <ul>
         *     <li>Only the first child in each path segment is traversed.  If there are multiple then they will be ignored.</li>
         * </ul>
         *
         * @param element
         * @param path
         * @param defaultValue
         * @return
         */
        public static String getTextContextFromPath(Element element, String path, String defaultValue) {
            return getTextContextFromPath(element, Arrays.asList(path.split("/")), defaultValue);
        }
        private static String getTextContextFromPath(Element element, List<String> path, String defaultValue) {
            final Element child = getFirstChildElement(element, path.get(0));
            if (child == null) {
                return defaultValue;
            }

            if (path.size() == 1) {
                String text = child.getTextContent();
                if (text.trim().isEmpty()) {
                    return defaultValue;
                } else {
                    return text;
                }
            }

            return getTextContextFromPath(child, path.subList(1, path.size()), defaultValue);
        }

        private static Element getFirstChildElement(Element element, String tagName) {
            final NodeList childNodes = element.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node elem = childNodes.item(i);
                if (elem instanceof Element) {
                    if (getLocalName(elem).equals(tagName)) {
                        return (Element) elem;
                    }
                }
            }
            return null;
        }

        private static String getLocalName(Node elem) {
            final String nodeName = elem.getNodeName();
            String[] split = nodeName.split(":", 2);
            return split.length == 2 ? split[1] : split[0];
        }
    }
}
