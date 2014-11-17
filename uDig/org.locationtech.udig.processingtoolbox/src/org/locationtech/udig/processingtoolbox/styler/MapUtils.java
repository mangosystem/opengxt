/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.styler;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Combo;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.referencing.CRS;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.ICatalog;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.ToolboxView;
import org.locationtech.udig.processingtoolbox.common.FeatureTypes;
import org.locationtech.udig.processingtoolbox.common.ForceCRSFeatureCollection;
import org.locationtech.udig.processingtoolbox.common.RasterSaveAsOp;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.style.sld.SLDContent;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * uDig Map Utilities class
 * 
 * @author MapPlus
 * 
 */
public class MapUtils {
    protected static final Logger LOGGER = Logging.getLogger(MapUtils.class);

    public enum VectorLayerType {
        ALL, POINT, LINESTRING, POLYGON
    }

    public enum FieldType {
        ALL, String, Number, Integer, Double
    }

    public static SimpleFeatureCollection getFeatures(IMap map, String layerName) {
        for (ILayer layer : map.getMapLayers()) {
            if (layer.getName().equals(layerName) && layer.hasResource(FeatureSource.class)) {
                try {
                    SimpleFeatureSource sfs = (SimpleFeatureSource) layer.getResource(
                            FeatureSource.class, new NullProgressMonitor());
                    // apply selected features
                    Filter filter = Filter.INCLUDE;
                    if (layer.getFilter() != Filter.EXCLUDE) {
                        filter = layer.getFilter();
                    }

                    // check layer & FeatureCollection's crs
                    SimpleFeatureCollection sfc = sfs.getFeatures(filter);
                    CoordinateReferenceSystem bCrs = sfc.getSchema().getCoordinateReferenceSystem();
                    if (!CRS.equalsIgnoreMetadata(layer.getCRS(), bCrs)) {
                        sfc = new ForceCRSFeatureCollection(sfc, layer.getCRS());
                    }
                    return sfc;
                } catch (IOException e) {
                    LOGGER.log(Level.FINER, e.getMessage(), e);
                }
            }
        }
        return null;
    }

    public static GridCoverage2D getGridCoverage(IMap map, String layerName) {
        for (ILayer layer : map.getMapLayers()) {
            if (layer.getName().equals(layerName)) {
                try {
                    if (layer.hasResource(GridCoverage2D.class)) {
                        return layer.getResource(GridCoverage2D.class, new NullProgressMonitor());
                    } else if (layer.getGeoResource().canResolve(GridCoverageReader.class)) {
                        GridCoverageReader reader = layer.getResource(GridCoverageReader.class,
                                null);
                        return (GridCoverage2D) reader.read(null);
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.FINER, e.getMessage(), e);
                }
            }
        }
        return null;
    }

    public static ILayer getLayer(IMap map, String layerName) {
        for (ILayer layer : map.getMapLayers()) {
            if (layer.getName().equals(layerName)) {
                return layer;
            }
        }
        return null;
    }

    public static void fillLayers(IMap map, Combo combo, VectorLayerType layerType) {
        combo.removeAll();
        for (ILayer layer : map.getMapLayers()) {
            if (layer.hasResource(FeatureSource.class)) {
                GeometryDescriptor descriptor = layer.getSchema().getGeometryDescriptor();
                Class<?> geometryBinding = descriptor.getType().getBinding();
                switch (layerType) {
                case ALL:
                    combo.add(layer.getName());
                    break;
                case LINESTRING:
                    if (geometryBinding.isAssignableFrom(LineString.class)
                            || geometryBinding.isAssignableFrom(MultiLineString.class)) {
                        combo.add(layer.getName());
                    }
                    break;
                case POINT:
                    if (geometryBinding.isAssignableFrom(Point.class)
                            || geometryBinding.isAssignableFrom(MultiPoint.class)) {
                        combo.add(layer.getName());
                    }
                    break;
                case POLYGON:
                    if (geometryBinding.isAssignableFrom(Polygon.class)
                            || geometryBinding.isAssignableFrom(MultiPolygon.class)) {
                        combo.add(layer.getName());
                    }
                    break;
                }
            }
        }
    }

    public static void fillLayers(IMap map, Combo combo, Class<?> resourceType) {
        combo.removeAll();
        for (ILayer layer : map.getMapLayers()) {
            if (layer.hasResource(resourceType)) {
                combo.add(layer.getName());
            }
        }
    }

    public static void fillLayers(IMap map, Combo combo) {
        combo.removeAll();
        for (ILayer layer : map.getMapLayers()) {
            combo.add(layer.getName());
        }
    }

