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
 * Raster Resample Type
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public enum ResampleType {
    /**
     * Performs a nearest neighbor assignment and is the fastest of the interpolation methods. It is used primarily for discrete data, such as a
     * land-use classification, since it will not change the values of the cells.
     */
    NEAREST,

    /**
     * Performs a bilinear interpolation and determines the new value of a cell based on a weighted distance average of the four nearest input cell
     * centers. It is useful for continuous data and will cause some smoothing of the data.
     */
    BILINEAR,

    /**
     * Performs a cubic convolution and determines the new value of a cell based on fitting a smooth curve through the 16 nearest input cell centers.
     * It is appropriate for continuous data, although it may result in the output raster containing values outside the range of the input raster.
     */
    BICUBIC
}
