package org.mapfish.print.servlet.fileloader;

import com.google.common.collect.Iterators;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import javax.servlet.ServletContext;

/**
 * A plugin that loads the config resources from urls starting with prefix: {@value
 * org.mapfish.print.servlet.fileloader.ServletConfigFileLoader#PREFIX}://.
 */
public final class ServletConfigFileLoader extends AbstractFileConfigFileLoader {

    private static final String PREFIX = "servlet";
    private static final int PREFIX_LENGTH = (PREFIX + "://").length();

    @Autowired
    private ServletContext servletContext;

    @Override
    protected Iterator<File> resolveFiles(final URI fileURI) {
        if (fileURI.getScheme() != null && fileURI.getScheme().equals("file") && new File(fileURI).exists()) {
            return Iterators.singletonIterator(new File(fileURI));
        }
        if (fileURI.toString().startsWith(PREFIX)) {
            String path = fileURI.toString().substring(PREFIX_LENGTH);
            final String realPath = this.servletContext.getRealPath(path);
            if (realPath == null) {
                return Collections.emptyIterator();
            }
            return Iterators.singletonIterator(new File(realPath));
        }
        return Collections.emptyIterator();
    }

    @Override
    public String getUriScheme() {
        return PREFIX;
    }

    public void setServletContext(final ServletContext servletContext) {
        this.servletContext = servletContext;
    }
}
