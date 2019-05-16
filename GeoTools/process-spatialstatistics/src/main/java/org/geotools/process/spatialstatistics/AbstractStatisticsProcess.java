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
package org.geotools.process.spatialstatistics;

import org.geotools.geometry.jts.JTS;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.impl.AbstractProcess;
import org.geotools.process.spatialstatistics.util.BBOXExpandingFilterVisitor;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.Filter;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * AbstractStatisticsProcess.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class AbstractStatisticsProcess extends AbstractProcess {

    public AbstractStatisticsProcess(ProcessFactory factory) {
        super(factory);
    }

    protected Filter expandBBox(Filter filter, double distance) {
        return (Filter) filter.accept(new BBOXExpandingFilterVisitor(distance, distance, distance,
                distance), null);
    }

    protected Geometry transformGeometry(Geometry input, CoordinateReferenceSystem targetCRS) {
        Geometry output = input;
        Object userData = input == null ? null : input.getUserData();

        if (userData != null && userData instanceof CoordinateReferenceSystem) {
            CoordinateReferenceSystem sourceCRS = (CoordinateReferenceSystem) userData;
            if (!CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
                try {
                    MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, true);
                    output = JTS.transform(input, transform);
                    output.setUserData(targetCRS);
                } catch (FactoryException e) {
                    throw new ProcessException(e);
                } catch (MismatchedDimensionException e) {
                    throw new ProcessException(e);
                } catch (TransformException e) {
                    throw new ProcessException(e);
                }
            }
        }

        output.setUserData(targetCRS);

        return output;
    }
}
