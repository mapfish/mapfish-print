package org.mapfish.print.servlet.fileloader;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;

/**
 * A plugin that loads the config resources from urls starting with prefix: {@value #PREFIX}://.
 */
public final class FileConfigFileLoader extends AbstractFileConfigFileLoader {
    public static final String PREFIX = "file";

    @Override
    protected Iterator<File> resolveFiles(final URI fileURI) {
        File file = platformIndependentUriToFile(fileURI);
        return Collections.singletonList(file).iterator();
    }

    @Override
    public String getUriScheme() {
        return PREFIX;
    }
}
