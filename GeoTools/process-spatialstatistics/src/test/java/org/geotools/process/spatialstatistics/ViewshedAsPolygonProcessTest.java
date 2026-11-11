package org.geotools.process.spatialstatistics;

import static org.junit.Assert.*;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.io.File;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.process.ProcessException;
import org.geotools.process.Processors;
import org.geotools.process.spatialstatistics.gridcoverage.RasterViewshedAsPolygonOperation;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultEngineeringCRS;
import org.geotools.util.NullProgressListener;
import org.junit.Assume;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.strtree.STRtree;

public class ViewshedAsPolygonProcessTest {
    private final GeometryFactory gf = new GeometryFactory();

    @Test
    public void preservesInvisibleZeroNoDataHolesAndAffineCellAreas() {
        double[][] mask = new double[5][5];
        for (int y = 0; y < 5; y++) java.util.Arrays.fill(mask[y], 1);
        mask[0][0] = mask[0][1] = mask[1][0] = mask[1][1] = 0;
        mask[2][2] = 255;
        GridCoverage2D visibility = TerrainTestSupport.dem(mask,
                new AffineTransform(30, 4, 8, -30, 198000, 451000),
                DefaultEngineeringCRS.CARTESIAN_2D, 255d);
        try {
            SimpleFeatureCollection result = new RasterViewshedAsPolygonOperation().execute(visibility, null);
            assertEquals(2, result.size());
            assertMatchesRaster(visibility, result);
            boolean hole = false;
            try (SimpleFeatureIterator it = result.features()) {
                while (it.hasNext()) {
                    MultiPolygon geometry = (MultiPolygon) it.next().getDefaultGeometry();
                    hole |= ((Polygon) geometry.getGeometryN(0)).getNumInteriorRing() > 0;
                }
            }
            assertTrue("The centre NoData cell must remain a hole", hole);
            // The converter borrows its input; disposing its JAI operation must not close it.
            assertEquals(255, visibility.getRenderedImage().getData().getSample(2, 2, 0));
        } finally {
            visibility.dispose(true);
        }
    }

    @Test
    public void diagonalCellsAreSeparateAndSingleCellRegionsAreKept() {
        GridCoverage2D visibility = TerrainTestSupport.dem(new double[][] { { 1, 255 }, { 255, 1 } });
        try {
            SimpleFeatureCollection result = new RasterViewshedAsPolygonOperation().execute(visibility, null);
            assertEquals(2, result.size());
            assertMatchesRaster(visibility, result);
        } finally { visibility.dispose(true); }
    }

    @Test
    public void emptyNoDataResultRetainsPolygonSchema() {
        GridCoverage2D visibility = TerrainTestSupport.dem(new double[][] { { 255, 255 } });
        try {
            SimpleFeatureCollection result = new RasterViewshedAsPolygonOperation().execute(visibility, null);
            assertTrue(result.isEmpty());
            assertEquals(Integer.class, result.getSchema().getDescriptor("Visible").getType().getBinding());
            assertEquals(MultiPolygon.class, result.getSchema().getGeometryDescriptor().getType().getBinding());
        } finally { visibility.dispose(true); }
    }

    @Test
    public void supportsNonZeroImageOrigin() {
        GridCoverage2D visibility = TerrainTestSupport.dem(new double[][] {
                { 1, 1, 0, 255 }, { 1, 0, 0, 0 }, { 255, 0, 1, 1 } },
                new AffineTransform(30, 4, 8, -30, 1000, 2000),
                DefaultEngineeringCRS.CARTESIAN_2D, 255d, 37, -19);
        try {
            assertMatchesRaster(visibility, new RasterViewshedAsPolygonOperation().execute(visibility, null));
        } finally { visibility.dispose(true); }
    }

