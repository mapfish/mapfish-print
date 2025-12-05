package org.mapfish.print.servlet.fileloader;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import jakarta.servlet.ServletContext;
import org.springframework.beans.factory.annotation.Autowired;

/** A plugin that loads the config resources from urls starting with prefix: {@value #PREFIX}://. */
public final class ServletConfigFileLoader extends AbstractFileConfigFileLoader {

  public static final String PREFIX = "servlet";
  public static final int PREFIX_LENGTH = (PREFIX + "://").length();

  @Autowired private ServletContext servletContext;

  @Override
  protected Iterator<File> resolveFiles(final URI fileURI) {
    if (fileURI.getScheme() != null
        && fileURI.getScheme().equals("file")
        && new File(fileURI).exists()) {
      return Collections.singletonList(new File(fileURI)).iterator();
    }
    if (fileURI.toString().startsWith(PREFIX)) {
      String path = fileURI.toString().substring(PREFIX_LENGTH);
      final String realPath = this.servletContext.getRealPath(path);
      if (realPath == null) {
        return Collections.emptyIterator();
      }
      return Collections.singletonList(new File(realPath)).iterator();
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
