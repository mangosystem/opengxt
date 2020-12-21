/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Creates a regular point features within extent and boundary sources.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class RegularPointsOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(RegularPointsOperation.class);

    public enum SizeUnit {
        Count, Distance
    }

    static final String TYPE_NAME = "points";

    static final String UID = "uid";

    private SimpleFeatureCollection boundsSource = null;

    private String the_geom = null;

    private PreparedGeometry boundsGeometry = null;

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

    public SimpleFeatureCollection execute(ReferencedEnvelope bbox, SizeUnit unit, Number width,
            Number height) throws IOException {
        if (unit == SizeUnit.Distance) {
            return execute(bbox, width.doubleValue(), height.doubleValue());
        } else {
            return execute(bbox, width.intValue(), height.intValue());
        }
    }

    private SimpleFeatureCollection execute(ReferencedEnvelope bbox, Double width, Double height)
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

    private SimpleFeatureCollection execute(ReferencedEnvelope bbox, Integer columns, Integer rows)
            throws IOException {
        CoordinateReferenceSystem crs = bbox.getCoordinateReferenceSystem();
        SimpleFeatureType schema = FeatureTypes.getDefaultType(TYPE_NAME, Point.class, crs);
        schema = FeatureTypes.add(schema, UID, Integer.class, 19);

        final double width = bbox.getWidth() / columns;
        final double height = bbox.getHeight() / rows;

        final double minX = bbox.getMinX();
        final double minY = bbox.getMinY();

        // insert features
        IFeatureInserter featureWriter = getFeatureWriter(schema);
        try {
            int featureID = 0;

            final double halfWidth = width / 2.0;
            final double halfHeight = height / 2.0;

            double ypos = minY;
            for (int row = 0; row < rows; row++) {
                double xpos = minX;
                for (int col = 0; col < columns; col++) {
                    Coordinate center = new Coordinate(xpos + halfWidth, ypos + halfHeight);
                    Geometry point = gf.createPoint(center);

                    if (boundsGeometry != null) {
                        if (!boundsGeometry.intersects(point)) {
                            xpos += width;
                            continue;
                        }
                    }

                    if (boundsSource != null) {
                        Filter filter = ff.intersects(ff.property(the_geom), ff.literal(point));
                        if (boundsSource.subCollection(filter).isEmpty()) {
                            xpos += width;
                            continue;
                        }
                    }

                    // create feature and set geometry
                    SimpleFeature newFeature = featureWriter.buildFeature();
                    newFeature.setAttribute(UID, featureID);
                    newFeature.setDefaultGeometry(point);

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
