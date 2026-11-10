package org.geotools.process.spatialstatistics;

import static org.junit.Assert.*;

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.TiledImage;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.process.spatialstatistics.gridcoverage.RasterViewshedOperation;
import org.geotools.referencing.crs.DefaultEngineeringCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;

public class ViewshedWindowTest {
    @Test
    public void croppedReadMatchesFullReadForRotatedShearedAndShiftedGrids() throws Exception {
        Random random = new Random(1705);
        for (int trial = 0; trial < 24; trial++) {
            double[][] heights = new double[15][19];
            for (int y = 0; y < heights.length; y++) {
                for (int x = 0; x < heights[0].length; x++) {
                    heights[y][x] = random.nextInt(11) == 0 ? -9999 : random.nextInt(20);
                }
            }
            double scale = trial % 3 == 0 ? 1000 : 30;
            AffineTransform local = new AffineTransform(scale, scale * .3, scale * .7,
                    -scale * .8, trial % 2 == 0 ? 1e9 : 0, -1e8);
            local.rotate(trial * .23);
            int cx = trial % 19, cy = (trial * 7) % 15;
            heights[cy][cx] = 5;
            Point2D observer = local.transform(new Point2D.Double(cx - .5 + random.nextDouble(),
                    cy - .5 + random.nextDouble()), null);
            // The chosen observer is inside the cell centred at (cx, cy).
            int minX = trial % 2 == 0 ? -23 : 17, minY = 31;
            AffineTransform absolute = new AffineTransform(local);
            absolute.translate(-minX, -minY);
            GridCoverage2D dem = TerrainTestSupport.dem(heights, absolute,
                    DefaultEngineeringCRS.CARTESIAN_2D, -9999d, minX, minY);
            try {
                assertWindowMatchesFull(dem, observer, scale * (.01 + trial % 8), trial % 2 == 0);
            } finally {
                dem.dispose(true);
            }
        }
    }

    @Test
    public void rowWriterCrossesHorizontalAndVerticalTileBoundaries() throws Exception {
        GridCoverage2D dem = TerrainTestSupport.dem(new double[263][273],
                new AffineTransform(30, 0, 0, -30, 15, -15),
                DefaultEngineeringCRS.CARTESIAN_2D, -9999d);
        try {
            assertWindowMatchesFull(dem, new Point2D.Double(4095, -3945), 3870, false);
        } finally {
            dem.dispose(true);
        }
    }

    @Test
    public void geographicWindowStillMatchesExactFullDistanceSearch() throws Exception {
        GridCoverage2D dem = TerrainTestSupport.dem(new double[13][17],
                new AffineTransform(.001, .0002, .0003, -.001, 127, 37.5),
                DefaultGeographicCRS.WGS84, -9999d);
        try {
            Point2D observer = ((AffineTransform) dem.getGridGeometry().getGridToCRS2D())
                    .transform(new Point2D.Double(8.2, 5.7), null);
            assertWindowMatchesFull(dem, observer, 200, false);
        } finally {
            dem.dispose(true);
        }
    }

