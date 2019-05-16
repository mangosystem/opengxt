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
import org.geotools.grid.hexagon.HexagonOrientation;
import org.geotools.process.spatialstatistics.core.DataUtils;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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

    private PreparedGeometry boundsGeometry = null;

    private int featureID = 0;

    private HexagonOrientation orientation = HexagonOrientation.FLAT;

    public void setOrientation(HexagonOrientation orientation) {
        this.orientation = orientation;
    }

    public void setBoundsGeometry(Geometry geometryBoundary) {
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

    public SimpleFeatureCollection execute(ReferencedEnvelope bbox, Double size) throws IOException {
        CoordinateReferenceSystem crs = bbox.getCoordinateReferenceSystem();
        SimpleFeatureType schema = FeatureTypes.getDefaultType(TYPE_NAME, Polygon.class, crs);
        schema = FeatureTypes.add(schema, UID, Integer.class, 19);

        if (orientation == HexagonOrientation.ANGLED) {
            return executeAngled(schema, bbox, size);
        } else {
            return executeNormal(schema, bbox, size);
        }
    }

    private SimpleFeatureCollection executeNormal(SimpleFeatureType schema,
            ReferencedEnvelope bbox, Double size) throws IOException {
        IFeatureInserter writer = getFeatureWriter(schema);
        try {
            featureID = 0;
            final double half = size / 2.0;
            final double h = Math.sqrt(Math.pow(size, 2.0) - Math.pow(half, 2.0));
            CoordinateList list = new CoordinateList();

            int yi = 0;
            double currentY = bbox.getMinY();
            while (currentY <= bbox.getMaxY()) {
                double currentX = bbox.getMinX();
                while (currentX <= bbox.getMaxX()) {
                    if (yi % 2 == 0) {
                        list.clear();
                        list.add(new Coordinate(currentX, currentY), false);
                        list.add(new Coordinate(currentX + half, currentY + h), false);
                        list.add(new Coordinate(currentX + size, currentY), false);
                        list.closeRing();
                        this.writeFeature(writer, gf.createPolygon(list.toCoordinateArray()));

                        list.clear();
                        list.add(new Coordinate(currentX + half, currentY + h), false);
                        list.add(new Coordinate(currentX + size + half, currentY + h), false);
                        list.add(new Coordinate(currentX + size, currentY), false);
                        list.closeRing();
                        this.writeFeature(writer, gf.createPolygon(list.toCoordinateArray()));
                    } else {
                        list.clear();
                        list.add(new Coordinate(currentX - half, currentY), false);
                        list.add(new Coordinate(currentX, currentY + h), false);
                        list.add(new Coordinate(currentX + half, currentY), false);
                        list.closeRing();
                        this.writeFeature(writer, gf.createPolygon(list.toCoordinateArray()));

                        list.clear();
                        list.add(new Coordinate(currentX, currentY + h), false);
                        list.add(new Coordinate(currentX + size, currentY + h), false);
                        list.add(new Coordinate(currentX + half, currentY), false);
                        list.closeRing();
                        this.writeFeature(writer, gf.createPolygon(list.toCoordinateArray()));
                    }
                    currentX += size;
                }
                yi++;
                currentY += h;
            }
        } catch (Exception e) {
            writer.rollback(e);
        } finally {
            writer.close();
        }
        return writer.getFeatureCollection();
    }

    private SimpleFeatureCollection executeAngled(SimpleFeatureType schema,
            ReferencedEnvelope bbox, Double size) throws IOException {
        IFeatureInserter writer = getFeatureWriter(schema);
        try {
            featureID = 0;
            Coordinate[] list = new Coordinate[4];

            int yi = 0;
            double currentY = bbox.getMinY();

            while (currentY <= bbox.getMaxY()) {
                int xi = 0;
                double currentX = bbox.getMinX();

                while (currentX <= bbox.getMaxX()) {
                    if (xi % 2 == 0 && yi % 2 == 0) {
                        list = new Coordinate[4];
                        list[0] = new Coordinate(currentX, currentY);
                        list[1] = new Coordinate(currentX, currentY + size);
                        list[2] = new Coordinate(currentX + size, currentY);
                        list[3] = new Coordinate(list[0]);
                        this.writeFeature(writer, gf.createPolygon(list));

                        list = new Coordinate[4];
                        list[0] = new Coordinate(currentX, currentY + size);
                        list[1] = new Coordinate(currentX + size, currentY + size);
                        list[2] = new Coordinate(currentX + size, currentY);
                        list[3] = new Coordinate(list[0]);
                        this.writeFeature(writer, gf.createPolygon(list));
                    } else if (xi % 2 == 0 && yi % 2 == 1) {
                        list = new Coordinate[4];
                        list[0] = new Coordinate(currentX, currentY);
                        list[1] = new Coordinate(currentX + size, currentY + size);
                        list[2] = new Coordinate(currentX + size, currentY);
                        list[3] = new Coordinate(list[0]);
                        this.writeFeature(writer, gf.createPolygon(list));

                        list = new Coordinate[4];
                        list[0] = new Coordinate(currentX, currentY);
                        list[1] = new Coordinate(currentX, currentY + size);
                        list[2] = new Coordinate(currentX + size, currentY + size);
                        list[3] = new Coordinate(list[0]);
                        this.writeFeature(writer, gf.createPolygon(list));
                    } else if (yi % 2 == 0 && xi % 2 == 1) {
                        list = new Coordinate[4];
                        list[0] = new Coordinate(currentX, currentY);
                        list[1] = new Coordinate(currentX, currentY + size);
                        list[2] = new Coordinate(currentX + size, currentY + size);
                        list[3] = new Coordinate(list[0]);
                        this.writeFeature(writer, gf.createPolygon(list));

                        list = new Coordinate[4];
                        list[0] = new Coordinate(currentX, currentY);
                        list[1] = new Coordinate(currentX + size, currentY + size);
                        list[2] = new Coordinate(currentX + size, currentY);
                        list[3] = new Coordinate(list[0]);
                        this.writeFeature(writer, gf.createPolygon(list));
                    } else if (yi % 2 == 1 && xi % 2 == 1) {
                        list = new Coordinate[4];
                        list[0] = new Coordinate(currentX, currentY);
                        list[1] = new Coordinate(currentX, currentY + size);
                        list[2] = new Coordinate(currentX + size, currentY);
                        list[3] = new Coordinate(list[0]);
                        this.writeFeature(writer, gf.createPolygon(list));

                        list = new Coordinate[4];
                        list[0] = new Coordinate(currentX, currentY + size);
                        list[1] = new Coordinate(currentX + size, currentY + size);
                        list[2] = new Coordinate(currentX + size, currentY);
                        list[3] = new Coordinate(list[0]);
                        this.writeFeature(writer, gf.createPolygon(list));
                    }
                    currentX += size;
                    xi++;
                }
                currentY += size;
                yi++;
            }
        } catch (Exception e) {
            writer.rollback(e);
        } finally {
            writer.close();
        }
        return writer.getFeatureCollection();
    }

    private void writeFeature(IFeatureInserter featureWriter, Geometry geometry) throws IOException {
        if (boundsGeometry != null) {
            if (!boundsGeometry.intersects(geometry)) {
                return;
            }
        }

        if (boundsSource != null) {
            Filter filter = getIntersectsFilter(the_geom, geometry);
            if (boundsSource.subCollection(filter).isEmpty()) {
                return;
            }
        }

        SimpleFeature newFeature = featureWriter.buildFeature();
        newFeature.setAttribute(UID, featureID);
        newFeature.setDefaultGeometry(geometry);
        featureWriter.write(newFeature);
    }
}