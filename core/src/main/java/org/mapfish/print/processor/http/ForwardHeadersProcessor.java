package org.mapfish.print.processor.http;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.annotation.Nullable;
import org.mapfish.print.attribute.HttpRequestHeadersAttribute;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.http.matcher.URIMatcher;
import org.mapfish.print.processor.http.matcher.UriMatchers;

/**
 * This processor forwards all the headers from the print request (from the Mapfish Print client) to
 * each http request made for the particular print job. All headers can be forwarded (if forwardAll
 * is set to true) or the specific headers to forward can be specified.
 *
 * <p>Example 1: Forward all headers from print request
 *
 * <pre><code>
 * - !forwardHeaders
 *   all: true
 * </code></pre>
 *
 * <p>Example 2: Forward specific headers (header1 and header2 will be forwarded)
 *
 * <pre><code>
 * - !forwardHeaders
 *   headers: [header1, header2]
 * </code></pre>
 *
 * <p>Can be applied conditionally using matchers, like in {@link RestrictUrisProcessor} (<a
 * href="processors.html#!restrictUris">!restrictUris</a> ). [[examples=http_processors]]
 */
public final class ForwardHeadersProcessor
    extends AbstractProcessor<ForwardHeadersProcessor.Param, Void>
    implements HttpProcessor<ForwardHeadersProcessor.Param> {

  private final UriMatchers matchers = new UriMatchers();
  private Set<String> headerNames = new HashSet<>();
  private boolean forwardAll = false;

  /** Constructor. */
  public ForwardHeadersProcessor() {
    super(Void.class);
  }

  /**
   * Set the header names to forward from the request. Should not be defined if all is set to true
   *
   * @param names the header names.
   */
  public void setHeaders(final Set<String> names) {
    // transform to lower-case because header names should be case-insensitive
    Set<String> lowerCaseNames = new HashSet<>();
    for (String name : names) {
      lowerCaseNames.add(name.toLowerCase());
    }
    this.headerNames = lowerCaseNames;
  }

  /**
   * The matchers used to select the urls that are going to be modified by the processor. For
   * example:
   *
   * <pre><code>
   * - !restrictUris
   *   matchers:
   *     - !localMatch
   *       dummy: true
   *     - !ipMatch
   *     ip: www.camptocamp.org
   *     - !dnsMatch
   *       host: mapfish-geoportal.demo-camptocamp.com
   *       port: 80
   *     - !dnsMatch
   *       host: labs.metacarta.com
   *       port: 80
   *     - !dnsMatch
   *       host: terraservice.net
   *       port: 80
   *     - !dnsMatch
   *       host: tile.openstreetmap.org
   *       port: 80
   *     - !dnsMatch
   *       host: www.geocat.ch
   *       port: 80
   * </code></pre>
   *
   * @param matchers the list of matcher to use to check if a url is permitted
   */
  public void setMatchers(final List<? extends URIMatcher> matchers) {
    this.matchers.setMatchers(matchers);
  }

  /**
   * If set to true then all headers are forwarded. If this is true headers should be empty (or
   * undefined)
   *
   * @param all if true forward all headers
   */
  public void setAll(final boolean all) {
    this.forwardAll = all;
  }

  @Override
  protected void extraValidation(
      final List<Throwable> validationErrors, final Configuration configuration) {
    if (!this.forwardAll && this.headerNames.isEmpty()) {
      validationErrors.add(new IllegalStateException("all is false and no headers are defined"));
    }
    if (this.forwardAll && !this.headerNames.isEmpty()) {
      validationErrors.add(
          new IllegalStateException(
              "all is true but headers is defined. Either all is true "
                  + "OR headers is specified"));
    }
  }

  @Override
  public MfClientHttpRequestFactory createFactoryWrapper(
      final Param param, final MfClientHttpRequestFactory requestFactory) {
    Map<String, List<String>> headers = new HashMap<>();

    for (Map.Entry<String, List<String>> entry : param.requestHeaders.getHeaders().entrySet()) {
      if (ForwardHeadersProcessor.this.forwardAll
          || ForwardHeadersProcessor.this.headerNames.contains(entry.getKey().toLowerCase())) {
        headers.put(entry.getKey(), entry.getValue());
      }
    }

    return AddHeadersProcessor.createFactoryWrapper(requestFactory, this.matchers, headers);
  }

  @Nullable
  @Override
  public Param createInputParameter() {
    return new Param();
  }

  @Nullable
  @Override
  public Void execute(final Param values, final ExecutionContext context) {
    values.clientHttpRequestFactoryProvider.set(
        createFactoryWrapper(values, values.clientHttpRequestFactoryProvider.get()));
    return null;
  }

  /** The parameters required by this processor. */
  public static class Param extends ClientHttpFactoryProcessorParam {
    /** The http headers from the print request. */
    public HttpRequestHeadersAttribute.Value requestHeaders;
  }
}
