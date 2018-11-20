package org.mapfish.print.processor.map;

import com.google.common.base.Predicate;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import com.vividsolutions.jts.geom.Envelope;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.URIUtils;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;

import java.net.URI;

import static java.lang.Double.parseDouble;

public final class TiledWMSUtil {
    private TiledWMSUtil() {
        // do nothing
    }

    public static void registerTiledWmsHandler(TestHttpClientFactory requestFactory, final String host) {
        requestFactory.registerHandler(
                new Predicate<URI>() {
                    @Override
                    public boolean apply(URI input) {
                        return (("" + input.getHost()).contains(host + ".wms")) ||
                                input.getAuthority().contains(host + ".wms");
                    }
                }, new TestHttpClientFactory.Handler() {
                    @Override
                    public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod)
                            throws Exception {
                        final Multimap<String, String> parameters = URIUtils.getParameters(uri);

                        final String rawBBox = parameters.get("BBOX").iterator().next();
                        String[] bbox = rawBBox.split(",");
                        Envelope envelope =
                                new Envelope(parseDouble(bbox[0]), parseDouble(bbox[2]), parseDouble(bbox[1]),
                                             parseDouble(bbox[3]));

                        String imageName;

                        if (equalEnv(envelope, -137.65094419245855, 19.24097872857841, -109.6544269789473,
                                     47.23749594208966)) {
                            imageName = "-137_6509,19_2409,-115_4743,41_4175.png";
                        } else if (equalEnv(envelope, -109.6544269789473, 19.24097872857841,
                                            -81.65790976543605, 47.23749594208966)) {
                            imageName = "-115_4743,19_2409,-93_2977,41_4175.png";
                        } else if (equalEnv(envelope, -81.65790976543605, 19.24097872857841,
                                            -53.66139255192479, 47.23749594208966)) {
                            imageName = "-93.2977_19.2409_-71.1211_41.4175.png";
                        } else if (equalEnv(envelope, -53.66139255192479, 19.24097872857841,
                                            -52.34905580754146, 47.23749594208966)) {
                            imageName = "-71.1211_19.2409_-48.9445_41.4175.png";
                        } else if (equalEnv(envelope, -137.65094419245855, 47.23749594208966,
                                            -109.6544269789473, 75.2340131556009)) {
                            imageName = "-137.6509_41.4175_-115.4743_63.5941.png";
                        } else if (equalEnv(envelope, -109.6544269789473, 47.23749594208966,
                                            -81.65790976543605, 75.2340131556009)) {
                            imageName = "-115.4743_41.4175_-93.2977_63.5941.png";
                        } else if (equalEnv(envelope, -81.65790976543605, 47.23749594208966,
                                            -53.66139255192479, 75.2340131556009)) {
                            imageName = "-93.2977_41.4175_-71.1211_63.5941.png";
                        } else if (equalEnv(envelope, -53.66139255192479, 47.23749594208966,
                                            -52.34905580754146, 75.2340131556009)) {
                            imageName = "-71.1211_41.4175_-48.9445_63.5941.png";
                        } else {
                            return error404(uri, httpMethod);
                        }
                        try {
                            byte[] bytes = Files.toByteArray(AbstractMapfishSpringTest.getFile
                                    (CreateMapProcessorFlexibleScaleCenterTiledWmsTest.class,
                                     "/map-data/tiled-wms-tiles/" + imageName));
                            return ok(uri, bytes, httpMethod);
                        } catch (AssertionError e) {
                            return error404(uri, httpMethod);
                        }
                    }

                    private boolean equalEnv(
                            Envelope envelope, double minx, double miny, double maxx, double maxy) {
                        double difference = 0.00001;
                        return Math.abs(envelope.getMinX() - minx) < difference &&
                                Math.abs(envelope.getMinY() - miny) < difference &&
                                Math.abs(envelope.getMaxX() - maxx) < difference &&
                                Math.abs(envelope.getMaxY() - maxy) < difference;
                    }
                }
        );
    }
}
