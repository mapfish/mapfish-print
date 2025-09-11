package org.mapfish.print.processor.jasper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignField;
import org.locationtech.jts.util.Assert;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.config.ConfigurationObject;
import org.mapfish.print.output.Values;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.CustomDependencies;

/**
 * This processor combines DataSources and individual processor outputs (or attribute values) into a
 * single DataSource which can be used in a jasper report's detail section.
 *
 * <p>An example use case is where we might have zero or many of tables and zero or many legends.
 * You can configure the template with a detail section that contains a subreport, the name of which
 * is a field in the DataSources and the DataSources for the sub-template another field. Then you
 * can merge the legend and the tables into a single DataSources. This way the report will nicely
 * expand depending on if you have a legend and how many tables you have in your report.
 * [[examples=merged_datasource]]
 */
public final class MergeDataSourceProcessor
    extends AbstractProcessor<MergeDataSourceProcessor.In, MergeDataSourceProcessor.Out>
    implements CustomDependencies {
  private List<Source> sources = new ArrayList<>();

  /** Constructor. */
  protected MergeDataSourceProcessor() {
    super(Out.class);
  }

  private static String indexString(final int i) {
    switch (i + 1) {
      case 1:
        return "1st";
      case 2:
        return "2nd";
      default:
        return (i + 1) + "th";
    }
  }

  /**
   * The <em>source</em> to add to the merged DataSource.
   *
   * <p>Each <em>source</em> indicates if it should be treated as a datasource or as a single item
   * to add to the merged DataSource. If the source indicates that it is a {@link
   * org.mapfish.print.processor.jasper.MergeDataSourceProcessor.SourceType#DATASOURCE} the object
   * each row in the datasource will be used to form a row in the merged DataSource. If the source
   * type is {@link org.mapfish.print.processor.jasper.MergeDataSourceProcessor.SourceType#SINGLE}
   * the object will be a single row even if it is in fact a DataSource.
   *
   * <p>See also: <a href="configuration.html#!mergeSource">!mergeSource</a>
   *
   * @param sources the source objects to merge
   */
  public void setSources(final List<Source> sources) {
    this.sources = sources;
  }

  @Override
  protected void extraValidation(
      final List<Throwable> validationErrors, final Configuration config) {
    if (this.sources == null || this.sources.isEmpty()) {
      validationErrors.add(
          new ConfigurationException(
              getClass().getSimpleName()
                  + " needs to have at minimum a single source. "
                  + "Although logically it should have more"));
      return;
    }

    for (int i = 0; i < this.sources.size(); i++) {
      Source source = this.sources.get(i);

      if (source.type == null) {
        validationErrors.add(
            new ConfigurationException(
                "The "
                    + indexString(i)
                    + " source in "
                    + getClass().getSimpleName()
                    + " needs to "
                    + "have a 'type' parameter defined."));
      } else {
        source.type.validate(i, validationErrors, source);
      }
    }
  }

  @Nullable
  @Override
  public In createInputParameter() {
    return new In();
  }

  @Nullable
  @Override
  public Out execute(final In values, final ExecutionContext context) throws Exception {
    List<Map<String, ?>> rows = new ArrayList<>();

    for (Source source : this.sources) {
      source.type.add(rows, values.values, source);
    }

    JRDataSource mergedDataSource = new JRMapCollectionDataSource(rows);
    return new Out(mergedDataSource);
  }

  @Nonnull
  @Override
  public Collection<String> getDependencies() {
    HashSet<String> sourceKeys = new HashSet<>();
    for (Source source : this.sources) {
      source.type.addValuesKeys(source, sourceKeys);
    }
    return sourceKeys;
  }

  /**
   * An enumeration of the different <em>types</em> of source objects. Essentially this describes
   * how the source should be merged into the final merged DataSource.
   */
  public enum SourceType {
    /**
     * Creates a single row from a set of values from the output and attribute objects.
     *
     * <p>In this case the key is not required, only the fields. Each field key will be the look up
     * key to find the object from the set of processor output and attributes. The field value will
     * be the column name for that value in the created row
     */
    SINGLE {
      @Override
      void add(final List<Map<String, ?>> rows, final Values values, final Source source) {

        Map<String, Object> row = new HashMap<>();
        for (Map.Entry<String, String> entry : source.fields.entrySet()) {
          final Object object = values.getObject(entry.getKey(), Object.class);
          row.put(entry.getValue(), object);
        }

        rows.add(row);
      }

      @Override
      void validate(
          final int rowIndex, final List<Throwable> validationErrors, final Source source) {
        if (source.key != null) {
          validationErrors.add(
              new ConfigurationException(
                  "The 'key' property is not required for source with the type "
                      + name()
                      + ". The "
                      + indexString(rowIndex)
                      + " source has a key property configured when it should not"));
        }
        if (source.fields.isEmpty()) {
          validationErrors.add(
              new ConfigurationException(
                  "The "
                      + indexString(rowIndex)
                      + " source in "
                      + getClass().getSimpleName()
                      + " has an invalid 'fields' "
                      + "parameter defined. There should be at least most one field defined"));
        }
      }

      @Override
      public void addValuesKeys(final Source source, final HashSet<String> sourceKeys) {
        sourceKeys.addAll(source.fields.keySet());
      }
    },
    /**
     * Indicates that the object is a DataSource and each row in it should be expanded to be a row
     * in the output table.
     *
     * <p>If the datasource does not exist or is null then this source will be skipped
     *
     * <p>The fields parameter of the source should contain all the fields to pull from the source
     * DataSource. Not all Fields need to be declared. For example if the source has 5 fields not
     * all of them need to be in the resulting merged datasource.
     */
    DATASOURCE {
      @Override
      void add(final List<Map<String, ?>> rows, final Values values, final Source source)
          throws JRException {
        JRDataSource dataSource = values.getObject(source.key, JRDataSource.class);
        Assert.isTrue(
            dataSource != null,
            "The Datasource object referenced by key: "
                + source.key
                + " does not exist.  Check that the key is correctly spelled in the config.yaml"
                + " file.\n"
                + "\t This is one of the sources for the !mergeDataSources.");

        JRDesignField jrField = new JRDesignField();

        while (dataSource.next()) {
          Map<String, Object> row = new HashMap<>();
          for (Map.Entry<String, String> field : source.fields.entrySet()) {
            jrField.setName(field.getKey());
            row.put(field.getValue(), dataSource.getFieldValue(jrField));
          }
          rows.add(row);
        }
      }

      @Override
      void validate(
          final int rowIndex, final List<Throwable> validationErrors, final Source source) {
        if (source.key.isEmpty()) {
          validationErrors.add(
              new ConfigurationException(
                  "The "
                      + indexString(rowIndex)
                      + " source in "
                      + MergeDataSourceProcessor.class.getSimpleName()
                      + " needs to have a 'key' parameter defined."));
        }
        if (source.fields.isEmpty()) {
          validationErrors.add(
              new ConfigurationException(
                  "The "
                      + indexString(rowIndex)
                      + " source in "
                      + MergeDataSourceProcessor.class.getSimpleName()
                      + " needs to have a 'fields' parameter defined."));
        }
      }

      @Override
      public void addValuesKeys(final Source source, final HashSet<String> sourceKeys) {
        sourceKeys.add(source.key);
      }
    };

    abstract void add(List<Map<String, ?>> rows, Values values, Source source) throws JRException;

    abstract void validate(int rowIndex, List<Throwable> validationErrors, Source source);

    abstract void addValuesKeys(Source source, HashSet<String> sourceKeys);
  }

  /** The input object for {@link org.mapfish.print.processor.jasper.MergeDataSourceProcessor}. */
  public static class In {
    /** The values used to look up the values to merge together. */
    public Values values;
  }

  /** The output object for {@link org.mapfish.print.processor.jasper.MergeDataSourceProcessor}. */
  public static class Out {
    /** The resulting datasource. */
    public final JRDataSource mergedDataSource;

    /**
     * Constructor.
     *
     * @param mergedDataSource the merged datasource
     */
    public Out(final JRDataSource mergedDataSource) {
      this.mergedDataSource = mergedDataSource;
    }
  }

  /**
   * Describes the objects used as sources for a merged data source (see <a
   * href="processors.html#!mergeDataSources">!mergeDataSources</a> processor).
   * [[examples=merged_datasource]]
   */
  public static final class Source implements ConfigurationObject {
    String key;
    SourceType type;
    Map<String, String> fields = new HashMap<>();

    static Source createSource(final String key, final SourceType type) {
      Source source = new Source();
      source.key = key;
      source.type = type;
      return source;
    }

    static Source createSource(
        final String key, final SourceType type, final Map<String, String> fields) {
      Source source = new Source();
      source.key = key;
      source.type = type;
      source.fields = fields;
      return source;
    }

    /**
     * The key to use when looking for the object among the attributes and the processor output
     * values.
     *
     * @param key the look up key
     */
    public void setKey(final String key) {
      this.key = key;
    }

    /**
     * The type of source. See {@link
     * org.mapfish.print.processor.jasper.MergeDataSourceProcessor.SourceType} for the options.
     *
     * @param type the type of source
     */
    public void setType(final SourceType type) {
      this.type = type;
    }

    /**
     * The names of each field in the DataSource. See {@link
     * org.mapfish.print.processor.jasper.MergeDataSourceProcessor.SourceType} for instructions on
     * how to declare the fields
     *
     * @param fields the field names
     */
    public void setFields(final Map<String, String> fields) {
      this.fields = fields;
    }

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration config) {
      // validation is done in MergeDataSourceProcessor
    }
  }
}
