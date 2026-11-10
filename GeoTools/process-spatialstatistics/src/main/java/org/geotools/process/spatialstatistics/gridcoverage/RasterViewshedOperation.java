/*
 * GeoTools - The Open Source Java GIS Toolkit
 * Copyright (C) 2026, Mango System inc.
 * Licensed under the GNU Lesser General Public License, version 2.1 or later.
 */
package org.geotools.process.spatialstatistics.gridcoverage;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.HashMap;
import java.util.Map;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;
import javax.measure.unit.SI;

import org.geotools.coverage.Category;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.process.ProcessException;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.util.NumberRange;
import org.geotools.resources.i18n.Vocabulary;
import org.geotools.resources.i18n.VocabularyKeys;
import it.geosolutions.jaiext.range.NoDataContainer;
import org.jaitools.tiledimage.DiskMemImage;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Single-observer viewshed on a piecewise constant DEM (terrain heights in metres).
 * Every cell whose interior intersects a ray is visited; corner-only contacts are ignored.
 * Observer and target cells are excluded from obstruction tests. Curvature is applied to
 * both terrain and target in the observer's reference plane. NoData is unknown, unless a
 * known cell already proves obstruction. Instances hold no request state.
 */
public class RasterViewshedOperation {
    public static final int INVISIBLE = 0;
    public static final int VISIBLE = 1;
    public static final int NODATA = 255;
    private static final double EARTH_DIAMETER = 2 * 6378137.0;
    private static final double HEIGHT_TOLERANCE = 1e-9;

