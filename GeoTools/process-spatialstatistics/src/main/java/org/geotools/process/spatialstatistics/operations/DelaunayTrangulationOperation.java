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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryComponentFilter;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.kdtree.KdNode;
import com.vividsolutions.jts.index.kdtree.KdTree;
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder;

/**
 * Creates Thiessen polygons from point input features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class DelaunayTrangulationOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(DelaunayTrangulationOperation.class);

    private static final String[] FIELDS = { "uid", "pointa", "pointb", "pointc" };

    private Geometry clipArea = null;

    private double proximalTolerance = 0.0d;

    private KdTree spatialIndex = new KdTree(0.0d);

    public Geometry getClipArea() {
        return clipArea;
    }

    public void setClipArea(Geometry clipArea) {
        this.clipArea = clipArea;
    }

    public void setProximalTolerance(double proximalTolerance) {
        this.proximalTolerance = proximalTolerance;
    }

    public double getProximalTolerance() {
        return proximalTolerance;
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection pointFeatures)
            throws IOException {
        CoordinateReferenceSystem crs = pointFeatures.getSchema().getCoordinateReferenceSystem();

        // create delaunay triangulation
        List<Coordinate> coordinateList = getCoordinateList(pointFeatures);
        ReferencedEnvelope clipEnvelope = pointFeatures.getBounds();

        Geometry clipPolygon = clipArea;
        if (clipArea == null) {
            double deltaX = clipEnvelope.getWidth() * 0.1;
            double deltaY = clipEnvelope.getHeight() * 0.1;
            clipEnvelope.expandBy(deltaX, deltaY);
            clipPolygon = gf.toGeometry(clipEnvelope);
        } else {
            clipEnvelope = new ReferencedEnvelope(clipArea.getEnvelopeInternal(), crs);
        }

        // fast test
        PreparedGeometry praparedGeom = PreparedGeometryFactory.prepare(clipPolygon);

        // Gets the faces of the computed triangulation as a GeometryCollection of Polygon.
        DelaunayTriangulationBuilder vdBuilder = new DelaunayTriangulationBuilder();
        vdBuilder.setSites(coordinateList);
        vdBuilder.setTolerance(proximalTolerance);
        Geometry triangleGeoms = vdBuilder.getTriangles(gf);
        coordinateList.clear();

        String geomName = pointFeatures.getSchema().getGeometryDescriptor().getLocalName();
        SimpleFeatureType featureType = FeatureTypes.getDefaultType(getOutputTypeName(), geomName,
                Polygon.class, crs);
        for (String field : FIELDS) {
            featureType = FeatureTypes.add(featureType, field, Integer.class);
        }

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        // insert features
        try {
            List<KdNode> nodeList = new ArrayList<KdNode>();
            for (int index = 0; index < triangleGeoms.getNumGeometries(); index++) {
                Geometry triangle = triangleGeoms.getGeometryN(index);
                if (triangle == null || triangle.isEmpty()) {
                    continue;
                }

                Geometry finalGeometry = triangle;
                if (clipArea != null) {
                    if (praparedGeom.disjoint(triangle)) {
                        continue;
                    } else {
                        Geometry clipped = triangle.intersection(clipPolygon);
                        if (clipped == null || clipped.isEmpty()) {
                            continue;
                        }

                        final List<Polygon> geoms = new ArrayList<Polygon>();
                        clipped.apply(new GeometryComponentFilter() {

                            @Override
                            public void filter(Geometry geom) {
                                if (geom instanceof Polygon) {
                                    geoms.add((Polygon) geom);
                                }
                            }
                        });

                        if (geoms.size() == 0) {
                            continue;
                        }

                        Polygon[] lsArray = (Polygon[]) geoms.toArray(new Polygon[geoms.size()]);
                        finalGeometry = triangle.getFactory().createMultiPolygon(lsArray);
                    }
                }

                // get neighbor point
                nodeList.clear();
                spatialIndex.query(triangle.getEnvelopeInternal(), nodeList);
                List<Integer> fidList = new ArrayList<Integer>();
                for (KdNode node : nodeList) {
                    if (triangle.disjoint(gf.createPoint(node.getCoordinate()))) {
                        continue;
                    }
                    fidList.add((Integer) node.getData());
                }

                // create feature
                SimpleFeature newFeature = featureWriter.buildFeature(Integer.toString(index));
                newFeature.setAttribute(FIELDS[0], index);
                if (fidList.size() >= 3) {
                    newFeature.setAttribute(FIELDS[1], fidList.get(0));
                    newFeature.setAttribute(FIELDS[2], fidList.get(1));
                    newFeature.setAttribute(FIELDS[3], fidList.get(2));
                }
                newFeature.setDefaultGeometry(finalGeometry);
                featureWriter.write(newFeature);
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }

    private Integer getFeatureID(String id) {
        int pos = id.indexOf(".");
        if (pos == -1) {
            return Integer.valueOf(id);
        } else {
            return Integer.valueOf(id.substring(pos + 1));
        }
    }

    private List<Coordinate> getCoordinateList(SimpleFeatureCollection inputFeatures) {
        spatialIndex = new KdTree(0.0d);
        List<Coordinate> coordinateList = new ArrayList<Coordinate>();
        SimpleFeatureIterator featureIter = inputFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();

                Coordinate coord = geometry.getCentroid().getCoordinate();
                spatialIndex.insert(coord, getFeatureID(feature.getID()));
                coordinateList.add(coord);
            }
        } finally {
            featureIter.close();
        }

        return coordinateList;
    }
}
