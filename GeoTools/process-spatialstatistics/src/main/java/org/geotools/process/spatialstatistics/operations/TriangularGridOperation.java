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
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Creates triangular grids from extent or bounds source features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class TriangularGridOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(TriangularGridOperation.class);

    private static final String TYPE_NAME = "triangulargrid";

    private static final String UID = "uid";

    private SimpleFeatureCollection boundsSource = null;

    private String the_geom = null;

    private Geometry boundsGeometry = null;

    private int featureID = 0;

    public void setBoundsGeometry(Geometry geometryBoundary) {
        this.boundsGeometry = geometryBoundary;
    }

    public void setBoundsSource(SimpleFeatureCollection boundsSource) {
        this.boundsSource = boundsSource;
        if (boundsSource == null) {
            this.the_geom = null;
        } else {
            this.the_geom = boundsSource.getSchema().getGeometryDescriptor().getLocalName();
        }
    }

    public SimpleFeatureCollection execute(ReferencedEnvelope bbox, Double size) throws IOException {
        CoordinateReferenceSystem crs = bbox.getCoordinateReferenceSystem();
        SimpleFeatureType schema = FeatureTypes.getDefaultType(TYPE_NAME, Polygon.class, crs);
        schema = FeatureTypes.add(schema, UID, Integer.class, 19);

        final double cellWidth = size;
        final double cellHeight = size;

        IFeatureInserter featureWriter = getFeatureWriter(schema);
        try {
            featureID = 0;
            Coordinate[] coords = new Coordinate[4];

            int yi = 0;
            double currentY = bbox.getMinY();

            while (currentY <= bbox.getMaxY()) {
                int xi = 0;
                double currentX = bbox.getMinX();

                while (currentX <= bbox.getMaxX()) {
                    if (xi % 2 == 0 && yi % 2 == 0) {
                        coords = new Coordinate[4];
                        coords[0] = new Coordinate(currentX, currentY);
                        coords[1] = new Coordinate(currentX, currentY + cellHeight);
                        coords[2] = new Coordinate(currentX + cellWidth, currentY);
                        coords[3] = new Coordinate(coords[0]);
                        this.writeFeature(featureWriter, gf.createPolygon(coords));

                        coords = new Coordinate[4];
                        coords[0] = new Coordinate(currentX, currentY + cellHeight);
                        coords[1] = new Coordinate(currentX + cellWidth, currentY + cellHeight);
                        coords[2] = new Coordinate(currentX + cellWidth, currentY);
                        coords[3] = new Coordinate(coords[0]);
                        this.writeFeature(featureWriter, gf.createPolygon(coords));
                    } else if (xi % 2 == 0 && yi % 2 == 1) {
                        coords = new Coordinate[4];
                        coords[0] = new Coordinate(currentX, currentY);
                        coords[1] = new Coordinate(currentX + cellWidth, currentY + cellHeight);
                        coords[2] = new Coordinate(currentX + cellWidth, currentY);
                        coords[3] = new Coordinate(coords[0]);
                        this.writeFeature(featureWriter, gf.createPolygon(coords));

                        coords = new Coordinate[4];
                        coords[0] = new Coordinate(currentX, currentY);
                        coords[1] = new Coordinate(currentX, currentY + cellHeight);
                        coords[2] = new Coordinate(currentX + cellWidth, currentY + cellHeight);
                        coords[3] = new Coordinate(coords[0]);
                        this.writeFeature(featureWriter, gf.createPolygon(coords));
                    } else if (yi % 2 == 0 && xi % 2 == 1) {
                        coords = new Coordinate[4];
                        coords[0] = new Coordinate(currentX, currentY);
                        coords[1] = new Coordinate(currentX, currentY + cellHeight);
                        coords[2] = new Coordinate(currentX + cellWidth, currentY + cellHeight);
                        coords[3] = new Coordinate(coords[0]);
                        this.writeFeature(featureWriter, gf.createPolygon(coords));

                        coords = new Coordinate[4];
                        coords[0] = new Coordinate(currentX, currentY);
                        coords[1] = new Coordinate(currentX + cellWidth, currentY + cellHeight);
                        coords[2] = new Coordinate(currentX + cellWidth, currentY);
                        coords[3] = new Coordinate(coords[0]);
                        this.writeFeature(featureWriter, gf.createPolygon(coords));
                    } else if (yi % 2 == 1 && xi % 2 == 1) {
                        coords = new Coordinate[4];
                        coords[0] = new Coordinate(currentX, currentY);
                        coords[1] = new Coordinate(currentX, currentY + cellHeight);
                        coords[2] = new Coordinate(currentX + cellWidth, currentY);
                        coords[3] = new Coordinate(coords[0]);
                        this.writeFeature(featureWriter, gf.createPolygon(coords));

                        coords = new Coordinate[4];
                        coords[0] = new Coordinate(currentX, currentY + cellHeight);
                        coords[1] = new Coordinate(currentX + cellWidth, currentY + cellHeight);
                        coords[2] = new Coordinate(currentX + cellWidth, currentY);
                        coords[3] = new Coordinate(coords[0]);
                        this.writeFeature(featureWriter, gf.createPolygon(coords));
                    }
                    currentX += cellWidth;
                    xi++;
                }
                currentY += cellHeight;
                yi++;
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }

    private void writeFeature(IFeatureInserter featureWriter, Geometry geometry) throws IOException {
        if (boundsGeometry != null) {
            if (!boundsGeometry.intersects(geometry)) {
                return;
            }
        }

        if (boundsSource != null) {
            Filter filter = ff.intersects(ff.property(the_geom), ff.literal(geometry));
            if (boundsSource.subCollection(filter).isEmpty()) {
                return;
            }
        }

        SimpleFeature newFeature = featureWriter.buildFeature(null);
        newFeature.setAttribute(UID, ++featureID);
        newFeature.setDefaultGeometry(geometry);
        featureWriter.write(newFeature);
    }
}