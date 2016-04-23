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
package org.geotools.process.spatialstatistics.transformation;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.Expression;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LengthIndexedLine;

/**
 * Splits line features based on distance or distance expression.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SplitByDistanceFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging
            .getLogger(SplitByDistanceFeatureCollection.class);

    private Expression distance;

    public SplitByDistanceFeatureCollection(SimpleFeatureCollection delegate, Expression distance) {
        super(new ExplodeFeatureCollection(delegate)); // explode features !

        this.distance = distance;
    }

    @Override
    public SimpleFeatureIterator features() {
        return new SplitByDistanceFeatureIterator(delegate.features(), getSchema(), distance);
    }

    @Override
    public int size() {
        return DataUtilities.count(features());
    }

    static class SplitByDistanceFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private int index = 0;

        private List<Geometry> lineStrings;

        private int featureID = 0;

        private SimpleFeatureBuilder builder;

        private Expression distance;

        private SimpleFeature nextFeature = null;

        private SimpleFeature origFeature = null;

        public SplitByDistanceFeatureIterator(SimpleFeatureIterator delegate,
                SimpleFeatureType schema, Expression distance) {
            this.delegate = delegate;

            this.index = 0;
            this.builder = new SimpleFeatureBuilder(schema);
            this.distance = distance;
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            while ((nextFeature == null && delegate.hasNext())
                    || (nextFeature == null && !delegate.hasNext() && index > 0)) {
                if (index == 0) {
                    origFeature = delegate.next();
                    Double interval = distance.evaluate(origFeature, Double.class);
                    Geometry geometry = (Geometry) origFeature.getDefaultGeometry();
                    lineStrings = splitLines(geometry, interval);
                }

                // create feature
                for (Object attribute : origFeature.getAttributes()) {
                    if (attribute instanceof Geometry) {
                        attribute = lineStrings.get(index);
                    }
                    builder.add(attribute);
                }
                nextFeature = builder.buildFeature(Integer.toString(++featureID));
                builder.reset();
                index++;

                if (index >= lineStrings.size()) {
                    index = 0;
                    origFeature = null;
                }
            }
            return nextFeature != null;
        }

        public SimpleFeature next() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException("hasNext() returned false!");
            }
            SimpleFeature result = nextFeature;
            nextFeature = null;
            return result;
        }

        private List<Geometry> splitLines(Geometry lineString, double interval) {
            List<Geometry> list = new ArrayList<Geometry>();
            LengthIndexedLine lil = new LengthIndexedLine(lineString);
            final int count = (int) Math.ceil(lineString.getLength() / interval);
            
            double startIndex = 0;
            for (int index = 0; index < count; index++) {
                LineString splits = (LineString) lil.extractLine(startIndex, startIndex + interval);

                if (splits != null && !splits.isEmpty() && splits.getLength() > 0) {
                    splits.setUserData(lineString.getUserData());
                    list.add(splits);
                }
                startIndex += interval;
            }
            return list;
        }
    }
}
