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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.enumeration.ThiessenAttributeMode;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.triangulate.VoronoiDiagramBuilder;

/**
 * Creates Thiessen polygons from point input features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ThiessenPolygonOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(ThiessenPolygonOperation.class);

    static final String TYPE_NAME = "ThiessenPolygon";

    static final String FID_FIELD = "TAGVALUE";

    private double proximalTolerance = 0d;

    private Geometry clipArea = null;

    private ThiessenAttributeMode attributeMode = ThiessenAttributeMode.OnlyFID;

    public void setAttributeMode(ThiessenAttributeMode attributeMode) {
        this.attributeMode = attributeMode;
    }

    public void setProximalTolerance(double proximalTolerance) {
        this.proximalTolerance = proximalTolerance;
    }

    public void setClipArea(Geometry clipArea) {
        this.clipArea = clipArea;
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection pointFeatures)
            throws IOException {
        SimpleFeatureType pointSchema = pointFeatures.getSchema();
        CoordinateReferenceSystem crs = pointSchema.getCoordinateReferenceSystem();

        // adjust extent
        List<Coordinate> coordinateList = getCoordinateList(pointFeatures);

        Geometry clipPolygon = clipArea;
        ReferencedEnvelope clipEnvelope = pointFeatures.getBounds();
        if (clipArea == null) {
            double deltaX = clipEnvelope.getWidth() * 0.2;
            double deltaY = clipEnvelope.getHeight() * 0.2;
            clipEnvelope.expandBy(deltaX, deltaY);
            clipPolygon = gf.toGeometry(clipEnvelope);
        } else {
            clipEnvelope = new ReferencedEnvelope(clipArea.getEnvelopeInternal(), crs);
        }

        // fast test
        PreparedGeometry praparedGeom = PreparedGeometryFactory.prepare(clipPolygon);

        // create voronoi diagram
        VoronoiDiagramBuilder vdBuilder = new VoronoiDiagramBuilder();
        vdBuilder.setClipEnvelope(clipEnvelope);
        vdBuilder.setSites(coordinateList);
        vdBuilder.setTolerance(proximalTolerance);

        Geometry thiessenGeoms = vdBuilder.getDiagram(gf);
        coordinateList.clear();

        STRtree spatialIndex = new STRtree();
        for (int k = 0; k < thiessenGeoms.getNumGeometries(); k++) {
            Geometry geometry = thiessenGeoms.getGeometryN(k);
            spatialIndex.insert(geometry.getEnvelopeInternal(), geometry);
        }

        SimpleFeatureType featureType = null;
        switch (attributeMode) {
        case OnlyFID:
            String shapeFieldName = pointSchema.getGeometryDescriptor().getLocalName();
            featureType = FeatureTypes.getDefaultType(pointSchema.getTypeName(), shapeFieldName,
                    Polygon.class, crs);
            break;
        case All:
            featureType = FeatureTypes.build(pointSchema, pointSchema.getTypeName(), Polygon.class);
            break;
        }
        featureType = FeatureTypes.add(featureType, FID_FIELD, Integer.class);

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);
        SimpleFeatureIterator featureIter = pointFeatures.features();
        try {
            int fid = 0;
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Point centroid = geometry.getCentroid();

                // get polygon
                Geometry voronoiPolygon = (Geometry) spatialIndex.nearestNeighbour(
                        geometry.getEnvelopeInternal(), geometry, new ItemDistance() {
                            @Override
                            public double distance(ItemBoundable item1, ItemBoundable item2) {
                                Geometry s1 = (Geometry) item1.getItem();
                                Geometry s2 = (Geometry) item2.getItem();
                                return s1.distance(s2);
                            }
                        });

                if (voronoiPolygon.contains(centroid)) {
                    Geometry finalVoronoi = voronoiPolygon;

                    if (praparedGeom.disjoint(voronoiPolygon)) {
                        continue;
                    } else if (!praparedGeom.contains(voronoiPolygon)) {
                        finalVoronoi = voronoiPolygon.intersection(clipPolygon);
                        if (finalVoronoi == null || finalVoronoi.isEmpty()) {
                            continue;
                        }
                    }

                    // create feature
                    SimpleFeature newFeature = featureWriter.buildFeature();
                    if (attributeMode == ThiessenAttributeMode.All) {
                        featureWriter.copyAttributes(feature, newFeature, false);
                    }

                    newFeature.setAttribute(FID_FIELD, fid++);
                    newFeature.setDefaultGeometry(finalVoronoi);
                    featureWriter.write(newFeature);
                } else {
                    LOGGER.log(Level.WARNING, "duplicated point feature!");
                }
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        return featureWriter.getFeatureCollection();
    }

    public SimpleFeatureCollection execute(List<Coordinate> coordinateList,
            CoordinateReferenceSystem crs) throws IOException {
        SimpleFeatureType featureType = FeatureTypes.getDefaultType(TYPE_NAME, Polygon.class, crs);
        featureType = FeatureTypes.add(featureType, FID_FIELD, Integer.class);

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        // build Voronoi Diagram
        double minx = Double.MAX_VALUE;
        double miny = Double.MAX_VALUE;
        double maxx = Double.MIN_VALUE;
        double maxy = Double.MIN_VALUE;

        List<Coordinate> coords = new ArrayList<Coordinate>(coordinateList.size());
        Iterator<Coordinate> it = coordinateList.iterator();
        while (it.hasNext()) {
            Coordinate coord = it.next();

            minx = Math.min(minx, coord.x);
            miny = Math.min(miny, coord.y);
            maxx = Math.max(maxx, coord.x);
            maxy = Math.max(maxy, coord.y);

            coords.add(coord);
        }

        ReferencedEnvelope clipEnvelope = new ReferencedEnvelope(minx, maxx, miny, maxy, crs);
        if (clipArea == null) {
            double deltaX = clipEnvelope.getWidth() * 0.2;
            double deltaY = clipEnvelope.getHeight() * 0.2;
            clipEnvelope.expandBy(deltaX, deltaY);
        } else {
            clipEnvelope = new ReferencedEnvelope(clipArea.getEnvelopeInternal(), crs);
        }

        VoronoiDiagramBuilder vdBuilder = new VoronoiDiagramBuilder();
        vdBuilder.setClipEnvelope(clipEnvelope);
        vdBuilder.setSites(coords);
        Geometry triangleGeoms = vdBuilder.getDiagram(gf);

        coords.clear();

        // insert features
        try {
            for (int k = 0; k < triangleGeoms.getNumGeometries(); k++) {
                Geometry curGeometry = triangleGeoms.getGeometryN(k);

                SimpleFeature newFeature = featureWriter.buildFeature();
                newFeature.setAttribute(FID_FIELD, k);

                if (clipArea == null) {
                    newFeature.setDefaultGeometry(curGeometry);
                } else {
                    if (clipArea.disjoint(curGeometry)) {
                        continue;
                    } else if (clipArea.contains(curGeometry)) {
                        newFeature.setDefaultGeometry(curGeometry);
                    } else {
                        Geometry interGeom = clipArea.intersection(curGeometry);
                        newFeature.setDefaultGeometry(interGeom);
                    }
                }

                featureWriter.write(newFeature);
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }

    public List<Coordinate> getCoordinateList(SimpleFeatureCollection inputFeatures) {
        List<Coordinate> pointList = new ArrayList<Coordinate>();

        SimpleFeatureIterator featureIter = inputFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                pointList.add(geometry.getCentroid().getCoordinate());
            }
        } finally {
            featureIter.close();
        }

        return pointList;
    }
}
