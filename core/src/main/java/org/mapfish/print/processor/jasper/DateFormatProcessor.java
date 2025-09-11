package org.mapfish.print.processor.jasper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import jakarta.annotation.Nullable;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.parser.HasDefaultValue;
import org.mapfish.print.processor.AbstractProcessor;

/**
 * A processor that creates a date formatter that can be used in Jasper.
 *
 * <p>Example Configuration:
 *
 * <pre><code>
 * attributes:
 *   ...
 *   timezone: !string
 *     default: PST
 * processors:
 *   ...
 *   - !dateFormat
 *     pattern: "EEEEE dd MMMMM yyyy HH:mm"
 * </code></pre>
 *
 * <p>Will take into account the "lang" field, if passed in the request:
 *
 * <pre><code>
 * {
 *   "lang": "fr_CH",
 *   "attributes": {
 *      ...
 *      "timezone": "CET"
 *   },
 *   ...
 * }
 * </code></pre>
 *
 * <p>In your template, you can use it like that:
 *
 * <pre><code>
 * &lt;jasperReport&gt;
 *   &lt;parameter name="dateFormat" class="java.text.DateFormat"/&gt;
 *   ...
 *   &lt;textField&gt;
 *     ...
 *     &lt;textFieldExpression&gt;$P{dateFormat}.format(TODAY())&lt;/textFieldExpression&gt;
 *   &lt;/textField&gt;
 * &lt;/jasperReport&gt;
 * </code></pre>
 */
public class DateFormatProcessor
    extends AbstractProcessor<DateFormatProcessor.Input, DateFormatProcessor.Output> {

  private String pattern;

  /** Constructor. */
  protected DateFormatProcessor() {
    super(Output.class);
  }

  @Override
  protected void extraValidation(
      final List<Throwable> validationErrors, final Configuration configuration) {
    if (pattern == null) {
      validationErrors.add(new ConfigurationException("'pattern' is mandatory"));
    }
  }

  @Nullable
  @Override
  public Input createInputParameter() {
    return new Input();
  }

  @Nullable
  @Override
  public Output execute(final Input values, final ExecutionContext context) throws Exception {

    SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, values.REPORT_LOCALE);
    if (values.timezone != null) {
      dateFormat.setTimeZone(TimeZone.getTimeZone(values.timezone));
    }
    return new Output(dateFormat);
  }

  /**
   * The pattern to use to format dates.
   *
   * <p>See https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
   *
   * @param pattern The pattern
   */
  public void setPattern(final String pattern) {
    this.pattern = pattern;
  }

  /** The Input of the processor. */
  public static final class Input {
    /**
     * The timezone to use.
     *
     * <p>Either an abbreviation such as "CET", a full name such as "Europe/Zurich", or a custom ID
     * such as "GMT-8:00". Defaults to the OS timezone.
     */
    @HasDefaultValue public String timezone;

    /** The values. */
    @SuppressWarnings("checkstyle:MemberName")
    public Locale REPORT_LOCALE;
  }

  /** The output of the processor. */
  public static final class Output {
    /** The date formatter. */
    public final DateFormat dateFormat;

    /**
     * Constructor.
     *
     * @param dateFormat the formatter.
     */
    public Output(final DateFormat dateFormat) {
      this.dateFormat = dateFormat;
    }
  }
}
