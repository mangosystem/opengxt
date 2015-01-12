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

import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * CentralFeature Visitor
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CentralFeatureVisitor extends AbstractDistributionVisitor {

    private DistanceMethod distanceMethod = DistanceMethod.Euclidean;

    public DistanceMethod getDistanceMethod() {
        return distanceMethod;
    }

    public void setDistanceMethod(DistanceMethod distanceMethod) {
        this.distanceMethod = distanceMethod;
    }

    @SuppressWarnings("unchecked")
    public void visit(Coordinate coordinate, Object caseVal, double weightVal, double pottentialVal) {
        caseVal = caseVal == null ? CASE_ALL : caseVal;
        CentralFeature centeralFeature = (CentralFeature) resuleMap.get(caseVal);

        if (centeralFeature == null) {
            centeralFeature = new CentralFeature();
            centeralFeature.setDistanceMethod(distanceMethod);
            resuleMap.put(caseVal, centeralFeature);
        }

        centeralFeature.addValue(coordinate, weightVal, pottentialVal);
    }
}
