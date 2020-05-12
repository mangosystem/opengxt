package org.geotools.process.spatialstatistics;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.ejml.simple.SimpleMatrix;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.SpatialEvent;
import org.geotools.process.spatialstatistics.core.WeightMatrixBuilder;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.process.spatialstatistics.relationship.OLSOperation;
import org.geotools.process.spatialstatistics.relationship.OLSResult;
import org.geotools.process.spatialstatistics.storage.DataStoreFactory;
import org.geotools.process.spatialstatistics.storage.ShapeExportOperation;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;

public class MultiWindRoseTest {

    static final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    public static void main(String[] args) throws Exception {
            System.setProperty("org.geotools.referencing.forceXY", "true");

            DataStore ds = DataStoreFactory.getDataStore("postgis", "", 8432, "public", "", "postgres", "");
           
            SimpleFeatureCollection pumpsFc = ds.getFeatureSource("pumps").getFeatures();
            SimpleFeatureCollection choleraFc = ds.getFeatureSource("cholera_deaths").getFeatures();
            
            MultiWindRoseMapProcessFactory mwrf = new MultiWindRoseMapProcessFactory();
            MultiWindRoseMapProcess mwrp = new MultiWindRoseMapProcess(mwrf);
            
            List<SimpleFeatureCollection> inputs = new ArrayList<SimpleFeatureCollection>();
            inputs.add(choleraFc);
            inputs.add(choleraFc);
            inputs.add(choleraFc);
            
            Map<String, Object> param = new HashMap<String, Object>();
            param.put(mwrf.inputFeatures.key, inputs);
            param.put(mwrf.weightFields.key, "count,,count");
            param.put(mwrf.centerFeatures.key, pumpsFc);
            param.put(mwrf.searchRadius.key, 150.0);
            param.put(mwrf.roseCount.key, 36);
            
            Map<String, Object> result = mwrp.execute(param, new NullProgressListener());
            
            SimpleFeatureCollection resultFc = (SimpleFeatureCollection) result.get(mwrf.result.key);
            ShapeExportOperation seo = new ShapeExportOperation();
            seo.setOutputDataStore(DataStoreFactory.getShapefileDataStore("/Users/jyajya/work/Test/open_data"));
            seo.setOutputTypeName("mwr2");
            seo.execute(resultFc);
            
            ds.dispose();
    }



    public static void printFeatures(SimpleFeatureCollection features) {
            SimpleFeatureIterator featureIter = features.features();
            try {
                    while (featureIter.hasNext()) {
                            SimpleFeature feature = featureIter.next();
                            System.out.println(feature);
                    }
            } finally {
                    featureIter.close();
            }
    }


}
