/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.storage;

import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;

@SuppressWarnings("nls")
public class ShapeExportOperation {
    protected static final Logger LOGGER = Logging.getLogger(ShapeExportOperation.class);

    private NamePolicy namePolicy = NamePolicy.NORMAL;

    protected final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    private String outputTypeName = null;

    private DataStore outputDataStore = null;

    // The shared instance
    private static ShapeExportOperation plugin = new ShapeExportOperation();

    public static ShapeExportOperation getDefault() {
        return plugin;
    }

    public void setNamePolicy(NamePolicy namePolicy) {
        this.namePolicy = namePolicy;
    }

    public NamePolicy getNamePolicy() {
        return namePolicy;
    }

    public void setOutputDataStore(DataStore outputDataStore) {
        this.outputDataStore = outputDataStore;
    }

    public DataStore getOutputDataStore() {
        if (outputDataStore == null) {
            // The default data store is a MemoryDataStore2
            outputDataStore = new MemoryDataStore2();
        }
        return outputDataStore;
    }

    public void setOutputTypeName(String outputTypeName) {
        this.outputTypeName = outputTypeName;
    }

    public String getOutputTypeName() {
        if (outputTypeName == null) {
            SimpleDateFormat dataFormat = new SimpleDateFormat("yyyyMMdd_hhmmss_S");
            String serialID = dataFormat.format(Calendar.getInstance().getTime());
            outputTypeName = "gp_" + serialID;
        }

        return outputTypeName;
    }

    private IFeatureInserter getFeatureWriter(SimpleFeatureType schema) throws IOException {
        if (DataStoreFactory.isShapefileDataStore(getOutputDataStore())) {
            URI uri = getOutputDataStore().getInfo().getSource();
            String folder = DataUtilities.urlToFile(uri.toURL()).getPath();
            return new ShapefileFeatureInserter(folder, schema);
        }

        // create schema
        SimpleFeatureStore featureStore = null;
        getOutputDataStore().createSchema(schema);

        final String typeName = schema.getTypeName();
        SimpleFeatureSource featureSource = getOutputDataStore().getFeatureSource(typeName);
        if (featureSource instanceof SimpleFeatureStore) {
            featureStore = (SimpleFeatureStore) featureSource;
            featureStore.setTransaction(new DefaultTransaction(typeName));
        } else {
            LOGGER.log(Level.WARNING, typeName + " does not support SimpleFeatureStore interface!");
            featureStore = (SimpleFeatureStore) featureSource;
        }

        return new FeatureInserter(featureStore);
    }

    public SimpleFeatureSource execute(SimpleFeatureSource inputFeatures, Filter filter)
            throws IOException {
        return execute(inputFeatures, filter, null);
    }

    public SimpleFeatureSource execute(SimpleFeatureSource inputFeatures, Filter filter,
            CoordinateReferenceSystem targetCRS) throws IOException {
        filter = filter == null ? Filter.INCLUDE : filter;
        return execute(inputFeatures.getFeatures(filter), targetCRS);
    }

    public SimpleFeatureSource execute(SimpleFeatureSource inputFeatures, Query query)
            throws IOException {
        return execute(inputFeatures, query, null);
    }

    public SimpleFeatureSource execute(SimpleFeatureSource inputFeatures, Query query,
            CoordinateReferenceSystem targetCRS) throws IOException {
        query = query == null ? Query.ALL : query;
        return execute(inputFeatures.getFeatures(query), targetCRS);
    }

    public SimpleFeatureSource execute(SimpleFeatureCollection inputFeatures) throws IOException {
        return execute(inputFeatures, null);
    }

    public SimpleFeatureSource execute(SimpleFeatureCollection inputFeatures,
            CoordinateReferenceSystem targetCRS) throws IOException {
        // prepare feature type
        final SimpleFeatureType inputSchema = inputFeatures.getSchema();
        final CoordinateReferenceSystem sourceCRS = inputSchema.getCoordinateReferenceSystem();

        MathTransform transform = null;
        if (targetCRS != null) {
            transform = getMathTransform(sourceCRS, targetCRS);
        }

        CoordinateReferenceSystem forcedCRS = transform == null ? sourceCRS : targetCRS;
        SimpleFeatureType featureType = buildShcema(inputSchema, forcedCRS, getOutputTypeName());

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = inputFeatures.features();
            while (featureIter.hasNext()) {
                final SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    LOGGER.warning(feature.getID() + " has null or empty geometry!");
                    continue;
                }

                if (transform != null) {
                    try {
                        geometry = JTS.transform(geometry, transform);
                    } catch (MismatchedDimensionException e) {
                        LOGGER.log(Level.FINER, e.getMessage(), e);
                    } catch (TransformException e) {
                        LOGGER.log(Level.FINER, e.getMessage(), e);
                    }
                }

                // create feature and set geometry
                SimpleFeature newFeature = featureWriter.buildFeature();
                featureWriter.copyAttributes(feature, newFeature, true);
                featureWriter.write(newFeature);
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        return featureWriter.getFeatureSource();
    }

    private SimpleFeatureType buildShcema(SimpleFeatureType featureType,
            CoordinateReferenceSystem crs, String typeName) {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName(getName(typeName));
        typeBuilder.setCRS(crs);

        AttributeTypeBuilder attBuilder = new AttributeTypeBuilder();
        List<AttributeDescriptor> attDescs = featureType.getAttributeDescriptors();
        for (AttributeDescriptor descriptor : attDescs) {
            if (descriptor instanceof GeometryDescriptor) {
                final GeometryDescriptor geomDesc = (GeometryDescriptor) descriptor;
                final Class<?> geomBinding = geomDesc.getType().getBinding();
                typeBuilder.add(getName(geomDesc.getLocalName()), geomBinding, crs);
            } else {
                final String localName = getName(descriptor.getLocalName());
                if (localName.equalsIgnoreCase("shape_leng")
                        || localName.equalsIgnoreCase("shape_area")) {
                    continue;
                } else {
                    typeBuilder.add(attBuilder.buildDescriptor(localName, descriptor.getType()));
                }
            }
        }

        return typeBuilder.buildFeatureType();
    }

    private String getName(String srcName) {
        switch (namePolicy) {
        case NORMAL:
            return srcName;
        case UPPERCASE:
            return srcName.toUpperCase();
        case LOWERCASE:
            return srcName.toLowerCase();
        }
        return srcName;
    }

    private MathTransform getMathTransform(CoordinateReferenceSystem sourceCRS,
            CoordinateReferenceSystem targetCRS) {
        if (sourceCRS == null || targetCRS == null) {
            LOGGER.log(Level.WARNING,
                    "Input CoordinateReferenceSystem is Unknown Coordinate System!");
            return null;
        }

        if (CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
            LOGGER.log(Level.WARNING, "Input and Output Coordinate Reference Systems are equal!");
            return null;
        }

        MathTransform transform = null;
        try {
            transform = CRS.findMathTransform(sourceCRS, targetCRS, false);
        } catch (FactoryException e1) {
            LOGGER.log(Level.WARNING, e1.getMessage(), 1);
        }

        return transform;
    }
}
