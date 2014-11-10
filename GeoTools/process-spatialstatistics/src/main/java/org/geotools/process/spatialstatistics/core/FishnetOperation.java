package org.geotools.process.spatialstatistics.core;

import java.io.IOException;
import java.util.logging.Logger;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.enumeration.FishnetType;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Creates a fishnet of rectangular cells.
 * 
 * @author Minpa Lee
 * 
 */
public class FishnetOperation {
    protected static final Logger LOGGER = Logging.getLogger(FishnetOperation.class);

    private GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(null);

    private FishnetType fishnetType = FishnetType.Rectangle;

    private Geometry geometryBoundary = null;

    private boolean boundaryInside = false;

    public void setBoundaryInside(boolean boundaryInside) {
        this.boundaryInside = boundaryInside;
    }

    public void setFishnetType(FishnetType fishnetType) {
        this.fishnetType = fishnetType;
    }

    public void setGeometryBoundary(Geometry geometryBoundary) {
        this.geometryBoundary = geometryBoundary;
    }

    public SimpleFeatureCollection execute(ReferencedEnvelope bbox, Double width, Double height)
            throws IOException {
        int columns = (int) Math.floor((bbox.getWidth() / width) + 0.5d);
        int rows = (int) Math.floor((bbox.getHeight() / height) + 0.5d);

        columns = columns * width < bbox.getWidth() ? columns + 1 : columns;
        rows = rows * height < bbox.getHeight() ? rows + 1 : rows;

        // 컬럼 및 로 수에 맞게 Envelope을 재계산한다.
        final double x1 = bbox.getMinX();
        final double y1 = bbox.getMinY();
        final double x2 = bbox.getMinX() + columns * width;
        final double y2 = bbox.getMinY() + rows * height;

        CoordinateReferenceSystem crs = bbox.getCoordinateReferenceSystem();
        ReferencedEnvelope finalBBox = new ReferencedEnvelope(crs);
        finalBBox.init(x1, x2, y1, y2);

        return execute(finalBBox, columns, rows);
    }

    public SimpleFeatureCollection execute(ReferencedEnvelope bbox, Integer columns, Integer rows)
            throws IOException {
        // prepare transactional feature store
        CoordinateReferenceSystem crs = bbox.getCoordinateReferenceSystem();
        SimpleFeatureType featureType = FeatureTypes.getDefaultType("fishnet", Polygon.class, crs);
        featureType = FeatureTypes.add(featureType, "uid", Integer.class, 38);

        // insert features
        final double width = bbox.getWidth() / columns;
        final double height = bbox.getHeight() / rows;

        final double minX = bbox.getMinX();
        final double minY = bbox.getMinY();
        ReferencedEnvelope bounds = new ReferencedEnvelope(crs);
        int featureID = 0;

        ListFeatureCollection fishnet = new ListFeatureCollection(featureType);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
        for (int row = 0; row < rows; row++) {
            final double ypos = minY + (height * row);
            for (int col = 0; col < columns; col++) {
                final double xpos = minX + (width * col);
                bounds.init(xpos, xpos + width, ypos, ypos + height);
                Geometry polyGeom = null;
                switch (fishnetType) {
                case Rectangle:
                    polyGeom = gf.toGeometry(bounds);
                    break;
                case Circle:
                    final double radius = bounds.getWidth() / 2.0;
                    polyGeom = gf.createPoint(bounds.centre()).buffer(radius);
                    break;
                }

                if (geometryBoundary != null) {
                    if (boundaryInside) {
                        if (!geometryBoundary.contains(polyGeom)) {
                            continue;
                        }
                    } else {
                        if (!geometryBoundary.intersects(polyGeom)) {
                            continue;
                        }
                    }
                }

                // create feature and set geometry
                builder.reset();
                builder.set("uid", ++featureID);
                SimpleFeature newFeature = builder.buildFeature("fishnet." + featureID);
                newFeature.setDefaultGeometry(polyGeom);
                fishnet.add(newFeature);
            }
        }

        return fishnet;
    }
}