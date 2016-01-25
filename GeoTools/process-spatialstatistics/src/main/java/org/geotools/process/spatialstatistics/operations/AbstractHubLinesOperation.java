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

    protected double maximumDistance = Double.MAX_VALUE;

    protected boolean preserveAttributes = true;

    protected boolean useCentroid = true;

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

    protected int getFeatureID(String id) {
        if (id.contains(".")) {
            int pos = id.lastIndexOf(".");
            return Integer.valueOf(id.substring(pos + 1));
        }
        return Integer.valueOf(id);
    }

    protected LineString getShortestLine(Geometry from, Geometry to, boolean centroid) {
        Geometry from_geom = from;
        Geometry to_geom = to;

        if (centroid) {
            if (from instanceof Polygon || from instanceof MultiPolygon) {
                from_geom = from.getCentroid();
            }

            if (to instanceof Polygon || to instanceof MultiPolygon) {
                to_geom = to.getCentroid();
            }
        }

        Coordinate[] closetCoords = DistanceOp.nearestPoints(from_geom, to_geom);
        LineString shortestLine = from.getFactory().createLineString(closetCoords);
        shortestLine.setUserData(from.getUserData());
        return shortestLine;
    }

}
