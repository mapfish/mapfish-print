package org.mapfish.print.processor.map;

import com.google.common.collect.Multimap;
import org.locationtech.jts.geom.Envelope;
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
                input -> (("" + input.getHost()).contains(host + ".wms")) ||
                        input.getAuthority().contains(host + ".wms"),
                new TestHttpClientFactory.Handler() {
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
                            imageName =
                                    "-137.65094419245855_19.24097872857841_-109.6544269789473_47" +
                                            ".23749594208966.png";
                        } else if (equalEnv(envelope, -109.6544269789473, 19.24097872857841,
                                            -81.65790976543605, 47.23749594208966)) {
                            imageName =
                                    "-109.6544269789473_19.24097872857841_-81.65790976543605_47" +
                                            ".23749594208966.png";
                        } else if (equalEnv(envelope, -81.65790976543605, 19.24097872857841,
                                            -53.66139255192479, 47.23749594208966)) {
                            imageName =
                                    "-81.65790976543605_19.24097872857841_-53.66139255192479_47" +
                                            ".23749594208966.png";
                        } else if (equalEnv(envelope, -53.66139255192479, 19.24097872857841,
                                            -52.34905580754146, 47.23749594208966)) {
                            imageName =
                                    "-53.66139255192479_19.24097872857841_-52.34905580754146_47" +
                                            ".23749594208966.png";
                        } else if (equalEnv(envelope, -137.65094419245855, 47.23749594208966,
                                            -109.6544269789473, 75.2340131556009)) {
                            imageName =
                                    "-137.65094419245855_47.23749594208966_-109.6544269789473_75" +
                                            ".2340131556009.png";
                        } else if (equalEnv(envelope, -109.6544269789473, 47.23749594208966,
                                            -81.65790976543605, 75.2340131556009)) {
                            imageName =
                                    "-109.6544269789473_47.23749594208966_-81.65790976543605_75" +
                                            ".2340131556009.png";
                        } else if (equalEnv(envelope, -81.65790976543605, 47.23749594208966,
                                            -53.66139255192479, 75.2340131556009)) {
                            imageName =
                                    "-81.65790976543605_47.23749594208966_-53.66139255192479_75" +
                                            ".2340131556009.png";
                        } else if (equalEnv(envelope, -53.66139255192479, 47.23749594208966,
                                            -52.34905580754146, 75.2340131556009)) {
                            imageName =
                                    "-53.66139255192479_47.23749594208966_-52.34905580754146_75" +
                                            ".2340131556009.png";
                        } else {
                            return error404(uri, httpMethod);
                        }
                        try {
                            byte[] bytes = AbstractMapfishSpringTest.getFileBytes(
                                    CreateMapProcessorFlexibleScaleCenterTiledWmsTest.class,
                                    "/map-data/tiled-wms-tiles/" + imageName);
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
