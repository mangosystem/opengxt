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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.GridElement;
import org.geotools.grid.GridFeatureBuilder;
import org.geotools.grid.hexagon.HexagonOrientation;
import org.geotools.grid.hexagon.Hexagons;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.impl.AbstractProcess;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Creates hexagon grids from extent or bounds source features
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class HexagonProcess extends AbstractProcess {
    protected static final Logger LOGGER = Logging.getLogger(HexagonProcess.class);

    private boolean started = false;

    public HexagonProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(ReferencedEnvelope extent,
            SimpleFeatureCollection boundsSource, Double sideLen, HexagonOrientation orientation,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(HexagonProcessFactory.extent.key, extent);
        map.put(HexagonProcessFactory.boundsSource.key, boundsSource);
        map.put(HexagonProcessFactory.sideLen.key, sideLen);
        map.put(HexagonProcessFactory.orientation.key, orientation);

        Process process = new HexagonProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(HexagonProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        if (started)
            throw new IllegalStateException("Process can only be run once");
        started = true;

        if (monitor == null)
            monitor = new NullProgressListener();
        try {
            monitor.started();
            monitor.setTask(Text.text("Grabbing arguments"));
            monitor.progress(10.0f);

            ReferencedEnvelope gridBounds = (ReferencedEnvelope) Params.getValue(input,
                    HexagonProcessFactory.extent, null);
            SimpleFeatureCollection boundsSource = (SimpleFeatureCollection) Params.getValue(input,
                    HexagonProcessFactory.boundsSource, null);
            if (gridBounds == null && boundsSource == null) {
                throw new NullPointerException("extent or boundsSource parameters required");
            }

            Double sideLen = (Double) Params.getValue(input, HexagonProcessFactory.sideLen, null);
            HexagonOrientation orientation = (HexagonOrientation) Params.getValue(input,
                    HexagonProcessFactory.orientation, HexagonProcessFactory.orientation.sample);
            if (sideLen == null || sideLen == 0) {
                throw new NullPointerException("sideLen parameter should be grater than 0");
            }

            monitor.setTask(Text.text("Processing ..."));
            monitor.progress(25.0f);

            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // start process
            HexagonOperation operation = new HexagonOperation();
            operation.setBoundsSource(boundsSource);
            operation.setOrientation(orientation);

            if (gridBounds == null) {
                gridBounds = boundsSource.getBounds();
            }
            SimpleFeatureCollection resultFc = operation.execute(gridBounds, sideLen);
            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(HexagonProcessFactory.RESULT.key, resultFc);
            monitor.complete(); // same as 100.0f

            return resultMap;
        } catch (Exception eek) {
            monitor.exceptionOccurred(eek);
            return null;
        } finally {
            monitor.dispose();
        }
    }

    public class HexagonOperation {
        static final String UID = "UID";

        final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

        private HexagonOrientation orientation = HexagonOrientation.FLAT;

        private SimpleFeatureCollection boundsSource = null;

        private Geometry geometryBoundary = null;

        public void setOrientation(HexagonOrientation orientation) {
            this.orientation = orientation;
        }

        public void setGeometryBoundary(Geometry geometryBoundary) {
            this.geometryBoundary = geometryBoundary;
        }

        public void setBoundsSource(SimpleFeatureCollection boundsSource) {
            this.boundsSource = boundsSource;
        }

        public SimpleFeatureCollection execute(ReferencedEnvelope gridBounds, double sideLen)
                throws IOException {
            CoordinateReferenceSystem crs = gridBounds.getCoordinateReferenceSystem();
            SimpleFeatureType featureType = FeatureTypes.getDefaultType("hexagon", Polygon.class,
                    crs);
            featureType = FeatureTypes.add(featureType, UID, Long.class, 38);

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

            SimpleFeatureSource gridFeatures = Hexagons.createGrid(gridBounds, sideLen,
                    orientation, gridBuilder);
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
            Geometry boundary = null;

            int fID = 0;

            public IntersectionGeometryBuilder(SimpleFeatureType type, Geometry boundary) {
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
        };

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
                Filter filter = ff.intersects(ff.property(the_geom),
                        ff.literal(gridElement.toGeometry()));
                return !featureSource.subCollection(filter).isEmpty();
            }
        };

    }

}
