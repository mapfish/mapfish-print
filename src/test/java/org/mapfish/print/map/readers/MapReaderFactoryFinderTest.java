package org.mapfish.print.map.readers;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.mapfish.print.ShellMapPrinter;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MapReaderFactoryFinderTest {

	@Test
	public void test() {
		MapReaderFactoryFinder ff = new ClassPathXmlApplicationContext(ShellMapPrinter.DEFAULT_SPRING_CONTEXT).getBean(MapReaderFactoryFinder.class);
		
		assertNotNull(ff.getFactory(null, "WMS"));
		assertNotNull(ff.getFactory(null, "Wms"));
		assertNotNull(ff.getFactory(null, "MapServer"));
		assertNotNull(ff.getFactory(null, "TileCache"));
		assertNotNull(ff.getFactory(null, "Osm"));
		assertNotNull(ff.getFactory(null, "Xyz"));
		assertNotNull(ff.getFactory(null, "Tms"));
		assertNotNull(ff.getFactory(null, "Vector"));
		assertNotNull(ff.getFactory(null, "Image"));
		assertNotNull(ff.getFactory(null, "TiledGoogle"));
		assertNotNull(ff.getFactory(null, "Google"));
		assertNotNull(ff.getFactory(null, "KaMapCache"));
		assertNotNull(ff.getFactory(null, "KaMap"));
		assertNotNull(ff.getFactory(null, "WMTS"));
	}

}
