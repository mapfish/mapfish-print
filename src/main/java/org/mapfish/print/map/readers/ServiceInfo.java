package org.mapfish.print.map.readers;

import org.apache.xerces.util.DOMUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;

/**
 * Represents data loaded from a server that describes the service
 *
 * Created by Jesse on 1/17/14.
 */
public class ServiceInfo {

    protected final static DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

    {
        documentBuilderFactory.setValidating(false);  //doesn't work?!?!?
    }

}
