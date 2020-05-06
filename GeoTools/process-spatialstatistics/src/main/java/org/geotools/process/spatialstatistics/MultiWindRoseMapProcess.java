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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.util.NullProgressListener;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.operations.MultiWindRoseOperation;
import org.geotools.process.spatialstatistics.storage.NamePolicy;
import org.geotools.process.spatialstatistics.transformation.WindroseAnchorFeatureCollection;
import org.geotools.process.spatialstatistics.transformation.WindroseFeatureCollection;
import org.geotools.text.Text;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.filter.FilterFactory2;
import org.opengis.util.ProgressListener;

/**
 * Creates a wind roses map from features.
 * 
 * @author jyajya, MangoSystem
 * 
 * @source $URL$
 */
public class MultiWindRoseMapProcess extends AbstractStatisticsProcess {
	protected static final Logger LOGGER = Logging.getLogger(MultiWindRoseMapProcess.class);

	private boolean started = false;

	public MultiWindRoseMapProcess(ProcessFactory factory) {
		super(factory);
	}

	public ProcessFactory getFactory() {
		return factory;
	}

	public static SimpleFeatureCollection process(Collection<SimpleFeatureCollection> inputFeatures, String weightFields,
			SimpleFeatureCollection centerFeatures, Geometry centerPoint, Double searchRadius, String valueField, int roseCnt,
			ProgressListener monitor) {
//        SimpleFeatureCollection inputFeatures, String weightField,
//        SimpleFeatureCollection centerFeatures, double searchRadius, String valueField, int roseCnt
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(MultiWindRoseMapProcessFactory.inputFeatures.key, inputFeatures);
		map.put(MultiWindRoseMapProcessFactory.weightFields.key, weightFields);
//        map.put(VA_WindRoseFactory.inputFeatures2.key, inputFeatures[1]);
//        map.put(VA_WindRoseFactory.weightField2.key, weightField[1]);
//        map.put(VA_WindRoseFactory.inputFeatures3.key, inputFeatures[2]);
//        map.put(VA_WindRoseFactory.weightField3.key, weightField[2]);
		map.put(MultiWindRoseMapProcessFactory.centerFeatures.key, centerFeatures);
		map.put(MultiWindRoseMapProcessFactory.centerPoint.key, centerPoint);
		map.put(MultiWindRoseMapProcessFactory.searchRadius.key, searchRadius);
		map.put(MultiWindRoseMapProcessFactory.roseCount.key, roseCnt);

		// parameterInfo.put(inputFeatures.key, inputFeatures);
		// parameterInfo.put(weightField.key, weightField);
		// parameterInfo.put(valueField.key, valueField);
		// parameterInfo.put(roseCount.key, roseCount);

		Process process = new MultiWindRoseMapProcess(null);
		Map<String, Object> resultMap;
		try {
			resultMap = process.execute(map, monitor);

			return (SimpleFeatureCollection) resultMap.get(MultiWindRoseMapProcessFactory.mult_rose.key);
		} catch (ProcessException e) {
			e.printStackTrace();
			LOGGER.log(Level.FINER, e.getMessage(), e);
		}

		return null;
	}

	@Override
	public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor) throws ProcessException {
		if (started)
			throw new IllegalStateException("Process can only be run once");
		started = true;

		if (monitor == null)
			monitor = new NullProgressListener();
		try {
			monitor.started();
			monitor.setTask(Text.text("Grabbing arguments"));
			monitor.progress(10.0f);

			monitor.setTask(Text.text("Processing " + this.getClass().getSimpleName()));
			monitor.progress(25.0f);

			if (monitor.isCanceled()) {
				return null; // user has canceled this operation
			}

		Collection<SimpleFeatureCollection> inputFeatures = (Collection<SimpleFeatureCollection>) Params.getValue(input,
					MultiWindRoseMapProcessFactory.inputFeatures, null);

			String weightFiels = (String) Params.getValue(input, MultiWindRoseMapProcessFactory.weightFields, null);

//            SimpleFeatureCollection inputFeatures2 = (SimpleFeatureCollection) ParamUtil.getParam(
//                    input, MultiWindRoseMapProcessFactory.inputFeatures2, null);
//
//            String weightField2 = (String) ParamUtil.getParam(input, VA_WindRoseFactory.weightField2,
//                    null);
//            SimpleFeatureCollection inputFeatures3 = (SimpleFeatureCollection) ParamUtil.getParam(
//                    input, MultiWindRoseMapProcessFactory.inputFeatures3, null);
//
//            String weightField3 = (String) ParamUtil.getParam(input, VA_WindRoseFactory.weightField3,
//                    null);
			SimpleFeatureCollection centerFeatures = (SimpleFeatureCollection) Params.getValue(input,
					MultiWindRoseMapProcessFactory.centerFeatures, null);
			Geometry centerPoint = (Geometry) Params.getValue(input,
					MultiWindRoseMapProcessFactory.centerPoint, null);
			Double searchRadius = (Double) Params.getValue(input, MultiWindRoseMapProcessFactory.searchRadius, null);
			Integer roseCount = (Integer) Params.getValue(input, MultiWindRoseMapProcessFactory.roseCount,
					Integer.valueOf(36));

			// start process
			SimpleFeatureCollection resultFc = null;
			SimpleFeatureCollection anchorFc = null;
			try {
				SimpleFeatureSource sfc = null;
				Collection<SimpleFeatureCollection> tgInputFeatures = inputFeatures;
				String[] tgWeightField = weightFiels.split(",");
				MultiWindRoseOperation process = new MultiWindRoseOperation();
				if(centerFeatures.size() > 0) {
					sfc = process.execute(tgInputFeatures, tgWeightField, centerFeatures, searchRadius, roseCount);
				} else {
					sfc = process.execute(tgInputFeatures, tgWeightField, centerPoint, searchRadius, roseCount);
				}

				resultFc = sfc == null ? null : sfc.getFeatures();
				anchorFc = process.getAnchor().getFeatures();
			} catch (Exception ee) {
				ee.printStackTrace();
				monitor.exceptionOccurred(ee);
			}
			// end process

			monitor.setTask(Text.text("Encoding result"));
			monitor.progress(90.0f);

			Map<String, Object> resultMap = new HashMap<String, Object>();
			resultMap.put(MultiWindRoseMapProcessFactory.mult_rose.key, resultFc);
			resultMap.put(MultiWindRoseMapProcessFactory.mult_rose_anchor.key, anchorFc);
			// resultMap.put(VA_RingMapFactory.ring_anchor.key, anchorFc);
			monitor.complete(); // same as 100.0f

			return resultMap;
		} catch (Exception eek) {
			eek.printStackTrace();
			monitor.exceptionOccurred(eek);
			return null;
		} finally {
			monitor.dispose();
		}
	}

}
