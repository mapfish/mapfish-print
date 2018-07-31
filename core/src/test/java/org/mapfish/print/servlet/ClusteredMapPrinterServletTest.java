package org.mapfish.print.servlet;

import org.junit.Ignore;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = {
        ClusteredMapPrinterServletTest.CLUSTERED_CONTEXT
})
@Ignore //db must be set up to run this test
public class ClusteredMapPrinterServletTest extends MapPrinterServletTest {

    public static final String CLUSTERED_CONTEXT =
            "classpath:org/mapfish/print/servlet/mapfish-spring-application-context-clustered.xml";

}
