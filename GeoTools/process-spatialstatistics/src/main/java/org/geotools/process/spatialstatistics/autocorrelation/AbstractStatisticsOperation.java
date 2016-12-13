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
package org.geotools.process.spatialstatistics.autocorrelation;

import java.io.File;
import java.util.logging.Logger;

import org.geotools.process.spatialstatistics.core.DistanceFactory;
import org.geotools.process.spatialstatistics.core.WeightMatrixBuilder;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.util.logging.Logging;

/**
 * Abstract Statistics Operation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class AbstractStatisticsOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(AbstractStatisticsOperation.class);

    protected WeightMatrixBuilder swMatrix = null;

    private double distanceBand = 0.0;

    private DistanceMethod distanceType = DistanceMethod.Euclidean;

    private StandardizationMethod standardizationType = StandardizationMethod.None;

    private SpatialConcept spatialConceptType = SpatialConcept.InverseDistance;

    private boolean isContiguity = false;

    private boolean selfNeighbors = false;

    private File spatialWeightsFile = null;

    protected final DistanceFactory factory = DistanceFactory.newInstance();

    public WeightMatrixBuilder getSwMatrix() {
        return swMatrix;
    }

    public void setDistanceType(DistanceMethod distanceType) {
        this.distanceType = distanceType;
    }

    public DistanceMethod getDistanceType() {
        return distanceType;
    }

    public void setDistanceBand(double distanceBand) {
        this.distanceBand = distanceBand;
    }

    public double getDistanceBand() {
        return distanceBand;
    }

    public void setStandardizationType(StandardizationMethod standardizationType) {
        this.standardizationType = standardizationType;
    }

    public StandardizationMethod getStandardizationType() {
        return standardizationType;
    }

    public void setSpatialWeightsFile(File spatialWeightsFile) {
        this.spatialWeightsFile = spatialWeightsFile;
    }

    public File getSpatialWeightsFile() {
        return spatialWeightsFile;
    }

    public void setSpatialConceptType(SpatialConcept spatialConcept) {
        this.spatialConceptType = spatialConcept;
        this.isContiguity = spatialConcept == SpatialConcept.ContiguityEdgesNodes
                || spatialConcept == SpatialConcept.ContiguityEdgesOnly
                || spatialConcept == SpatialConcept.ContiguityNodesOnly;
    }

    public SpatialConcept getSpatialConceptType() {
        return spatialConceptType;
    }

    public boolean isContiguity() {
        return isContiguity;
    }

    public boolean isSelfNeighbors() {
        return selfNeighbors;
    }

    public void setSelfNeighbors(boolean selfNeighbors) {
        this.selfNeighbors = selfNeighbors;
    }
}