    @Test
    public void processUsesViewshedInputsDefaultsAndCompletesAfterPolygonization() {
        double[][] heights = new double[9][11];
        for (int y = 0; y < heights.length; y++) heights[y][4] = 40;
        GridCoverage2D dem = TerrainTestSupport.dem(heights);
        Geometry observer = gf.createPoint(new Coordinate(2.5, 4.5));
        GridCoverage2D expected = RasterViewshedProcess.process(dem, observer,
                null, 4d, null, null, null, null);
        try {
            RecordingProgress progress = new RecordingProgress();
            SimpleFeatureCollection result = RasterViewshedAsPolygonProcess.process(dem, observer,
                    null, 4d, null, null, null, progress);
            assertMatchesRaster(expected, result);
            assertTrue(progress.completed);
            assertEquals(100f, progress.last, 0);
            // The input DEM remains usable after the process disposes its intermediate raster.
            GridCoverage2D again = RasterViewshedProcess.process(dem, observer, null, 4d, null, null, null, null);
            try { assertEquals(expected.getEnvelope(), again.getEnvelope()); }
            finally { again.dispose(true); }
        } finally {
            expected.dispose(true);
            dem.dispose(true);
        }
    }

    @Test
    public void factoryIsRegisteredWithIdenticalInputContractAndFeatureResult() {
        assertNotNull(Processors.createProcess(new NameImpl("statistics", "ViewshedAsPolygon")));
        RasterViewshedAsPolygonProcessFactory factory = new RasterViewshedAsPolygonProcessFactory();
        assertEquals(new RasterViewshedProcessFactory().getParameterInfo(), factory.getParameterInfo());
        assertEquals(SimpleFeatureCollection.class, factory.getResultInfo(null).get("result").type);
        assertFalse(factory.getTitle().toString().isEmpty());
    }

    @Test
    public void propagatesInvalidInputAndCancellation() {
        GridCoverage2D dem = TerrainTestSupport.dem(new double[][] { { 0, 0, 0 } });
        try {
            try {
                RasterViewshedAsPolygonProcess.process(dem, gf.createPoint(new Coordinate(.5, .5)),
                        1.6, -1d, false, false, .13, null);
                fail("Negative radius must be rejected");
            } catch (ProcessException expected) { assertNotNull(expected.getMessage()); }
            RecordingProgress progress = new RecordingProgress() {
                @Override public void progress(float value) {
                    super.progress(value);
                    if (value >= 80) setCanceled(true);
                }
            };
            try {
                RasterViewshedAsPolygonProcess.process(dem, gf.createPoint(new Coordinate(.5, .5)),
                        1.6, 0d, false, false, .13, progress);
                fail("Cancellation between the raster and polygon stages must propagate");
            } catch (ProcessException expected) { assertTrue(expected.getMessage().contains("cancelled")); }
            assertFalse(progress.completed);
        } finally { dem.dispose(true); }
        GridCoverage2D invalid = TerrainTestSupport.dem(new double[][] { { 2 } });
        try {
            try {
                new RasterViewshedAsPolygonOperation().execute(invalid, null);
                fail("Unexpected visibility values must fail instead of yielding partial output");
            } catch (ProcessException expected) { assertTrue(expected.getMessage().contains("0, 1 or 255")); }
        } finally { invalid.dispose(true); }
    }

    /** Opt-in actual DEM validation; normal builds need no external dataset. */
    @Test
    public void demPolygonsReproduceEveryRasterCellAndClassArea() throws Exception {
        String path = System.getProperty("viewshed.dem");
        Assume.assumeNotNull(path);
        GeoTiffReader reader = new GeoTiffReader(new File(path));
        GridCoverage2D dem = reader.read(null);
        try {
            AffineTransform transform = (AffineTransform) dem.getGridGeometry().getGridToCRS2D();
            Point2D center = transform.transform(new Point2D.Double(
                    dem.getRenderedImage().getMinX() + dem.getRenderedImage().getWidth() / 2,
                    dem.getRenderedImage().getMinY() + dem.getRenderedImage().getHeight() / 2), null);
            Geometry observer = gf.createPoint(new Coordinate(center.getX(), center.getY()));
            for (double radius : new double[] { 1000, 5000, 10000, 0 }) {
                GridCoverage2D expected = RasterViewshedProcess.process(dem, observer,
                        1.6, radius, false, false, .13, null);
                try {
                    SimpleFeatureCollection result = RasterViewshedAsPolygonProcess.process(dem, observer,
                            1.6, radius, false, false, .13, null);
                    long checked = assertMatchesRaster(expected, result);
                    System.out.println("ViewshedAsPolygon radius=" + radius + " features=" + result.size()
                            + " checkedCells=" + checked + " mismatches=0");
                } finally { expected.dispose(true); }
            }
        } finally {
            dem.dispose(true);
            reader.dispose();
        }
    }

