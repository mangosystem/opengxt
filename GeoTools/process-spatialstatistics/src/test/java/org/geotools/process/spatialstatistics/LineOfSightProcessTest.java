package org.geotools.process.spatialstatistics;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.util.factory.GeoTools;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

public class LineOfSightProcessTest extends SpatialStatisticsTestCase {

    @Test
    public void test() throws Exception {
        File file = new File(FileUtils.toFile(url(this, null)), "sfdem.tif");

        // open coverage
        AbstractGridFormat format = GridFormatFinder.findFormat(file);
        GridCoverage2DReader reader = format.getReader(file);
        GridCoverage2D inputCoverage = reader.read(null);
        assertNotNull(inputCoverage);

        // build parameters
        GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(GeoTools.getDefaultHints());
        Geometry observerPoint = gf.createPoint(new Coordinate(601189.4037, 4917842.7523));
        Geometry targetPoint = gf.createPoint(new Coordinate(600605.9174, 4922919.0826));

        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterLinearLOSProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterLinearLOSProcessFactory.observerPoint.key, observerPoint);
        map.put(RasterLinearLOSProcessFactory.observerOffset.key, 1.6d);
        map.put(RasterLinearLOSProcessFactory.targetPoint.key, targetPoint);
        map.put(RasterLinearLOSProcessFactory.useCurvature.key, Boolean.TRUE);

        // execute process
        SimpleFeatureCollection result = null;

        org.geotools.process.Process process = new RasterLinearLOSProcess(null);
        Map<String, Object> resultMap = process.execute(map, null);
        result = (SimpleFeatureCollection) resultMap.get(RasterLinearLOSProcessFactory.RESULT.key);
        assertTrue(result.size() > 0);
    }
}
