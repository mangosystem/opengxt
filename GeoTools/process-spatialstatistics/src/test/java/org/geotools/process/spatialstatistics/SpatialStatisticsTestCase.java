package org.geotools.process.spatialstatistics;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.junit.After;
import org.junit.Before;

public abstract class SpatialStatisticsTestCase extends TestCase {
    private static final String DIRECTORY = "test-data";

    protected DataStore dataStore;

    @Override
    @Before
    protected void setUp() throws Exception {
        super.setUp();
        Map<String, Object> params = new HashMap<String, Object>();

        final URL url = url(this, null);
        params.put(ShapefileDataStoreFactory.URLP.key, url);
        params.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, true);
        params.put(ShapefileDataStoreFactory.DBFCHARSET.key, "UTF-8");
        params.put(ShapefileDataStoreFactory.MEMORY_MAPPED.key, Boolean.TRUE);

        dataStore = DataStoreFinder.getDataStore(params);
    }

    @Override
    @After
    protected void tearDown() throws Exception {
        super.tearDown();
        dataStore.dispose();
    }

    protected URL url(final Object caller, final String path) throws FileNotFoundException {
        final URL url = getResource(caller, path);
        if (url == null) {
            throw new FileNotFoundException("Can not locate test-data for \"" + path + '"');
        }
        return url;
    }

    private static URL getResource(final Object caller, String name) {
        if (name == null || (name = name.trim()).length() == 0) {
            name = DIRECTORY;
        } else {
            name = DIRECTORY + '/' + name;
        }
        if (caller != null) {
            final Class<?> c = (caller instanceof Class) ? (Class<?>) caller : caller.getClass();
            return c.getResource(name);
        } else {
            return Thread.currentThread().getContextClassLoader().getResource(name);
        }
    }
}
