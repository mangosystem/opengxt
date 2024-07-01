package org.geotools.process.spatialstatistics;

import java.util.Set;

import org.geotools.api.feature.type.Name;
import org.geotools.feature.NameImpl;
import org.junit.Test;

public class CountFeaturesProcessFactoryTest extends SpatialStatisticsTestCase {

    CountFeaturesProcessFactory factory = new CountFeaturesProcessFactory();

    @Test
    public void test() {
        Set<Name> names = factory.getNames();
        assertFalse(names.isEmpty());
        assertTrue(names.contains(new NameImpl("statistics", "CountFeatures")));
    }
}
