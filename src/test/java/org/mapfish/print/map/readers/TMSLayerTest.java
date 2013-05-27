package org.mapfish.print.map.readers;

import org.junit.Test;
import org.mapfish.print.ShellMapPrinter;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.assertNotNull;

public class TMSLayerTest {

    @Override
    protected void setUp() throws Exception {

    }

    @Override
    protected void tearDown() throws Exception {

        //super.tearDown();
    }

        @Test
	public void testNoOrigin() {
		MapReaderFactoryFinder ff = new ClassPathXmlApplicationContext(ShellMapPrinter.DEFAULT_SPRING_CONTEXT).getBean(MapReaderFactoryFinder.class);

            //"Preparing for future tests
            assertTrue(true);
	}

    @Test
    public void testOrigin() {
        //"Preparing for future tests
        assertTrue(true);
    }

}
