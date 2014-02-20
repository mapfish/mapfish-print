/*
 * Copyright (C) 2013  Camptocamp
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

package org.mapfish.print.map.readers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.junit.Test;
import org.mapfish.print.FakeHttpd;
import org.mapfish.print.MapTestBasic;
import org.mapfish.print.Transformer;
import org.mapfish.print.map.ParallelMapTileLoader;
import org.mapfish.print.map.renderers.TileRenderer;
import org.mapfish.print.map.renderers.TileRenderer.Format;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;
import org.pvalsecc.misc.URIUtils;

public class HttpMapReaderTest extends MapTestBasic {
	
    @Test
    public void testMergeAllLayersWithParam() throws Exception {    	
        URI commonURI = createUri(loadJson("mergeable/test1.json"))[0];
        final Map<String, List<String>> parameters = URIUtils.getParameters(commonURI.getRawQuery().toUpperCase());
        assertEquals(""+commonURI, "ATTRIBUTE1=1;ATTRIBUTE1=1", parameters.get("CQL_FILTER").get(0));
        assertCommonParams(commonURI, parameters, "TRUE");
    }
    
    @Test
    public void testMergeSomeLayersWithParam() throws Exception {    	
        URI commonURI = createUri(loadJson("mergeable/test2.json"))[0];
        final Map<String, List<String>> parameters = URIUtils.getParameters(commonURI.getRawQuery().toUpperCase());
        assertEquals(""+commonURI, "ATTRIBUTE1=1;INCLUDE", parameters.get("CQL_FILTER").get(0));        
        assertCommonParams(commonURI, parameters, "TRUE");
    }
    
    @Test
    public void testMergeNoLayersWithParam() throws Exception {    	
        URI commonURI = createUri(loadJson("mergeable/test3.json"))[0];
        final Map<String, List<String>> parameters = URIUtils.getParameters(commonURI.getRawQuery().toUpperCase());
        assertNull(""+commonURI, parameters.get("CQL_FILTER"));        
        assertCommonParams(commonURI, parameters, "TRUE");
    }
    
    @Test
    public void testCantMergeIfDifferentCustomParam() throws Exception {    	
        URI[] commonURIs = createUri(loadJson("mergeable/test4.json"));
        
        Map<String, List<String>> parameters = URIUtils.getParameters(commonURIs[0].getRawQuery().toUpperCase());
        assertEquals(""+commonURIs[0], "ATTRIBUTE1=1", parameters.get("CQL_FILTER").get(0));
        assertCommonParams(commonURIs[0], parameters, "TRUE");
        
        parameters = URIUtils.getParameters(commonURIs[1].getRawQuery().toUpperCase());
        assertEquals(""+commonURIs[1], "ATTRIBUTE1=1", parameters.get("CQL_FILTER").get(0));        
        assertCommonParams(commonURIs[1], parameters, "FALSE");
    }
    
    @Test
    public void testCantMergeIfDifferentContexts() throws Exception {        
        URI[] commonURIs = createUri(loadJson("mergeable/test6.json"));
        
        Map<String, List<String>> parameters = URIUtils.getParameters(commonURIs[0].getRawQuery().toUpperCase());
        assertEquals(""+commonURIs[0], "ATTRIBUTE1=1", parameters.get("OTHER_PARAM").get(0));
        
        parameters = URIUtils.getParameters(commonURIs[1].getRawQuery().toUpperCase());
        assertEquals(""+commonURIs[1], "ATTRIBUTE1=1", parameters.get("OTHER_PARAM").get(0));        
    }
    
    @Test
    public void testMergeIfSameContexts() throws Exception {        
        URI[] commonURIs = createUri(loadJson("mergeable/test7.json"));
        
        Map<String, List<String>> parameters = URIUtils.getParameters(commonURIs[0].getRawQuery().toUpperCase());
        assertEquals(""+commonURIs[0], "ATTRIBUTE1=1,ATTRIBUTE1=1", parameters.get("OTHER_PARAM").get(0));        
    }
    
    protected void assertCommonParams(URI uri, Map<String, List<String>> parameters, String expectedTransparent) {
        assertEquals(""+uri, "INIMAGE", parameters.get("EXCEPTIONS").get(0));
        assertEquals(""+uri, expectedTransparent, parameters.get("TRANSPARENT").get(0));
    }    
    
    @Override
    protected PJsonObject createGlobalParams() throws IOException {
        return loadJson("mergeable/global.json");
    }

    private URI[] createUri(PJsonObject jsonParams, FakeHttpd.Route... routes)
            throws IOException, JSONException, URISyntaxException {

        PJsonArray layers = jsonParams.getJSONArray("layers");
        HTTPMapReader mapReader = null;
        HTTPMapReader currentReader = null;
        for (int count = 0; count < layers.size(); count++) {
            PJsonObject layer = layers.getJSONObject(count);
            PJsonArray layersInt = layer.getJSONArray("layers");
            for(int countInt = 0; countInt < layersInt.size(); countInt++) {
                currentReader = createMapReader(layer);
                if (mapReader == null) {
                    mapReader = currentReader;
                } else {
                    mapReader.testMerge(currentReader);
                }
            }
        }

        return new URI[] { mapReader.createCommonURI(null, "", true),
                currentReader.createCommonURI(null, "", true) };

    }
	
    private HTTPMapReader createMapReader(PJsonObject layer) {
        final HTTPMapReader mapReader = new HTTPMapReader(context, layer) {

            @Override
            protected void renderTiles(TileRenderer formater,
                    Transformer transformer, URI commonUri,
                    ParallelMapTileLoader parallelMapTileLoader)
                    throws IOException, URISyntaxException {

            }

            @Override
            protected Format getFormat() {
                return null;
            }

            @Override
            protected void addCommonQueryParams(
                    Map<String, List<String>> result, Transformer transformer,
                    String srs, boolean first) {
            }

            @Override
            public String toString() {
                return null;
            }

        };
        return mapReader;
    }
    
}
