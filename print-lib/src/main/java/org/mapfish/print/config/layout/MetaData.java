/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Server
 *
 * MapFish Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MapFish Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.config.layout;

import com.lowagie.text.Document;
import org.mapfish.print.PDFUtils;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.utils.PJsonObject;

/**
 * Bean to configure the metaData part of a layout.
 * <p/>
 * See http://trac.mapfish.org/trac/mapfish/wiki/PrintModuleServer#Metadatadefinition
 */
public class MetaData {
    private String title;
    private String author;
    private String subject;
    private String keywords;
    private String creator;
    private boolean supportLegacyReader;

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public void setSupportLegacyReader(boolean supportLegacyReader) {
        this.supportLegacyReader = supportLegacyReader;
    }

    public boolean isSupportLegacyReader() {
        return supportLegacyReader;
    }

    public void render(PJsonObject params, RenderingContext context) {
        final Document doc = context.getDocument();

        if (title != null) {
            doc.addTitle(PDFUtils.evalString(context, params, title));
        }

        if (author != null) {
            doc.addAuthor(PDFUtils.evalString(context, params, author));
        }

        if (subject != null) {
            doc.addSubject(PDFUtils.evalString(context, params, subject));
        }

        if (keywords != null) {
            doc.addKeywords(PDFUtils.evalString(context, params, keywords));
        }

        if (creator != null) {
            doc.addCreator(PDFUtils.evalString(context, params, creator));
        }
    }
}
