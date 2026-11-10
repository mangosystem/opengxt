package org.geotools.process.spatialstatistics;

import static org.junit.Assert.*;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.NameImpl;
import org.geotools.process.ProcessException;
import org.geotools.process.Processors;
import org.geotools.process.spatialstatistics.gridcoverage.RasterViewshedOperation;
import org.geotools.referencing.crs.DefaultEngineeringCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.NullProgressListener;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public class ViewshedProcessTest {
    private GridCoverage2D run(GridCoverage2D dem, double x, double y, double offset,
            double radius, boolean curve, boolean refract, double factor) {
        return new RasterViewshedOperation().execute(dem, new Coordinate(x, y), offset,
                radius, curve, refract, factor, null);
    }

    private int cell(GridCoverage2D result, int x, int y) {
        return result.getRenderedImage().getData().getSample(x, y, 0);
    }

    @Test
    public void flatDemKeepsValidMinimumAndObserverVisible() {
        GridCoverage2D dem = TerrainTestSupport.dem(new double[7][7],
                new AffineTransform(1, 0, 0, 1, .5, .5),
                DefaultEngineeringCRS.CARTESIAN_2D, null);
        GridCoverage2D result = run(dem, 3.5, 3.5, 0, 0, false, false, .13);
        try {
            Raster raster = result.getRenderedImage().getData();
            for (int y = 0; y < 7; y++) {
                for (int x = 0; x < 7; x++) {
                    assertEquals(1, raster.getSample(x, y, 0));
                }
            }
            assertEquals(DataBuffer.TYPE_BYTE, result.getRenderedImage().getSampleModel().getDataType());
            assertArrayEquals(new double[] { 255 }, result.getSampleDimension(0).getNoDataValues(), 0);
            assertArrayEquals(new double[] { 255 },
                    org.geotools.resources.coverage.CoverageUtilities.getBackgroundValues(result), 0);
        } finally {
            result.dispose(true);
            dem.dispose(true);
        }
    }

    @Test
    public void distinguishesUnknownTerrainFromProvenObstruction() {
        GridCoverage2D dem = TerrainTestSupport.dem(new double[][] {
                { 0, Double.NaN, 0, 20, 0 },
                { 0, -9999, 0, 0, 0 } });
        GridCoverage2D result = run(dem, .5, .5, 1.6, 0, false, false, .13);
        try {
            assertEquals(1, cell(result, 0, 0));
            assertEquals(255, cell(result, 1, 0));
            assertEquals(255, cell(result, 2, 0));
            assertEquals(255, cell(result, 3, 0));
            assertEquals(0, cell(result, 4, 0));
            assertEquals(255, cell(result, 1, 1));
        } finally {
            result.dispose(true);
            dem.dispose(true);
        }
    }

    @Test
    public void checksNarrowRidgeBeyondOneHundredSamplesAndAllowsHigherTargets() {
        double[][] heights = new double[1][402];
        heights[0][151] = 100;
        heights[0][401] = 1000;
        GridCoverage2D dem = TerrainTestSupport.dem(heights);
        GridCoverage2D result = run(dem, .5, .5, 1.6, 0, false, false, .13);
        try {
            assertEquals(0, cell(result, 400, 0));
            assertEquals(1, cell(result, 401, 0));
        } finally {
            result.dispose(true);
            dem.dispose(true);
        }
    }

    @Test
    public void cropsRadiusAndPreservesRotatedGridTransform() throws Exception {
        AffineTransform transform = new AffineTransform(30, 4, 8, -30, 1000, 2000);
        Point2D observer = transform.transform(new Point2D.Double(3, 3), null);
        GridCoverage2D dem = TerrainTestSupport.dem(new double[7][7], transform,
                DefaultEngineeringCRS.CARTESIAN_2D, Integer.valueOf(-9999));
        GridCoverage2D result = run(dem, observer.getX(), observer.getY(), 1.6, 40, false, false, .13);
        try {
            assertEquals(3, result.getRenderedImage().getWidth());
            assertEquals(3, result.getRenderedImage().getHeight());
            AffineTransform expected = new AffineTransform(transform);
            expected.translate(2, 2);
            assertEquals(expected, result.getGridGeometry().getGridToCRS2D());
            assertEquals(255, cell(result, 0, 0));
            assertEquals(1, cell(result, 1, 1));
        } finally {
            result.dispose(true);
            dem.dispose(true);
        }
    }

    @Test
    public void curvatureAndRefractionUseMetresAndCorrectBothTerrainAndTarget() {
        GridCoverage2D dem = TerrainTestSupport.dem(new double[1][12],
                new AffineTransform(1000, 0, 0, 1000, 500, 500),
                DefaultEngineeringCRS.CARTESIAN_2D, -9999d);
        GridCoverage2D plain = run(dem, 500, 500, 1.6, 0, false, false, .13);
        GridCoverage2D curved = run(dem, 500, 500, 1.6, 0, true, false, .13);
        GridCoverage2D fullyRefracted = run(dem, 500, 500, 1.6, 0, true, true, 1);
        try {
            assertEquals(1, cell(plain, 10, 0));
            assertEquals(0, cell(curved, 10, 0));
            assertEquals(1, cell(fullyRefracted, 10, 0));
        } finally {
            plain.dispose(true);
            curved.dispose(true);
            fullyRefracted.dispose(true);
            dem.dispose(true);
        }
    }

    @Test
    public void geographicDistanceIsMetricWithAndWithoutRadius() {
        AffineTransform transform = new AffineTransform(.001, 0, 0, -.001, 127, 37.5);
        GridCoverage2D dem = TerrainTestSupport.dem(new double[1][12], transform,
                DefaultGeographicCRS.WGS84, -9999d);
        GridCoverage2D cropped = run(dem, 127, 37.5, 1.6, 100, false, false, .13);
        GridCoverage2D all = run(dem, 127, 37.5, 1.6, 0, false, false, .13);
        try {
            assertEquals(2, cropped.getRenderedImage().getWidth());
            assertEquals(12, all.getRenderedImage().getWidth());
            assertEquals(1, cell(all, 11, 0));
        } finally {
            cropped.dispose(true);
            all.dispose(true);
            dem.dispose(true);
        }
    }

    @Test
    public void validatesObserverOptionsAndCancellation() {
        GridCoverage2D dem = TerrainTestSupport.dem(new double[][] { { -9999, 0, 0 } });
        try {
            expectInvalid(dem, .5, .5, 1.6, 0, false, false, .13);
            expectInvalid(dem, -1, .5, 1.6, 0, false, false, .13);
            expectInvalid(dem, 1.5, .5, -1, 0, false, false, .13);
            expectInvalid(dem, 1.5, .5, 1.6, -1, false, false, .13);
            expectInvalid(dem, 1.5, .5, Double.NaN, 0, false, false, .13);
            expectInvalid(dem, 1.5, .5, 1.6, 0, false, true, .13);
            expectInvalid(dem, 1.5, .5, 1.6, 0, true, true, 1.1);
            NullProgressListener listener = new NullProgressListener();
            listener.setCanceled(true);
            try {
                new RasterViewshedOperation().execute(dem, new Coordinate(1.5, .5), 1.6,
                        0, false, false, .13, listener);
                fail("Cancellation must stop computation.");
            } catch (ProcessException expected) {
                assertTrue(expected.getMessage().contains("cancelled"));
            }
        } finally {
            dem.dispose(true);
        }
    }

    @Test
    public void factoryIsRegisteredAndDefaultsReachTheProcess() {
        assertNotNull(Processors.createProcess(new NameImpl("statistics", "Viewshed")));
        RasterViewshedProcessFactory factory = new RasterViewshedProcessFactory();
        assertEquals(1.6d, factory.getParameterInfo().get("observerOffset").sample);
        assertEquals(0d, factory.getParameterInfo().get("maxDistance").sample);
        GridCoverage2D dem = TerrainTestSupport.dem(new double[][] { { 0, 0, 0 } });
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("inputCoverage", dem);
        input.put("observerPoint", new GeometryFactory().createPoint(new Coordinate(.5, .5)));
        GridCoverage2D result = (GridCoverage2D) factory.create().execute(input, null).get("result");
        try {
            assertEquals(1, cell(result, 2, 0));
        } finally {
            result.dispose(true);
            dem.dispose(true);
        }
    }

    @Test
    public void ddaAgreesWithIndependentCellRectangleIntersectionReference() {
        Random random = new Random(17);
        for (int trial = 0; trial < 8; trial++) {
            double[][] heights = new double[9][11];
            for (int y = 0; y < 9; y++) {
                for (int x = 0; x < 11; x++) {
                    heights[y][x] = random.nextInt(9) == 0 ? -9999 : random.nextDouble() * 12;
                }
            }
            double ox = 5 + random.nextDouble(), oy = 4 + random.nextDouble();
            heights[4][5] = 5;
            GridCoverage2D dem = TerrainTestSupport.dem(heights);
            GridCoverage2D result = run(dem, ox, oy, 1.6, 0, false, false, .13);
            try {
                for (int y = 0; y < 9; y++) {
                    for (int x = 0; x < 11; x++) {
                        assertEquals("trial=" + trial + " target=" + x + "," + y,
                                reference(heights, ox, oy, x, y, 6.6), cell(result, x, y));
                    }
                }
            } finally {
                result.dispose(true);
                dem.dispose(true);
            }
        }
    }

    private int reference(double[][] z, double ox, double oy, int tx, int ty, double from) {
        if (tx == (int) ox && ty == (int) oy) return 1;
        if (z[ty][tx] == -9999) return 255;
        boolean unknown = false;
        double dx = tx + .5 - ox, dy = ty + .5 - oy;
        for (int y = 0; y < z.length; y++) {
            for (int x = 0; x < z[0].length; x++) {
                if ((x == (int) ox && y == (int) oy) || (x == tx && y == ty)) continue;
                double enter = 0, exit = 1;
                if (dx == 0) {
                    if (ox <= x || ox >= x + 1) continue;
                } else {
                    double a = (x - ox) / dx, b = (x + 1 - ox) / dx;
                    enter = Math.max(enter, Math.min(a, b));
                    exit = Math.min(exit, Math.max(a, b));
                }
                if (dy == 0) {
                    if (oy <= y || oy >= y + 1) continue;
                } else {
                    double a = (y - oy) / dy, b = (y + 1 - oy) / dy;
                    enter = Math.max(enter, Math.min(a, b));
                    exit = Math.min(exit, Math.max(a, b));
                }
                if (exit - enter <= 1e-12) continue;
                if (z[y][x] == -9999) unknown = true;
                else if (z[y][x] - Math.min(from + (z[ty][tx] - from) * enter,
                        from + (z[ty][tx] - from) * exit) > 1e-9) return 0;
            }
        }
        return unknown ? 255 : 1;
    }

    private void expectInvalid(GridCoverage2D dem, double x, double y, double offset,
            double radius, boolean curve, boolean refract, double factor) {
        try {
            GridCoverage2D result = run(dem, x, y, offset, radius, curve, refract, factor);
            result.dispose(true);
            fail("Expected invalid viewshed input.");
        } catch (ProcessException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    public void geotiffRoundTripPreservesInvisibleZeroAndNoData255() throws Exception {
        java.nio.file.Path directory = java.nio.file.Files.createTempDirectory("viewshed-roundtrip-");
        java.io.File file = directory.resolve("viewshed.tif").toFile();
        GridCoverage2D dem = TerrainTestSupport.dem(new double[][] { { 0, 100, 0, -9999 } },
                new AffineTransform(1, 0, 0, -1, .5, .5),
                org.geotools.referencing.CRS.decode("EPSG:3857", true), -9999d);
        GridCoverage2D result = run(dem, .5, .5, 1.6, 0, false, false, .13);
        try {
            org.geotools.gce.geotiff.GeoTiffWriter writer = new org.geotools.gce.geotiff.GeoTiffWriter(file);
            try { writer.write(result, null); } finally { writer.dispose(); }
            org.geotools.gce.geotiff.GeoTiffReader reader = new org.geotools.gce.geotiff.GeoTiffReader(file);
            try {
                GridCoverage2D decoded = reader.read(null);
                try {
                    assertEquals(0, cell(decoded, 2, 0));
                    assertEquals(255, cell(decoded, 3, 0));
                    assertEquals(255d, org.geotools.process.spatialstatistics.gridcoverage.RasterHelper
                            .getNoDataValue(decoded), 0);
                } finally { decoded.dispose(true); }
            } finally { reader.dispose(); }
        } finally {
            result.dispose(true);
            dem.dispose(true);
            try {
                java.nio.file.Files.deleteIfExists(file.toPath());
                java.nio.file.Files.deleteIfExists(directory);
            } catch (java.nio.file.FileSystemException lockedByImageIO) {
                // Old ImageIO readers can retain a Windows file handle until JVM shutdown.
                directory.toFile().deleteOnExit();
                file.deleteOnExit();
            }
        }
    }

    @Test
    public void projectedFeetAreConvertedToMetresForRadius() throws Exception {
        GridCoverage2D dem = TerrainTestSupport.dem(new double[1][4],
                new AffineTransform(300, 0, 0, -300, 1000000, 200000),
                org.geotools.referencing.CRS.decode("EPSG:2263", true), -9999d);
        GridCoverage2D result = run(dem, 1000000, 200000, 1.6, 100, false, false, .13);
        try {
            assertEquals(2, result.getRenderedImage().getWidth());
        } finally {
            result.dispose(true);
            dem.dispose(true);
        }
    }

    @Test
    public void geographicFullExtentCurvatureUsesMetricDistances() {
        GridCoverage2D dem = TerrainTestSupport.dem(new double[1][12],
                new AffineTransform(.01, 0, 0, -.01, 127, 37.5),
                DefaultGeographicCRS.WGS84, -9999d);
        GridCoverage2D result = run(dem, 127, 37.5, 1.6, 0, true, false, .13);
        try {
            assertEquals(0, cell(result, 11, 0));
        } finally {
            result.dispose(true);
            dem.dispose(true);
        }
    }
}
