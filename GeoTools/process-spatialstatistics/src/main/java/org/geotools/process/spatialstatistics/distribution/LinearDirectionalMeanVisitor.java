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

import com.vividsolutions.jts.geom.Geometry;

/**
 * LinearDirectionalMean Visitor
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class LinearDirectionalMeanVisitor extends AbstractDistributionVisitor {

    private boolean orientationOnly = false;

    public void setOrientationOnly(boolean orientationOnly) {
        this.orientationOnly = orientationOnly;
    }

    @SuppressWarnings("unchecked")
    public void visit(Geometry lineString, Object caseVal) {
        caseVal = caseVal == null ? CASE_ALL : caseVal;
        LinearDirectionalMean directionalMean = (LinearDirectionalMean) resuleMap.get(caseVal);

        if (directionalMean == null) {
            directionalMean = new LinearDirectionalMean();
            directionalMean.setOrientationOnly(orientationOnly);
            resuleMap.put(caseVal, directionalMean);
        }

        directionalMean.addValue(lineString);
    }
}
