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
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.enumeration.ThiessenAttributeMode;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;

/**
 * Creates Thiessen polygons from point input features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ThiessenPolygonOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(ThiessenPolygonOperation.class);

    private static final String FID_FIELD = "TAGVALUE";

    private double proximalTolerance = 0d;

    private Geometry clipArea = null;

    private ThiessenAttributeMode attributeMode = ThiessenAttributeMode.ONLY_FID;

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

        List<Geometry> thiessenList = new ArrayList<Geometry>();
        for (int k = 0; k < thiessenGeoms.getNumGeometries(); k++) {
            thiessenList.add(thiessenGeoms.getGeometryN(k));
        }

        SimpleFeatureType featureType = null;
        switch (attributeMode) {
        case ONLY_FID:
            String shapeFieldName = pointSchema.getGeometryDescriptor().getLocalName();
            featureType = FeatureTypes.getDefaultType(getOutputTypeName(), shapeFieldName,
                    Polygon.class, crs);
            break;
        case ALL:
            featureType = FeatureTypes.build(pointSchema, getOutputTypeName(), Polygon.class);
            break;
        }
        featureType = FeatureTypes.add(featureType, FID_FIELD, Integer.class);

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);
        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = pointFeatures.features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();

                // get polygon
                Geometry voronoiPolygon = getIntersectGeometry(thiessenList, geometry);

                if (voronoiPolygon != null) {
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
                    SimpleFeature newFeature = featureWriter.buildFeature(null);
                    if (attributeMode == ThiessenAttributeMode.ALL) {
                        featureWriter.copyAttributes(feature, newFeature, false);
                    }

                    int fid = FeatureTypes.getFID(feature);
                    newFeature.setAttribute(FID_FIELD, fid);

                    newFeature.setDefaultGeometry(finalVoronoi);

                    featureWriter.write(newFeature);

                    // remove geometry from geometry list
                    thiessenList.remove(voronoiPolygon);
                } else {
                    // print("duplicated point feature!");
                }
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        return featureWriter.getFeatureCollection();
    }

    public SimpleFeatureCollection execute(List<Coordinate> coordinateList) throws IOException {
        CoordinateReferenceSystem crs = null;
        SimpleFeatureType featureType = FeatureTypes.getDefaultType(getOutputTypeName(),
                Polygon.class, crs);
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

                SimpleFeature newFeature = featureWriter.buildFeature(null);
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

        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = inputFeatures.features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();

                Coordinate coord = geometry.getCentroid().getCoordinate();
                pointList.add(coord);
            }
        } finally {
            featureIter.close();
        }

        return pointList;
    }

    private Geometry getIntersectGeometry(List<Geometry> geomList, Geometry origPoint) {
        for (Geometry curGeometry : geomList) {
            if (curGeometry.intersects(origPoint)) {
                return curGeometry;
            }
        }
        return null;
    }

}
