package org.geotools.process.spatialstatistics;

import java.util.HashMap;
import java.util.Map;

import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.grid.hexagon.HexagonOrientation;
import org.junit.Test;

public class HexagonProcessTest extends SpatialStatisticsTestCase {

    @Test
    public void test() throws Exception {
        SimpleFeatureSource source = dataStore.getFeatureSource("hexa");
        assertTrue(source.getCount(Query.ALL) > 0);

        Map<String, Object> input = new HashMap<String, Object>();
        input.put(HexagonProcessFactory.extent.key, source.getBounds());
        input.put(HexagonProcessFactory.boundsSource.key, source.getFeatures());
        input.put(HexagonProcessFactory.orientation.key, HexagonOrientation.ANGLED);
        input.put(HexagonProcessFactory.sideLen.key, source.getBounds().getWidth() / 5.0);

        SimpleFeatureCollection result = null;

        // direct
        org.geotools.process.Process process = new HexagonProcess(null);
        Map<String, Object> resultMap = process.execute(input, null);
        result = (SimpleFeatureCollection) resultMap.get(HexagonProcessFactory.RESULT.key);
        assertTrue(result.size() > 0);

        // process factory
        HexagonProcessFactory factory = new HexagonProcessFactory();
        process = factory.create();
        resultMap = process.execute(input, null);
        result = (SimpleFeatureCollection) resultMap.get(AreaProcessFactory.RESULT.key);
        assertTrue(result.size() > 0);
    }

}
