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
package org.geotools.process.spatialstatistics.enumeration;

/**
 * Kernel Type
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 * @reference https://en.wikipedia.org/wiki/Kernel_(statistics), org.jaitools.media.jai.kernel.KernelFactory.ValueType
 */
public enum KernelType {
    /**
     * Inside elements have value 1.0; outside 0.0
     */
    Binary,

    /**
     * Value is PI/4 * cos(uPI/2) where u is proportional distance to the key element.
     */
    Cosine,

    /**
     * Value is the distance to the kernel's key element.
     */
    Distance,

    /**
     * Value is 3(1 - u^2)/4 where u is proportional distance to the key element.
     */
    Epanechnikov,

    /**
     * Value is 1/sqrt(2PI) e^(-u^2 / 2) where u is proportional distance to the key element.
     */
    Gaussian,

    /**
     * Value is the inverse distance to the kernel's key element.
     */
    InverseDistance,

    /**
     * Also known as Silverman kernel.
     */
    Quadratic,

    /**
     * Also known as biweight.
     */
    Quartic,

    /**
     * Value is 1 - u where u is proportional distance to the key element.
     */
    Triangular,

    /**
     * Triweight
     */
    Triweight,

    /**
     * Tricube
     */
    Tricube
}
