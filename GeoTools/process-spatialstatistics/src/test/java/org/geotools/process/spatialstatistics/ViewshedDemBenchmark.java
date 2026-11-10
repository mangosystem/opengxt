package org.geotools.process.spatialstatistics;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

/** Opt-in executable benchmark. Each radius is launched in a fresh Java 8 JVM. */
public final class ViewshedDemBenchmark {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("DEM path, output directory and radius in metres required.");
        }
        Path directory = Paths.get(args[1]);
        Files.createDirectories(directory);
        double radius = Double.parseDouble(args[2]);
        long readStart = System.nanoTime();
        GeoTiffReader reader = new GeoTiffReader(new File(args[0]));
        GridCoverage2D dem = reader.read(null);
        double readMs = (System.nanoTime() - readStart) / 1e6;
        Point2D observer = ((AffineTransform) dem.getGridGeometry().getGridToCRS2D()).transform(
                new Point2D.Double(dem.getRenderedImage().getMinX() + dem.getRenderedImage().getWidth() / 2,
                        dem.getRenderedImage().getMinY() + dem.getRenderedImage().getHeight() / 2), null);
        Path metrics = directory.resolve("metrics.csv");
        Files.write(metrics, ("radius_m,iteration,read_ms,elapsed_ms,baseline_heap_bytes,peak_heap_bytes,heap_delta_bytes,"
                + "width,height,visible,invisible,nodata,sha256\n").getBytes(StandardCharsets.UTF_8));
        try {
            for (int iteration = 0; iteration < 4; iteration++) {
                System.gc();
                Thread.sleep(100);
                final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
                final long baseline = memory.getHeapMemoryUsage().getUsed();
                final AtomicLong peak = new AtomicLong(baseline);
                final AtomicBoolean sampling = new AtomicBoolean(true);
                Thread sampler = new Thread(new Runnable() {
                    public void run() {
                        while (sampling.get()) {
                            long used = memory.getHeapMemoryUsage().getUsed();
                            peak.set(Math.max(peak.get(), used));
                            try { Thread.sleep(2); } catch (InterruptedException e) { return; }
                        }
                    }
                }, "viewshed-heap-sampler");
                sampler.setDaemon(true);
                sampler.start();
                GridCoverage2D result;
                long start = System.nanoTime();
                double elapsedMs;
                try {
                    result = RasterViewshedProcess.process(dem, new GeometryFactory().createPoint(
                            new Coordinate(observer.getX(), observer.getY())), 1.6, radius,
                            false, false, .13, null);
                } finally {
                    elapsedMs = (System.nanoTime() - start) / 1e6;
                    sampling.set(false);
                    sampler.join();
                }
                peak.set(Math.max(peak.get(), memory.getHeapMemoryUsage().getUsed()));
                try {
                    Raster data = result.getRenderedImage().getData();
                    long[] counts = new long[256];
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    for (int y = 0; y < data.getHeight(); y++) {
                        byte[] row = new byte[data.getWidth()];
                        for (int x = 0; x < data.getWidth(); x++) {
                            int value = data.getSample(x + data.getMinX(), y + data.getMinY(), 0);
                            if (value != 0 && value != 1 && value != 255) {
                                throw new AssertionError("Unexpected visibility value " + value);
                            }
                            counts[value]++;
                            row[x] = (byte) value;
                        }
                        digest.update(row);
                    }
                    StringBuilder checksum = new StringBuilder();
                    for (byte value : digest.digest()) checksum.append(String.format("%02x", value & 255));
                    String row = String.format(Locale.ROOT,
                            "%.0f,%d,%.3f,%.3f,%d,%d,%d,%d,%d,%d,%d,%d,%s%n", radius, iteration,
                            readMs, elapsedMs, baseline, peak.get(), peak.get() - baseline,
                            data.getWidth(), data.getHeight(), counts[1], counts[0], counts[255], checksum);
                    Files.write(metrics, row.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
                    System.out.print(row);
                    if (iteration == 3) {
                        GeoTiffWriter writer = new GeoTiffWriter(directory.resolve("viewshed.tif").toFile());
                        try { writer.write(result, null); } finally { writer.dispose(); }
                    }
                } finally {
                    result.dispose(true);
                }
            }
            Files.write(directory.resolve("environment.txt"), ("java=" + System.getProperty("java.version")
                    + "\nvm=" + System.getProperty("java.vm.name")
                    + "\nmax_heap_bytes=" + Runtime.getRuntime().maxMemory()
                    + "\nprocessors=" + Runtime.getRuntime().availableProcessors()
                    + "\nimplementation=" + org.geotools.process.spatialstatistics.gridcoverage.RasterViewshedOperation.class
                            .getProtectionDomain().getCodeSource().getLocation()
                    + "\nobserver=" + observer.getX() + "," + observer.getY()
                    + "\ncrs=" + dem.getCoordinateReferenceSystem2D().toWKT()
                    + "\n").getBytes(StandardCharsets.UTF_8));
        } finally {
            dem.dispose(true);
            reader.dispose();
        }
    }
}
