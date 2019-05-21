package org.geotools.process.spatialstatistics;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.gridcoverage.RasterAspectOperation;
import org.geotools.process.spatialstatistics.gridcoverage.RasterClipOperation;
import org.geotools.process.spatialstatistics.gridcoverage.RasterHelper;
import org.geotools.util.factory.GeoTools;
import org.junit.Test;
import org.locationtech.jts.geom.GeometryFactory;

public class SurfaceAnalysisProcessTest extends SpatialStatisticsTestCase {

    static GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(GeoTools.getDefaultHints());

    @Test
    public void test() throws Exception {
        File file = new File(FileUtils.toFile(url(this, null)), "sfdem.tif");

        // open coverage
        AbstractGridFormat format = GridFormatFinder.findFormat(file);
        GridCoverage2DReader reader = format.getReader(file);
        GridCoverage2D inputCoverage = reader.read(null);
        assertNotNull(inputCoverage);

        ReferencedEnvelope extent = new ReferencedEnvelope(inputCoverage.getEnvelope());
        extent.expandBy(-extent.getWidth() / 3, -extent.getWidth() / 3);

        RasterAspectOperation aspectOp = new RasterAspectOperation();

        GridCoverage2D croped = new RasterClipOperation().execute(inputCoverage, extent);
        RasterHelper.describe(croped);
        assertNotNull(aspectOp.execute(croped));
    }
}
