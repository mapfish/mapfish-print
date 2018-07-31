package org.apache.batik.dom.svg;

/**
 * GeoTools wants batik 1.7 and Jasper wants version 1.8. SAXSVGDocumentFactory has been moved to another
 * package. This class does the bridge.
 */
public class SAXSVGDocumentFactory extends org.apache.batik.anim.dom.SAXSVGDocumentFactory {
    /**
     * Creates a new SVGDocumentFactory object.
     *
     * @param parser The SAX2 parser classname.
     */
    public SAXSVGDocumentFactory(final String parser) {
        super(parser);
    }

    /**
     * Creates a new SVGDocumentFactory object.
     *
     * @param parser The SAX2 parser classname.
     * @param dd Whether a document descriptor must be generated.
     */
    public SAXSVGDocumentFactory(final String parser, final boolean dd) {
        super(parser, dd);
    }
}