    @Test
    public void readsOnlySourceTilesIntersectingTheSelectedWindow() throws Exception {
        SampleModel model = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_DOUBLE, 64, 64, 1);
        RecordingImage image = new RecordingImage(model);
        // Allocate tiles before recording, so lazy zero tiles cannot affect the count.
        for (int y = 0; y < image.getHeight(); y += 64) {
            for (int x = 0; x < image.getWidth(); x += 64) image.setSample(x, y, 0, 0);
        }
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("GC_NODATA", -9999d);
        GridCoverage2D dem = CoverageFactoryFinder.getGridCoverageFactory(null).create("DEM", image,
                DefaultEngineeringCRS.CARTESIAN_2D, new AffineTransform2D(30, 0, 0, 30, 15, 15),
                new GridSampleDimension[] { new GridSampleDimension("Elevation") }, null, properties);
        try {
            for (double radius : new double[] { 1890, 1000 }) {
                image.readTiles.clear();
                image.record = true;
                GridCoverage2D result = run(dem, new Point2D.Double(7695, 5775), radius, false);
                image.record = false;
                try {
                    assertEquals("Only the four tiles touching the radius window should be read",
                            new HashSet<Point>(java.util.Arrays.asList(new Point(3, 2), new Point(4, 2),
                                    new Point(3, 3), new Point(4, 3))), image.readTiles);
                    assertEquals(radius == 1890 ? 127 : 67, result.getRenderedImage().getWidth());
                } finally {
                    result.dispose(true);
                }
            }
        } finally {
            image.record = false;
            dem.dispose(true);
        }
    }

    @Test
    public void candidateBoundsUseProjectedUnitsAndClampVeryLargeRadii() throws Exception {
        GridCoverage2D dem = TerrainTestSupport.dem(new double[41][41],
                new AffineTransform(300, 0, 0, -300, 1000000, 200000),
                org.geotools.referencing.CRS.decode("EPSG:2263", true), -9999d);
        try {
            Point2D observer = new Point2D.Double(1006000, 194000);
            assertWindowMatchesFull(dem, observer, 1000, false);
            assertWindowMatchesFull(dem, observer, Double.MAX_VALUE, false);
        } finally {
            dem.dispose(true);
        }
    }

    private void assertWindowMatchesFull(GridCoverage2D dem, Point2D observer,
            double radius, boolean curvature) throws Exception {
        GridCoverage2D full = run(dem, observer, 0, curvature);
        GridCoverage2D cropped = run(dem, observer, radius, curvature);
        try {
            Raster reference = full.getRenderedImage().getData();
            Raster actual = cropped.getRenderedImage().getData();
            AffineTransform transform = (AffineTransform) full.getGridGeometry().getGridToCRS2D();
            AffineTransform croppedTransform = (AffineTransform) cropped.getGridGeometry().getGridToCRS2D();
            Point2D start = transform.inverseTransform(croppedTransform.transform(new Point2D.Double(), null), null);
            int x0 = (int) Math.round(start.getX()), y0 = (int) Math.round(start.getY());
            Point2D op = transform.inverseTransform(observer, null);
            org.geotools.referencing.GeodeticCalculator geodetic = null;
            if (org.geotools.referencing.CRS.getHorizontalCRS(dem.getCoordinateReferenceSystem2D())
                    instanceof org.opengis.referencing.crs.GeographicCRS) {
                geodetic = new org.geotools.referencing.GeodeticCalculator(dem.getCoordinateReferenceSystem2D());
                geodetic.setStartingPosition(new org.geotools.geometry.DirectPosition2D(
                        dem.getCoordinateReferenceSystem2D(), observer.getX(), observer.getY()));
            }
            int observerX = (int) Math.floor(op.getX() + .5), observerY = (int) Math.floor(op.getY() + .5);
            double unitX = 1, unitY = 1;
            if (geodetic == null) {
                unitX = dem.getCoordinateReferenceSystem2D().getCoordinateSystem().getAxis(0)
                        .getUnit().getConverterTo(javax.measure.unit.SI.METER).convert(1);
                unitY = dem.getCoordinateReferenceSystem2D().getCoordinateSystem().getAxis(1)
                        .getUnit().getConverterTo(javax.measure.unit.SI.METER).convert(1);
            }
            int xmin = observerX, ymin = observerY, xmax = observerX, ymax = observerY;
            for (int y = 0; y < reference.getHeight(); y++) {
                for (int x = 0; x < reference.getWidth(); x++) {
                    double wx = transform.getScaleX() * x + transform.getShearX() * y + transform.getTranslateX();
                    double wy = transform.getShearY() * x + transform.getScaleY() * y + transform.getTranslateY();
                    double distance = Math.hypot((wx - observer.getX()) * unitX,
                            (wy - observer.getY()) * unitY);
                    if (geodetic != null) {
                        geodetic.setDestinationPosition(new org.geotools.geometry.DirectPosition2D(
                                dem.getCoordinateReferenceSystem2D(), wx, wy));
                        distance = geodetic.getOrthodromicDistance();
                    }
                    boolean included = distance <= radius
                            || (x == observerX && y == observerY);
                    if (included) {
                        xmin = Math.min(xmin, x); xmax = Math.max(xmax, x);
                        ymin = Math.min(ymin, y); ymax = Math.max(ymax, y);
                    }
                    if (x >= x0 && x < x0 + actual.getWidth() && y >= y0 && y < y0 + actual.getHeight()) {
                        assertEquals("target " + x + "," + y, included ? reference.getSample(x, y, 0) : 255,
                                actual.getSample(x - x0, y - y0, 0));
                    }
                }
            }
            assertEquals(xmin, x0); assertEquals(ymin, y0);
            assertEquals(xmax - xmin + 1, actual.getWidth());
            assertEquals(ymax - ymin + 1, actual.getHeight());
        } finally {
            cropped.dispose(true);
            full.dispose(true);
        }
    }

    private GridCoverage2D run(GridCoverage2D dem, Point2D observer, double radius, boolean curvature) {
        return new RasterViewshedOperation().execute(dem, new Coordinate(observer.getX(), observer.getY()),
                1.6, radius, curvature, false, .13, null);
    }

    private static final class RecordingImage extends TiledImage {
        final Set<Point> readTiles = new HashSet<Point>();
        boolean record;

        RecordingImage(SampleModel model) {
            super(0, 0, 513, 385, 0, 0, model, PlanarImage.createColorModel(model));
        }

        @Override
        public Raster getTile(int x, int y) {
            if (record) readTiles.add(new Point(x, y));
            return super.getTile(x, y);
        }
    }
}
