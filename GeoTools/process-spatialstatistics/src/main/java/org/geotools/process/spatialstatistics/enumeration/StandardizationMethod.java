package org.geotools.process.spatialstatistics.enumeration;

public enum StandardizationMethod {
    /**
     * No standardization of spatial weights is applied.
     */
    NONE,

    /**
     * Spatial weights are standardized; each weight is divided by its row sum (the sum of the
     * weights of all neighboring features).
     */
    ROW,

    /**
     * Nearby neighboring features have a larger influence on the computations for a target
     * feature than features that are far away.
     */
    GLOBAL

}
