/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2014, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.process.spatialstatistics.operations;

import java.io.IOException;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.DataUtils;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.enumeration.FishnetType;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

/**
 * Creates a fishnet of rectangular cells.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class FishnetOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(FishnetOperation.class);

    static final String TYPE_NAME = "fishnet";

    static final String UID = "uid";

    private FishnetType fishnetType = FishnetType.Rectangle;

    private SimpleFeatureCollection boundsSource = null;

    private String the_geom = null;

    private PreparedGeometry boundsGeometry = null;

    private boolean boundaryInside = false;

    public void setBoundaryInside(boolean boundaryInside) {
        this.boundaryInside = boundaryInside;
    }

    public void setFishnetType(FishnetType fishnetType) {
        this.fishnetType = fishnetType;
    }

    public void setGeometryBoundary(Geometry geometryBoundary) {
        if (geometryBoundary == null) {
            this.boundsGeometry = null;
        } else {
            this.boundsGeometry = PreparedGeometryFactory.prepare(geometryBoundary);
        }
    }

    public void setBoundsSource(SimpleFeatureCollection boundsSource) {
        this.boundsSource = boundsSource;
        if (boundsSource == null) {
            this.the_geom = null;
        } else {
            this.the_geom = boundsSource.getSchema().getGeometryDescriptor().getLocalName();

            // use SpatialIndexFeatureCollection
            this.boundsSource = DataUtils.toSpatialIndexFeatureCollection(boundsSource);
        }
    }

    public SimpleFeatureCollection execute(ReferencedEnvelope bbox, Double width, Double height)
            throws IOException {
        int columns = (int) Math.floor((bbox.getWidth() / width) + 0.5d);
        int rows = (int) Math.floor((bbox.getHeight() / height) + 0.5d);

        columns = columns * width < bbox.getWidth() ? columns + 1 : columns;
        rows = rows * height < bbox.getHeight() ? rows + 1 : rows;

        // recalculate envelope : origin = lower left
        final double x1 = bbox.getMinX();
        final double y1 = bbox.getMinY();
        final double x2 = bbox.getMinX() + (columns * width);
        final double y2 = bbox.getMinY() + (rows * height);

        CoordinateReferenceSystem crs = bbox.getCoordinateReferenceSystem();
        ReferencedEnvelope finalBBox = new ReferencedEnvelope(crs);
        finalBBox.init(x1, x2, y1, y2);

        return execute(finalBBox, columns, rows);
    }

    public SimpleFeatureCollection execute(ReferencedEnvelope bbox, Integer columns, Integer rows)
            throws IOException {
        CoordinateReferenceSystem crs = bbox.getCoordinateReferenceSystem();
        SimpleFeatureType schema = FeatureTypes.getDefaultType(TYPE_NAME, Polygon.class, crs);
        schema = FeatureTypes.add(schema, UID, Integer.class, 19);

        final double width = bbox.getWidth() / columns;
        final double height = bbox.getHeight() / rows;

        final double minX = bbox.getMinX();
        final double minY = bbox.getMinY();

        // insert features
        IFeatureInserter featureWriter = getFeatureWriter(schema);
        try {
            int featureID = 0;
            ReferencedEnvelope bounds = new ReferencedEnvelope(crs);
            double ypos = minY;
            for (int row = 0; row < rows; row++) {
                double xpos = minX;
                for (int col = 0; col < columns; col++) {
                    bounds.init(xpos, xpos + width, ypos, ypos + height);

                    Geometry cellGeom = null;
                    switch (fishnetType) {
                    case Rectangle:
                        cellGeom = gf.toGeometry(bounds);
                        break;
                    case Circle:
                        double radius = bounds.getWidth() / 2.0;
                        cellGeom = gf.createPoint(bounds.centre()).buffer(radius);
                        break;
                    }

                    if (boundsGeometry != null) {
                        if (boundaryInside) {
                            if (!boundsGeometry.contains(cellGeom)) {
                                xpos += width;
                                continue;
                            }
                        } else {
                            if (!boundsGeometry.intersects(cellGeom)) {
                                xpos += width;
                                continue;
                            }
                        }
                    }

                    if (boundsSource != null) {
                        Filter filter = getIntersectsFilter(the_geom, cellGeom);
                        if (boundaryInside) {
                            filter = ff.within(ff.property(the_geom), ff.literal(cellGeom));
                        }
                        if (boundsSource.subCollection(filter).isEmpty()) {
                            xpos += width;
                            continue;
                        }
                    }

                    // create feature and set geometry
                    SimpleFeature newFeature = featureWriter.buildFeature();
                    newFeature.setAttribute(UID, featureID);
                    newFeature.setDefaultGeometry(cellGeom);

                    featureWriter.write(newFeature);
                    xpos += width;
                }
                ypos += height;
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }
}