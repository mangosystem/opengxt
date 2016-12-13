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
package org.geotools.process.spatialstatistics.operations;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.GridElement;
import org.geotools.grid.GridFeatureBuilder;
import org.geotools.grid.hexagon.HexagonOrientation;
import org.geotools.grid.hexagon.Hexagons;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

/**
 * Creates a fishnet of rectangular cells.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class HexagonOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(HexagonOperation.class);

    static final String UID = "uid";

    private HexagonOrientation orientation = HexagonOrientation.FLAT;

    private SimpleFeatureCollection boundsSource = null;

    private PreparedGeometry geometryBoundary = null;

    public void setOrientation(HexagonOrientation orientation) {
        this.orientation = orientation;
    }

    public void setGeometryBoundary(Geometry geometryBoundary) {
        if (geometryBoundary == null) {
            this.geometryBoundary = null;
        } else {
            this.geometryBoundary = PreparedGeometryFactory.prepare(geometryBoundary);
        }
    }

    public void setBoundsSource(SimpleFeatureCollection boundsSource) {
        this.boundsSource = boundsSource;
    }

    public SimpleFeatureCollection execute(ReferencedEnvelope gridBounds, double sideLen)
            throws IOException {
        // expand bbox
        gridBounds.expandBy(sideLen);

        CoordinateReferenceSystem crs = gridBounds.getCoordinateReferenceSystem();
        SimpleFeatureType featureType = FeatureTypes.getDefaultType("hexagon", Polygon.class, crs);
        featureType = FeatureTypes.add(featureType, UID, Integer.class, 19);

        GridFeatureBuilder gridBuilder = null;
        if (boundsSource != null) {
            gridBuilder = new IntersectionFsBuilder(featureType, boundsSource);
        } else if (geometryBoundary != null) {
            gridBuilder = new IntersectionGeometryBuilder(featureType, geometryBoundary);
        } else {
            gridBuilder = new DefaultGridBuilder(featureType);
        }

        if (boundsSource != null || geometryBoundary != null) {
            gridBounds.expandBy(sideLen); // for intersection hexagon
            if (orientation == HexagonOrientation.ANGLED) {
                gridBounds.expandBy(sideLen); // for intersection hexagon
            }
        }

        SimpleFeatureSource gridFeatures = null;
        gridFeatures = Hexagons.createGrid(gridBounds, sideLen, orientation, gridBuilder);
        return gridFeatures.getFeatures();
    }

    final class DefaultGridBuilder extends GridFeatureBuilder {
        private int fID = 0;

        public DefaultGridBuilder(SimpleFeatureType type) {
            super(type);
        }

        @Override
        public void setAttributes(GridElement gridElement, Map<String, Object> attributes) {
            attributes.put(UID, ++fID);
        }
    }

    final class IntersectionGeometryBuilder extends GridFeatureBuilder {
        PreparedGeometry boundary = null;

        int fID = 0;

        public IntersectionGeometryBuilder(SimpleFeatureType type, PreparedGeometry boundary) {
            super(type);
            this.boundary = boundary;
        }

        @Override
        public void setAttributes(GridElement gridElement, Map<String, Object> attributes) {
            attributes.put(UID, ++fID);
        }

        @Override
        public boolean getCreateFeature(GridElement gridElement) {
            return boundary.intersects(gridElement.toGeometry());
        }
    }

    final class IntersectionFsBuilder extends GridFeatureBuilder {
        SimpleFeatureCollection featureSource = null;

        String the_geom = null;

        int fID = 0;

        public IntersectionFsBuilder(SimpleFeatureType type, SimpleFeatureCollection source) {
            super(type);
            this.featureSource = source;
            this.the_geom = featureSource.getSchema().getGeometryDescriptor().getLocalName();
        }

        @Override
        public void setAttributes(GridElement gridElement, Map<String, Object> attributes) {
            attributes.put(UID, ++fID);
        }

        @Override
        public boolean getCreateFeature(GridElement gridElement) {
            Filter filter = getIntersectsFilter(the_geom, gridElement.toGeometry());
            return !featureSource.subCollection(filter).isEmpty();
        }
    }

}