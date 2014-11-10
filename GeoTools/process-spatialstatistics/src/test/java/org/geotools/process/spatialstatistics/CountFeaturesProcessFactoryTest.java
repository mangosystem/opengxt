package org.geotools.process.spatialstatistics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.geotools.feature.NameImpl;
import org.junit.Test;
import org.opengis.feature.type.Name;

public class CountFeaturesProcessFactoryTest {

    CountFeaturesProcessFactory factory = new CountFeaturesProcessFactory();

    @Test
    public void testLookup() {
        Set<Name> names = factory.getNames(); 
        assertFalse(names.isEmpty());
        assertTrue(names.contains(new NameImpl("ss", "Count Features")));
    }
}
