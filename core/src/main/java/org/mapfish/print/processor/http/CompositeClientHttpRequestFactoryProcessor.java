package org.mapfish.print.processor.http;

import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.Nullable;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.output.Values;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.ProcessorUtils;

/**
 * A processor that wraps several {@link AbstractClientHttpRequestFactoryProcessor}s.
 *
 * <p>This makes it more convenient to configure multiple processors that modify {@link
 * org.mapfish.print.http.MfClientHttpRequestFactory} objects.
 *
 * <p>Consider the case where you need to:
 *
 * <ul>
 *   <li>Restrict allowed URIS using the !restrictUris processor
 *   <li>Forward all headers from print request to all requests using !forwardHeaders
 *   <li>Change the url using the !mapUri processor
 * </ul>
 *
 * <p>In this case the !mapUri processor must execute before the !restrictUris processor but it is
 * difficult to enforce this, the inputMapping and outputMapping must be carefully designed in order
 * to do it. The following should work but compare it with the example below:
 *
 * <pre><code>
 * - !mapUri
 *   mapping:
 *     (http)://localhost(.*): "$1://127.0.0.1$2"
 *   outputMapper: {clientHttpRequestFactoryProvider: clientHttpRequestFactoryMapped}
 * - !forwardHeaders
 *   all: true
 *   inputMapper: {clientHttpRequestFactoryMapped :clientHttpRequestFactoryProvider}
 *   outputMapper: {clientHttpRequestFactoryProvider: clientHttpRequestFactoryWithHeaders}
 * - !restrictUris
 *   matchers: [!localMatch {}]
 *   inputMapper: {clientHttpRequestFactoryWithHeaders:clientHttpRequestFactoryProvider}
 *     </code></pre>
 *
 * <p>The recommended way to write the above configuration is as follows:
 *
 * <pre><code>
 * - !configureHttpRequests
 *   httpProcessors:
 *     - !mapUri
 *       mapping:
 *         (http)://localhost(.*): "$1://127.0.0.1$2"
 *     - !forwardHeaders
 *       all: true
 *     - !restrictUris
 *       matchers: [!localMatch {}]
 * </code></pre>
 *
 * [[examples=http_processors]]
 */
public final class CompositeClientHttpRequestFactoryProcessor
    extends AbstractProcessor<CompositeClientHttpRequestFactoryProcessor.Input, Void>
    implements HttpProcessor<CompositeClientHttpRequestFactoryProcessor.Input> {
  private List<HttpProcessor> httpProcessors = new ArrayList<>();

  /** Constructor. */
  protected CompositeClientHttpRequestFactoryProcessor() {
    super(Void.class);
  }

  /**
   * Sets all the http processors that will executed by this processor.
   *
   * @param httpProcessors the sub processors
   */
  public void setHttpProcessors(final List<HttpProcessor> httpProcessors) {
    this.httpProcessors = httpProcessors;
  }

  @SuppressWarnings("unchecked")
  @Override
  public MfClientHttpRequestFactory createFactoryWrapper(
      final Input input, final MfClientHttpRequestFactory requestFactory) {
    MfClientHttpRequestFactory finalRequestFactory = requestFactory;
    // apply the parts in reverse so that the last part is the inner most wrapper (will be last to
    // be
    // called)
    for (int i = this.httpProcessors.size() - 1; i > -1; i--) {
      final HttpProcessor processor = this.httpProcessors.get(i);
      Object populatedInput = ProcessorUtils.populateInputParameter(processor, input.values);
      finalRequestFactory = processor.createFactoryWrapper(populatedInput, finalRequestFactory);
    }
    return finalRequestFactory;
  }

  @Override
  protected void extraValidation(
      final List<Throwable> validationErrors, final Configuration configuration) {
    if (this.httpProcessors.isEmpty()) {
      validationErrors.add(
          new IllegalStateException("There are no composite elements for this " + "processor"));
    } else {
      for (Object part : this.httpProcessors) {
        if (!(part instanceof HttpProcessor)) {
          validationErrors.add(
              new IllegalStateException(
                  "One of the parts of "
                      + getClass().getSimpleName()
                      + " is not a "
                      + HttpProcessor.class.getSimpleName()));
        }
      }
    }
  }

  @Nullable
  @Override
  public Input createInputParameter() {
    return new Input();
  }

  @Nullable
  @Override
  public Void execute(final Input values, final ExecutionContext context) {
    values.clientHttpRequestFactoryProvider.set(
        createFactoryWrapper(values, values.clientHttpRequestFactoryProvider.get()));
    return null;
  }

  @Override
  public void toString(final StringBuilder builder, final int indent, final String parent) {
    super.toString(builder, indent, parent);
    for (HttpProcessor sub : this.httpProcessors) {
      sub.toString(builder, indent + 1, this.toString());
    }
  }

  /** The input. */
  public static class Input extends ClientHttpFactoryProcessorParam {
    /** The values. */
    public Values values;
  }
}
