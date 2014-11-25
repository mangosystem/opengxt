package org.geotools.process.spatialstatistics;

import java.util.Set;

import org.geotools.feature.NameImpl;
import org.junit.Test;
import org.opengis.feature.type.Name;

public class CountFeaturesProcessFactoryTest extends SpatialStatisticsTestCase {

    CountFeaturesProcessFactory factory = new CountFeaturesProcessFactory();

    @Test
    public void testLookup() {
        Set<Name> names = factory.getNames();
        assertFalse(names.isEmpty());
        assertTrue(names.contains(new NameImpl("statistics", "CountFeatures")));
    }
}
