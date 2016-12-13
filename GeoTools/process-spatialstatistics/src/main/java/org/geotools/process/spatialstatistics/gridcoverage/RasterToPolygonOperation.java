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
package org.geotools.process.spatialstatistics.gridcoverage;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptor;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.image.jai.Registry;
import org.geotools.process.ProcessException;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.jaitools.media.jai.vectorize.VectorizeDescriptor;
import org.jaitools.media.jai.vectorize.VectorizeRIF;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;

/**
 * Converts a raster dataset to polygon features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @reference org.geotools.process.raster.PolygonExtractionProcess
 * 
 * @source $URL$
 */
public class RasterToPolygonOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterToPolygonOperation.class);

    static {
        Registry.registerRIF(JAI.getDefaultInstance(), new VectorizeDescriptor(),
                new VectorizeRIF(), Registry.JAI_TOOLS_PRODUCT);
    }

    public SimpleFeatureCollection execute(GridCoverage2D inputGc, Integer bandIndex,
            boolean weeding, String valueField) throws ProcessException, IOException {
        return execute(inputGc, 0, weeding, valueField, true, null);
    }

    public SimpleFeatureCollection execute(GridCoverage2D inputGc, Integer bandIndex,
            boolean weeding, String valueField, boolean insideEdges, List<Number> noDataValues)
            throws ProcessException, IOException {

        valueField = valueField == null || valueField.isEmpty() ? "value" : valueField;

        // build nodata list
        List<Number> outsideValues = new ArrayList<Number>();
        if (noDataValues != null) {
            outsideValues.addAll(noDataValues);
        } else {
            outsideValues.add(RasterHelper.getNoDataValue(inputGc));
        }
        outsideValues.add(RasterHelper.getSuggestedNoDataValue(inputGc));

        double tolerance = 0;
        if (weeding) {
            // ArcGIS: The Douglas-Puecker algorithm for line generalization is used
            // with a tolerance of sqrt(0.5) * cell size.
            tolerance = Math.sqrt(0.5) * RasterHelper.getCellSize(inputGc);
        }

        // prepare feature type
        CoordinateReferenceSystem crs = inputGc.getCoordinateReferenceSystem();
        SimpleFeatureType featureType = FeatureTypes.getDefaultType("RasterToVector",
                Polygon.class, crs);

        RasterPixelType pixelType = RasterHelper.getTransferType(inputGc);
        switch (pixelType) {
        case FLOAT:
        case DOUBLE:
            featureType = FeatureTypes.add(featureType, valueField, Double.class, 38);
            break;
        default:
            featureType = FeatureTypes.add(featureType, valueField, Integer.class, 38);
            break;
        }
        featureType = FeatureTypes.add(featureType, "area", Double.class, 38);

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);
        try {
            GridGeometry2D gg2D = inputGc.getGridGeometry();
            AffineTransform mt = (AffineTransform) gg2D.getGridToCRS2D(PixelOrientation.UPPER_LEFT);

            AffineTransformation affineTrans = new AffineTransformation(mt.getScaleX(),
                    mt.getShearX(), mt.getTranslateX(), mt.getShearY(), mt.getScaleY(),
                    mt.getTranslateY());

            // perform jai operation
            Object o = JAI.getDefaultInstance().getOperationRegistry()
                    .getDescriptor(OperationDescriptor.class, "Vectorize");
            System.out.println(o);
            final ParameterBlockJAI pb = new ParameterBlockJAI("Vectorize");
            pb.setSource("source0", inputGc.getRenderedImage());
            pb.setParameter("band", Integer.valueOf(bandIndex));
            pb.setParameter("outsideValues", outsideValues);
            pb.setParameter("insideEdges", Boolean.valueOf(insideEdges));

            final RenderedOp dest = JAI.create("Vectorize", pb);
            @SuppressWarnings("unchecked")
            final Collection<Polygon> property = (Collection<Polygon>) dest
                    .getProperty(VectorizeDescriptor.VECTOR_PROPERTY_NAME);

            for (Polygon polygon : property) {
                if (polygon == null || polygon.isEmpty()) {
                    continue;
                }

                Double value = (Double) polygon.getUserData();
                polygon.setUserData(null);

                // filter coordinates in place
                polygon.apply(affineTrans);

                if (weeding) {
                    polygon = (Polygon) DouglasPeuckerSimplifier.simplify(polygon, tolerance);
                    if (polygon == null || polygon.isEmpty()) {
                        continue;
                    }
                }

                // create feature and set geometry
                SimpleFeature newFeature = featureWriter.buildFeature();
                newFeature.setDefaultGeometry(polygon);
                newFeature.setAttribute("area", polygon.getArea());

                switch (pixelType) {
                case FLOAT:
                case DOUBLE:
                    newFeature.setAttribute(valueField, value.doubleValue());
                    break;
                default:
                    newFeature.setAttribute(valueField, value.intValue());
                    break;
                }

                featureWriter.write(newFeature);
            }
        } catch (IllegalArgumentException iae) {
            featureWriter.rollback(iae);
        } catch (IOException e) {
            featureWriter.rollback(e);
        } catch (Exception e) {
            featureWriter.rollback(e);
        } catch (Throwable t) {
            t.printStackTrace();
            featureWriter.rollback();
        } finally {
            featureWriter.close(null);
        }

        return featureWriter.getFeatureCollection();
    }
}
