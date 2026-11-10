package org.geotools.process.spatialstatistics;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.gridcoverage.RasterFunctionalSurface;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class LOSOptionsTest {
    private final GeometryFactory gf = new GeometryFactory();

    @Test
    public void preservesLegacyResultsAndLocksVisibilityWithoutShorteningRay() {
        GridCoverage2D dem = TerrainTestSupport.dem(new double[][] { { 0, 0, 10, 0, 0, 100, 0 } });
        try {
            RasterFunctionalSurface surface = new RasterFunctionalSurface(dem);
            LineString ray = gf.createLineString(new Coordinate[] {
                    new Coordinate(.5, .5), new Coordinate(6.5, .5) });
            LineString legacy = surface.getLineOfSight(ray, 1.6, false, false, .13);
            LineString explicitFalse = surface.getLineOfSight(ray, 1.6, false, false, .13, false);
            LineString locked = surface.getLineOfSight(ray, 1.6, false, false, .13, true);
            assertArrayEquals(new double[] { 1, 1, 1, 0, 0, 1, 0 }, flags(legacy), 0);
            assertArrayEquals(flags(legacy), flags(explicitFalse), 0);
            assertArrayEquals(new double[] { 1, 1, 1, 0, 0, 0, 0 }, flags(locked), 0);
            assertEquals(legacy.getLength(), locked.getLength(), 0);
            assertTrue(legacy.getEndPoint().equalsExact(locked.getEndPoint()));

            Map<String, Object> input = new HashMap<String, Object>();
            input.put("inputCoverage", dem);
            input.put("observerPoint", gf.createPoint(new Coordinate(.5, .5)));
            input.put("targetPoint", gf.createPoint(new Coordinate(6.5, .5)));
            input.put("observerOffset", 1.6d);
            SimpleFeatureCollection before = (SimpleFeatureCollection) new RasterLinearLOSProcess(null)
                    .execute(input, null).get("result");
            input.put("stopAtFirstInvisible", false);
            SimpleFeatureCollection omitted = (SimpleFeatureCollection) new RasterLinearLOSProcess(null)
                    .execute(input, null).get("result");
            assertEquals(length(before, 1), length(omitted, 1), 0);
            input.put("stopAtFirstInvisible", true);
            SimpleFeatureCollection after = (SimpleFeatureCollection) new RasterLinearLOSProcess(null)
                    .execute(input, null).get("result");
            assertEquals(4, length(before, 1), 1e-10);
            assertEquals(3, length(after, 1), 1e-10);
            assertEquals(3, length(after, 0), 1e-10);
        } finally {
            dem.dispose(true);
        }
    }

    @Test
    public void radialRaysResetTheirBlockedState() {
        double[][] heights = new double[13][13];
        for (int y = 0; y < 13; y++) {
            for (int x = 0; x < 13; x++) {
                int d = Math.max(Math.abs(x - 6), Math.abs(y - 6));
                heights[y][x] = d == 2 ? 10 : d == 5 ? 100 : 0;
            }
        }
        GridCoverage2D dem = TerrainTestSupport.dem(heights);
        try {
            Geometry observer = gf.createPoint(new Coordinate(6.5, 6.5));
            SimpleFeatureCollection old = RasterRadialLOSProcess.process(dem, observer, 1.6,
                    6d, 4, false, false, .13, null);
            SimpleFeatureCollection explicitFalse = RasterRadialLOSProcess.process(dem, observer, 1.6,
                    6d, 4, false, false, .13, false, null);
            SimpleFeatureCollection locked = RasterRadialLOSProcess.process(dem, observer, 1.6,
                    6d, 4, false, false, .13, true, null);
            assertNotNull(old);
            assertNotNull(locked);
            assertEquals(length(old, 1), length(explicitFalse, 1), 1e-10);
            assertEquals(24, length(locked, 1) + length(locked, 0), 1e-8);
            Set<Integer> angles = new HashSet<Integer>();
            try (SimpleFeatureIterator it = locked.features()) {
                while (it.hasNext()) {
                    SimpleFeature f = it.next();
                    if (((Number) f.getAttribute("Visible")).intValue() == 1) {
                        angles.add(((Number) f.getAttribute("Angle")).intValue());
                    }
                }
            }
            assertEquals(4, angles.size());
            assertTrue(length(old, 1) > length(locked, 1));
        } finally {
            dem.dispose(true);
        }
    }

    private double[] flags(LineString line) {
        Coordinate[] coordinates = line.getCoordinates();
        double[] result = new double[coordinates.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = coordinates[i].z;
        }
        return result;
    }

    private double length(SimpleFeatureCollection features, int visible) {
        double sum = 0;
        try (SimpleFeatureIterator it = features.features()) {
            while (it.hasNext()) {
                SimpleFeature f = it.next();
                if (((Number) f.getAttribute("Visible")).intValue() == visible) {
                    sum += ((Geometry) f.getDefaultGeometry()).getLength();
                }
            }
        }
        return sum;
    }
}
