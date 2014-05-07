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

package org.mapfish.print.servlet.fileloader;

import com.google.common.collect.Iterators;

import java.io.File;
import java.net.URI;
import java.util.Iterator;

/**
 * A plugin that loads the config resources from urls starting with prefix:
 * {@value org.mapfish.print.servlet.fileloader.FileConfigFileLoader#PREFIX}://.
 *
 * @author Jesse on 4/28/2014.
 */
public final class FileConfigFileLoader extends AbstractFileConfigFileLoader {
    static final String PREFIX = "file";

    @Override
    protected Iterator<File> resolveFiles(final URI fileURI) {
        final File file = new File(fileURI);

        return Iterators.singletonIterator(file);
    }

    @Override
    public String getUriScheme() {
        return PREFIX;
    }
}
