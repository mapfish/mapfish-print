/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.config;

import com.google.common.base.Optional;

import java.util.List;

/**
 * A Configuration object for containing metadata that will be embedded in the PDF and parameters to use when encoding the
 * PDF.
 * <p/>
 * Naturally this only applies to reports that are exported as PDFs.
 *
 * @author Jesse on 9/12/2014.
 */
// CSOFF: DesignForExtension  -- Note: This is disabled so that I can use Mockito.spy in tests
public class PDFConfig implements ConfigurationObject {
    private static final String MAPFISH_PRINT = "Mapfish Print";
    private Optional<Boolean> compressed = Optional.absent();
    private Optional<String> title = Optional.absent();
    private Optional<String> author = Optional.absent();
    private Optional<String> subject = Optional.absent();
    private Optional<String> keywords = Optional.absent();
    private Optional<String> creator = Optional.absent();

    PDFConfig getMergedInstance(final PDFConfig other) {
        final PDFConfig merged = new PDFConfig();
        merged.compressed = this.compressed.or(other.compressed);
        merged.title = this.title.or(other.title);
        merged.author = this.author.or(other.author);
        merged.subject = this.subject.or(other.subject);
        merged.keywords = this.keywords.or(other.keywords);
        merged.creator = this.creator.or(other.creator);

        return merged;
    }

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        // no validation required
    }

    public boolean isCompressed() {
        return this.compressed.or(false);
    }

    /**
     * If this property is set to true then the resulting PDF will be a compressed PDF. By default the PDF is not compressed.
     *
     * @param compressed if the pdf should be compressed.
     */
    public void setCompressed(final boolean compressed) {
        this.compressed = Optional.of(compressed);
    }

    public String getTitle() {
        return this.title.or(MAPFISH_PRINT);
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
        return this.author.or(MAPFISH_PRINT);
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
        return this.subject.or(MAPFISH_PRINT);
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
        return this.keywords.or(MAPFISH_PRINT);
    }

    /**
     * The keywords to include in the PDF metadata.
     *
     * @param keywords the keywords of the PDF.
     */
    public void setKeywords(final List<String> keywords) {
        StringBuilder builder = new StringBuilder();
        for (String keyword : keywords) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(keyword.trim());
        }
        this.keywords = Optional.of(builder.toString());
    }

    public String getCreator() {
        return this.creator.or(MAPFISH_PRINT);
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
