package org.mapfish.print.config;

import org.mapfish.print.OptionalUtils;

import java.util.List;
import java.util.Optional;

/**
 * <p>A Configuration object for containing metadata that will be embedded in the PDF and parameters to use
 * when encoding the PDF.
 * </p>
 * <p>Naturally this only applies to reports that are exported as PDFs.</p>
 * [[examples=config_aliases_defaults,verboseExample]]
 */
public class PDFConfig implements ConfigurationObject {
    private static final String MAPFISH_PRINT = "Mapfish Print";
    private Optional<Boolean> compressed = Optional.empty();
    private Optional<String> title = Optional.empty();
    private Optional<String> author = Optional.empty();
    private Optional<String> subject = Optional.empty();
    private Optional<String> keywords = Optional.empty();
    private Optional<String> creator = Optional.empty();

    PDFConfig getMergedInstance(final PDFConfig other) {
        final PDFConfig merged = new PDFConfig();
        merged.compressed = OptionalUtils.or(this.compressed, other.compressed);
        merged.title = OptionalUtils.or(this.title, other.title);
        merged.author = OptionalUtils.or(this.author, other.author);
        merged.subject = OptionalUtils.or(this.subject, other.subject);
        merged.keywords = OptionalUtils.or(this.keywords, other.keywords);
        merged.creator = OptionalUtils.or(this.creator, other.creator);

        return merged;
    }

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        // no validation required
    }

    public boolean isCompressed() {
        return this.compressed.orElse(false);
    }

    /**
     * If this property is set to true then the resulting PDF will be a compressed PDF. By default the PDF is
     * not compressed.
     *
     * @param compressed if the pdf should be compressed.
     */
    public void setCompressed(final boolean compressed) {
        this.compressed = Optional.of(compressed);
    }

    public String getTitle() {
        return this.title.orElse(MAPFISH_PRINT);
    }

    /**
     * Set the title of the PDF.
     *
     * @param title the title of the PDF.
     */
    public void setTitle(final String title) {
        this.title = Optional.of(title);
    }

    public String getAuthor() {
        return this.author.orElse(MAPFISH_PRINT);
    }

    /**
     * Set the author of the PDF.
     *
     * @param author the author of the PDF.
     */
    public void setAuthor(final String author) {
        this.author = Optional.of(author);
    }

    public String getSubject() {
        return this.subject.orElse(MAPFISH_PRINT);
    }

    /**
     * Set the subject of the PDF.
     *
     * @param subject the subject of the PDF.
     */
    public void setSubject(final String subject) {
        this.subject = Optional.of(subject);
    }

    public String getKeywordsAsString() {
        return this.keywords.orElse(MAPFISH_PRINT);
    }

    /**
     * The keywords to include in the PDF metadata.
     *
     * @param keywords the keywords of the PDF.
     */
    public void setKeywords(final List<String> keywords) {
        StringBuilder builder = new StringBuilder();
        for (String keyword: keywords) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(keyword.trim());
        }
        this.keywords = Optional.of(builder.toString());
    }

    public String getCreator() {
        return this.creator.orElse(MAPFISH_PRINT);
    }

    /**
     * Set the creator of the PDF.
     *
     * @param creator the creator of the PDF.
     */
    public void setCreator(final String creator) {
        this.creator = Optional.of(creator);
    }

}
