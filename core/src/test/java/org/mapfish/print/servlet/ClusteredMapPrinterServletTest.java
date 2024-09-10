package org.mapfish.print.servlet;

import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = {ClusteredMapPrinterServletTest.CLUSTERED_CONTEXT})
public class ClusteredMapPrinterServletTest extends MapPrinterServletTest {

  public static final String CLUSTERED_CONTEXT =
      "classpath:org/mapfish/print/servlet/mapfish-spring-application-context-clustered.xml";
}
