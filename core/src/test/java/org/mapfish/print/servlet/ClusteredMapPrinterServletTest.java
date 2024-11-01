package org.mapfish.print.servlet;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = {ClusteredMapPrinterServletTest.CLUSTERED_CONTEXT})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ClusteredMapPrinterServletTest extends MapPrinterServletTest {

  public static final String CLUSTERED_CONTEXT =
      "classpath:org/mapfish/print/servlet/mapfish-spring-application-context-clustered-test.xml";
}
