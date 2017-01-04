/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.tools;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.spatialstatistics.core.WeightMatrixBuilder;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.locationtech.udig.project.ILayer;
import org.opengis.filter.Filter;

/**
 * AMOEBA Parameter
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class AmoebaParameter {

    public enum CriteriaType {
        Maximization, Minimization, Custom
    }

    public enum SAAlgorithmType {
        GetisOrderGiStar, LeesSiStar
    }

    public enum SeedOption {
        All, Selected, Custom
    }

    public enum OverlapClusterOption {
        Remove, Avoid
    }

    // 1. input
    public SAAlgorithmType algorithm = SAAlgorithmType.GetisOrderGiStar;

    public SimpleFeatureCollection features = null;

    public String field = null;

    public SpatialConcept spatialConcept = SpatialConcept.ContiguityEdgesNodes;

    public DistanceMethod distanceMethod = DistanceMethod.Euclidean;

    public StandardizationMethod standardization = StandardizationMethod.Row;

    public double searchDistance = Double.valueOf(0d);

    public boolean selfNeighbors = true;

    // 2. output
    public WeightMatrixBuilder matrix = null;

    public SimpleFeatureCollection output = null;
    
    public ILayer outputLayer = null;

    public String statField = null;

    public CriteriaType criteriaType = CriteriaType.Custom;

    public Filter criteriaFilter = null;

    public Filter seedFilter = Filter.INCLUDE;

    // options
    public boolean onlyMaxOne = true;

    public SeedOption seedOption = SeedOption.Custom;

    public OverlapClusterOption overlapCluster = OverlapClusterOption.Avoid;

    public boolean useExclusionFilter = false;

    public Filter exclusionFilter = null;

    public boolean excludeSingleCulster = false;

    public boolean excludeFromCluster = false;

    public boolean sortDescending = true;

    public SimpleFeatureCollection cluster = null;
}