    public GridCoverage2D execute(GridCoverage2D input, Coordinate observer,
            double observerOffset, double maxDistance, boolean useCurvature,
            boolean useRefraction, double refractionFactor, ProgressListener monitor) {
        if (input == null || observer == null || !Double.isFinite(observer.x)
                || !Double.isFinite(observer.y)) {
            throw new ProcessException("A DEM and a finite observer coordinate are required.");
        }
        if (!Double.isFinite(observerOffset) || observerOffset < 0
                || !Double.isFinite(maxDistance) || maxDistance < 0
                || !Double.isFinite(refractionFactor) || refractionFactor < 0
                || refractionFactor > 1 || (useRefraction && !useCurvature)) {
            throw new ProcessException("Invalid viewshed height, distance or curvature/refraction options.");
        }
        try {
            if (monitor != null) {
                monitor.started();
            }
            RenderedImage image = input.getRenderedImage();
            final int width = image.getWidth(), height = image.getHeight();
            AffineTransform corner = new AffineTransform(
                    (AffineTransform) input.getGridGeometry().getGridToCRS2D());
            // GeoTools' default grid transform maps integer grid coordinates to cell centres.
            corner.translate(image.getMinX() - .5, image.getMinY() - .5);
            Point2D pixel = corner.inverseTransform(new Point2D.Double(observer.x, observer.y), null);
            final double ox = pixel.getX(), oy = pixel.getY();
            if (ox < 0 || oy < 0 || ox >= width || oy >= height) {
                throw new ProcessException("observerPoint is outside the DEM.");
            }
            int observerX = (int) Math.floor(ox), observerY = (int) Math.floor(oy);
            Distance distance = new Distance(input.getCoordinateReferenceSystem2D(), observer);

            // Bound the projected metric circle in grid space, then retain the original
            // exact cell-centre inclusion test. Geographic distances keep the full search.
            int x0 = 0, y0 = 0, x1 = width, y1 = height;
            if (maxDistance > 0) {
                x0 = observerX;
                y0 = observerY;
                x1 = observerX + 1;
                y1 = observerY + 1;
                Rectangle search = distance.searchWindow(corner, ox, oy, maxDistance, width, height);
                for (int y = search.y; y < search.y + search.height; y++) {
                    checkpoint(monitor);
                    for (int x = search.x; x < search.x + search.width; x++) {
                        if (distance.metres(corner, x + .5, y + .5) <= maxDistance) {
                            x0 = Math.min(x0, x);
                            y0 = Math.min(y0, y);
                            x1 = Math.max(x1, x + 1);
                            y1 = Math.max(y1, y + 1);
                        }
                    }
                }
            }

            int outWidth = x1 - x0, outHeight = y1 - y0;
            // A straight grid ray between observer and target stays inside this window.
            // Keep the original grid coordinates in the DDA to preserve floating-point
            // decisions; only array indexing is relative to the read window.
            double[] elevations = new double[Math.multiplyExact(outWidth, outHeight)];
            double[] noData = noDataValues(input);
            Rectangle readBounds = new Rectangle(image.getMinX() + x0, image.getMinY() + y0,
                    outWidth, outHeight);
            RectIter reader = RectIterFactory.create(image, readBounds);
            reader.startLines();
            for (int y = 0; y < outHeight; y++) {
                checkpoint(monitor);
                reader.startPixels();
                for (int x = 0; x < outWidth; x++) {
                    double z = reader.getSampleDouble(0);
                    elevations[y * outWidth + x] = valid(z, noData) ? z : Double.NaN;
                    // The Done methods also switch the JAI iterator's backing tile.
                    reader.nextPixelDone();
                }
                reader.nextLineDone();
            }
            double from = elevations[(observerY - y0) * outWidth + observerX - x0] + observerOffset;
            if (!Double.isFinite(from)) {
                throw new ProcessException("observerPoint falls on DEM NoData.");
            }
            SampleModel model = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_BYTE, 128, 128, 1);
            DiskMemImage output = new DiskMemImage(0, 0, outWidth, outHeight, 0, 0, model,
                    PlanarImage.createColorModel(model));
            boolean success = false;
            try {
                WritableRectIter writer = RectIterFactory.createWritable(output, output.getBounds());
                writer.startLines();
                for (int y = y0; y < y1; y++) {
                    checkpoint(monitor);
                    writer.startPixels();
                    for (int x = x0; x < x1; x++) {
                        double target = elevations[(y - y0) * outWidth + x - x0];
                        double d = distance.metres(corner, x + .5, y + .5);
                        int value = NODATA;
                        if (x == observerX && y == observerY) {
                            value = VISIBLE;
                        } else if (Double.isFinite(target) && (maxDistance == 0 || d <= maxDistance)) {
                            double drop = useCurvature ? d * d / EARTH_DIAMETER
                                    * (useRefraction ? 1 - refractionFactor : 1) : 0;
                            value = visibility(elevations, outWidth, x0, y0, ox, oy, observerX, observerY,
                                    x, y, from, target, drop);
                        }
                        writer.setSample(0, value);
                        writer.nextPixelDone();
                    }
                    writer.nextLineDone();
                    if (monitor != null) {
                        monitor.progress(100f * (y - y0 + 1) / outHeight);
                    }
                }
                AffineTransform outputCentre = new AffineTransform(corner);
                outputCentre.translate(x0 + .5, y0 + .5);
                GridSampleDimension band = new GridSampleDimension("Visibility", new Category[] {
                        new Category("Invisible", Color.RED, 0, true),
                        new Category("Visible", Color.GREEN, 1, true),
                        new Category(Vocabulary.formatInternational(VocabularyKeys.NODATA),
                                new Color[] { new Color(0, 0, 0, 0) },
                                NumberRange.create(255, 255), false) }, null);
                Map<String, Object> properties = new HashMap<String, Object>();
                // GeoTools 17's GeoTIFF writer only honours a NoDataContainer or the
                // localized NoData category, rather than a numeric GC_NODATA property.
                properties.put("GC_NODATA", new NoDataContainer((double) NODATA));
                GridCoverage2D result = CoverageFactoryFinder.getGridCoverageFactory(null).create(
                        "Viewshed", output, input.getCoordinateReferenceSystem2D(),
                        new AffineTransform2D(outputCentre), new GridSampleDimension[] { band },
                        null, properties);
                success = true;
                if (monitor != null) {
                    monitor.complete();
                }
                return result;
            } finally {
                if (!success) {
                    output.dispose();
                }
            }
        } catch (ProcessException e) {
            throw e;
        } catch (Exception e) {
            throw new ProcessException("Cannot compute viewshed.", e);
        }
    }

    /** Grid DDA with an analytic obstruction test over each traversed cell interval. */
    private static int visibility(double[] z, int width, int x0, int y0, double ox, double oy,
            int observerX, int observerY, int tx, int ty, double from, double to, double drop) {
        double dx = tx + .5 - ox, dy = ty + .5 - oy;
        int sx = dx > 0 ? 1 : -1, sy = dy > 0 ? 1 : -1;
        double dtX = dx == 0 ? Double.POSITIVE_INFINITY : 1 / Math.abs(dx);
        double dtY = dy == 0 ? Double.POSITIVE_INFINITY : 1 / Math.abs(dy);
        int x = observerX, y = observerY;
        double nextX = dx == 0 ? Double.POSITIVE_INFINITY
                : ((sx > 0 ? x + 1 : x) - ox) / dx;
        double nextY = dy == 0 ? Double.POSITIVE_INFINITY
                : ((sy > 0 ? y + 1 : y) - oy) / dy;
        double entry = 0;
        boolean unknown = false;
        while (x != tx || y != ty) {
            double exit = Math.min(1, Math.min(nextX, nextY));
            if ((x != observerX || y != observerY) && exit - entry > 1e-12) {
                double ground = z[(y - y0) * width + x - x0];
                if (!Double.isFinite(ground)) {
                    unknown = true;
                } else {
                    // ground - corrected sightline = g-h0-(ht-h0)t+drop*t*(1-t).
                    double maximum = Math.max(obstruction(ground, from, to, drop, entry),
                            obstruction(ground, from, to, drop, exit));
                    if (drop > 0) {
                        double vertex = (drop - (to - from)) / (2 * drop);
                        if (vertex > entry && vertex < exit) {
                            maximum = Math.max(maximum, obstruction(ground, from, to, drop, vertex));
                        }
                    }
                    if (maximum > HEIGHT_TOLERANCE) {
                        return INVISIBLE;
                    }
                }
            }
            // Simultaneous crossings visit the diagonal cell, not corner-only neighbours.
            boolean tie = Math.abs(nextX - nextY) <= 1e-12;
            boolean crossX = nextX < nextY || tie;
            boolean crossY = nextY < nextX || tie;
            if (crossX) {
                x += sx;
                nextX += dtX;
            }
            if (crossY) {
                y += sy;
                nextY += dtY;
            }
            entry = exit;
        }
        return unknown ? NODATA : VISIBLE;
    }

    private static double obstruction(double ground, double from, double to, double drop, double t) {
        return ground - from - (to - from) * t + drop * t * (1 - t);
    }

    private static void checkpoint(ProgressListener monitor) {
        if (Thread.currentThread().isInterrupted() || (monitor != null && monitor.isCanceled())) {
            throw new ProcessException("Viewshed cancelled.");
        }
    }

    // Do not use RasterHelper's fallback to the minimum value: a valid DEM minimum is terrain.
    private static double[] noDataValues(GridCoverage2D input) {
        Object property = input.getProperty("GC_NODATA");
        if (property instanceof Number) {
            return new double[] { ((Number) property).doubleValue() };
        }
        if (property instanceof it.geosolutions.jaiext.range.NoDataContainer) {
            return new double[] { ((NoDataContainer) property).getAsSingleValue() };
        }
        return input.getSampleDimension(0).getNoDataValues();
    }

    private static boolean valid(double value, double[] noData) {
        if (!Double.isFinite(value)) {
            return false;
        }
        if (noData != null) {
            for (double missing : noData) {
                if (value == missing) {
                    return false;
                }
            }
        }
        return true;
    }

    private static final class Distance {
        private final Coordinate observer;
        private final CoordinateReferenceSystem crs;
        private final GeodeticCalculator geodetic;
        private final double unitX, unitY;

        Distance(CoordinateReferenceSystem crs, Coordinate observer) throws TransformException {
            if (crs == null) {
                throw new ProcessException("A DEM coordinate reference system is required.");
            }
            this.crs = crs;
            this.observer = observer;
            if (CRS.getHorizontalCRS(crs) instanceof GeographicCRS) {
                geodetic = new GeodeticCalculator(crs);
                geodetic.setStartingPosition(new DirectPosition2D(crs, observer.x, observer.y));
                unitX = unitY = 1;
            } else {
                geodetic = null;
                unitX = crs.getCoordinateSystem().getAxis(0).getUnit().getConverterTo(SI.METER).convert(1);
                unitY = crs.getCoordinateSystem().getAxis(1).getUnit().getConverterTo(SI.METER).convert(1);
            }
        }

        /** Conservative affine image of a metric circle, with a round-off guard in cells. */
        Rectangle searchWindow(AffineTransform transform, double ox, double oy,
                double radius, int width, int height) throws java.awt.geom.NoninvertibleTransformException {
            if (geodetic != null) {
                return new Rectangle(0, 0, width, height);
            }
            AffineTransform inverse = transform.createInverse();
            double extentX = radius * Math.hypot(inverse.getScaleX() / unitX,
                    inverse.getShearX() / unitY);
            double extentY = radius * Math.hypot(inverse.getShearY() / unitX,
                    inverse.getScaleY() / unitY);
            if (!Double.isFinite(extentX) || !Double.isFinite(extentY)) {
                return new Rectangle(0, 0, width, height);
            }
            // Cell centres lie at index + .5. Clamp as doubles before casting to
            // avoid integer overflow for very large radii. The extra cells also
            // cover round-off at exact radius boundaries and large CRS origins.
            int x0 = (int) Math.max(0, Math.min(width, Math.floor(ox - extentX - .5) - 1));
            int y0 = (int) Math.max(0, Math.min(height, Math.floor(oy - extentY - .5) - 1));
            int x1 = (int) Math.max(0, Math.min(width, Math.ceil(ox + extentX - .5) + 2));
            int y1 = (int) Math.max(0, Math.min(height, Math.ceil(oy + extentY - .5) + 2));
            return new Rectangle(x0, y0, x1 - x0, y1 - y0);
        }

        double metres(AffineTransform transform, double x, double y) throws TransformException {
            double wx = transform.getScaleX() * x + transform.getShearX() * y + transform.getTranslateX();
            double wy = transform.getShearY() * x + transform.getScaleY() * y + transform.getTranslateY();
            if (geodetic != null) {
                geodetic.setDestinationPosition(new DirectPosition2D(crs, wx, wy));
                return geodetic.getOrthodromicDistance();
            }
            return Math.hypot((wx - observer.x) * unitX, (wy - observer.y) * unitY);
        }
    }
}
