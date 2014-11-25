package org.geotools.process.spatialstatistics;

import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.geotools.data.property.PropertyDataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.test.TestData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AreaProcessTest extends SpatialStatisticsTestCase {
    DataStore dataStore;

    @Before
    protected void setUp() throws Exception {
        super.setUp();
        dataStore = new PropertyDataStore(TestData.file(this, null));
    }

    @After
    protected void tearDown() throws Exception {
        super.tearDown();
        dataStore.dispose();
    }

    @Test
    public void test() throws Exception {
        SimpleFeatureSource source = dataStore.getFeatureSource("features");
        assertTrue(source.getCount(Query.ALL) > 0);

        Map<String, Object> input = new HashMap<String, Object>();
        input.put(AreaProcessFactory.inputFeatures.key, source.getFeatures());

        // direct
        org.geotools.process.Process process = new AreaProcess(null);
        Map<String, Object> resultMap = process.execute(input, null);
        Object result = resultMap.get(AreaProcessFactory.RESULT.key);
        assertNotNull("result area = ", result);

        // process factory
        AreaProcessFactory factory = new AreaProcessFactory();
        process = factory.create();
        resultMap = process.execute(input, null);
        result = resultMap.get(AreaProcessFactory.RESULT.key);
        assertNotNull("result area = ", result);
    }
}
