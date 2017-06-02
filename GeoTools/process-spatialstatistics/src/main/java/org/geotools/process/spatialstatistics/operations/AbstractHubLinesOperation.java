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

import java.util.logging.Logger;

import org.geotools.process.spatialstatistics.util.BezierCurve;
import org.geotools.util.logging.Logging;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.distance.DistanceOp;

/**
 * Abstract hub lines operation.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public abstract class AbstractHubLinesOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(AbstractHubLinesOperation.class);

    protected static final String HUB_DIST = "hubdist";

    protected static final String TYPE_NAME = "HubLines";

    protected double maximumDistance = Double.MAX_VALUE;

    protected boolean preserveAttributes = true;

    protected boolean useCentroid = true;

    protected boolean useBezierCurve = false;

    public boolean isUseBezierCurve() {
        return useBezierCurve;
    }

    public void setUseBezierCurve(boolean useBezierCurve) {
        this.useBezierCurve = useBezierCurve;
    }

    public boolean isUseCentroid() {
        return useCentroid;
    }

    public void setUseCentroid(boolean useCentroid) {
        this.useCentroid = useCentroid;
    }

    public boolean isPreserveAttributes() {
        return preserveAttributes;
    }

    public void setPreserveAttributes(boolean preserverAttributes) {
        this.preserveAttributes = preserverAttributes;
    }

    public void setMaximumDistance(double maximumDistance) {
        if (Double.isNaN(maximumDistance) || Double.isInfinite(maximumDistance)
                || maximumDistance == 0) {
            this.maximumDistance = Double.MAX_VALUE;
        } else {
            this.maximumDistance = maximumDistance;
        }
    }

    public double getMaximumDistance() {
        return maximumDistance;
    }

    protected LineString getShortestLine(Geometry from, Geometry to, boolean centroid) {
        Geometry start = from;
        Geometry end = to;

        if (centroid) {
            if (from instanceof Polygon || from instanceof MultiPolygon) {
                start = from.getCentroid();
            }

            if (to instanceof Polygon || to instanceof MultiPolygon) {
                end = to.getCentroid();
            }
        }

        Coordinate[] coordinates = DistanceOp.nearestPoints(start, end);
        LineString shortestLine = from.getFactory().createLineString(coordinates);
        
        if (useBezierCurve) {
            shortestLine = new BezierCurve().create(shortestLine);
        }
        shortestLine.setUserData(from.getUserData());

        return shortestLine;
    }

}
