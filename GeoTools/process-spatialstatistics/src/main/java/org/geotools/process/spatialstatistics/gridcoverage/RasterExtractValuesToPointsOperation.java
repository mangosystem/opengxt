package org.geotools.process.spatialstatistics.gridcoverage;

import java.io.IOException;
import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.SlopeType;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Extracts the cell values of a raster based on a set of point features and records the values in the attribute table of an output feature class.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterExtractValuesToPointsOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging
            .getLogger(RasterExtractValuesToPointsOperation.class);

    public enum ExtractionType {
        Default, SlopeAsDegree, SlopeAsPercentrise, Aspect
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures,
            String valueField, GridCoverage2D surfaceRaster) throws IOException {
        return execute(inputFeatures, valueField, surfaceRaster, ExtractionType.Default);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures,
            String valueField, GridCoverage2D surfaceRaster, ExtractionType valueType)
            throws IOException {
        SimpleFeatureType inputSchema = inputFeatures.getSchema();

        // prepare feature type
        SimpleFeatureType featureType = FeatureTypes.build(inputSchema, getOutputTypeName());
        featureType = FeatureTypes.add(featureType, valueField, Double.class);

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        final double noData = RasterHelper.getNoDataValue(surfaceRaster);
        RasterFunctionalSurface surface = new RasterFunctionalSurface(surfaceRaster);

        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = inputFeatures.features();
            while (featureIter.hasNext()) {
                final SimpleFeature feature = featureIter.next();
                final Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }

                // do process
                double val = noData;
                switch (valueType) {
                case Default:
                    val = surface.getElevation(geometry.getCentroid());
                    break;
                case SlopeAsDegree:
                    val = surface.getSlope(geometry.getCentroid(), SlopeType.DEGREE);
                    break;
                case SlopeAsPercentrise:
                    val = surface.getSlope(geometry.getCentroid(), SlopeType.PERCENTRISE);
                    break;
                case Aspect:
                    val = surface.getAspect(geometry.getCentroid());
                    break;
                }

                // copy feature and set value
                SimpleFeature newFeature = featureWriter.buildFeature(null);
                featureWriter.copyAttributes(feature, newFeature, true);
                if (!SSUtils.compareDouble(val, noData)) {
                    newFeature.setAttribute(valueField, val);
                }

                featureWriter.write(newFeature);
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        return featureWriter.getFeatureCollection();
    }

}
