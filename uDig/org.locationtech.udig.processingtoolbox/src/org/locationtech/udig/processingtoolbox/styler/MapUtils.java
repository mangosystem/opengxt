/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
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
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.storage.RasterExportOperation;
import org.geotools.process.spatialstatistics.transformation.ForceCRSFeatureCollection;
import org.geotools.referencing.CRS;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.ICatalog;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.ToolboxView;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.style.sld.SLDContent;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * uDig Map Utilities class
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class MapUtils {
    protected static final Logger LOGGER = Logging.getLogger(MapUtils.class);

    public enum VectorLayerType {
        /**
         * all feature type.
         */
        ALL,

        /**
         * point, multipoint.
         */
        POINT,

        /**
         * linestring, multilinestring.
         */
        LINESTRING,

        /**
         * polygon, multipolygon.
         */
        POLYGON,

        /**
         * linestring, multilinestring, polygon, multipolygon.
         */
        POLYLINE,

        /**
         * multipoint, multilinestring, multipolygon.
         */
        MULTIPART
    }

    public enum FieldType {
        ALL, String, Number, Integer, Double
    }

    public static SimpleFeatureCollection getFeatures(ILayer layer) {
        try {
            SimpleFeatureSource sfs = (SimpleFeatureSource) layer.getResource(FeatureSource.class,
                    new NullProgressMonitor());
            Filter filter = Filter.INCLUDE;
            if (ToolboxView.getSelectedOnly() && layer.getFilter() != Filter.EXCLUDE) {
                filter = layer.getFilter();
            }

            // check layer & FeatureCollection's crs
            SimpleFeatureCollection features = sfs.getFeatures(filter);
            CoordinateReferenceSystem crs = features.getSchema().getCoordinateReferenceSystem();
            if (!CRS.equalsIgnoreMetadata(layer.getCRS(), crs)) {
                features = new ForceCRSFeatureCollection(features, layer.getCRS());
            }
            return features;
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
        return null;
    }

    public static SimpleFeatureSource getFeatureSource(IMap map, String layerName) {
        for (ILayer layer : map.getMapLayers()) {
            if (layer.getName() != null && layer.getName().equals(layerName)
                    && layer.hasResource(FeatureSource.class)) {
                try {
                    return (SimpleFeatureSource) layer.getResource(FeatureSource.class,
                            new NullProgressMonitor());
                } catch (IOException e) {
                    LOGGER.log(Level.FINER, e.getMessage(), e);
                }
            }
        }
        return null;
    }

    public static SimpleFeatureCollection getFeatures(IMap map, String layerName) {
        for (ILayer layer : map.getMapLayers()) {
            if (layer.getName() != null && layer.getName().equals(layerName)
                    && layer.hasResource(FeatureSource.class)) {
                return getFeatures(layer);
            }
        }
        return null;
    }

    public static GridCoverage2D getGridCoverage(ILayer layer) {
        try {
            if (layer.hasResource(GridCoverage2D.class)) {
                return layer.getResource(GridCoverage2D.class, new NullProgressMonitor());
            } else if (layer.getGeoResource().canResolve(GridCoverageReader.class)) {
                GridCoverageReader reader = layer.getResource(GridCoverageReader.class, null);
                return (GridCoverage2D) reader.read(null);
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
        return null;
    }

    public static GridCoverage2D getGridCoverage(IMap map, String layerName) {
        for (ILayer layer : map.getMapLayers()) {
            if (layer.getName() != null && layer.getName().equals(layerName)) {
                try {
                    if (layer.hasResource(GridCoverage2D.class)) {
                        return layer.getResource(GridCoverage2D.class, new NullProgressMonitor());
                    } else if (layer.getGeoResource().canResolve(GridCoverageReader.class)) {
                        GridCoverageReader reader = layer.getResource(GridCoverageReader.class,
                                new NullProgressMonitor());
                        return (GridCoverage2D) reader.read(null);
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.FINER, e.getMessage(), e);
                }
            }
        }
        return null;
    }

    public static boolean isFeatureLayer(ILayer layer) {
        return layer.hasResource(FeatureSource.class);
    }

    public static boolean isRatserLayer(ILayer layer) {
        return layer.hasResource(GridCoverage2D.class)
                || layer.getGeoResource().canResolve(GridCoverageReader.class);
    }

    public static ILayer getLayer(IMap map, String layerName) {
        for (ILayer layer : map.getMapLayers()) {
            if (layer.getName() != null && layer.getName().equals(layerName)) {
                return layer;
            }
        }
        return null;
    }

    public static ILayer addGeometryToMap(IMap map, Geometry source, String layerName) {
        CoordinateReferenceSystem crs = map.getViewportModel().getCRS();
        if (source.getUserData() != null
                && CoordinateReferenceSystem.class
                        .isAssignableFrom(source.getUserData().getClass())) {
            crs = (CoordinateReferenceSystem) source.getUserData();
        }
        return addGeometryToMap(map, source, crs, layerName);
    }

    public static ILayer addGeometryToMap(IMap map, Geometry source, CoordinateReferenceSystem crs,
            String layerName) {
        SimpleFeatureType schema = FeatureTypes.getDefaultType(layerName, source.getClass(), crs);
        schema = FeatureTypes.add(schema, "name", String.class, 20); //$NON-NLS-1$
        schema = FeatureTypes.add(schema, "weight", Integer.class, 19); //$NON-NLS-1$

        ListFeatureCollection features = new ListFeatureCollection(schema);
        SimpleFeatureBuilder typeBuilder = new SimpleFeatureBuilder(schema);

        SimpleFeature feature = typeBuilder.buildFeature(null);
        feature.setAttribute("name", source.getGeometryType()); //$NON-NLS-1$
        feature.setAttribute("weight", 1); //$NON-NLS-1$

        feature.setDefaultGeometry(source);
        features.add(feature);

        return addFeaturesToMap(map, features, layerName);
    }

    public static ILayer addFeatureToMap(IMap map, SimpleFeature feature, String layerName) {
        ListFeatureCollection features = new ListFeatureCollection(feature.getFeatureType());
        features.add(feature);

        return addFeaturesToMap(map, features, layerName);
    }

    public static ILayer addFeaturesToMap(IMap map, SimpleFeatureCollection source, String layerName) {
        return addFeaturesToMap(map, source, layerName, null);
    }

    public static ILayer addFeaturesToMap(IMap map, SimpleFeatureCollection source,
            String layerName, Style style) {
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
            if (style != null) {
                // put the style on the blackboard
                layer.getStyleBlackboard().clear();
                layer.getStyleBlackboard().put(SLDContent.ID, style);
                layer.getStyleBlackboard().flush();
            }
            layer.setVisible(true);

            // refresh
            layer.refresh(layer.getBounds(new NullProgressMonitor(), null));
            return layer;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ILayer addFeaturesToMap(IMap map, File shapefile) {
        return addFeaturesToMap(map, shapefile, FilenameUtils.getBaseName(shapefile.getPath()));
    }

    public static ILayer addFeaturesToMap(IMap map, File shapefile, String layerName) {
        return addFeaturesToMap(map, shapefile, layerName, null);
    }

    public static ILayer addFeaturesToMap(IMap map, File shapefile, String layerName, Style style) {
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
                    if (style != null) {
                        // put the style on the blackboard
                        layer.getStyleBlackboard().clear();
                        layer.getStyleBlackboard().put(SLDContent.ID, style);
                        layer.getStyleBlackboard().flush();
                    }
                    layer.setVisible(true);

                    // refresh
                    layer.refresh(layer.getBounds(new NullProgressMonitor(), null));
                    return layer;
                }
            }
        } catch (MalformedURLException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
        return null;
    }

    public static GridCoverage2D saveAsGeoTiff(GridCoverage2D source, File filePath)
            throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
        RasterExportOperation saveAs = new RasterExportOperation();
        return saveAs.saveAsGeoTiff(source, filePath.getAbsolutePath());
    }

    public static ILayer addGridCoverageToMap(IMap map, GridCoverage2D source, File filePath,
            Style style) {
        try {
            if (filePath == null || !filePath.exists()) {
                String tempDir = ToolboxView.getWorkspace();
                filePath = File.createTempFile("udig_", ".tif", new File(tempDir)); //$NON-NLS-1$//$NON-NLS-2$
                RasterExportOperation saveAs = new RasterExportOperation();
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
                    if (style != null) {
                        // put the style on the blackboard
                        layer.getStyleBlackboard().put(SLDContent.ID, style);
                        layer.getStyleBlackboard().setSelected(new String[] { SLDContent.ID });
                    }
                    layer.setVisible(true);

                    // refresh
                    layer.refresh(layer.getBounds(new NullProgressMonitor(), null));
                    return layer;
                }
            }
        } catch (IOException e) {
            ToolboxPlugin.log(e.getMessage());
        }
        return null;
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

    @SuppressWarnings("nls")
    public static boolean confirmSpatialFile(File outputFile) {
        // single file or multiple files
        final String fileName = outputFile.getName().toLowerCase();
        if (fileName.endsWith(".shp")) {
            String[] extensions = new String[] { "shx", "dbf", "prj", "sbn", "sbx", "qix", "fix",
                    "lyr", "cpg", "qpj", "qml" };
            String baseName = FilenameUtils.getBaseName(outputFile.getPath());
            for (String ext : extensions) {
                File file = new File(outputFile.getParent(), baseName + "." + ext);
                if (file.exists()) {
                    file.delete();
                }
            }
            outputFile.delete();
        } else if (fileName.endsWith(".tif") || fileName.endsWith(".tiff")) {
            String[] extensions = new String[] { ".tfw", ".aux", ".aux.xml", ".ovr", ".rrd",
                    ".xml", ".vat.dbf" };
            String baseName = FilenameUtils.getBaseName(outputFile.getPath());
            for (String ext : extensions) {
                File file = new File(outputFile.getParent(), baseName + "." + ext);
                if (file.exists()) {
                    file.delete();
                }
            }

            baseName = FilenameUtils.getName(outputFile.getPath());
            for (String ext : extensions) {
                File file = new File(outputFile.getParent(), baseName + ext);
                if (file.exists()) {
                    file.delete();
                }
            }
            outputFile.delete();
        } else {
            // .gml, .kml ...
            if (!outputFile.delete()) {
                return false;
            }
        }
        return true;
    }
}
