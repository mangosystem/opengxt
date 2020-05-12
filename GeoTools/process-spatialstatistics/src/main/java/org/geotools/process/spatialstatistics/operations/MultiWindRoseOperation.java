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
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.AttributeDescriptorImpl;
import org.geotools.feature.type.AttributeTypeImpl;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.process.spatialstatistics.storage.NamePolicy;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.Intersects;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * Create Multiple windroses.
 * 
 * @author jyajya, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class MultiWindRoseOperation extends GeneralOperation {
        protected static final Logger LOGGER = Logging.getLogger(MultiWindRoseOperation.class);

        private NamePolicy namePolicy = NamePolicy.NORMAL;
        private String[] directions = new String[] { "E", "ENE", "NE", "NNE", "N", "NNW", "NW", "WNW", "W", "WSW", "SW",
                        "SSW", "S", "SSE", "SE", "ESE" };

        public void setNamePolicy(NamePolicy namePolicy) {
                this.namePolicy = namePolicy;
        }

        public NamePolicy getNamePolicy() {
                return namePolicy;
        }

        private GeometryFactory fGf = JTSFactoryFinder.getGeometryFactory(null);

        private SimpleFeatureSource anchor;

        static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

        public SimpleFeatureSource execute(Collection<SimpleFeatureCollection> inputFeatureses, String[] weightFields,
                        SimpleFeatureCollection centerFeatures, Double searchRadius, int roseCnt) throws IOException {
                String valueField = "rose_val";
                int idx = 0;

                SimpleFeatureCollection[] tmpInputFeatures = new SimpleFeatureCollection[inputFeatureses.size()];
                tmpInputFeatures = inputFeatureses.toArray(tmpInputFeatures);
                List<String> tmpFieldList = new ArrayList<String>();
                List<Class<?>> tmpClassList = new ArrayList<Class<?>>();
                for (idx = 0; idx < tmpInputFeatures.length; idx++) {
                        if (tmpInputFeatures[idx] == null) {
                                tmpFieldList.add(null);
                                tmpFieldList.add(null);
                                tmpFieldList.add(null);
                                tmpFieldList.add(null);
                                tmpFieldList.add(null);
                                tmpFieldList.add(null);
                                tmpFieldList.add(null);
                                tmpClassList.add(null);
                                tmpClassList.add(null);
                                tmpClassList.add(null);
                                tmpClassList.add(null);
                                tmpClassList.add(null);
                                tmpClassList.add(null);
                                tmpClassList.add(null);
                                continue;
                        }
                        tmpFieldList.add(valueField + idx);
                        tmpFieldList.add("rose_cnt" + idx);
                        tmpFieldList.add("rose_min" + idx);
                        tmpFieldList.add("rose_max" + idx);
                        tmpFieldList.add("rose_mean" + idx);
                        tmpFieldList.add("rose_mdn" + idx);
                        tmpFieldList.add("rose_stdev" + idx);
                        tmpClassList.add(Double.class);
                        tmpClassList.add(Integer.class);
                        tmpClassList.add(Double.class);
                        tmpClassList.add(Double.class);
                        tmpClassList.add(Double.class);
                        tmpClassList.add(Double.class);
                        tmpClassList.add(Double.class);
                }
                tmpFieldList.add("from_d");
                tmpFieldList.add("to_d");
                tmpFieldList.add(valueField + "_x");
                tmpFieldList.add(valueField + "_y");
                tmpFieldList.add(valueField + "_radius");
                tmpClassList.add(Double.class);
                tmpClassList.add(Double.class);
                tmpClassList.add(Double.class);
                tmpClassList.add(Double.class);
                tmpClassList.add(Double.class);

                String[] tmpFields = new String[tmpFieldList.size()];
                tmpFields = tmpFieldList.toArray(tmpFields);

                Class<?>[] tmpClasses = new Class<?>[tmpClassList.size()];
                tmpClasses = tmpClassList.toArray(tmpClasses);

                String[] roseFields = new String[] { valueField, "rose_cnt", "rose_min", "rose_max", "rose_mean", "rose_mdn",
                                "rose_stdev", "rose_tg" };
                Class<?>[] roseClasses = new Class[] { Double.class, Integer.class, Double.class, Double.class, Double.class,
                                Double.class, Double.class, String.class };
                String[] anchorFields = new String[] { "distance", "direction", "degree" };
                Class<?>[] anchorClasses = new Class[] { Double.class, String.class, Double.class };

                final SimpleFeatureType centerSchema = centerFeatures.getSchema();
                // SimpleFeatureType featureType = buildShcema(centerSchema,
                // centerSchema.getCoordinateReferenceSystem(), getOutputTypeName(),
                // new String[] { valueField }, new Class[] { Double.class });

                SimpleFeatureType targetType = copyShcema(centerSchema, centerSchema.getCoordinateReferenceSystem(),
                                "counting_rose", Polygon.class, tmpFields, tmpClasses);
                SimpleFeatureType roseType = copyShcema(centerSchema, centerSchema.getCoordinateReferenceSystem(), "multi_rose",
                                Polygon.class, roseFields, roseClasses);
                SimpleFeatureType anchorType = copyShcema(centerSchema, centerSchema.getCoordinateReferenceSystem(),
                                "wind_rose_anchor", LineString.class, anchorFields, anchorClasses);
                IFeatureInserter countingWriter = getFeatureWriter(targetType);
                IFeatureInserter roseWriter = getFeatureWriter(roseType);
                IFeatureInserter anchorWriter = getFeatureWriter(anchorType);

                SimpleFeatureIterator csfi = centerFeatures.features();
                try {
                        double maxVal = 0;
                        double minVal = 0;
                        double radius = 0;
                        while (csfi.hasNext()) {
                                SimpleFeature csf = csfi.next();
                                Geometry center = (Geometry) csf.getDefaultGeometry();
                                ReferencedEnvelope env = null;
                                Coordinate centerCoords = null;
                                Envelope jtsEnv = null;
                                if (center instanceof Polygon || center instanceof MultiPolygon) {
                                        jtsEnv = center.getEnvelopeInternal();
                                        centerCoords = center.getCentroid().getCoordinate();
                                } else {
                                        centerCoords = center.getCentroid().getCoordinate();
                                        center = center.buffer(searchRadius);
                                        jtsEnv = center.getEnvelopeInternal();
                                }

                                env = new ReferencedEnvelope(jtsEnv.getMinX(), jtsEnv.getMaxX(), jtsEnv.getMinY(), jtsEnv.getMaxY(),
                                                centerFeatures.getSchema().getCoordinateReferenceSystem());

                                double maxx = env.getMaximum(0);
                                double minx = env.getMinimum(0);
                                double maxy = env.getMaximum(1);
                                double miny = env.getMinimum(1);

                                // Envelope env = inputFeatures.getBounds();
                                radius = Math.max(radius, Math.min((maxx - minx), (maxy - miny)) / 2);
                                double countingRadius = searchRadius;

                                double stepAngle = 360.0 / roseCnt;
                                double halfStep = stepAngle / 2.0;

                                for (int colIdx = 0; colIdx < roseCnt; colIdx++) {
                                        // final SimpleFeature feature = featureIter.next();
                                        double fromDeg = halfStep + (colIdx * stepAngle);
                                        double toDeg = halfStep + ((colIdx + 1) * stepAngle);
                                        double defaultRadius = countingRadius;

                                        idx = 0;
                                        Double fval = 0.;
                                        SimpleFeature newFeature = countingWriter.buildFeature();
                                        Polygon cell = createRingCell(fGf, centerCoords, fromDeg, toDeg, defaultRadius);
                                        for (idx = 0; idx < tmpInputFeatures.length; idx++) {
                                                if (tmpInputFeatures[idx] == null) {
                                                        continue;
                                                }
                                                PropertyName pn = ff
                                                                .property(tmpInputFeatures[idx].getSchema().getGeometryDescriptor().getName());
                                                Filter f = ff.intersects(pn, ff.literal(cell));

                                                PropertyName pn1 = ff
                                                                .property(tmpInputFeatures[idx].getSchema().getGeometryDescriptor().getName());
                                                Intersects intersects = ff.intersects(pn1, ff.literal(center));

                                                And and = ff.and(f, intersects);
                                                SimpleFeatureCollection subCollection = tmpInputFeatures[idx].subCollection(and);
                                                SimpleFeatureIterator featureIter = null;

                                                double curVal = 0;
                                                int cnt = 0;
                                                Double min = null;
                                                Double max = null;
                                                List<Double> vlist = new ArrayList<Double>();
                                                try {
                                                        featureIter = subCollection.features();
                                                        while (featureIter.hasNext()) {
                                                                SimpleFeature sf = featureIter.next();
                                                                if (weightFields[idx] != null && !weightFields[idx].trim().equalsIgnoreCase("")) {
                                                                        double v = Double.parseDouble("" + sf.getAttribute(weightFields[idx]));
                                                                        vlist.add(v);
                                                                        min = (min == null) ? v : Math.min(min, v);
                                                                        max = (max == null) ? v : Math.max(max, v);
                                                                        curVal = curVal + v;
                                                                } else {
                                                                        min = 1.;
                                                                        max = 1.;
                                                                        vlist.add(1.0);
                                                                        curVal++;
                                                                }
                                                                cnt++;
                                                        }
                                                        int fieldIdx = idx * 7;
                                                        newFeature.setAttribute(tmpFields[fieldIdx], curVal);
                                                        newFeature.setAttribute(tmpFields[fieldIdx + 1], cnt);
                                                        newFeature.setAttribute(tmpFields[fieldIdx + 2], min);
                                                        newFeature.setAttribute(tmpFields[fieldIdx + 3], max);
                                                        newFeature.setAttribute(tmpFields[fieldIdx + 4], getMean(vlist));
                                                        newFeature.setAttribute(tmpFields[fieldIdx + 5], getMedian(vlist));
                                                        newFeature.setAttribute(tmpFields[fieldIdx + 6], getStdev(vlist, getMean(vlist)));
                                                        // System.out.println(idx +"/"+curVal);
                                                        fval = fval + curVal;

                                                } catch (Exception e) {
                                                        e.printStackTrace();
                                                } finally {
                                                        featureIter.close();
                                                }
                                        }

                                        maxVal = Math.max(maxVal, fval);
                                        minVal = Math.max(minVal, fval);
                                        copyAttribute(csf, newFeature);
                                        newFeature.setDefaultGeometry(cell);
                                        // newFeature.setAttribute(valueField, fval);
                                        newFeature.setAttribute(valueField + "_x", centerCoords.x);
                                        newFeature.setAttribute(valueField + "_y", centerCoords.y);
                                        newFeature.setAttribute("from_d", fromDeg);
                                        newFeature.setAttribute("to_d", toDeg);
                                        newFeature.setAttribute(valueField + "_radius", radius);
                                        countingWriter.write(newFeature);

                                }
                        }
                        csfi.close();
                        countingWriter.close();

                        SimpleFeatureIterator countFeatureIt = countingWriter.getFeatureSource().getFeatures().features();
                        while (countFeatureIt.hasNext()) {
                                SimpleFeature sf = countFeatureIt.next();
                                double sumVal = 0;
                                Double[] curVals = new Double[tmpInputFeatures.length];
                                idx = 0;
                                for (int idx2 = 0; idx2 < tmpInputFeatures.length; idx2++) {
                                        int fieldIdx = idx2 * 7;
                                        if (tmpInputFeatures[idx2] == null) {
                                                curVals[idx2] = null;
                                                continue;
                                        }
                                        double o = (Double) sf.getAttribute(tmpFields[fieldIdx]);
                                        curVals[idx2] = o;
                                        sumVal = sumVal + o;
                                }
                                double x = (Double) sf.getAttribute(valueField + "_x");
                                double y = (Double) sf.getAttribute(valueField + "_y");
                                CoordinateSequence cs = new CoordinateArraySequence(new Coordinate[] { new Coordinate(x, y) });
                                Point center = new Point(cs, fGf);

                                // double radius = (Double) sf.getAttribute(valueField +"_radius");
                                double roseRadius = sumVal / maxVal * radius;

                                double fromDeg = (Double) sf.getAttribute("from_d");
                                double toDeg = (Double) sf.getAttribute("to_d");

                                double defaultRadius = 0;
                                idx = 0;
                                for (int i = 0; i < tmpInputFeatures.length; i++) {
                                        if (idx >= curVals.length || curVals[idx] == null) {
                                                idx++;
                                                continue;
                                        }
                                        double fromRadius = defaultRadius;
                                        double toRadius = 0;
                                        SimpleFeature newFeature = roseWriter.buildFeature();
                                        copyAttribute(sf, newFeature);
                                        if (sumVal != 0) {
                                                toRadius = curVals[idx] / sumVal * roseRadius;
                                                // System.out.println(curVals[idx] + "/" + sumVal + "/" + roseRadius + "/"
                                                // + fromRadius + "/" + toRadius);
                                                defaultRadius = fromRadius + toRadius;
                                                Polygon cell = createRingCell(fGf, center.getCoordinate(), fromDeg, toDeg, fromRadius,
                                                                fromRadius + toRadius);

                                                newFeature.setDefaultGeometry(cell);
                                                newFeature.setAttribute(valueField, curVals[idx]);
                                        } else {
                                                toRadius = 0;
                                                newFeature.setAttribute(valueField, curVals[idx]);
                                        }

                                        newFeature.setAttribute("rose_cnt", sf.getAttribute("rose_cnt" + idx));
                                        newFeature.setAttribute("rose_min", sf.getAttribute("rose_min" + idx));
                                        newFeature.setAttribute("rose_max", sf.getAttribute("rose_max" + idx));
                                        newFeature.setAttribute("rose_mean", sf.getAttribute("rose_mean" + idx));
                                        newFeature.setAttribute("rose_mdn", sf.getAttribute("rose_mdn" + idx));
                                        newFeature.setAttribute("rose_stdev", sf.getAttribute("rose_stdev" + idx));
                                        newFeature.setAttribute("rose_tg", tmpInputFeatures[idx].getSchema().getName().getLocalPart());
                                        roseWriter.write(newFeature);
                                        idx++;
                                }

                                for (double start = radius / 5.; start <= radius; start = start + (radius) / 5.) {
                                        LineString line = createRingDistance(fGf, center.getCoordinate(), start);
                                        SimpleFeature newFeature = anchorWriter.buildFeature();
                                        newFeature.setAttribute("distance", start);
                                        newFeature.setDefaultGeometry(line);
                                        copyAttribute(sf, newFeature);
                                        anchorWriter.write(newFeature);
                                }
                                LineString maxLine = createRingDistance(fGf, center.getCoordinate(), radius);
                                SimpleFeature maxFeature = anchorWriter.buildFeature();
                                maxFeature.setDefaultGeometry(maxLine);
                                maxFeature.setAttribute("distance", radius);
                                copyAttribute(sf, maxFeature);
                                anchorWriter.write(maxFeature);
                                int dirIdx = 0;
                                for (double start = 0; start < 360; start = start + 22.5) {
                                        LineString line = createRingDirection(fGf, center.getCoordinate(), radius, start);
                                        SimpleFeature newFeature = anchorWriter.buildFeature();
                                        newFeature.setDefaultGeometry(line);
                                        newFeature.setAttribute("direction", directions[dirIdx]);
                                        newFeature.setAttribute("degree", start);
                                        copyAttribute(sf, newFeature);
                                        anchorWriter.write(newFeature);
                                        dirIdx++;
                                }
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                        roseWriter.rollback();
                        anchorWriter.rollback();
                } finally {
                        csfi.close();
                        roseWriter.close();
                        anchorWriter.close();
                }
                anchor = anchorWriter.getFeatureSource();
                return roseWriter.getFeatureSource();
        }

        public SimpleFeatureSource execute(Collection<SimpleFeatureCollection> inputFeatureses, String[] weightFields,
                        Geometry centerPoint, Double searchRadius, int roseCnt) throws IOException {
                String valueField = "rose_val";
                int idx = 0;

                SimpleFeatureCollection[] tmpInputFeatures = new SimpleFeatureCollection[inputFeatureses.size()];
                tmpInputFeatures = inputFeatureses.toArray(tmpInputFeatures);
                List<String> tmpFieldList = new ArrayList<String>();
                List<Class<?>> tmpClassList = new ArrayList<Class<?>>();
                for (idx = 0; idx < tmpInputFeatures.length; idx++) {
                        if (tmpInputFeatures[idx] == null) {
                                tmpFieldList.add(null);
                                tmpFieldList.add(null);
                                tmpFieldList.add(null);
                                tmpFieldList.add(null);
                                tmpFieldList.add(null);
                                tmpFieldList.add(null);
                                tmpFieldList.add(null);
                                tmpClassList.add(null);
                                tmpClassList.add(null);
                                tmpClassList.add(null);
                                tmpClassList.add(null);
                                tmpClassList.add(null);
                                tmpClassList.add(null);
                                tmpClassList.add(null);
                                continue;
                        }
                        tmpFieldList.add(valueField + idx);
                        tmpFieldList.add("rose_cnt" + idx);
                        tmpFieldList.add("rose_min" + idx);
                        tmpFieldList.add("rose_max" + idx);
                        tmpFieldList.add("rose_mean" + idx);
                        tmpFieldList.add("rose_mdn" + idx);
                        tmpFieldList.add("rose_stdev" + idx);
                        tmpClassList.add(Double.class);
                        tmpClassList.add(Integer.class);
                        tmpClassList.add(Double.class);
                        tmpClassList.add(Double.class);
                        tmpClassList.add(Double.class);
                        tmpClassList.add(Double.class);
                        tmpClassList.add(Double.class);
                }
                tmpFieldList.add("from_d");
                tmpFieldList.add("to_d");
                tmpFieldList.add(valueField + "_x");
                tmpFieldList.add(valueField + "_y");
                tmpFieldList.add(valueField + "_radius");
                tmpClassList.add(Double.class);
                tmpClassList.add(Double.class);
                tmpClassList.add(Double.class);
                tmpClassList.add(Double.class);
                tmpClassList.add(Double.class);

                String[] tmpFields = new String[tmpFieldList.size()];
                tmpFields = tmpFieldList.toArray(tmpFields);

                Class<?>[] tmpClasses = new Class<?>[tmpClassList.size()];
                tmpClasses = tmpClassList.toArray(tmpClasses);

                String[] roseFields = new String[] { valueField, "rose_cnt", "rose_min", "rose_max", "rose_mean", "rose_mdn",
                                "rose_stdev", "rose_tg" };
                Class<?>[] roseClasses = new Class[] { Double.class, Integer.class, Double.class, Double.class, Double.class,
                                Double.class, Double.class, String.class };
                String[] anchorFields = new String[] { "distance", "direction", "degree" };
                Class<?>[] anchorClasses = new Class[] { Double.class, String.class, Double.class };

//        final SimpleFeatureType centerSchema = centerFeatures.getSchema();
                // SimpleFeatureType featureType = buildShcema(centerSchema,
                // centerSchema.getCoordinateReferenceSystem(), getOutputTypeName(),
                // new String[] { valueField }, new Class[] { Double.class });

                final SimpleFeatureType centerSchema = FeatureTypes.getDefaultType("windrose", Polygon.class,
                                tmpInputFeatures[0].getSchema().getCoordinateReferenceSystem());
                SimpleFeatureType targetType = copyShcema(centerSchema, centerSchema.getCoordinateReferenceSystem(),
                                "counting_rose", Polygon.class, tmpFields, tmpClasses);
                SimpleFeatureType roseType = copyShcema(centerSchema, centerSchema.getCoordinateReferenceSystem(), "multi_rose",
                                Polygon.class, roseFields, roseClasses);
                SimpleFeatureType anchorType = copyShcema(centerSchema, centerSchema.getCoordinateReferenceSystem(),
                                "wind_rose_anchor", LineString.class, anchorFields, anchorClasses);
                IFeatureInserter countingWriter = getFeatureWriter(targetType);
                IFeatureInserter roseWriter = getFeatureWriter(roseType);
                IFeatureInserter anchorWriter = getFeatureWriter(anchorType);

                try {
                        double maxVal = 0;
                        double minVal = 0;
                        double radius = 0;
                        {
                                Geometry center = centerPoint;
                                ReferencedEnvelope env = null;
                                Coordinate centerCoords = null;
                                Envelope jtsEnv = null;
                                if (center instanceof Polygon || center instanceof MultiPolygon) {
                                        jtsEnv = center.getEnvelopeInternal();
                                        centerCoords = center.getCentroid().getCoordinate();
                                } else {
                                        centerCoords = center.getCentroid().getCoordinate();
                                        center = center.buffer(searchRadius);
                                        jtsEnv = center.getEnvelopeInternal();
                                }

                                env = new ReferencedEnvelope(jtsEnv.getMinX(), jtsEnv.getMaxX(), jtsEnv.getMinY(), jtsEnv.getMaxY(),
                                                tmpInputFeatures[0].getSchema().getCoordinateReferenceSystem());

                                double maxx = env.getMaximum(0);
                                double minx = env.getMinimum(0);
                                double maxy = env.getMaximum(1);
                                double miny = env.getMinimum(1);

                                // Envelope env = inputFeatures.getBounds();
                                radius = Math.max(radius, Math.min((maxx - minx), (maxy - miny)) / 2);
                                double countingRadius = searchRadius;

                                double stepAngle = 360.0 / roseCnt;
                                double halfStep = stepAngle / 2.0;

                                for (int colIdx = 0; colIdx < roseCnt; colIdx++) {
                                        // final SimpleFeature feature = featureIter.next();
                                        double fromDeg = halfStep + (colIdx * stepAngle);
                                        double toDeg = halfStep + ((colIdx + 1) * stepAngle);
                                        double defaultRadius = countingRadius;

                                        idx = 0;
                                        Double fval = 0.;
                                        SimpleFeature newFeature = countingWriter.buildFeature();
                                        Polygon cell = createRingCell(fGf, centerCoords, fromDeg, toDeg, defaultRadius);
                                        for (idx = 0; idx < tmpInputFeatures.length; idx++) {
                                                if (tmpInputFeatures[idx] == null) {
                                                        continue;
                                                }
                                                PropertyName pn = ff
                                                                .property(tmpInputFeatures[idx].getSchema().getGeometryDescriptor().getName());
                                                Filter f = ff.intersects(pn, ff.literal(cell));

                                                PropertyName pn1 = ff
                                                                .property(tmpInputFeatures[idx].getSchema().getGeometryDescriptor().getName());
                                                Intersects intersects = ff.intersects(pn1, ff.literal(center));

                                                And and = ff.and(f, intersects);
                                                SimpleFeatureCollection subCollection = tmpInputFeatures[idx].subCollection(and);
                                                SimpleFeatureIterator featureIter = null;

                                                double curVal = 0;
                                                int cnt = 0;
                                                Double min = null;
                                                Double max = null;
                                                List<Double> vlist = new ArrayList<Double>();
                                                try {
                                                        featureIter = subCollection.features();
                                                        while (featureIter.hasNext()) {
                                                                SimpleFeature sf = featureIter.next();
                                                                if (weightFields[idx] != null && !weightFields[idx].trim().equalsIgnoreCase("")) {
                                                                        double v = Double.parseDouble("" + sf.getAttribute(weightFields[idx]));
                                                                        vlist.add(v);
                                                                        min = (min == null) ? v : Math.min(min, v);
                                                                        max = (max == null) ? v : Math.max(max, v);
                                                                        curVal = curVal + v;
                                                                } else {
                                                                        min = 1.;
                                                                        max = 1.;
                                                                        vlist.add(1.0);
                                                                        curVal++;
                                                                }
                                                                cnt++;
                                                        }
                                                        int fieldIdx = idx * 7;
                                                        newFeature.setAttribute(tmpFields[fieldIdx], curVal);
                                                        newFeature.setAttribute(tmpFields[fieldIdx + 1], cnt);
                                                        newFeature.setAttribute(tmpFields[fieldIdx + 2], min);
                                                        newFeature.setAttribute(tmpFields[fieldIdx + 3], max);
                                                        newFeature.setAttribute(tmpFields[fieldIdx + 4], getMean(vlist));
                                                        newFeature.setAttribute(tmpFields[fieldIdx + 5], getMedian(vlist));
                                                        newFeature.setAttribute(tmpFields[fieldIdx + 6], getStdev(vlist, getMean(vlist)));
                                                        // System.out.println(idx +"/"+curVal);
                                                        fval = fval + curVal;

                                                } catch (Exception e) {
                                                        e.printStackTrace();
                                                } finally {
                                                        featureIter.close();
                                                }
                                        }

                                        maxVal = Math.max(maxVal, fval);
                                        minVal = Math.max(minVal, fval);
//                    copyAttribute(csf, newFeature);
                                        newFeature.setDefaultGeometry(cell);
                                        // newFeature.setAttribute(valueField, fval);
                                        newFeature.setAttribute(valueField + "_x", centerCoords.x);
                                        newFeature.setAttribute(valueField + "_y", centerCoords.y);
                                        newFeature.setAttribute("from_d", fromDeg);
                                        newFeature.setAttribute("to_d", toDeg);
                                        newFeature.setAttribute(valueField + "_radius", radius);
                                        countingWriter.write(newFeature);

                                }
                        }
//            csfi.close();
                        countingWriter.close();

                        //////////////////////////////////////////////////////////////////////////////////////
                        //////////////////////////////////////////////////////////////////////////////////////

                        SimpleFeatureIterator countFeatureIt = countingWriter.getFeatureSource().getFeatures().features();
                        while (countFeatureIt.hasNext()) {
                                SimpleFeature sf = countFeatureIt.next();
                                double sumVal = 0;
                                Double[] curVals = new Double[tmpInputFeatures.length];
                                idx = 0;
                                for (int idx2 = 0; idx2 < tmpInputFeatures.length; idx2++) {
                                        int fieldIdx = idx2 * 7;
                                        if (tmpInputFeatures[idx2] == null) {
                                                curVals[idx2] = null;
                                                continue;
                                        }
                                        double o = (Double) sf.getAttribute(tmpFields[fieldIdx]);
                                        curVals[idx2] = o;
                                        sumVal = sumVal + o;
                                }
                                double x = (Double) sf.getAttribute(valueField + "_x");
                                double y = (Double) sf.getAttribute(valueField + "_y");
                                CoordinateSequence cs = new CoordinateArraySequence(new Coordinate[] { new Coordinate(x, y) });
                                Point center = new Point(cs, fGf);

                                // double radius = (Double) sf.getAttribute(valueField +"_radius");
                                double roseRadius = sumVal / maxVal * radius;

                                double fromDeg = (Double) sf.getAttribute("from_d");
                                double toDeg = (Double) sf.getAttribute("to_d");

                                double defaultRadius = 0;
                                idx = 0;
                                for (int i = 0; i < tmpInputFeatures.length; i++) {
                                        if (idx >= curVals.length || curVals[idx] == null) {
                                                idx++;
                                                continue;
                                        }
                                        double fromRadius = defaultRadius;
                                        double toRadius = 0;
                                        SimpleFeature newFeature = roseWriter.buildFeature();
                                        copyAttribute(sf, newFeature);
                                        if (sumVal != 0) {
                                                toRadius = curVals[idx] / sumVal * roseRadius;
                                                // System.out.println(curVals[idx] + "/" + sumVal + "/" + roseRadius + "/"
                                                // + fromRadius + "/" + toRadius);
                                                defaultRadius = fromRadius + toRadius;
                                                Polygon cell = createRingCell(fGf, center.getCoordinate(), fromDeg, toDeg, fromRadius,
                                                                fromRadius + toRadius);

                                                newFeature.setDefaultGeometry(cell);
                                                newFeature.setAttribute(valueField, curVals[idx]);
                                        } else {
                                                toRadius = 0;
                                                newFeature.setAttribute(valueField, curVals[idx]);
                                        }

                                        newFeature.setAttribute("rose_cnt", sf.getAttribute("rose_cnt" + idx));
                                        newFeature.setAttribute("rose_min", sf.getAttribute("rose_min" + idx));
                                        newFeature.setAttribute("rose_max", sf.getAttribute("rose_max" + idx));
                                        newFeature.setAttribute("rose_mean", sf.getAttribute("rose_mean" + idx));
                                        newFeature.setAttribute("rose_mdn", sf.getAttribute("rose_mdn" + idx));
                                        newFeature.setAttribute("rose_stdev", sf.getAttribute("rose_stdev" + idx));
                                        newFeature.setAttribute("rose_tg", tmpInputFeatures[idx].getSchema().getName().getLocalPart());
                                        roseWriter.write(newFeature);
                                        idx++;
                                }

                                for (double start = radius / 5.; start <= radius; start = start + (radius) / 5.) {
                                        LineString line = createRingDistance(fGf, center.getCoordinate(), start);
                                        SimpleFeature newFeature = anchorWriter.buildFeature();
                                        newFeature.setAttribute("distance", start);
                                        newFeature.setDefaultGeometry(line);
                                        copyAttribute(sf, newFeature);
                                        anchorWriter.write(newFeature);
                                }
                                LineString maxLine = createRingDistance(fGf, center.getCoordinate(), radius);
                                SimpleFeature maxFeature = anchorWriter.buildFeature();
                                maxFeature.setDefaultGeometry(maxLine);
                                maxFeature.setAttribute("distance", radius);
                                copyAttribute(sf, maxFeature);
                                anchorWriter.write(maxFeature);
                                int dirIdx = 0;
                                for (double start = 0; start < 360; start = start + 22.5) {
                                        LineString line = createRingDirection(fGf, center.getCoordinate(), radius, start);
                                        SimpleFeature newFeature = anchorWriter.buildFeature();
                                        newFeature.setDefaultGeometry(line);
                                        newFeature.setAttribute("direction", directions[dirIdx]);
                                        newFeature.setAttribute("degree", start);
                                        copyAttribute(sf, newFeature);
                                        anchorWriter.write(newFeature);
                                        dirIdx++;
                                }
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                        roseWriter.rollback();
                        anchorWriter.rollback();
                } finally {
//                      csfi.close();
                        roseWriter.close();
                        anchorWriter.close();
                }
                anchor = anchorWriter.getFeatureSource();
                return roseWriter.getFeatureSource();
        }

        public LineString createRingDistance(GeometryFactory gf, Coordinate centroid, double toRadius) {
                double step = 5.;
                double radian = 0.0;
                List<Coordinate> outer_ring = new ArrayList<Coordinate>();
                Coordinate first = null;
                for (int index = 0; index < 72; index = index + 1) {
                        radian = toRadians(0 + ((double) index * step));
                        outer_ring.add(createCoordinate(centroid, radian, toRadius));
                        if (first == null) {
                                first = createCoordinate(centroid, radian, toRadius);
                        }
                }
                outer_ring.add(first);
                Coordinate[] coords = outer_ring.toArray(new Coordinate[outer_ring.size()]);
                LineString ring = gf.createLineString(coords);
                return ring;
        }

        public LineString createRingDirection(GeometryFactory gf, Coordinate centroid, double toRadius, double step) {
                double radian = 0.0;
                List<Coordinate> outer_ring = new ArrayList<Coordinate>();
                outer_ring.add(centroid);
                radian = toRadians(step);
                outer_ring.add(createCoordinate(centroid, radian, toRadius));
                Coordinate[] coords = outer_ring.toArray(new Coordinate[outer_ring.size()]);
                LineString ring = gf.createLineString(coords);
                return ring;
        }

        private void copyAttribute(SimpleFeature csf, SimpleFeature newFeature) {
                List<AttributeDescriptor> attDescs = csf.getFeatureType().getAttributeDescriptors();
                for (AttributeDescriptor descriptor : attDescs) {
                        if (descriptor instanceof GeometryDescriptor) {

                        } else {
                                String localname = descriptor.getLocalName().toString();
                                if (localname.equalsIgnoreCase("shape_leng") || localname.equalsIgnoreCase("shape_area")) {
                                        continue;
                                } else {
                                        try {
                                                newFeature.setAttribute(localname, csf.getAttribute(localname));
                                        } catch (Exception e) {
                                        }
                                }
                        }
                }

        }

        public Polygon createRingCell(GeometryFactory gf, Coordinate centroid, double from_deg, double to_deg,
                        double to_radius) {
                double step = Math.abs(to_deg - from_deg) / 36.;
                double radian = 0.0;
                List<Coordinate> outer_ring = new ArrayList<Coordinate>();
                outer_ring.add(centroid);
                for (int index = 0; index < 36; index = index + 1) {
                        radian = toRadians(from_deg + ((double) index * step));
                        outer_ring.add(createCoordinate(centroid, radian, to_radius));
                }

                outer_ring.add(centroid);
                Coordinate[] coords = outer_ring.toArray(new Coordinate[outer_ring.size()]);
                Polygon ring = gf.createPolygon(gf.createLinearRing(coords), null);
                return ring;
        }

        public Polygon createRingCell(GeometryFactory gf, Coordinate centroid, double from_deg, double to_deg,
                        double from_radius, double to_radius) {
                double step = Math.abs(to_deg - from_deg) / 36.;
                double radian = 0.0;
                List<Coordinate> outer_ring = new ArrayList<Coordinate>();
                Coordinate first = null;
                if (from_radius == 0) {  
                        outer_ring.add(centroid);
                } else {
                        for (int index = 0; index < 36 + 1; index = index + 1) {
                                radian = toRadians(from_deg + ((double) index * step));
                                outer_ring.add(createCoordinate(centroid, radian, from_radius));
                                if (first == null) {
                                        first = createCoordinate(centroid, radian, from_radius);
                                }
                        }
                }

                for (int index = 36; index > -1; index = index - 1) {
                        radian = toRadians(from_deg + (index * step));
                        outer_ring.add(createCoordinate(centroid, radian, to_radius));
                }

                if (from_radius != 0) {
                        outer_ring.add(first);
                } else {
                        outer_ring.add(centroid);
                }
                Coordinate[] coords = outer_ring.toArray(new Coordinate[outer_ring.size()]);
                Polygon ring = gf.createPolygon(gf.createLinearRing(coords), null);
                return ring;
        }

        private Coordinate createCoordinate(Coordinate source, double radian, double radius) {
                double dx = Math.cos(radian) * radius;
                double dy = Math.sin(radian) * radius;
                return new Coordinate(source.x + dx, source.y + dy);
        }

        private double toDegree(double radians) {
                return radians * (180.0 / Math.PI);
        }

        private double toRadians(double degree) {
                return Math.PI / 180.0 * degree;
        }

        private SimpleFeatureType buildShcema(SimpleFeatureType featureType, CoordinateReferenceSystem crs, String typeName,
                        String[] attNames, Class[] bindings) {
                SimpleFeatureTypeBuilder sftBuilder = new SimpleFeatureTypeBuilder();
                sftBuilder.setName(getName(typeName));
                sftBuilder.setCRS(crs);

                AttributeTypeBuilder attBuilder = new AttributeTypeBuilder();
                List<AttributeDescriptor> attDescs = featureType.getAttributeDescriptors();
                for (AttributeDescriptor descriptor : attDescs) {
                        if (descriptor instanceof GeometryDescriptor) {
                                final GeometryDescriptor geomDesc = (GeometryDescriptor) descriptor;
                                final Class<?> geomBinding = Polygon.class;
                                sftBuilder.add(getName(geomDesc.getLocalName()), geomBinding, crs);
                        }
                }

                if (attNames != null) {
                        for (int idx = 0; idx < attNames.length; idx++) {
                                String attName = attNames[idx];
                                if (attName == null) {
                                        continue;
                                }
                                Class binding = bindings[idx];
                                final String localName = getName(attName);
                                AttributeType attType = new AttributeTypeImpl(new NameImpl(localName), binding, false, false, null,
                                                null, null);
                                AttributeDescriptor attDesc = new AttributeDescriptorImpl(attType, new NameImpl(localName), 0, 1, true,
                                                null);
                                sftBuilder.add(attBuilder.buildDescriptor(localName, attDesc.getType()));
                        }
                }
                return sftBuilder.buildFeatureType();
        }

        private SimpleFeatureType copyShcema(SimpleFeatureType featureType, CoordinateReferenceSystem crs, String typeName,
                        Class<?> geomBinding, String[] attNames, Class[] bindings) {
                SimpleFeatureTypeBuilder sftBuilder = new SimpleFeatureTypeBuilder();
                sftBuilder.setName(getName(typeName));
                sftBuilder.setCRS(crs);

                AttributeTypeBuilder attBuilder = new AttributeTypeBuilder();
                List<AttributeDescriptor> attDescs = featureType.getAttributeDescriptors();
                for (AttributeDescriptor descriptor : attDescs) {
                        if (descriptor instanceof GeometryDescriptor) {
                                final GeometryDescriptor geomDesc = (GeometryDescriptor) descriptor;
                                sftBuilder.add(getName(geomDesc.getLocalName()), geomBinding, crs);
                        } else {
                                final String localName = getName(descriptor.getLocalName());
                                if (localName.equalsIgnoreCase("shape_leng") || localName.equalsIgnoreCase("shape_area")) {
                                        continue;
                                } else {
                                        sftBuilder.add(attBuilder.buildDescriptor(localName, descriptor.getType()));
                                }
                        }
                }

                if (attNames != null) {
                        for (int idx = 0; idx < attNames.length; idx++) {
                                String attName = attNames[idx];
                                if (attName == null) {
                                        continue;
                                }
                                Class binding = bindings[idx];
                                final String localName = getName(attName);
                                AttributeType attType = new AttributeTypeImpl(new NameImpl(localName), binding, false, false, null,
                                                null, null);
                                AttributeDescriptor attDesc = new AttributeDescriptorImpl(attType, new NameImpl(localName), 0, 1, true,
                                                null);
                                sftBuilder.add(attBuilder.buildDescriptor(localName, attDesc.getType()));
                        }
                }
                return sftBuilder.buildFeatureType();
        }

        private SimpleFeatureType buildLineShcema(SimpleFeatureType featureType, CoordinateReferenceSystem crs,
                        String typeName) {
                SimpleFeatureTypeBuilder sftBuilder = new SimpleFeatureTypeBuilder();
                sftBuilder.setName(getName(typeName));
                sftBuilder.setCRS(crs);

                AttributeTypeBuilder attBuilder = new AttributeTypeBuilder();
                List<AttributeDescriptor> attDescs = featureType.getAttributeDescriptors();
                for (AttributeDescriptor descriptor : attDescs) {
                        if (descriptor instanceof GeometryDescriptor) {
                                final GeometryDescriptor geomDesc = (GeometryDescriptor) descriptor;
                                sftBuilder.add(getName(geomDesc.getLocalName()), LineString.class, crs);
                        } else {
                                final String localName = getName(descriptor.getLocalName());
                                if (localName.equalsIgnoreCase("shape_leng") || localName.equalsIgnoreCase("shape_area")) {
                                        continue;
                                } else {
                                        sftBuilder.add(attBuilder.buildDescriptor(localName, descriptor.getType()));
                                }
                        }
                }

                return sftBuilder.buildFeatureType();
        }

        private String getName(String srcName) {
                switch (namePolicy) {
                case NORMAL:
                        return srcName;
                case UPPERCASE:
                        return srcName.toUpperCase();
                case LOWERCASE:
                        return srcName.toLowerCase();
                }
                return srcName;
        }

        private SimpleFeatureType buildShcema(SimpleFeatureType featureType, CoordinateReferenceSystem crs,
                        Class geomBinding, String typeName, String[] attNames, Class[] bindings) {
                SimpleFeatureTypeBuilder sftBuilder = new SimpleFeatureTypeBuilder();
                sftBuilder.setName(getName(typeName));
                sftBuilder.setCRS(crs);

                AttributeTypeBuilder attBuilder = new AttributeTypeBuilder();
                List<AttributeDescriptor> attDescs = featureType.getAttributeDescriptors();
                for (AttributeDescriptor descriptor : attDescs) {
                        if (descriptor instanceof GeometryDescriptor) {
                                final GeometryDescriptor geomDesc = (GeometryDescriptor) descriptor;
                                // final Class<?> geomBinding = Polygon.class;
                                sftBuilder.add(getName(geomDesc.getLocalName()), geomBinding, crs);
                        }
                }

                if (attNames != null) {
                        for (int idx = 0; idx < attNames.length; idx++) {
                                String attName = attNames[idx];
                                if (attName == null) {
                                        continue;
                                }
                                Class binding = bindings[idx];
                                final String localName = getName(attName);
                                AttributeType attType = new AttributeTypeImpl(new NameImpl(localName), binding, false, false, null,
                                                null, null);
                                AttributeDescriptor attDesc = new AttributeDescriptorImpl(attType, new NameImpl(localName), 0, 1, true,
                                                null);
                                sftBuilder.add(attBuilder.buildDescriptor(localName, attDesc.getType()));
                        }
                }
                return sftBuilder.buildFeatureType();
        }

        private MathTransform getMathTransform(CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS) {
                if (sourceCRS == null || targetCRS == null) {
                        LOGGER.log(Level.WARNING, "Input CoordinateReferenceSystem is Unknown Coordinate System!");
                        return null;
                }

                if (CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
                        LOGGER.log(Level.WARNING, "Input and Output Coordinate Reference Systems are equal!");
                        return null;
                }

                MathTransform transform = null;
                try {
                        transform = CRS.findMathTransform(sourceCRS, targetCRS, false);
                } catch (FactoryException e1) {
                        LOGGER.log(Level.WARNING, e1.getMessage(), 1);
                }

                return transform;
        }

        public static void main(String[] args) {
        }

        public SimpleFeatureSource getAnchor() {
                return anchor;
        }

        public void setAnchor(SimpleFeatureSource anchor) {
                this.anchor = anchor;
        }

        public Double getMean(List<Double> list) {
                double sum = 0;
                if (list == null || list.size() == 0) {
                        return null;
                }
                for (Double d : list) {
                        sum += d;
                }
                return sum / (double) list.size();
        }

        public Double getMedian(List<Double> list) {
                int rest = list.size() % 2;
                int mid = list.size() / 2;
                if (list.size() == 1) {
                        return list.get(0);
                }
                if (list.size() == 0) {
                        return null;
                }
                if (rest == 1) {
                        return list.get(mid);
                } else {
                        return (list.get(mid - 1) + list.get(mid)) / 2.;
                }
        }

        public Double getStdev(List<Double> list, double mean) {
                double sum = 0;
                if (list.size() == 0) {
                        return null;
                }
                for (Double d : list) {
                        sum += Math.pow(d - mean, 2);
                }
                return Math.sqrt(sum / (double) list.size());
        }
}