    public static void fillFields(Combo combo, SimpleFeatureType schema, FieldType fieldType) {
        combo.removeAll();
        for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
            if (descriptor instanceof GeometryDescriptor) {
                continue;
            }

            Class<?> binding = descriptor.getType().getBinding();
            switch (fieldType) {
            case ALL:
                combo.add(descriptor.getLocalName());
                break;
            case Double:
                if (Double.class.isAssignableFrom(binding) || Float.class.isAssignableFrom(binding)) {
                    combo.add(descriptor.getLocalName());
                }
                break;
            case Integer:
                if (Short.class.isAssignableFrom(binding)
                        || Integer.class.isAssignableFrom(binding)
                        || Long.class.isAssignableFrom(binding)) {
                    combo.add(descriptor.getLocalName());
                }
                break;
            case Number:
                if (Number.class.isAssignableFrom(binding)) {
                    combo.add(descriptor.getLocalName());
                }
                break;
            case String:
                if (String.class.isAssignableFrom(binding)) {
                    combo.add(descriptor.getLocalName());
                }
                break;
            }
        }
    }

    public static void addGeometryToMap(IMap map, Geometry source, String layerName) {
        CoordinateReferenceSystem crs = map.getViewportModel().getCRS();
        if (source.getUserData() != null
                && CoordinateReferenceSystem.class
                        .isAssignableFrom(source.getUserData().getClass())) {
            crs = (CoordinateReferenceSystem) source.getUserData();
        }
        addGeometryToMap(map, source, crs, layerName);
    }

    public static void addGeometryToMap(IMap map, Geometry source, CoordinateReferenceSystem crs,
            String layerName) {
        SimpleFeatureType schema = FeatureTypes.getDefaultType(layerName, source.getClass(), crs);

        ListFeatureCollection features = new ListFeatureCollection(schema);
        SimpleFeatureBuilder typeBuilder = new SimpleFeatureBuilder(schema);

        SimpleFeature feature = typeBuilder.buildFeature(null);
        feature.setDefaultGeometry(source);
        features.add(feature);

        addFeaturesToMap(map, features, layerName);
    }

    public static void addFeatureToMap(IMap map, SimpleFeature feature, String layerName) {
        ListFeatureCollection features = new ListFeatureCollection(feature.getFeatureType());
        features.add(feature);

        addFeaturesToMap(map, features, layerName);
    }

    public static void addFeaturesToMap(IMap map, SimpleFeatureCollection source, String layerName) {
        try {
            ICatalog catalog = CatalogPlugin.getDefault().getLocalCatalog();
            IGeoResource resource = catalog.createTemporaryResource(source.getSchema());

            SimpleFeatureStore store = (SimpleFeatureStore) resource.resolve(FeatureStore.class,
                    new NullProgressMonitor());
            store.addFeatures(source);

            // create layer
            final int pos = map.getMapLayers().size();
            List<IGeoResource> resourceList = Collections.singletonList(resource);
            Layer layer = (Layer) ApplicationGIS.addLayersToMap(map, resourceList, pos).get(0);
            layer.setName(layerName);
            layer.setVisible(true);

            // refresh
            layer.refresh(layer.getBounds(new NullProgressMonitor(), null));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addFeaturesToMap(IMap map, File shapefile) {
        String name = FilenameUtils.removeExtension(FilenameUtils.getName(shapefile.getPath()));
        addFeaturesToMap(map, shapefile, name);
    }

    public static void addFeaturesToMap(IMap map, File shapefile, String layerName) {
        try {
            CatalogPlugin catalogPlugin = CatalogPlugin.getDefault();
            ICatalog localCatalog = catalogPlugin.getLocalCatalog();

            URL resourceId = DataUtilities.fileToURL(shapefile);
            List<IService> services = catalogPlugin.getServiceFactory().createService(resourceId);
            for (IService service : services) {
                localCatalog.add(service);
                for (IGeoResource resource : service.resources(new NullProgressMonitor())) {
                    List<IGeoResource> resourceList = Collections.singletonList(resource);
                    final int pos = map.getMapLayers().size();
                    Layer layer = (Layer) ApplicationGIS.addLayersToMap(map, resourceList, pos)
                            .get(0);
                    layer.setName(layerName);
                    layer.setVisible(true);

                    // refresh
                    layer.refresh(layer.getBounds(new NullProgressMonitor(), null));
                }
            }
        } catch (MalformedURLException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
    }

    public static GridCoverage2D saveAsGeoTiff(GridCoverage2D source, File filePath) {
        RasterSaveAsOp saveAs = new RasterSaveAsOp();
        return saveAs.saveAsGeoTiff(source, filePath.getAbsolutePath());
    }

    public static void addGridCoverageToMap(IMap map, GridCoverage2D source, File filePath,
            Style style) {
        try {
            if (filePath == null || !filePath.exists()) {
                String tempDir = ToolboxView.getWorkspace();
                filePath = File.createTempFile("udig_", ".tif", new File(tempDir)); //$NON-NLS-1$//$NON-NLS-2$
                RasterSaveAsOp saveAs = new RasterSaveAsOp();
                source = saveAs.saveAsGeoTiff(source, filePath.getAbsolutePath());
            }

            CatalogPlugin catalogPlugin = CatalogPlugin.getDefault();
            ICatalog localCatalog = catalogPlugin.getLocalCatalog();

            final URL resourceId = DataUtilities.fileToURL(filePath);
            List<IService> services = catalogPlugin.getServiceFactory().createService(resourceId);
            for (IService service : services) {
                localCatalog.add(service);
                for (IGeoResource resource : service.resources(new NullProgressMonitor())) {
                    final int pos = findBestRasterLayerPosition(map);
                    List<IGeoResource> resourceList = Collections.singletonList(resource);
                    Layer layer = (Layer) ApplicationGIS.addLayersToMap(map, resourceList, pos)
                            .get(0);
                    layer.setName(source.getName().toString());
                    layer.setVisible(true);

                    if (style != null) {
                        // put the style on the blackboard
                        layer.getStyleBlackboard().put(SLDContent.ID, style);
                        layer.getStyleBlackboard().setSelected(new String[] { SLDContent.ID });
                    }

                    // refresh
                    layer.refresh(layer.getBounds(new NullProgressMonitor(), null));
                }
            }
        } catch (IOException e) {
            ToolboxPlugin.log(e.getMessage());
        }
    }

    private static int findBestRasterLayerPosition(IMap map) {
        int pos = 0;
        for (int index = 0; index < map.getMapLayers().size(); index++) {
            ILayer layer = map.getMapLayers().get(index);
            if (layer.hasResource(GridCoverage2D.class)
                    || layer.getGeoResource().canResolve(GridCoverageReader.class)) {
                pos = index - 1;
                break;
            }
        }
        return pos;
    }
}
