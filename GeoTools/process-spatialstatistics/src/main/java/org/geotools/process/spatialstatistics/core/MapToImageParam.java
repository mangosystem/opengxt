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
package org.geotools.process.spatialstatistics.core;

import java.awt.Color;
import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Map To Image Parameters
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class MapToImageParam {
    protected static final Logger LOGGER = Logging.getLogger(MapToImageParam.class);

    SimpleFeatureCollection inputFeatures;

    GridCoverage2D inputCoverage;

    Filter filter = Filter.INCLUDE;

    Style style;

    CoordinateReferenceSystem srs;

    ReferencedEnvelope mapExtent;

    Integer width;

    Integer height;

    String format = "image/png";

    Boolean transparent = Boolean.FALSE;

    Color backgroundColor = Color.WHITE;

    public MapToImageParam() {

    }

    public SimpleFeatureCollection getInputFeatures() {
        return inputFeatures;
    }

    public void setInputFeatures(SimpleFeatureCollection inputFeatures) {
        this.inputFeatures = inputFeatures;
    }

    public GridCoverage2D getInputCoverage() {
        return inputCoverage;
    }

    public void setInputCoverage(GridCoverage2D inputCoverage) {
        this.inputCoverage = inputCoverage;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public Style getStyle() {
        return style;
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    public CoordinateReferenceSystem getSrs() {
        return srs;
    }

    public void setSrs(CoordinateReferenceSystem srs) {
        this.srs = srs;
    }

    public ReferencedEnvelope getMapExtent() {
        return mapExtent;
    }

    public void setMapExtent(ReferencedEnvelope mapExtent) {
        this.mapExtent = mapExtent;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Boolean getTransparent() {
        return transparent;
    }

    public void setTransparent(Boolean transparent) {
        this.transparent = transparent;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}
