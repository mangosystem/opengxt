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
package org.locationtech.udig.processingtoolbox.tools;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.WeightMatrixBuilder;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.tools.AmoebaParameter.SAAlgorithmType;
import org.locationtech.udig.processingtoolbox.tools.AmoebaParameter.SeedOption;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;

/**
 * Spatial AMOEBA(A Multidirectional Optimal Ecotope-Based Algorithm)
 * 
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @reference
 * 
 *            Aldstadt, J. and Getis, A., 2006, Using AMOEBA to create a spatial weights matrix and
 *            identify spatial clusters, Geographical Analysis, 38(4), 327-343.
 * 
 * @source $URL$
 */
public class SpatialAMOEBAOperation {
    protected static final Logger LOGGER = Logging.getLogger(SpatialAMOEBAOperation.class);

    final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    private ProgressListener monitor = null;

    public SpatialAMOEBAOperation(ProgressListener monitor) {
        this.monitor = monitor;
        if (monitor == null) {
            this.monitor = new NullProgressListener();
        }
    }

    private Map<Object, Double> getSeeds(AmoebaParameter param) {
        Map<Object, Double> map = new HashMap<Object, Double>();

        // TODO 절대값 사용 ?????????
        Filter filter = param.seedOption == SeedOption.All ? Filter.INCLUDE : param.seedFilter;
        SimpleFeatureIterator iter = param.output.subCollection(filter).features();
        try {
            Expression expression = ff.property(param.statField);
            while (iter.hasNext()) {
                SimpleFeature feature = iter.next();
                Double value = expression.evaluate(feature, Double.class);
                if (value != null) {
                    map.put(feature.getID(), value);
                }
            }
        } finally {
            iter.close();
        }

        // sort by value
        Comparator<Object> comp = new ValueComparator<Object, Double>(map, param.sortDescending);
        Map<Object, Double> seeds = new TreeMap<Object, Double>(comp);
        seeds.putAll(map);

        return seeds;
    }

    static final class ValueComparator<K, V extends Comparable<V>> implements Comparator<K> {
        private Map<K, V> map = new HashMap<K, V>();

        private boolean descending = true;

        public ValueComparator(Map<K, V> map, boolean descending) {
            this.descending = descending;
            this.map.putAll(map);
        }

        @Override
        public int compare(K key1, K key2) {
            int compare = map.get(key1).compareTo(map.get(key2));
            // should not merge keys with same value
            compare = compare == 0 ? 1 : compare;
            return descending ? -compare : compare;
        }
    }

    public SimpleFeatureCollection execute(AmoebaParameter param) {
        // 1. 클러스터 탐지 시작할 시드(피처 혹은 셀) 선택
        // 높은 값 순으로 모든 셀, 낮은 값 순으로 모든 셀, 사용자가 선택한 셀, 평균 이상인 셀, 표준점수(z-score)가 ** 이상인 셀 등
        Map<Object, Double> seeds = getSeeds(param);

        SimpleFeatureType schema = FeatureTypes.add(param.output.getSchema(), AmoebaWizard.ST,
                Double.class, 38);
        SimpleFeature feature = new SimpleFeatureBuilder(schema).buildFeature(null);

        WeightMatrixBuilder wm = param.matrix;
        Hashtable<Object, Double> neighbors = null;

        for (Iterator<Entry<Object, Double>> iter = seeds.entrySet().iterator(); iter.hasNext();) {
            Entry<Object, Double> entry = iter.next();
            System.out.println(entry.getKey() + " = " + entry.getValue());

            // 2. 제일 첫 시드부터 탐지 시작
            // 0 step:
            // - 이웃 없이 시드 하나만에 대해 지정된 통계량(예: Lee’s Si) 계산
            // - (이 값은 Gi* 의 경우 z-score , Si*의 경우 z-score의 제곱과 같음) 
            // - 설정된 기준을 충족하는가? 예라면 1 step으로 넘어감, 아니오라면 종료
            double zScore = entry.getValue();
            if (param.algorithm == SAAlgorithmType.LeesSiStar) {
                zScore = Math.pow(zScore, 2);
            }

            feature.setAttribute(AmoebaWizard.ST, zScore);
            if (!param.criteriaFilter.evaluate(feature)) {
                iter.remove();
                continue;
            }

            // 1 step: 시드의 이웃 추가 - 다양한 방식 가능
            // - 1) 이웃 중 통계량 기준을 충족하는 최대(최소) 값 하나
            // - 2) 이웃 중 통계량 기준을 충족하는 최대(최소) 영역
            // - - [시드+추가된 이웃]에 대해 통계량 계산 설정된 기준을 충족하는가?
            // - - 예라면 1 step의 처리영역을 저장 한 후 2 step으로 넘어감,
            // - - 아니오라면 0 step의 결과가 최종 클러스터 영역이 됨
            neighbors = wm.getWeightMatrix().getItems().get(entry.getKey());
            for (Entry<Object, Double> nb : neighbors.entrySet()) {

            }

            // 2 step: [1 step의 처리영역]에 대해 이웃 추가 
            // - [1 step의 영역+추가된 이웃]에 대해 통계량 계산 설정된 기준을 충족하는가?
            // - 예라면 2 step의 처리영역을 저장 한 후 3 step으로 넘어감, 아니오라면 1 step의 결과가 최종 클러스터 영역이 됨
            // - 더 이상 충족하는 이웃이 없을때까지 위 과정을 반복하여 스텝증가

            // 3. 다음 시드에 대해 클러스터 탐지 시작하여 모든 해당 시드에 대해서 처리
            iter.remove();
            System.out.println(seeds.size());
        }

        // 4. 탐지된 클러스터들 정리
        // - 1개 피처로 구성된 클러스터 제거(옵션)
        // - 클러스터 영역 폴리곤 추출 및 지도화
        // - 클러스터 영역 폴리곤에 대한 통계 요약: 피쳐(셀) 수, 변수값들의 평균, Si* 통계량 …

        return param.output;
    }
}
