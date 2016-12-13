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
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Collect Event combines coincident points.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class CollectEventsOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(CollectEventsOperation.class);

    static final String COUNT_FIELD = "icount";

    private double tolerance = 0.1d;

    public double getTolerance() {
        return tolerance;
    }

    public void setTolerance(double tolerance) {
        this.tolerance = tolerance;
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection points, String countField)
            throws IOException {
        String typeName = points.getSchema().getTypeName();
        SimpleFeatureType schema = FeatureTypes.build(points.getSchema(), typeName);
        if (countField == null || countField.isEmpty()) {
            countField = COUNT_FIELD;
        }
        schema = FeatureTypes.add(schema, countField, Integer.class);
        Class<?> outputBinding = schema.getDescriptor(countField).getType().getBinding();

        List<Event> events = buildIndex(points);
        List<Event> coincidentEvents = new ArrayList<Event>();
        List<String> processedMap = new ArrayList<String>();

        IFeatureInserter featureWriter = getFeatureWriter(schema);
        SimpleFeatureIterator featureIter = points.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                String featureID = feature.getID();
                if (processedMap.contains(featureID)) {
                    continue;
                }

                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Coordinate coordinate = geometry.getCoordinate();

                // count coincident events
                int featureCount = 1;
                for (Event event : events) {
                    if (processedMap.contains(event.getFID()) || event.equalsFID(featureID)
                            || event.distance(coordinate) > tolerance) {
                        continue;
                    }

                    featureCount++;
                    processedMap.add(event.getFID());
                    coincidentEvents.add(event);
                }

                // remove coincident events
                if (coincidentEvents.size() > 0) {
                    events.removeAll(coincidentEvents);
                    coincidentEvents.clear();
                }

                // create & insert feature
                SimpleFeature newFeature = featureWriter.buildFeature();
                featureWriter.copyAttributes(feature, newFeature, true);
                Object countVal = Converters.convert(featureCount, outputBinding);
                newFeature.setAttribute(countField, countVal);

                featureWriter.write(newFeature);
                processedMap.add(featureID);
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }

    private List<Event> buildIndex(SimpleFeatureCollection points) {
        List<Event> events = new ArrayList<Event>();
        SimpleFeatureIterator featureIter = points.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                events.add(new Event(feature.getID(), geometry.getCoordinate()));
            }
        } finally {
            featureIter.close();
        }
        return events;
    }

    final class Event {

        private String fid;

        private Coordinate coordinate;

        public Event(String fid, Coordinate coordinate) {
            this.fid = fid;
            this.coordinate = coordinate;
        }

        public String getFID() {
            return this.fid;
        }

        public boolean equalsFID(String fid) {
            return this.fid.equals(fid);
        }

        public double distance(Coordinate coordinate) {
            return this.coordinate.distance(coordinate);
        }
    }

}