    private long assertMatchesRaster(GridCoverage2D raster, SimpleFeatureCollection polygons) {
        assertTrue(CRS.equalsIgnoreMetadata(raster.getCoordinateReferenceSystem2D(),
                polygons.getSchema().getCoordinateReferenceSystem()));
        assertEquals(Integer.class, polygons.getSchema().getDescriptor("Visible").getType().getBinding());
        assertEquals(Double.class, polygons.getSchema().getDescriptor("area").getType().getBinding());
        AffineTransform worldToGrid;
        try {
            worldToGrid = ((AffineTransform) raster.getGridGeometry().getGridToCRS2D(
                    org.opengis.metadata.spatial.PixelOrientation.UPPER_LEFT)).createInverse();
        } catch (java.awt.geom.NoninvertibleTransformException e) {
            throw new AssertionError(e);
        }
        STRtree index = new STRtree();
        double[] areas = new double[2];
        try (SimpleFeatureIterator it = polygons.features()) {
            while (it.hasNext()) {
                SimpleFeature feature = it.next();
                assertTrue(feature.getDefaultGeometry() instanceof MultiPolygon);
                MultiPolygon geometry = (MultiPolygon) feature.getDefaultGeometry();
                assertTrue("Invalid polygon: " + feature.getID(), geometry.isValid());
                for (Coordinate coordinate : geometry.getCoordinates()) {
                    Point2D grid = worldToGrid.transform(new Point2D.Double(coordinate.x, coordinate.y), null);
                    assertEquals("Boundary must remain on a cell edge", Math.rint(grid.getX()), grid.getX(), 1e-7);
                    assertEquals("Boundary must remain on a cell edge", Math.rint(grid.getY()), grid.getY(), 1e-7);
                }
                int value = (Integer) feature.getAttribute("Visible");
                assertTrue(value == 0 || value == 1);
                assertEquals(geometry.getArea(), (Double) feature.getAttribute("area"), 0);
                areas[value] += geometry.getArea();
                index.insert(geometry.getEnvelopeInternal(), new Region(geometry, value));
            }
        }
        index.build();
        Raster pixels = raster.getRenderedImage().getData();
        AffineTransform transform = (AffineTransform) raster.getGridGeometry().getGridToCRS2D();
        long[] counts = new long[2];
        for (int y = pixels.getMinY(); y < pixels.getMinY() + pixels.getHeight(); y++) {
            for (int x = pixels.getMinX(); x < pixels.getMinX() + pixels.getWidth(); x++) {
                Point2D world = transform.transform(new Point2D.Double(x, y), null);
                Point point = gf.createPoint(new Coordinate(world.getX(), world.getY()));
                @SuppressWarnings("unchecked") List<Region> candidates = index.query(new Envelope(point.getCoordinate()));
                int matches = 0, actual = 255;
                for (Region region : candidates) {
                    if (region.geometry.covers(point)) { matches++; actual = region.value; }
                }
                int expected = pixels.getSample(x, y, 0);
                assertEquals("Class mismatch at " + x + "," + y, expected, actual);
                assertEquals(expected == 255 ? 0 : 1, matches);
                if (expected < 2) counts[expected]++;
            }
        }
        double cellArea = Math.abs(transform.getDeterminant());
        for (int value = 0; value < 2; value++) {
            double expected = counts[value] * cellArea;
            assertEquals("Area for Visible=" + value, expected, areas[value], Math.max(1e-7, expected * 1e-10));
        }
        return (long) pixels.getWidth() * pixels.getHeight();
    }

    private static class RecordingProgress extends NullProgressListener {
        float last;
        boolean completed;
        @Override public void progress(float value) {
            assertTrue("Progress must not move backwards", value >= last);
            last = value;
        }
        @Override public void complete() { completed = true; last = 100; }
    }

    private static final class Region {
        final PreparedGeometry geometry;
        final int value;
        Region(Geometry geometry, int value) {
            this.geometry = PreparedGeometryFactory.prepare(geometry);
            this.value = value;
        }
    }
}
