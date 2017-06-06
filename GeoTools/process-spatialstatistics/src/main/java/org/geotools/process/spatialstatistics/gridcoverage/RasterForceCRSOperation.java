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
package org.geotools.process.spatialstatistics.gridcoverage;

import java.util.Map;
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Force CRS for the raster dataset to another CRS.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterForceCRSOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterForceCRSOperation.class);

    public GridCoverage2D execute(GridCoverage2D sourceCoverage, CoordinateReferenceSystem forcedCRS)
            throws ProcessException {
        ReferencedEnvelope sourceEnv = new ReferencedEnvelope(sourceCoverage.getEnvelope());
        ReferencedEnvelope newEnv = new ReferencedEnvelope(sourceEnv, forcedCRS);

        PlanarImage image = (PlanarImage) sourceCoverage.getRenderedImage();
        GridSampleDimension[] bands = sourceCoverage.getSampleDimensions();
        GridCoverage[] sources = new GridCoverage[] { sourceCoverage };
        Map<?, ?> properties = sourceCoverage.getProperties();

        GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);

        return factory.create(sourceCoverage.getName(), image, newEnv, bands, sources, properties);
    }
}