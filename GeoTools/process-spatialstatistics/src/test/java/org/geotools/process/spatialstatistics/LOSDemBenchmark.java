package org.geotools.process.spatialstatistics;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.process.spatialstatistics.gridcoverage.RasterFunctionalSurface;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/** Actual-DEM LOS benchmark and complete ordered-sample option regression check (180 rays). */
public final class LOSDemBenchmark {
    public static void main(String[] args) throws Exception {
        Path directory = Paths.get(args[1]);
        Files.createDirectories(directory);
        final double radius = Double.parseDouble(args[2]);
        GeoTiffReader reader = new GeoTiffReader(new File(args[0]));
        GridCoverage2D dem = reader.read(null);
        Point2D center = ((AffineTransform) dem.getGridGeometry().getGridToCRS2D()).transform(
                new Point2D.Double(dem.getRenderedImage().getWidth() / 2,
                        dem.getRenderedImage().getHeight() / 2), null);
        Coordinate origin = new Coordinate(center.getX(), center.getY());
        GeometryFactory gf = new GeometryFactory();
        Geometry observer = gf.createPoint(origin);
        Geometry target = gf.createPoint(new Coordinate(origin.x + radius, origin.y));
        Path metrics = directory.resolve("metrics.csv");
        Files.write(metrics, ("algorithm,radius_m,stop_at_first_invisible,iteration,elapsed_ms,"
                + "baseline_heap_bytes,peak_heap_bytes,heap_delta_bytes,features,visible_length,invisible_length\n")
                .getBytes(StandardCharsets.UTF_8));
        try {
            for (String algorithm : new String[] { "Linear", "Radial" }) {
                double legacyLength = Double.NaN, legacyVisible = Double.NaN;
                for (boolean locked : new boolean[] { false, true }) {
                    for (int iteration = 0; iteration < 4; iteration++) {
                        System.gc();
                        Thread.sleep(100);
                        final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
                        long baseline = memory.getHeapMemoryUsage().getUsed();
                        final AtomicLong peak = new AtomicLong(baseline);
                        final AtomicBoolean sampling = new AtomicBoolean(true);
                        Thread sampler = new Thread(new Runnable() {
                            public void run() {
                                while (sampling.get()) {
                                    peak.set(Math.max(peak.get(), memory.getHeapMemoryUsage().getUsed()));
                                    try { Thread.sleep(2); } catch (InterruptedException e) { return; }
                                }
                            }
                        }, "los-heap-sampler");
                        sampler.setDaemon(true);
                        sampler.start();
                        SimpleFeatureCollection result;
                        long start = System.nanoTime();
                        double elapsed;
                        try {
                            result = algorithm.equals("Linear")
                                    ? RasterLinearLOSProcess.process(dem, observer, 1.6, target,
                                            false, false, .13, locked, null)
                                    : RasterRadialLOSProcess.process(dem, observer, 1.6, radius,
                                            180, false, false, .13, locked, null);
                        } finally {
                            elapsed = (System.nanoTime() - start) / 1e6;
                            sampling.set(false);
                            sampler.join();
                        }
                        peak.set(Math.max(peak.get(), memory.getHeapMemoryUsage().getUsed()));
                        if (result == null) throw new AssertionError("LOS returned null");
                        double[] lengths = new double[2];
                        try (SimpleFeatureIterator features = result.features()) {
                            while (features.hasNext()) {
                                SimpleFeature feature = features.next();
                                int visible = ((Number) feature.getAttribute("Visible")).intValue();
                                if (visible != 0 && visible != 1) throw new AssertionError("Invalid flag");
                                lengths[visible] += ((Geometry) feature.getDefaultGeometry()).getLength();
                            }
                        }
                        if (!locked) {
                            legacyLength = lengths[0] + lengths[1];
                            legacyVisible = lengths[1];
                        } else if (Math.abs(legacyLength - lengths[0] - lengths[1]) > 1e-5
                                || lengths[1] > legacyVisible + 1e-5) {
                            throw new AssertionError("The option changed ray geometry or restored visibility.");
                        }
                        String row = String.format(Locale.ROOT,
                                "%s,%.0f,%s,%d,%.3f,%d,%d,%d,%d,%.6f,%.6f%n", algorithm,
                                radius, locked, iteration, elapsed, baseline, peak.get(), peak.get() - baseline,
                                result.size(), lengths[1], lengths[0]);
                        Files.write(metrics, row.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
                        System.out.print(row);
                    }
                }
            }
            long compared = 0;
            RasterFunctionalSurface surface = new RasterFunctionalSurface(dem);
            for (int ray = 0; ray < 180; ray++) {
                double angle = ray * 2 * Math.PI / 180;
                Coordinate end = new Coordinate(origin.x + radius * Math.cos(angle),
                        origin.y + radius * Math.sin(angle));
                LineString legacy = surface.getLineOfSight(origin, end, 1.6, false, false, .13);
                LineString locked = surface.getLineOfSight(origin, end, 1.6, false, false, .13, true);
                Coordinate[] a = legacy.getCoordinates(), b = locked.getCoordinates();
                if (a.length != b.length) throw new AssertionError("Ray samples changed");
                boolean blocked = false;
                for (int i = 0; i < a.length; i++) {
                    blocked |= a[i].z == 0;
                    if (a[i].x != b[i].x || a[i].y != b[i].y || b[i].z != (blocked ? 0 : a[i].z)) {
                        throw new AssertionError("Invalid option result at ray " + ray + ", sample " + i);
                    }
                    compared++;
                }
            }
            Files.write(directory.resolve("accuracy.txt"), ("rays=180\nordered_samples=" + compared
                    + "\nmismatches=0\ngeometry_length_preserved=true\n").getBytes(StandardCharsets.UTF_8));
        } finally {
            dem.dispose(true);
            reader.dispose();
        }
    }
}
