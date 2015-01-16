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
package org.geotools.process.spatialstatistics.distribution;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.FeatureTypes.SimpleShapeType;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

/**
 * Identifies the mean direction, length, and geographic center for a set of lines.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class LinearDirectionalMeanOperation extends AbstractDisributionOperator {
    protected static final Logger LOGGER = Logging.getLogger(LinearDirectionalMeanOperation.class);

    String[] FIELDS = { "CompassA", "DirMean", "CirVar", "AveX", "AveY", "AveLen" };

    public SimpleFeatureCollection execute(SimpleFeatureCollection features,
            boolean orientationOnly, String caseField) throws IOException {
        SimpleFeatureType schema = features.getSchema();
        if (FeatureTypes.getSimpleShapeType(schema) != SimpleShapeType.LINESTRING) {
            LOGGER.log(Level.SEVERE, schema.getTypeName() + " is not a linestring features!");
            return null;
        }

        caseField = FeatureTypes.validateProperty(schema, caseField);
        int idxCase = caseField == null ? -1 : schema.indexOf(caseField);

        LinearDirectionalMeanVisitor visitor = new LinearDirectionalMeanVisitor();
        visitor.setOrientationOnly(orientationOnly);

        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = features.features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty())
                    continue;

                // Case Field
                Object caseVal = idxCase == -1 ? ALL : feature.getAttribute(idxCase);

                visitor.visit(geometry, caseVal);
            }
        } finally {
            featureIter.close();
        }

        // build feature collection
        CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
        String geomName = schema.getGeometryDescriptor().getLocalName();

        SimpleFeatureType featureType = FeatureTypes.getDefaultType(this.getOutputTypeName(),
                geomName, LineString.class, crs);
        for (String field : FIELDS) {
            featureType = FeatureTypes.add(featureType, field, Double.class, 38);
        }

        if (idxCase != -1) {
            featureType = FeatureTypes.add(featureType, schema.getDescriptor(caseField));
        }

        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        @SuppressWarnings("unchecked")
        HashMap<Object, LinearDirectionalMean> resultMap = visitor.getResult();
        Iterator<Object> iter = resultMap.keySet().iterator();
        try {
            while (iter.hasNext()) {
                Object caseVal = iter.next();
                LinearDirectionalMean curDm = resultMap.get(caseVal);

                // FIELDS = {"CompassA", "DirMean", "CirVar", "AveX", "AveY", "AveLen"};
                SimpleFeature newFeature = featureWriter.buildFeature(null);
                newFeature.setDefaultGeometry(curDm.getDirectionalLine());

                // create feature and set geometry
                if (idxCase != -1) {
                    newFeature.setAttribute(caseField, caseVal);
                }

                // #### Re-adjust Angle Back towards North ####
                double degreeAngle = curDm.getDegreeAngle();
                if (orientationOnly) {
                    degreeAngle = degreeAngle - 180.0;
                }

                newFeature.setAttribute(FIELDS[0], degreeAngle);
                newFeature.setAttribute(FIELDS[1], curDm.getDirMean());
                newFeature.setAttribute(FIELDS[2], curDm.getCirVar());
                newFeature.setAttribute(FIELDS[3], curDm.getMeanX());
                newFeature.setAttribute(FIELDS[4], curDm.getMeanY());
                newFeature.setAttribute(FIELDS[5], curDm.getMeanLength());

                featureWriter.write(newFeature);
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }

}
