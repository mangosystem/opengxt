/*
 * GeoTools - The Open Source Java GIS Toolkit
 * Copyright (C) 2026, Mango System inc.
 * Licensed under the GNU Lesser General Public License, version 2.1 or later.
 */
package org.geotools.process.spatialstatistics.gridcoverage;

import java.awt.geom.AffineTransform;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.Collection;
import java.util.Collections;

import javax.media.jai.JAI;
import javax.media.jai.ImageLayout;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.image.jai.Registry;
import org.geotools.process.ProcessException;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.jaitools.media.jai.vectorize.VectorizeDescriptor;
import org.jaitools.media.jai.vectorize.VectorizeRIF;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.util.AffineTransformation;

/** Polygonizes a 0/1/255 viewshed, preserving valid zero values and exact cell edges. */
public class RasterViewshedAsPolygonOperation {
    public static final String VALUE_FIELD = "Visible";
    public static final String AREA_FIELD = "area";

    static {
        Registry.registerRIF(JAI.getDefaultInstance(), new VectorizeDescriptor(),
                new VectorizeRIF(), Registry.JAI_TOOLS_PRODUCT);
    }

    /** The caller retains ownership of the input coverage. Area uses squared CRS units. */
    public SimpleFeatureCollection execute(GridCoverage2D visibility, ProgressListener monitor) {
        if (visibility == null) {
            throw new ProcessException("A viewshed coverage is required.");
        }
        checkpoint(monitor);
        if (monitor != null) monitor.started();
        SimpleFeatureType schema = FeatureTypes.getDefaultType("ViewshedAsPolygon", MultiPolygon.class,
                visibility.getCoordinateReferenceSystem2D());
        schema = FeatureTypes.add(schema, VALUE_FIELD, Integer.class);
        schema = FeatureTypes.add(schema, AREA_FIELD, Double.class);
        ListFeatureCollection features = new ListFeatureCollection(schema);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(schema);

        RenderedOp vectorized = null;
        ZeroOriginImage normalized = null;
        try {
            RenderedImage image = visibility.getRenderedImage();
            AffineTransform gridToWorld = (AffineTransform) visibility.getGridGeometry()
                    .getGridToCRS2D(PixelOrientation.UPPER_LEFT);
            if (image.getMinX() != 0 || image.getMinY() != 0) {
                // JAITools 1.4 Vectorize assumes a zero image origin. Translate raster
                // views with integer coordinates, without resampling or copying pixels.
                gridToWorld = new AffineTransform(gridToWorld);
                gridToWorld.translate(image.getMinX(), image.getMinY());
                normalized = new ZeroOriginImage(image);
                image = normalized;
            }
            AffineTransformation transform = new AffineTransformation(gridToWorld.getScaleX(),
                    gridToWorld.getShearX(), gridToWorld.getTranslateX(), gridToWorld.getShearY(),
                    gridToWorld.getScaleY(), gridToWorld.getTranslateY());
            ParameterBlockJAI parameters = new ParameterBlockJAI("Vectorize");
            parameters.setSource("source0", image);
            parameters.setParameter("band", 0);
            // Generic RasterToPolygon also excludes the suggested BYTE NoData (zero).
            // A viewshed must retain zero as a valid invisible class.
            parameters.setParameter("outsideValues", Collections.<Number>singletonList(RasterViewshedOperation.NODATA));
            parameters.setParameter("insideEdges", Boolean.TRUE);
            parameters.setParameter("removeCollinear", Boolean.TRUE);
            parameters.setParameter("filterThreshold", 0d);
            vectorized = JAI.create("Vectorize", parameters);
            @SuppressWarnings("unchecked")
            Collection<Polygon> polygons = (Collection<Polygon>) vectorized
                    .getProperty(VectorizeDescriptor.VECTOR_PROPERTY_NAME);
            checkpoint(monitor);
            int count = 0;
            for (Polygon polygon : polygons) {
                checkpoint(monitor);
                if (polygon == null || polygon.isEmpty()) continue;
                Object raw = polygon.getUserData();
                if (!(raw instanceof Number)) {
                    throw new ProcessException("Missing visibility value on polygon.");
                }
                double value = ((Number) raw).doubleValue();
                if (value != RasterViewshedOperation.INVISIBLE && value != RasterViewshedOperation.VISIBLE) {
                    throw new ProcessException("Viewshed values must be 0, 1 or 255.");
                }
                polygon.setUserData(null);
                polygon.apply(transform);
                // Follow the repository's MultiPolygon convention for WPS/GML output.
                builder.add(polygon.getFactory().createMultiPolygon(new Polygon[] { polygon }));
                builder.add((int) value);
                builder.add(polygon.getArea());
                features.add(builder.buildFeature(schema.getTypeName() + "." + (++count)));
                if (monitor != null) monitor.progress(100f * count / polygons.size());
            }
            if (monitor != null) monitor.complete();
            return features;
        } catch (ProcessException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ProcessException("Cannot polygonize viewshed.", e);
        } finally {
            if (vectorized != null) vectorized.dispose();
            if (normalized != null) normalized.dispose();
        }
    }

    private static final class ZeroOriginImage extends PlanarImage {
        private final RenderedImage source;

        ZeroOriginImage(RenderedImage source) {
            super(new ImageLayout(source).setMinX(0).setMinY(0)
                    .setTileGridXOffset(Math.subtractExact(source.getTileGridXOffset(), source.getMinX()))
                    .setTileGridYOffset(Math.subtractExact(source.getTileGridYOffset(), source.getMinY())),
                    null, null);
            this.source = source;
        }

        @Override
        public Raster getTile(int x, int y) {
            Raster tile = source.getTile(x, y);
            return tile == null ? null : tile.createTranslatedChild(
                    Math.subtractExact(tile.getMinX(), source.getMinX()),
                    Math.subtractExact(tile.getMinY(), source.getMinY()));
        }
    }

    private static void checkpoint(ProgressListener monitor) {
        if (Thread.currentThread().isInterrupted() || (monitor != null && monitor.isCanceled())) {
            throw new ProcessException("Viewshed polygonization cancelled.");
        }
    }
}
