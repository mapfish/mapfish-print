package org.mapfish.print.map.readers;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.mapfish.print.ShellMapPrinter;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MapReaderFactoryFinderTest {

	@Test
	public void test() {
		MapReaderFactoryFinder ff = new ClassPathXmlApplicationContext(ShellMapPrinter.DEFAULT_SPRING_CONTEXT).getBean(MapReaderFactoryFinder.class);
		
		assertNotNull(ff.getFactory("WMS"));		
		assertNotNull(ff.getFactory("Wms"));		
		assertNotNull(ff.getFactory("MapServer"));		
		assertNotNull(ff.getFactory("TileCache"));		
		assertNotNull(ff.getFactory("Osm"));		
		assertNotNull(ff.getFactory("Xyz"));		
		assertNotNull(ff.getFactory("Tms"));		
		assertNotNull(ff.getFactory("Vector"));		
		assertNotNull(ff.getFactory("Image"));		
		assertNotNull(ff.getFactory("TiledGoogle"));		
		assertNotNull(ff.getFactory("Google"));		
		assertNotNull(ff.getFactory("KaMapCache"));		
		assertNotNull(ff.getFactory("KaMap"));		
		assertNotNull(ff.getFactory("WMTS"));		
	}

}
